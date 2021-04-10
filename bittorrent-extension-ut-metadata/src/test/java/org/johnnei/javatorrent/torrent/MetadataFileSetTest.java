package org.johnnei.javatorrent.torrent;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.johnnei.javatorrent.test.DummyEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link MetadataFileSet}
 */
public class MetadataFileSetTest {

	private static final String SINGLE_FILE_TORRENT = "/org/johnnei/javatorrent/phases/gimp-2.8.16-setup-1.exe.torrent";

	@Test
	public void testConstructorNullTorrent(@TempDir Path tmp) throws Exception {
		Exception e = assertThrows(IllegalArgumentException.class, () -> new MetadataFileSet(null, tmp.resolve("metadata.torrent")));
		assertThat(e.getMessage(), containsString("Hash"));
	}

	@Test
	public void testConstructorNullMetadata() throws Exception {
		Exception e = assertThrows(IllegalArgumentException.class, () -> new MetadataFileSet(DummyEntity.createUniqueTorrentHash(), null));
		assertThat(e.getMessage(), containsString("Metadata"));
	}

	@Test
	public void testConstructorNonExistingMetadata() throws Exception {
		Exception e = assertThrows(
			IllegalArgumentException.class,
			() -> new MetadataFileSet(DummyEntity.createUniqueTorrentHash(), Path.of("dfasfsdgafhjkdsafdjhask.torrent"))
		);
		assertThat(e.getMessage(), containsString("Metadata"));
	}

	@Test
	public void testConstructor() throws Exception {
		Path torrentFile = Path.of(TorrentFileSetTest.class.getResource(SINGLE_FILE_TORRENT).toURI());
		MetadataFileSet cut = new MetadataFileSet(DummyEntity.createUniqueTorrentHash(), torrentFile);

		assertEquals(Files.size(torrentFile), cut.getPieceSize(), "Piece size should be equal to the file size");
		assertEquals(Files.size(torrentFile), cut.getTotalFileSize(), "Total size should be equal to the file size");
	}

	@Test
	public void getBitfieldBytes() throws Exception {
		Path torrentFile = Path.of(TorrentFileSetTest.class.getResource(SINGLE_FILE_TORRENT).toURI());

		MetadataFileSet cut = new MetadataFileSet(DummyEntity.createUniqueTorrentHash(), torrentFile);
		Exception e = assertThrows(UnsupportedOperationException.class, cut::getBitfieldBytes);
		assertThat(e.getMessage(), containsString("UT_METADATA"));
	}
}
