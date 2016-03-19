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

	private static final String MULTI_FILE_TORRENT = "apache.php.and.mysql.windows.torrent";

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

		assertEquals("Should have had a single file info", 30, cut.getFiles().size());

		FileInfo fileInfo = cut.getFiles().get(0);
		/*assertEquals("Filename should have been archlinux-2016.03.01-dual.iso", "archlinux-2016.03.01-dual.iso", fileInfo.getFilename());
		assertEquals("Filesize should have been 743440384", 743440384L, fileInfo.getSize());
		assertEquals("Piece count should have been 1418", 1418, fileInfo.getPieceCount());
		assertEquals("First byte offset should have been 0", 0L, fileInfo.getFirstByteOffset());*/

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