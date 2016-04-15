package org.johnnei.javatorrent.phases;

import java.io.File;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.ut_metadata.protocol.UTMetadata;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
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

import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static java.util.Arrays.asList;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link PhaseMetadata}
 */
public class PhaseMetadataTest extends EasyMockSupport {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testIsDone() throws Exception {
		File metadataFile = new File(PhasePreMetadata.class.getResource("gimp-2.8.16-setup-1.exe.torrent").toURI());
		Torrent torrentMock = createMock(Torrent.class);
		MetadataFileSet metadataFileSetMock = createNiceMock(MetadataFileSet.class);
		TorrentClient torrentClientMock = createMock(TorrentClient.class);

		expect(torrentMock.getMetadata()).andReturn(Optional.of(metadataFileSetMock));
		expect(metadataFileSetMock.isDone()).andReturn(true);

		replayAll();

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock, metadataFile, temporaryFolder.newFolder());
		boolean isDone = cut.isDone();

		verifyAll();

		assertTrue("Torrent file should have been marked done", isDone);
	}

	@Test
	public void testIsDoneMissingMetadata() throws Exception {
		File metadataFile = new File(PhasePreMetadata.class.getResource("gimp-2.8.16-setup-1.exe.torrent").toURI());
		Torrent torrentMock = createMock(Torrent.class);
		TorrentClient torrentClientMock = createMock(TorrentClient.class);

		expect(torrentMock.getMetadata()).andReturn(Optional.empty());

		replayAll();

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock, metadataFile, temporaryFolder.newFolder());
		boolean isDone = cut.isDone();

		verifyAll();

		assertFalse("Torrent file should have been marked done", isDone);
	}

	@Test
	public void testProcess() throws Exception {
		File metadataFile = new File(PhasePreMetadata.class.getResource("gimp-2.8.16-setup-1.exe.torrent").toURI());
		Torrent torrentMock = createNiceMock(Torrent.class);
		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);
		IPieceSelector pieceSelectorMock = createMock(IPieceSelector.class);
		Piece pieceMockOne = createNiceMock(Piece.class);
		Piece pieceMockTwo = createNiceMock(Piece.class);
		Block blockMock = createNiceMock(Block.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);

		Peer peerWithoutExtensions = createMock("peerWithoutExtensions", Peer.class);
		Peer peerWithoutUtMetadata = createMock("peerWithoutUtMetadata", Peer.class);
		Peer peerWithEmptyPiece = createMock("peerWithEmptyPiece", Peer.class);
		Peer peerWithThreeBlocks = createMock("peerWithThreeBlocks", Peer.class);
		Peer peerWithOneBlock = createMock("peerWithOneBlock", Peer.class);
		Peer peerWithStolenBlock = createMock("peerWithStolenBlock", Peer.class);

		PeerExtensions extensionWithoutUtMetadata = createMock(PeerExtensions.class);
		PeerExtensions extensionWithUtMetadata = createMock(PeerExtensions.class);

		expect(torrentMock.getPeers())
				.andReturn(
						asList(peerWithoutExtensions, peerWithoutUtMetadata, peerWithEmptyPiece, peerWithThreeBlocks, peerWithOneBlock, peerWithStolenBlock)
				);
		expect(torrentMock.getPieceSelector()).andStubReturn(pieceSelectorMock);

		expect(peerWithoutExtensions.getModuleInfo(PeerExtensions.class)).andReturn(Optional.empty());
		expect(peerWithoutUtMetadata.getModuleInfo(PeerExtensions.class)).andReturn(Optional.of(extensionWithoutUtMetadata));
		expect(peerWithEmptyPiece.getModuleInfo(PeerExtensions.class)).andReturn(Optional.of(extensionWithUtMetadata));
		expect(peerWithThreeBlocks.getModuleInfo(PeerExtensions.class)).andStubReturn(Optional.of(extensionWithUtMetadata));
		expect(peerWithOneBlock.getModuleInfo(PeerExtensions.class)).andStubReturn(Optional.of(extensionWithUtMetadata));
		expect(peerWithStolenBlock.getModuleInfo(PeerExtensions.class)).andStubReturn(Optional.of(extensionWithUtMetadata));

		expect(extensionWithoutUtMetadata.hasExtension(UTMetadata.NAME)).andReturn(false);
		expect(extensionWithUtMetadata.hasExtension(UTMetadata.NAME)).andStubReturn(true);
		expect(extensionWithUtMetadata.getExtensionId(UTMetadata.NAME)).andReturn(1);

		expect(pieceSelectorMock.getPieceForPeer(eq(peerWithEmptyPiece))).andReturn(Optional.empty());
		expect(pieceSelectorMock.getPieceForPeer(eq(peerWithThreeBlocks))).andReturn(Optional.of(pieceMockOne));
		expect(pieceSelectorMock.getPieceForPeer(eq(peerWithOneBlock))).andReturn(Optional.of(pieceMockTwo));
		expect(pieceSelectorMock.getPieceForPeer(eq(peerWithStolenBlock))).andReturn(Optional.of(pieceMockTwo));

		expect(pieceMockOne.hasBlockWithStatus(BlockStatus.Needed)).andReturn(true);
		expect(pieceMockOne.hasBlockWithStatus(BlockStatus.Needed)).andReturn(false);

		expect(pieceMockTwo.hasBlockWithStatus(BlockStatus.Needed)).andReturn(true).times(2);
		expect(peerWithThreeBlocks.getFreeWorkTime()).andReturn(1);
		expect(peerWithOneBlock.getFreeWorkTime()).andReturn(0);
		expect(peerWithStolenBlock.getFreeWorkTime()).andReturn(1);

		expect(pieceMockOne.getRequestBlock()).andReturn(Optional.of(blockMock));
		expect(pieceMockTwo.getRequestBlock()).andReturn(Optional.empty());

		peerWithThreeBlocks.addBlockRequest(0, 0, 0, PeerDirection.Download);
		expect(peerWithThreeBlocks.getBitTorrentSocket()).andReturn(socketMock);
		socketMock.enqueueMessage(isA(MessageExtension.class));

		replayAll();

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock, metadataFile, temporaryFolder.newFolder());
		cut.process();

		verifyAll();
	}

	@Test
	public void testOnPhaseExit() throws Exception {
		File metadataFile = new File(PhasePreMetadata.class.getResource("gimp-2.8.16-setup-1.exe.torrent").toURI());
		Torrent torrentMock = createMock(Torrent.class);
		TorrentClient torrentClientMock = createMock(TorrentClient.class);

		torrentMock.setFileSet(isA(TorrentFileSet.class));

		replayAll();

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock, metadataFile, temporaryFolder.newFolder());
		cut.onPhaseExit();

		verifyAll();
	}

	@Test
	public void testOnPhaseEnter() throws Exception {
		File metadataFile = new File(PhasePreMetadata.class.getResource("gimp-2.8.16-setup-1.exe.torrent").toURI());
		Torrent torrentMock = createMock(Torrent.class);
		TorrentClient torrentClientMock = createMock(TorrentClient.class);

		torrentMock.setPieceSelector(isA(MetadataSelect.class));
		expect(torrentMock.getHashArray()).andStubReturn(new byte[] {
				(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8,        0x7f, (byte) 0xb1,
				0x3b, 0x34,        0x37, 0x78,        0x2e,        0x2c, 0x78,        0x20, (byte) 0xbb,        0x38 });

		replayAll();

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock, metadataFile, temporaryFolder.newFolder());
		cut.onPhaseEnter();

		verifyAll();

		assertTrue("Torrent file should have matched", cut.foundMatchingFile);
		assertTrue("Torrent file should have been marked done", cut.isDone());
	}

	@Test
	public void testOnPhaseEnterMissingFile() throws Exception {
		File metadataFile = new File("this_file_should_never_exist_hopefully.torrent");
		Torrent torrentMock = createNiceMock(Torrent.class);
		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);

		torrentMock.setPieceSelector(isA(MetadataSelect.class));
		expect(torrentMock.getMetadata()).andReturn(Optional.empty());

		replayAll();

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock, metadataFile, temporaryFolder.newFolder());
		cut.onPhaseEnter();
		boolean isDone = cut.isDone();

		verifyAll();

		assertFalse("Torrent file should not have matched", cut.foundMatchingFile);
		assertFalse("Torrent file should not have been marked done", isDone);
	}

	@Test
	public void testOnPhaseEnterMismatchedHash() throws Exception {
		File metadataFile = new File(PhasePreMetadata.class.getResource("gimp-2.8.16-setup-1.exe.torrent").toURI());
		Torrent torrentMock = createMock(Torrent.class);
		TorrentClient torrentClientMock = createMock(TorrentClient.class);

		torrentMock.setPieceSelector(isA(MetadataSelect.class));
		expect(torrentMock.getHashArray()).andStubReturn(new byte[0]);

		replayAll();

		PhaseMetadata cut = new PhaseMetadata(torrentClientMock, torrentMock, metadataFile, temporaryFolder.newFolder());
		cut.onPhaseEnter();

		verifyAll();

		assertFalse("Torrent file should not have matched", cut.foundMatchingFile);
	}

}