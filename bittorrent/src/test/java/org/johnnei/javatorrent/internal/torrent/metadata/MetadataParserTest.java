package org.johnnei.javatorrent.internal.torrent.metadata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.bittorrent.encoding.BencodedInteger;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedList;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedString;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.TorrentFileSetTest;
import org.johnnei.javatorrent.torrent.fileset.FileEntry;
import org.johnnei.javatorrent.utils.StringUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

class MetadataParserTest {

	private static final byte[] EMPTY_HASH = new byte[20];

	private static final BencodedString PIECE_HASH = new BencodedString(EMPTY_HASH);

	private static final String SINGLE_FILE_TORRENT = "/org/johnnei/javatorrent/torrent/gimp-2.8.16-setup-1.exe.torrent";

	private static final String MULTI_FILE_TORRENT = "/org/johnnei/javatorrent/torrent/my.sql.apache.2.2.php.notepad.torrent";

	@Test
	@DisplayName("Parse - Single file structured metadata file")
	public void testParseSingleFileTorrent() throws Exception {
		Metadata cut = Metadata.Builder.from(
			Path.of(MetadataParserTest.class.getResource(SINGLE_FILE_TORRENT).toURI())
		).build();


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
	@DisplayName("Parse - Multi file structured metadata file")
	public void testParseMultiFileTorrent() throws Exception {
		Metadata cut = Metadata.Builder.from(
			Path.of(MetadataParserTest.class.getResource(MULTI_FILE_TORRENT).toURI())
		).build();

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
	@DisplayName("Throws on root bencoded value is not dictionary")
	public void testFailToParseIncorrectRootType() {
		Metadata.Builder builder = new Metadata.Builder(EMPTY_HASH);
		assertThrows(IllegalArgumentException.class, () -> MetadataParser.readMetadata(builder, new BencodedList()));
	}

	@Test
	@DisplayName("Throws on info entry missing piece length entry")
	public void testFailToParseMissingPieceLength() {
		Metadata.Builder builder = new Metadata.Builder(EMPTY_HASH);

		BencodedMap info = new BencodedMap();
		info.put("length", new BencodedInteger(42));
		info.put("files", new BencodedList());

		BencodedMap metadata = new BencodedMap();
		metadata.put("info", info);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MetadataParser.readMetadata(builder, metadata));
		assertThat(exception.getMessage(), containsString("Piece length"));
	}

	@Test
	@DisplayName("Throws on info entry invalid type piece length entry")
	public void testFailToParseInvalidTypePieceLength() {
		Metadata.Builder builder = new Metadata.Builder(EMPTY_HASH);

		BencodedMap info = new BencodedMap();
		info.put("length", new BencodedInteger(42));
		info.put("piece length", new BencodedString("42"));

		BencodedMap metadata = new BencodedMap();
		metadata.put("info", info);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MetadataParser.readMetadata(builder, metadata));
		assertThat(exception.getMessage(), containsString("Piece length"));
	}

	@Test
	@DisplayName("Throws on info entry invalid piece length entry (<= 0)")
	public void testFailToParseInvalidPieceLength() {
		Metadata.Builder builder = new Metadata.Builder(EMPTY_HASH);

		BencodedMap info = new BencodedMap();
		info.put("length", new BencodedInteger(42));
		info.put("piece length", new BencodedInteger(0));

		BencodedMap metadata = new BencodedMap();
		metadata.put("info", info);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MetadataParser.readMetadata(builder, metadata));
		assertThat(exception.getMessage(), containsString("> 0"));
	}

	@Test
	@DisplayName("Throws on info entry invalid pieces entry")
	public void testFailToParseInvalidTypePieces() {
		Metadata.Builder builder = new Metadata.Builder(EMPTY_HASH);

		BencodedMap info = new BencodedMap();
		info.put("length", new BencodedInteger(42));
		info.put("piece length", new BencodedInteger(42));
		info.put("pieces", new BencodedMap());

		BencodedMap metadata = new BencodedMap();
		metadata.put("info", info);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MetadataParser.readMetadata(builder, metadata));
		assertThat(exception.getMessage(), containsString("\"pieces\""));
	}

	@Test
	@DisplayName("Throws on info entry invalid length pieces entry")
	public void testFailToParseInvalidLengthPieces() {
		Metadata.Builder builder = new Metadata.Builder(EMPTY_HASH);

		BencodedMap info = new BencodedMap();
		info.put("length", new BencodedInteger(42));
		info.put("piece length", new BencodedInteger(42));
		info.put("pieces", new BencodedString(new byte[30]));

		BencodedMap metadata = new BencodedMap();
		metadata.put("info", info);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MetadataParser.readMetadata(builder, metadata));
		assertThat(exception.getMessage(), containsString("\"pieces\""));
	}

	@Test
	@DisplayName("Throws on info entry missing name entry")
	public void testFailToParseSingleFileMissingName() {
		Metadata.Builder builder = new Metadata.Builder(EMPTY_HASH);

		BencodedMap info = new BencodedMap();
		info.put("length", new BencodedInteger(42));
		info.put("piece length", new BencodedInteger(42));
		info.put("pieces", PIECE_HASH);

		BencodedMap metadata = new BencodedMap();
		metadata.put("info", info);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MetadataParser.readMetadata(builder, metadata));
		assertThat(exception.getMessage(), containsString("\"name\""));
	}

	@Test
	@DisplayName("Throws on info entry invalid name entry")
	public void testFailToParseSingleFileInvalidTypeName() {
		Metadata.Builder builder = new Metadata.Builder(EMPTY_HASH);

		BencodedMap info = new BencodedMap();
		info.put("length", new BencodedInteger(42));
		info.put("piece length", new BencodedInteger(42));
		info.put("pieces", PIECE_HASH);
		info.put("name", new BencodedInteger(42));

		BencodedMap metadata = new BencodedMap();
		metadata.put("info", info);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MetadataParser.readMetadata(builder, metadata));
		assertThat(exception.getMessage(), containsString("\"name\""));
	}

	@Test
	@DisplayName("Throws on info entry containing both length entry and files entry")
	public void testFailToParseBothSingleAndMultiFileStructure() {
		Metadata.Builder builder = new Metadata.Builder(EMPTY_HASH);

		BencodedMap info = new BencodedMap();
		info.put("piece length", new BencodedInteger(42));
		info.put("pieces", PIECE_HASH);

		info.put("length", new BencodedInteger(42));

		info.put("files", new BencodedList());

		BencodedMap metadata = new BencodedMap();
		metadata.put("info", info);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MetadataParser.readMetadata(builder, metadata));
		assertThat(exception.getMessage(), containsString("Conflict"));
	}

	@Test
	@DisplayName("Throws on files entry containing entry with empty path list")
	public void testFailToParseFilesEntryContainingEmptyPath() {
		Metadata.Builder builder = new Metadata.Builder(EMPTY_HASH);

		BencodedMap info = new BencodedMap();
		info.put("piece length", new BencodedInteger(42));
		info.put("pieces", PIECE_HASH);

		BencodedList files = new BencodedList();
		BencodedMap fileEntry = new BencodedMap();
		fileEntry.put("path", new BencodedList());
		fileEntry.put("length", new BencodedInteger(42));
		files.add(fileEntry);

		info.put("files", files);

		BencodedMap metadata = new BencodedMap();
		metadata.put("info", info);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MetadataParser.readMetadata(builder, metadata));
		assertThat(exception.getMessage(), containsString("valid path"));
	}

	@Test
	@DisplayName("Throws on files entry containing entry with path non string only list")
	public void testFailToParseFilesEntryContainingNonStringPathElement() {
		Metadata.Builder builder = new Metadata.Builder(EMPTY_HASH);

		BencodedMap info = new BencodedMap();
		info.put("piece length", new BencodedInteger(42));
		info.put("pieces", PIECE_HASH);

		BencodedList files = new BencodedList();
		BencodedMap fileEntry = new BencodedMap();
		BencodedList path = new BencodedList();
		path.add(new BencodedString("folder"));
		path.add(new BencodedInteger(42));
		path.add(new BencodedString("subfolder"));

		fileEntry.put("path", path);
		fileEntry.put("length", new BencodedInteger(42));
		files.add(fileEntry);

		info.put("files", files);

		BencodedMap metadata = new BencodedMap();
		metadata.put("info", info);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MetadataParser.readMetadata(builder, metadata));
		assertThat(exception.getMessage(), containsString("non string element"));
	}

	@Test
	@DisplayName("Throws on files entry containing entry with non int file length")
	public void testFailToParseFilesEntryContainingNonIntLength() {
		Metadata.Builder builder = new Metadata.Builder(EMPTY_HASH);

		BencodedMap info = new BencodedMap();
		info.put("piece length", new BencodedInteger(42));
		info.put("pieces", PIECE_HASH);

		BencodedList files = new BencodedList();
		BencodedMap fileEntry = new BencodedMap();
		BencodedList path = new BencodedList();
		path.add(new BencodedString("file.txt"));

		fileEntry.put("path", path);
		fileEntry.put("length", new BencodedString("42"));
		files.add(fileEntry);

		info.put("files", files);

		BencodedMap metadata = new BencodedMap();
		metadata.put("info", info);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> MetadataParser.readMetadata(builder, metadata));
		assertThat(exception.getMessage(), containsString("\"length\""));
	}

}
