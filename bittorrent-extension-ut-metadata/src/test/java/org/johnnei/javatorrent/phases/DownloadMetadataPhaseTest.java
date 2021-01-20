package org.johnnei.javatorrent.phases;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.module.UTMetadataExtension;
import org.johnnei.javatorrent.protocol.extension.ExtensionModule;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.files.Block;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;
import org.johnnei.javatorrent.ut.metadata.protocol.UTMetadata;
import org.johnnei.junit.jupiter.Folder;
import org.johnnei.junit.jupiter.TempFolderExtension;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DownloadMetadataPhase}
 */
@ExtendWith(TempFolderExtension.class)
public class DownloadMetadataPhaseTest {

	private static final byte[] TORRENT_FILE_HASH = new byte[] {
			(byte) 0xc8,        0x36, (byte) 0x9f,        0x0b, (byte) 0xa4,
			(byte) 0xbf,        0x6c, (byte) 0xd8,        0x7f, (byte) 0xb1,
			0x3b,        0x34,        0x37,        0x78,        0x2e,
			0x2c,        0x78,        0x20, (byte) 0xbb,        0x38
	};

	private File metadataFile;
	private Torrent torrentMock;
	private Metadata metadataMock;
	private TorrentClient torrentClientMock;

	@BeforeEach
	public void setUpTorrentClient(@Folder Path tmp) throws Exception {
		if (metadataFile == null) {
			metadataFile = new File(DiscoverMetadataSizePhase.class.getResource("gimp-2.8.16-setup-1.exe.torrent").toURI());
		}
		torrentMock = mock(Torrent.class);
		metadataMock = mock(Metadata.class);
		ExtensionModule extensionModuleMock = mock(ExtensionModule.class);
		UTMetadataExtension metadataExtensionMock = mock(UTMetadataExtension.class);
		torrentClientMock = mock(TorrentClient.class);

		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(torrentClientMock.getModule(ExtensionModule.class)).thenReturn(Optional.of(extensionModuleMock));
		when(extensionModuleMock.getExtensionByName("ut_metadata")).thenReturn(Optional.of(metadataExtensionMock));
		when(metadataExtensionMock.getTorrentFile(torrentMock)).thenReturn(metadataFile);
		when(metadataExtensionMock.getDownloadFolder()).thenReturn(tmp.resolve("folder").toFile());
	}

	@Test
	public void testIsDone() throws Exception {
		MetadataFileSet metadataFileSetMock = mock(MetadataFileSet.class);

		when(metadataMock.getFileSet()).thenReturn(Optional.of(metadataFileSetMock));
		when(metadataFileSetMock.isDone()).thenReturn(true);

		DownloadMetadataPhase cut = new DownloadMetadataPhase(torrentClientMock, torrentMock);

		assertTrue(cut.isDone(), "Torrent file should have been marked done");
	}

	@Test
	public void testIsDoneMissingMetadata() throws Exception {
		when(metadataMock.getFileSet()).thenReturn(Optional.empty());

		DownloadMetadataPhase cut = new DownloadMetadataPhase(torrentClientMock, torrentMock);

		assertFalse(cut.isDone(), "Torrent file should have been marked done");
	}

	@Test
	public void testOnPhaseExit() throws Exception {
		when(torrentMock.isDownloadingMetadata()).thenReturn(true);
		when(torrentMock.getDisplayName()).thenReturn("OnPhaseExit");

		when(metadataMock.getFileEntries()).thenReturn(Collections.emptyList());
		when(metadataMock.getPieceHashes()).thenReturn(Collections.emptyList());

		DownloadMetadataPhase cut = new DownloadMetadataPhase(torrentClientMock, torrentMock);
		cut.onPhaseExit();

		ArgumentCaptor<byte[]> bufferCapture = ArgumentCaptor.forClass(byte[].class);
		verify(torrentMock).setFileSet(isA(TorrentFileSet.class));
		verify(metadataMock).initializeMetadata(bufferCapture.capture());

		assertArrayEquals(TORRENT_FILE_HASH, SHA1.hash(bufferCapture.getValue()), "Incorrect data has been submitted");
	}

	@Test
	public void testOnPhaseEnter() throws Exception {
		when(metadataMock.getHash()).thenReturn(new byte[] {
				(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8,        0x7f, (byte) 0xb1,
				0x3b, 0x34,        0x37, 0x78,        0x2e,        0x2c, 0x78,        0x20, (byte) 0xbb,        0x38 });

		DownloadMetadataPhase cut = new DownloadMetadataPhase(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		assertTrue(cut.foundMatchingFile, "Torrent file should have matched");
		assertTrue(cut.isDone(), "Torrent file should have been marked done");
	}

	@Test
	public void testOnPhaseEnterMissingFile(@Folder Path tmp) throws Exception {
		metadataFile = new File("this_file_should_not_exist.torrent");
		setUpTorrentClient(tmp);

		when(metadataMock.getFileSet()).thenReturn(Optional.empty());

		DownloadMetadataPhase cut = new DownloadMetadataPhase(torrentClientMock, torrentMock);
		cut.onPhaseEnter();
		boolean isDone = cut.isDone();

		assertFalse(cut.foundMatchingFile, "Torrent file should not have matched");
		assertFalse(isDone, "Torrent file should not have been marked done");
	}

	@Test
	public void testOnPhaseEnterMismatchedHash() throws Exception {
		when(metadataMock.getHash()).thenReturn(new byte[0]);

		DownloadMetadataPhase cut = new DownloadMetadataPhase(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		assertFalse(cut.foundMatchingFile, "Torrent file should not have matched");
	}

	@Test
	public void testOnPhaseExitDoNothingWhenPreDownloaded() throws Exception {
		when(torrentMock.isDownloadingMetadata()).thenReturn(false);

		DownloadMetadataPhase cut = new DownloadMetadataPhase(torrentClientMock, torrentMock);
		cut.onPhaseExit();

		verify(torrentMock).isDownloadingMetadata();
		verifyNoMoreInteractions(torrentMock);
	}

}
