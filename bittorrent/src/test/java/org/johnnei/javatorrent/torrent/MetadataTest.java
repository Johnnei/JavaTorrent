package org.johnnei.javatorrent.torrent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.bittorrent.encoding.BencodedInteger;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedString;
import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestUtils;
import org.johnnei.javatorrent.torrent.fileset.FileEntry;
import org.johnnei.javatorrent.utils.StringUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link Metadata}
 */
public class MetadataTest {

	private static final String SINGLE_FILE_TORRENT = "gimp-2.8.16-setup-1.exe.torrent";

	private static final String MULTI_FILE_TORRENT = "my.sql.apache.2.2.php.notepad.torrent";

	private Metadata getMultiFileTorrent() throws Exception {
		File file = new File(TorrentFileSetTest.class.getResource(MULTI_FILE_TORRENT).toURI());
		return new Metadata.Builder().readFromFile(file).build();
	}

	private Metadata getSingleFileTorrent() throws Exception {
		File file = new File(TorrentFileSetTest.class.getResource(SINGLE_FILE_TORRENT).toURI());
		return new Metadata.Builder().readFromFile(file).build();
	}

	@Test
	public void testSingleFileTorrentFile() throws Exception {
		Metadata cut = getSingleFileTorrent();

		assertEquals(1, cut.getFileEntries().size(), "Should have had a single file info");
		assertEquals(262144, cut.getPieceSize(), "Piece size has not been correctly copied from the metadata");

		FileEntry fileInfo = cut.getFileEntries().get(0);
		assertEquals("gimp-2.8.16-setup-1.exe", fileInfo.getFileName(), "Filename should have been gimp-2.8.16-setup-1.exe");
		assertEquals(96823808L, fileInfo.getSize(), "Filesize should have been 743440384");
		assertEquals(0L, fileInfo.getFirstByteOffset(), "First byte offset should have been 0");

		try (BufferedReader inputStream = new BufferedReader(new InputStreamReader(new FileInputStream(
			new File(TorrentFileSetTest.class.getResource(SINGLE_FILE_TORRENT + ".hashes").toURI()))))) {
			int index = 0;
			for (byte[] pieceHash : cut.getPieceHashes()) {
				String expectedPieceHash = inputStream.readLine();
				assertEquals(
					expectedPieceHash,
					StringUtils.byteArrayToString(pieceHash),
					String.format("Piece %d hash should have matched the one in the .hashes file", index)
				);
				index++;
			}
		}

		assertEquals("gimp-2.8.16-setup-1.exe", cut.getName(), "Incorrect name");
	}

	@Test
	public void testMultiFileTorrentFile() throws Exception {
		Metadata cut = getMultiFileTorrent();

		assertEquals(6, cut.getFileEntries().size(), "Should have had a single file info");

		final String[] fileNames = {
			"apache_2.2.9-win32-x86-openssl-0.9.8h-r2.msi", "HowToInstallGuide.txt", "httpd.conf",
			"mysql-essential-5.0.51b-win32.msi", "npp.5.6.8.Installer.exe", "php-5.3.2-src.zip"
		};
		final long[] fileSizes = { 5414400, 547, 18124, 23816192, 3336170, 19823435 };
		final long[] firstByteOffsets = { 0, 5414400, 5414947, 5433071, 29249263, 32585433 };

		for (int index = 0; index < cut.getFileEntries().size(); index++) {
			final int i = index;
			FileEntry fileInfo = cut.getFileEntries().get(i);
			assertAll(
				() -> assertEquals(fileNames[i], fileInfo.getFileName(), String.format("Incorrect filename for entry %d", i)),
				() -> assertEquals(fileSizes[i], fileInfo.getSize(), String.format("Incorrect file size for entry %d", i)),
				() -> assertEquals(firstByteOffsets[i], fileInfo.getFirstByteOffset(), String.format("Incorrect first byte offset for entry %d", i))
			);
		}

		try (BufferedReader inputStream = new BufferedReader(new InputStreamReader(new FileInputStream(
			new File(TorrentFileSetTest.class.getResource(MULTI_FILE_TORRENT + ".hashes").toURI()))))) {
			int index = 0;
			for (byte[] pieceHash : cut.getPieceHashes()) {
				String expectedPieceHash = inputStream.readLine();
				assertEquals(
					expectedPieceHash,
					StringUtils.byteArrayToString(pieceHash),
					String.format("Piece %d hash should have matched the one in the .hashes file", index)
				);
				index++;
			}
		}
	}

	@Test
	public void testGetHashString() {
		Metadata cut = new Metadata.Builder()
			.setHash(DummyEntity.createUniqueTorrentHash())
			.build();

		assertTrue(cut.getHashString().matches("^[a-fA-F0-9]{40}$"), "Incorrect hash output");
	}

	@Test
	public void testSetFileSet() {
		AbstractFileSet fileSetMock = mock(AbstractFileSet.class);

		Metadata cut = new Metadata.Builder()
			.setHash(DummyEntity.createUniqueTorrentHash())
			.build();

		TestUtils.assertNotPresent("Fileset should not have been set before the set call", cut.getFileSet());

		cut.setFileSet(fileSetMock);
		assertEquals(fileSetMock, assertPresent("Fileset was set.", cut.getFileSet()), "Incorrect fileset has been returned after set.");
	}

	@Test
	public void testGetHash() {
		byte[] hash = DummyEntity.createUniqueTorrentHash();

		Metadata cut = new Metadata.Builder()
			.setHash(hash)
			.build();

		assertArrayEquals(hash, cut.getHash(), "Incorrect hash has been returned");
		assertNotEquals(hash, cut.getHash(), "Returned array should be a clone to prevent modification of the hash");
	}

	@Test
	public void testGetNameWithoutNameEntry() {
		byte[] hash = new byte[]{
			0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10,
			0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10
		};

		Metadata cut = new Metadata.Builder()
			.setHash(hash)
			.build();

		assertEquals("magnet(1010101010101010101010101010101010101010)", cut.getName(), "Incorrect name format for hash-only name");
	}

	@Test
	public void testEquality() {
		byte[] arrayOne = DummyEntity.createRandomBytes(20);
		byte[] arrayTwo = DummyEntity.createRandomBytes(20);

		Metadata base = new Metadata.Builder().setHash(arrayOne).build();
		Metadata equalToBase = new Metadata.Builder().setHash(Arrays.copyOf(arrayOne, 20)).build();
		Metadata notEqualToBase = new Metadata.Builder().setHash(arrayTwo).build();

		TestUtils.assertEqualityMethods(base, equalToBase, notEqualToBase);
	}

	@Test
	public void testInitializeMetadataIncorrectHashOfBuffer() {
		BencodedMap metadataMap = new BencodedMap();
		metadataMap.put("length", new BencodedString("dummy"));
		metadataMap.put("pieces", new BencodedString("dummy"));
		metadataMap.put("piece length", new BencodedInteger(42));

		byte[] buffer = metadataMap.serialize();

		Metadata cut = new Metadata.Builder()
			.setHash(DummyEntity.createUniqueTorrentHash())
			.build();

		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.initializeMetadata(buffer));
		assertThat(e.getMessage(), containsString("hash"));
	}

	@Test
	public void testInitializeMetadataMissingPieceLengthInInfoEntry() {
		BencodedMap metadataMap = new BencodedMap();
		BencodedMap infoMap = new BencodedMap();
		infoMap.put("length", new BencodedString("dummy"));
		infoMap.put("pieces", new BencodedString("dummy"));
		metadataMap.put("info", infoMap);

		byte[] buffer = metadataMap.serialize();
		byte[] hash = SHA1.hash(buffer);

		Metadata cut = new Metadata.Builder()
			.setHash(hash)
			.build();

		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.initializeMetadata(buffer));
		assertThat(e.getMessage(), containsString("missing"));
	}

	@Test
	public void testInitializeMetadataMissingPieceLength() {
		BencodedMap metadataMap = new BencodedMap();
		metadataMap.put("length", new BencodedString("dummy"));
		metadataMap.put("pieces", new BencodedString("dummy"));

		byte[] buffer = metadataMap.serialize();
		byte[] hash = SHA1.hash(buffer);

		Metadata cut = new Metadata.Builder()
			.setHash(hash)
			.build();

		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.initializeMetadata(buffer));
		assertThat(e.getMessage(), containsString("missing"));
	}

	@Test
	public void testInitializeMetadataMissingPiecesEntry() {
		BencodedMap metadataMap = new BencodedMap();
		metadataMap.put("length", new BencodedString("dummy"));
		metadataMap.put("piece length", new BencodedString("dummy"));

		byte[] buffer = metadataMap.serialize();
		byte[] hash = SHA1.hash(buffer);

		Metadata cut = new Metadata.Builder()
			.setHash(hash)
			.build();

		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.initializeMetadata(buffer));
		assertThat(e.getMessage(), containsString("missing"));
	}

	@Test
	public void testSetFileSetErrorOnSecondCall() {
		AbstractFileSet fileSetMock = mock(AbstractFileSet.class);
		Metadata cut = new Metadata.Builder()
			.setHash(DummyEntity.createUniqueTorrentHash())
			.build();

		cut.setFileSet(fileSetMock);
		Exception e = assertThrows(IllegalStateException.class, () -> cut.setFileSet(fileSetMock));
		assertThat(e.getMessage(), containsString("fileset"));
	}

	@Test
	public void testInitializeMetadataErrorOnSecondCall() {
		BencodedMap metadataMap = new BencodedMap();
		metadataMap.put("name", new BencodedString("name"));
		metadataMap.put("length", new BencodedInteger(0));
		metadataMap.put("pieces", new BencodedString(""));
		metadataMap.put("piece length", new BencodedInteger(0));

		byte[] buffer = metadataMap.serialize();
		byte[] hash = SHA1.hash(buffer);

		Metadata cut = new Metadata.Builder()
			.setHash(hash)
			.build();

		cut.initializeMetadata(buffer);
		Exception e = assertThrows(IllegalStateException.class, () -> cut.initializeMetadata(buffer));
		assertThat(e.getMessage(), containsString("metadata"));
	}

}
