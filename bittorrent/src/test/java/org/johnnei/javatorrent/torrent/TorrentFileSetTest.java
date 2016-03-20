package org.johnnei.javatorrent.torrent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.utils.StringUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link TorrentFileSet}
 */
public class TorrentFileSetTest {

	private static final String SINGLE_FILE_TORRENT = "gimp-2.8.16-setup-1.exe.torrent";

	private static final String MULTI_FILE_TORRENT = "my.sql.apache.2.2.php.notepad.torrent";

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testSingleFileTorrentFile() throws Exception {
		File file = new File(TorrentFileSetTest.class.getResource(SINGLE_FILE_TORRENT).toURI());

		TorrentFileSet cut = new TorrentFileSet(file, temporaryFolder.newFolder());

		assertEquals("Should have had a single file info", 1, cut.getFiles().size());

		FileInfo fileInfo = cut.getFiles().get(0);
		assertEquals("Filename should have been gimp-2.8.16-setup-1.exe", "gimp-2.8.16-setup-1.exe", fileInfo.getFilename());
		assertEquals("Filesize should have been 743440384", 96823808L, fileInfo.getSize());
		assertEquals("Piece count should have been 1418", 370, fileInfo.getPieceCount());
		assertEquals("First byte offset should have been 0", 0L, fileInfo.getFirstByteOffset());

		try (BufferedReader inputStream = new BufferedReader(new InputStreamReader(new FileInputStream(
				new File(TorrentFileSetTest.class.getResource(SINGLE_FILE_TORRENT + ".hashes").toURI()))))) {
			for (Piece piece : cut.pieces) {
				String pieceHash = inputStream.readLine();
				byte[] pieceHashBytes = Whitebox.getInternalState(piece, "expectedHash");
				assertEquals(
						String.format("Piece %d hash should have matched the one in the .hashes file", piece.getIndex()),
						pieceHash,
						StringUtils.byteArrayToString(pieceHashBytes));
			}
		}
	}

	@Test
	public void testMultiFileTorrentFile() throws Exception {
		File file = new File(TorrentFileSetTest.class.getResource(MULTI_FILE_TORRENT).toURI());

		TorrentFileSet cut = new TorrentFileSet(file, temporaryFolder.newFolder());

		assertEquals("Should have had a single file info", 6, cut.getFiles().size());

		final String[] fileNames = {
				"apache_2.2.9-win32-x86-openssl-0.9.8h-r2.msi", "HowToInstallGuide.txt", "httpd.conf",
				"mysql-essential-5.0.51b-win32.msi", "npp.5.6.8.Installer.exe", "php-5.3.2-src.zip"
		};
		final long[] fileSizes = { 5414400, 547, 18124, 23816192, 3336170, 19823435 };
		final int[] pieceCounts = { 83, 1, 1, 365, 52, 304 };
		final long[] firstByteOffsets = { 0, 5414400, 5414947, 5433071, 29249263, 32585433 };

		for (int i = 0; i < cut.getFiles().size(); i++) {
			FileInfo fileInfo = cut.getFiles().get(i);
			assertEquals(String.format("Incorrect filename for entry %d", i), fileNames[i], fileInfo.getFilename());
			assertEquals(String.format("Incorrect file size for entry %d", i), fileSizes[i], fileInfo.getSize());
			assertEquals(String.format("Incorrect piece count for entry %d", i), pieceCounts[i], fileInfo.getPieceCount());
			assertEquals(String.format("Incorrect first byte offset for entry %d", i), firstByteOffsets[i], fileInfo.getFirstByteOffset());
		}

		try (BufferedReader inputStream = new BufferedReader(new InputStreamReader(new FileInputStream(
				new File(TorrentFileSetTest.class.getResource(MULTI_FILE_TORRENT + ".hashes").toURI()))))) {
			for (Piece piece : cut.pieces) {
				String pieceHash = inputStream.readLine();
				byte[] pieceHashBytes = Whitebox.getInternalState(piece, "expectedHash");
				assertEquals(
						String.format("Piece %d hash should have matched the one in the .hashes file", piece.getIndex()),
						pieceHash,
						StringUtils.byteArrayToString(pieceHashBytes));
			}
		}
	}
}