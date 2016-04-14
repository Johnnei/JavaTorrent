package org.johnnei.javatorrent.download.algos;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.module.MetadataInformation;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.algos.choking.PermissiveStrategy;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link PhasePreMetadata}
 */
public class PhasePreMetadataTest extends EasyMockSupport {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testIsDone() throws Exception {
		File metadataFile = temporaryFolder.newFile();
		Torrent torrentMock = createMock(Torrent.class);
		Peer peerMockOne = createMock(Peer.class);
		Peer peerMockTwo = createMock(Peer.class);

		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);

		expect(torrentMock.getPeers()).andStubReturn(Arrays.asList(peerMockOne, peerMockTwo));
		expect(peerMockOne.getModuleInfo(eq(MetadataInformation.class))).andReturn(Optional.empty());
		expect(peerMockTwo.getModuleInfo(eq(MetadataInformation.class))).andReturn(Optional.of(new MetadataInformation()));

		replayAll();

		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock, metadataFile);
		boolean result = cut.isDone();

		verifyAll();

		assertTrue("Peer has information so it should have been done.", result);
	}

	@Test
	public void testProcess() throws Exception {
		File metadataFile = temporaryFolder.newFile();
		Torrent torrentMock = createMock(Torrent.class);
		TorrentClient torrentClientMock = createMock(TorrentClient.class);

		replayAll();

		// No interaction expected.
		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock, metadataFile);
		cut.process();

		verifyAll();
	}

	@Test
	public void testOnPhaseEnter() throws Exception {
		File metadataFile = new File(PhasePreMetadata.class.getResource("gimp-2.8.16-setup-1.exe.torrent").toURI());
		Torrent torrentMock = createMock(Torrent.class);
		TorrentClient torrentClientMock = createMock(TorrentClient.class);

		expect(torrentMock.getHashArray()).andStubReturn(new byte[] {
				(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8,        0x7f, (byte) 0xb1,
				       0x3b, 0x34,        0x37, 0x78,        0x2e,        0x2c, 0x78,        0x20, (byte) 0xbb,        0x38 });

		replayAll();

		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock, metadataFile);
		cut.onPhaseEnter();

		verifyAll();

		assertTrue("Torrent file should have matched", cut.foundMatchingFile);
	}

	@Test
	public void testOnPhaseEnterMissingFile() throws Exception {
		File metadataFile = new File("this_file_should_never_exist_hopefully.torrent");
		Torrent torrentMock = createNiceMock(Torrent.class);
		TorrentClient torrentClientMock = createNiceMock(TorrentClient.class);

		replayAll();

		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock, metadataFile);
		cut.onPhaseEnter();

		verifyAll();

		assertFalse("Torrent file should not have matched", cut.foundMatchingFile);
	}

	@Test
	public void testOnPhaseEnterMismatchedHash() throws Exception {
		File metadataFile = new File(PhasePreMetadata.class.getResource("gimp-2.8.16-setup-1.exe.torrent").toURI());
		Torrent torrentMock = createMock(Torrent.class);
		TorrentClient torrentClientMock = createMock(TorrentClient.class);

		expect(torrentMock.getHashArray()).andStubReturn(new byte[0]);

		replayAll();

		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock, metadataFile);
		cut.onPhaseEnter();

		verifyAll();

		assertFalse("Torrent file should not have matched", cut.foundMatchingFile);
	}

	@Test
	public void testOnPhaseExit() throws Exception {
		File metadataFile = temporaryFolder.newFile();
		Torrent torrentMock = createMock(Torrent.class);
		TorrentClient torrentClientMock = createMock(TorrentClient.class);

		expect(torrentMock.getHashArray()).andStubReturn(DummyEntity.createUniqueTorrentHash());
		torrentMock.setMetadata(notNull());

		replayAll();

		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock, metadataFile);
		cut.onPhaseExit();

		verifyAll();
	}

	@Test
	public void testGetChokingStrategy() {
		File metadataFile = new File("file.torrent");
		Torrent torrentMock = createMock(Torrent.class);
		TorrentClient torrentClientMock = createMock(TorrentClient.class);

		replayAll();

		// No interaction expected.
		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock, metadataFile);
		IChokingStrategy result = cut.getChokingStrategy();

		verifyAll();

		assertNotNull("Value can never be null", result);
		assertTrue("Strategy should have been Permissive", result instanceof PermissiveStrategy);
	}

}