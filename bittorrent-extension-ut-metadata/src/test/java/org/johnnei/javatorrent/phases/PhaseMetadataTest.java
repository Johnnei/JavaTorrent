package org.johnnei.javatorrent.phases;

import java.io.File;
import java.util.Collections;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.module.UTMetadataExtension;
import org.johnnei.javatorrent.protocol.extension.ExtensionModule;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.algos.pieceselector.IPieceSelector;
import org.johnnei.javatorrent.torrent.algos.pieceselector.MetadataSelect;
import org.johnnei.javatorrent.torrent.files.Block;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;
import org.johnnei.javatorrent.ut.metadata.protocol.UTMetadata;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests {@link PhaseMetadata}
 */
public class PhaseMetadataTest {

	private static final byte[] TORRENT_FILE_HASH = new byte[] {
			(byte) 0xc8,        0x36, (byte) 0x9f,        0x0b, (byte) 0xa4,
			(byte) 0xbf,        0x6c, (byte) 0xd8,        0x7f, (byte) 0xb1,
			0x3b,        0x34,        0x37,        0x78,        0x2e,
			0x2c,        0x78,        0x20, (byte) 0xbb,        0x38
	};

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File metadataFile;
	private Torrent torrentMock;
	private Metadata metadataMock;
	private TorrentClient torrentClientMock;

	private void setUpTorrentClient() throws Exception {
		if (metadataFile == null) {
			metadataFile = new File(PhasePreMetadata.class.getResource("gimp-2.8.16-setup-1.exe.torrent").toURI());
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
		when(metadataExtensionMock.getDownloadFolder()).thenReturn(temporaryFolder.newFolder());
	}

	@Test
	public void testIsDone() throws Exception {
		setUpTorrentClient();
		MetadataFileSet metadataFileSetMock = mock(MetadataFileSet.class);

		when(metadataMock.getFileSet()).thenReturn(Optional.of(metadataFileSetMock));
		when(metadataFileSetMock.isDone()).thenReturn(true);

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock);
		boolean isDone = cut.isDone();

		assertTrue("Torrent file should have been marked done", isDone);
	}

	@Test
	public void testIsDoneMissingMetadata() throws Exception {
		setUpTorrentClient();

		when(metadataMock.getFileSet()).thenReturn(Optional.empty());

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock);
		boolean isDone = cut.isDone();

		assertFalse("Torrent file should have been marked done", isDone);
	}

	@Test
	public void testProcess() throws Exception {
		setUpTorrentClient();

		IPieceSelector pieceSelectorMock = mock(IPieceSelector.class);
		Piece pieceMockOne = mock(Piece.class);
		Piece pieceMockTwo = mock(Piece.class);
		Block blockMock = mock(Block.class);

		Peer peerWithoutExtensions = mock(Peer.class, "peerWithoutExtensions");
		Peer peerWithoutUtMetadata = mock(Peer.class, "peerWithoutUtMetadata");
		Peer peerWithEmptyPiece = mock(Peer.class, "peerWithEmptyPiece");
		Peer peerWithThreeBlocks = mock(Peer.class, "peerWithThreeBlocks");
		Peer peerWithOneBlock = mock(Peer.class, "peerWithOneBlock");
		Peer peerWithStolenBlock = mock(Peer.class, "peerWithStolenBlock");

		PeerExtensions extensionWithoutUtMetadata = mock(PeerExtensions.class);
		PeerExtensions extensionWithUtMetadata = mock(PeerExtensions.class);

		when(torrentMock.getPeers())
				.thenReturn(
						asList(peerWithoutExtensions, peerWithoutUtMetadata, peerWithEmptyPiece, peerWithThreeBlocks, peerWithOneBlock, peerWithStolenBlock)
				);
		when(torrentMock.getPieceSelector()).thenReturn(pieceSelectorMock);

		when(peerWithoutExtensions.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.empty());
		when(peerWithoutUtMetadata.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.of(extensionWithoutUtMetadata));
		when(peerWithEmptyPiece.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.of(extensionWithUtMetadata));
		when(peerWithThreeBlocks.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.of(extensionWithUtMetadata));
		when(peerWithOneBlock.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.of(extensionWithUtMetadata));
		when(peerWithStolenBlock.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.of(extensionWithUtMetadata));

		when(extensionWithoutUtMetadata.hasExtension(UTMetadata.NAME)).thenReturn(false);
		when(extensionWithUtMetadata.hasExtension(UTMetadata.NAME)).thenReturn(true);

		when(pieceSelectorMock.getPieceForPeer(peerWithEmptyPiece)).thenReturn(Optional.empty());
		when(pieceSelectorMock.getPieceForPeer(peerWithThreeBlocks)).thenReturn(Optional.of(pieceMockOne));
		when(pieceSelectorMock.getPieceForPeer(peerWithOneBlock)).thenReturn(Optional.of(pieceMockTwo));
		when(pieceSelectorMock.getPieceForPeer(peerWithStolenBlock)).thenReturn(Optional.of(pieceMockTwo));

		when(pieceMockOne.hasBlockWithStatus(BlockStatus.Needed)).thenReturn(true, false);
		when(pieceMockTwo.hasBlockWithStatus(BlockStatus.Needed)).thenReturn(true, false);
		when(peerWithThreeBlocks.getFreeWorkTime()).thenReturn(1);
		when(peerWithOneBlock.getFreeWorkTime()).thenReturn(0);
		when(peerWithStolenBlock.getFreeWorkTime()).thenReturn(1);

		when(pieceMockOne.getRequestBlock()).thenReturn(Optional.of(blockMock));
		when(pieceMockTwo.getRequestBlock()).thenReturn(Optional.empty());

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock);
		cut.process();

		verify(peerWithThreeBlocks).addBlockRequest(pieceMockOne, 0, 0, PeerDirection.Download);
	}

	@Test
	public void testOnPhaseExit() throws Exception {
		setUpTorrentClient();

		when(torrentMock.isDownloadingMetadata()).thenReturn(true);
		when(torrentMock.getDisplayName()).thenReturn("OnPhaseExit");

		when(metadataMock.getFileEntries()).thenReturn(Collections.emptyList());
		when(metadataMock.getPieceHashes()).thenReturn(Collections.emptyList());

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock);
		cut.onPhaseExit();

		ArgumentCaptor<byte[]> bufferCapture = ArgumentCaptor.forClass(byte[].class);
		verify(torrentMock).setFileSet(isA(TorrentFileSet.class));
		verify(metadataMock).initializeMetadata(bufferCapture.capture());

		assertArrayEquals("Incorrect data has been submitted", TORRENT_FILE_HASH, SHA1.hash(bufferCapture.getValue()));
	}

	@Test
	public void testOnPhaseEnter() throws Exception {
		setUpTorrentClient();

		torrentMock.setPieceSelector(isA(MetadataSelect.class));
		when(metadataMock.getHash()).thenReturn(new byte[] {
				(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8,        0x7f, (byte) 0xb1,
				0x3b, 0x34,        0x37, 0x78,        0x2e,        0x2c, 0x78,        0x20, (byte) 0xbb,        0x38 });

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		assertTrue("Torrent file should have matched", cut.foundMatchingFile);
		assertTrue("Torrent file should have been marked done", cut.isDone());
	}

	@Test
	public void testOnPhaseEnterMissingFile() throws Exception {
		metadataFile = new File("this_file_should_not_exist.torrent");
		setUpTorrentClient();

		when(metadataMock.getFileSet()).thenReturn(Optional.empty());

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock);
		cut.onPhaseEnter();
		boolean isDone = cut.isDone();

		verify(torrentMock).setPieceSelector(isA(MetadataSelect.class));

		assertFalse("Torrent file should not have matched", cut.foundMatchingFile);
		assertFalse("Torrent file should not have been marked done", isDone);
	}

	@Test
	public void testOnPhaseEnterMismatchedHash() throws Exception {
		setUpTorrentClient();

		when(metadataMock.getHash()).thenReturn(new byte[0]);

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		verify(torrentMock).setPieceSelector(isA(MetadataSelect.class));

		assertFalse("Torrent file should not have matched", cut.foundMatchingFile);
	}

	@Test
	public void testOnPhaseExitDoNothingWhenPreDownloaded() throws Exception {
		setUpTorrentClient();

		when(torrentMock.isDownloadingMetadata()).thenReturn(false);

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock);
		cut.onPhaseExit();

		verify(torrentMock).isDownloadingMetadata();
		verifyNoMoreInteractions(torrentMock);
	}

}