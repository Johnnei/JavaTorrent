package org.johnnei.javatorrent.torrent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.fileset.FileEntry;
import org.johnnei.javatorrent.utils.StringUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

		assertEquals("Should have had a single file info", 1, cut.getFileEntries().size());
		assertEquals("Piece size has not been correctly copied from the metadata", 262144, cut.getPieceSize());

		FileEntry fileInfo = cut.getFileEntries().get(0);
		assertEquals("Filename should have been gimp-2.8.16-setup-1.exe", "gimp-2.8.16-setup-1.exe", fileInfo.getFileName());
		assertEquals("Filesize should have been 743440384", 96823808L, fileInfo.getSize());
		assertEquals("First byte offset should have been 0", 0L, fileInfo.getFirstByteOffset());

		try (BufferedReader inputStream = new BufferedReader(new InputStreamReader(new FileInputStream(
				new File(TorrentFileSetTest.class.getResource(SINGLE_FILE_TORRENT + ".hashes").toURI()))))) {
			int index = 0;
			for (byte[] pieceHash: cut.getPieceHashes()) {
				String expectedPieceHash = inputStream.readLine();
				assertEquals(
						String.format("Piece %d hash should have matched the one in the .hashes file", index),
						expectedPieceHash,
						StringUtils.byteArrayToString(pieceHash));
				index++;
			}
		}
	}

	@Test
	public void testMultiFileTorrentFile() throws Exception {
		Metadata cut = getMultiFileTorrent();

		assertEquals("Should have had a single file info", 6, cut.getFileEntries().size());

		final String[] fileNames = {
				"apache_2.2.9-win32-x86-openssl-0.9.8h-r2.msi", "HowToInstallGuide.txt", "httpd.conf",
				"mysql-essential-5.0.51b-win32.msi", "npp.5.6.8.Installer.exe", "php-5.3.2-src.zip"
		};
		final long[] fileSizes = { 5414400, 547, 18124, 23816192, 3336170, 19823435 };
		final long[] firstByteOffsets = { 0, 5414400, 5414947, 5433071, 29249263, 32585433 };

		for (int i = 0; i < cut.getFileEntries().size(); i++) {
			FileEntry fileInfo = cut.getFileEntries().get(i);
			assertEquals(String.format("Incorrect filename for entry %d", i), fileNames[i], fileInfo.getFileName());
			assertEquals(String.format("Incorrect file size for entry %d", i), fileSizes[i], fileInfo.getSize());
			assertEquals(String.format("Incorrect first byte offset for entry %d", i), firstByteOffsets[i], fileInfo.getFirstByteOffset());
		}

		try (BufferedReader inputStream = new BufferedReader(new InputStreamReader(new FileInputStream(
				new File(TorrentFileSetTest.class.getResource(MULTI_FILE_TORRENT + ".hashes").toURI()))))) {
			int index = 0;
			for (byte[] pieceHash: cut.getPieceHashes()) {
				String expectedPieceHash = inputStream.readLine();
				assertEquals(
						String.format("Piece %d hash should have matched the one in the .hashes file", index),
						expectedPieceHash,
						StringUtils.byteArrayToString(pieceHash));
				index++;
			}
		}
	}


	@Test
	public void testGetHash() {
		Metadata cut = new Metadata.Builder()
				.setHash(DummyEntity.createRandomBytes(20))
				.build();

		assertTrue("Incorrect hash output", cut.getHashString().matches("^[a-fA-F0-9]{40}$"));
	}

}