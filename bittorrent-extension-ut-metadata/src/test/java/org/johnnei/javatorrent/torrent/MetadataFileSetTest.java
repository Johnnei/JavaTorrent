package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.junit.jupiter.Folder;
import org.johnnei.junit.jupiter.TempFolderExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link MetadataFileSet}
 */
@ExtendWith(TempFolderExtension.class)
public class MetadataFileSetTest {

	private static final String SINGLE_FILE_TORRENT = "/org/johnnei/javatorrent/phases/gimp-2.8.16-setup-1.exe.torrent";

	@Test
	public void testConstructorNullTorrent(@Folder Path tmp) throws Exception {
		Exception e = assertThrows(IllegalArgumentException.class, () -> new MetadataFileSet(null, tmp.resolve("metadata.torrent").toFile()));
		assertThat(e.getMessage(), containsString("Torrent"));
	}

	@Test
	public void testConstructorNullMetadata() throws Exception {
		Exception e = assertThrows(IllegalArgumentException.class, () -> new MetadataFileSet(DummyEntity.createUniqueTorrent(), null));
		assertThat(e.getMessage(), containsString("Metadata"));
	}

	@Test
	public void testConstructorNonExistingMetadata() throws Exception {
		Exception e = assertThrows(
			IllegalArgumentException.class,
			() -> new MetadataFileSet(DummyEntity.createUniqueTorrent(), new File("dfasfsdgafhjkdsafdjhask.torrent"))
		);
		assertThat(e.getMessage(), containsString("Metadata"));
	}

	@Test
	public void testConstructor() throws Exception {
		File torrentFile = new File(TorrentFileSetTest.class.getResource(SINGLE_FILE_TORRENT).toURI());
		MetadataFileSet cut = new MetadataFileSet(DummyEntity.createUniqueTorrent(), torrentFile);

		assertEquals(torrentFile.length(), cut.getPieceSize(), "Piece size should be equal to the file size");
		assertEquals(torrentFile.length(), cut.getTotalFileSize(), "Total size should be equal to the file size");
	}

	@Test
	public void getBitfieldBytes() throws Exception {
		File torrentFile = new File(TorrentFileSetTest.class.getResource(SINGLE_FILE_TORRENT).toURI());

		MetadataFileSet cut = new MetadataFileSet(DummyEntity.createUniqueTorrent(), torrentFile);
		Exception e = assertThrows(UnsupportedOperationException.class, cut::getBitfieldBytes);
		assertThat(e.getMessage(), containsString("UT_METADATA"));
	}
}
