package org.johnnei.javatorrent.torrent;

import java.io.File;

import org.johnnei.javatorrent.test.DummyEntity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link MetadataFileSet}
 */
public class MetadataFileSetTest {

	private static final String SINGLE_FILE_TORRENT = "gimp-2.8.16-setup-1.exe.torrent";

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testConstructorNullTorrent() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Torrent");

		new MetadataFileSet(null, temporaryFolder.newFile());
	}

	@Test
	public void testConstructorNullMetadata() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Metadata");

		new MetadataFileSet(DummyEntity.createUniqueTorrent(), null);
	}

	@Test
	public void testConstructorNonExistingMetadata() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Metadata");

		new MetadataFileSet(DummyEntity.createUniqueTorrent(), new File("dfasfsdgafhjkdsafdjhask.torrent"));
	}

	@Test
	public void testConstructor() throws Exception {
		File torrentFile = new File(TorrentFileSetTest.class.getResource(SINGLE_FILE_TORRENT).toURI());
		MetadataFileSet cut = new MetadataFileSet(DummyEntity.createUniqueTorrent(), torrentFile);

		assertEquals("Piece size should be equal to the file size", torrentFile.length(), cut.getPieceSize());
	}

	@Test
	public void getBitfieldBytes() throws Exception {
		thrown.expect(UnsupportedOperationException.class);
		thrown.expectMessage("UT_METADATA");
		File torrentFile = new File(TorrentFileSetTest.class.getResource(SINGLE_FILE_TORRENT).toURI());

		MetadataFileSet cut = new MetadataFileSet(DummyEntity.createUniqueTorrent(), torrentFile);
		cut.getBitfieldBytes();
	}
}