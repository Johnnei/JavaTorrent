package org.johnnei.javatorrent.phases;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.module.MetadataInformation;
import org.johnnei.javatorrent.module.UTMetadataExtension;
import org.johnnei.javatorrent.protocol.extension.ExtensionModule;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.algos.choking.PermissiveStrategy;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.utils.StringUtils;

import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link PhasePreMetadata}
 */
public class PhasePreMetadataTest extends EasyMockSupport {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File metadataFile;
	private Torrent torrentMock;
	private TorrentClient torrentClientMock;

	private void setUpTorrentClient() throws Exception {
		metadataFile = new File(PhasePreMetadata.class.getResource("gimp-2.8.16-setup-1.exe.torrent").toURI());
		torrentMock = createMock(Torrent.class);
		ExtensionModule extensionModuleMock = createMock(ExtensionModule.class);
		UTMetadataExtension metadataExtensionMock = createMock(UTMetadataExtension.class);
		torrentClientMock = createMock(TorrentClient.class);

		expect(torrentClientMock.getModule(eq(ExtensionModule.class))).andReturn(Optional.of(extensionModuleMock));
		expect(extensionModuleMock.getExtensionByName(eq("ut_metadata"))).andReturn(Optional.of(metadataExtensionMock));
		expect(metadataExtensionMock.getTorrentFile(eq(torrentMock))).andAnswer(() -> metadataFile);
		expect(metadataExtensionMock.getDownloadFolder()).andReturn(temporaryFolder.newFolder());
	}

	@Test
	public void testIsDone() throws Exception {
		setUpTorrentClient();
		metadataFile = temporaryFolder.newFile();
		Peer peerMockOne = createMock(Peer.class);
		Peer peerMockTwo = createMock(Peer.class);

		expect(torrentMock.getPeers()).andStubReturn(Arrays.asList(peerMockOne, peerMockTwo));
		expect(peerMockOne.getModuleInfo(eq(MetadataInformation.class))).andReturn(Optional.empty());
		expect(peerMockTwo.getModuleInfo(eq(MetadataInformation.class))).andReturn(Optional.of(new MetadataInformation()));

		replayAll();

		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock);
		boolean result = cut.isDone();

		verifyAll();

		assertTrue("Peer has information so it should have been done.", result);
	}

	@Test
	public void testIsDoneNoPeers() throws Exception {
		setUpTorrentClient();
		metadataFile = temporaryFolder.newFile();

		expect(torrentMock.getPeers()).andStubReturn(Collections.emptyList());

		replayAll();

		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock);
		boolean result = cut.isDone();

		verifyAll();

		assertFalse("No peers registered so can't be done.", result);
	}

	@Test
	public void testProcess() throws Exception {
		setUpTorrentClient();
		metadataFile = temporaryFolder.newFile();

		replayAll();

		// No interaction expected.
		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock);
		cut.process();

		verifyAll();
	}

	@Test
	public void testOnPhaseEnter() throws Exception {
		setUpTorrentClient();

		expect(torrentMock.getHashArray()).andStubReturn(new byte[] {
				(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8,        0x7f, (byte) 0xb1,
				       0x3b, 0x34,        0x37, 0x78,        0x2e,        0x2c, 0x78,        0x20, (byte) 0xbb,        0x38 });

		replayAll();

		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		verifyAll();

		assertTrue("Torrent file should have matched", cut.foundMatchingFile);
	}

	@Test
	public void testOnPhaseEnterMissingFile() throws Exception {
		setUpTorrentClient();
		metadataFile = new File("this_file_should_never_exist_hopefully.torrent");

		replayAll();

		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		verifyAll();

		assertFalse("Torrent file should not have matched", cut.foundMatchingFile);
	}

	@Test
	public void testOnPhaseEnterMismatchedHash() throws Exception {
		setUpTorrentClient();

		expect(torrentMock.getHashArray()).andStubReturn(new byte[0]);

		replayAll();

		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		verifyAll();

		assertFalse("Torrent file should not have matched", cut.foundMatchingFile);
	}

	@Test
	public void testOnPhaseExit() throws Exception {
		setUpTorrentClient();
		metadataFile = temporaryFolder.newFile();

		byte[] hash = DummyEntity.createUniqueTorrentHash();

		Peer peerMock = createMock(Peer.class);

		MetadataInformation info = new MetadataInformation();
		info.setMetadataSize(42);
		expect(torrentMock.getPeers()).andStubReturn(Collections.singletonList(peerMock));
		expect(peerMock.getModuleInfo(eq(MetadataInformation.class))).andReturn(Optional.of(info));

		expect(torrentMock.getHashArray()).andStubReturn(hash);
		expect(torrentMock.getHash()).andStubReturn(StringUtils.byteArrayToString(hash));
		torrentMock.setMetadata(notNull());

		replayAll();

		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock);
		cut.isDone();
		cut.onPhaseExit();

		verifyAll();

		assertTrue(String.format("Metadata file %s should have been created", metadataFile), metadataFile.exists());
		assertEquals("Incorrect metadata size", 42, metadataFile.length());
	}

	@Test
	public void testGetChokingStrategy() throws Exception {
		setUpTorrentClient();

		replayAll();

		// No interaction expected.
		PhasePreMetadata cut = new PhasePreMetadata(torrentClientMock, torrentMock);
		IChokingStrategy result = cut.getChokingStrategy();

		verifyAll();

		assertNotNull("Value can never be null", result);
		assertTrue("Strategy should have been Permissive", result instanceof PermissiveStrategy);
	}

}