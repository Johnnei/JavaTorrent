package org.johnnei.javatorrent.phases;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.module.MetadataInformation;
import org.johnnei.javatorrent.module.UTMetadataExtension;
import org.johnnei.javatorrent.protocol.extension.ExtensionModule;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.utils.StringUtils;
import org.johnnei.junit.jupiter.Folder;
import org.johnnei.junit.jupiter.TempFolderExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DiscoverMetadataSizePhase}
 */
@ExtendWith(TempFolderExtension.class)
public class DiscoverMetadataSizePhaseTest {

	private File metadataFile;
	private Torrent torrentMock;
	private Metadata metadataMock;
	private TorrentClient torrentClientMock;

	@BeforeEach
	public void setUpTorrentClient(@Folder Path tmp) throws Exception {
		metadataFile = new File(DiscoverMetadataSizePhase.class.getResource("gimp-2.8.16-setup-1.exe.torrent").toURI());
		torrentMock = mock(Torrent.class);
		metadataMock = mock(Metadata.class);
		ExtensionModule extensionModuleMock = mock(ExtensionModule.class);
		UTMetadataExtension metadataExtensionMock = mock(UTMetadataExtension.class);
		torrentClientMock = mock(TorrentClient.class);

		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(torrentClientMock.getModule(eq(ExtensionModule.class))).thenReturn(Optional.of(extensionModuleMock));
		when(extensionModuleMock.getExtensionByName(eq("ut_metadata"))).thenReturn(Optional.of(metadataExtensionMock));
		when(metadataExtensionMock.getTorrentFile(eq(torrentMock))).thenAnswer(inv -> metadataFile);
		when(metadataExtensionMock.getDownloadFolder()).thenReturn(tmp.resolve("folder").toFile());
	}

	@Test
	public void testIsDone() throws Exception {
		Peer peerMockOne = mock(Peer.class);
		Peer peerMockTwo = mock(Peer.class);

		when(torrentMock.getPeers()).thenReturn(Arrays.asList(peerMockOne, peerMockTwo));
		when(peerMockOne.getModuleInfo(eq(MetadataInformation.class))).thenReturn(Optional.empty());
		when(peerMockTwo.getModuleInfo(eq(MetadataInformation.class))).thenReturn(Optional.of(new MetadataInformation()));

		DiscoverMetadataSizePhase cut = new DiscoverMetadataSizePhase(torrentClientMock, torrentMock);

		assertTrue(cut.isDone(), "Peer has information so it should have been done.");
	}

	@Test
	public void testIsDoneNoPeers() throws Exception {
		when(torrentMock.getPeers()).thenReturn(Collections.emptyList());

		DiscoverMetadataSizePhase cut = new DiscoverMetadataSizePhase(torrentClientMock, torrentMock);

		assertFalse(cut.isDone(), "No peers registered so can't be done.");
	}

	@Test
	public void testProcess() throws Exception {
		// No interaction expected.
		DiscoverMetadataSizePhase cut = new DiscoverMetadataSizePhase(torrentClientMock, torrentMock);
		cut.process();
	}

	@Test
	public void testOnPhaseEnter() throws Exception {
		when(metadataMock.getHash()).thenReturn(new byte[] {
				(byte) 0xc8, 0x36, (byte) 0x9f, 0x0b, (byte) 0xa4, (byte) 0xbf, 0x6c, (byte) 0xd8,        0x7f, (byte) 0xb1,
				       0x3b, 0x34,        0x37, 0x78,        0x2e,        0x2c, 0x78,        0x20, (byte) 0xbb,        0x38 });

		DiscoverMetadataSizePhase cut = new DiscoverMetadataSizePhase(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		assertTrue(cut.foundMatchingFile, "Torrent file should have matched");
	}

	@Test
	public void testOnPhaseEnterMissingFile() throws Exception {
		metadataFile = new File("this_file_should_never_exist_hopefully.torrent");

		DiscoverMetadataSizePhase cut = new DiscoverMetadataSizePhase(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		assertFalse(cut.foundMatchingFile, "Torrent file should not have matched");
	}

	@Test
	public void testOnPhaseEnterMismatchedHash() throws Exception {
		when(metadataMock.getHash()).thenReturn(new byte[0]);

		DiscoverMetadataSizePhase cut = new DiscoverMetadataSizePhase(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		assertFalse(cut.foundMatchingFile, "Torrent file should not have matched");
	}

	@Test
	public void testOnPhaseExit(@Folder Path path) throws Exception {
		metadataFile = path.resolve("metadata.torrent").toFile();

		byte[] hash = DummyEntity.createUniqueTorrentHash();

		Peer peerMock = mock(Peer.class);

		MetadataInformation info = new MetadataInformation();
		info.setMetadataSize(42);
		when(torrentMock.getPeers()).thenReturn(Collections.singletonList(peerMock));
		when(peerMock.getModuleInfo(eq(MetadataInformation.class))).thenReturn(Optional.of(info));

		when(metadataMock.getHash()).thenReturn(hash);
		when(metadataMock.getHashString()).thenReturn(StringUtils.byteArrayToString(hash));

		DiscoverMetadataSizePhase cut = new DiscoverMetadataSizePhase(torrentClientMock, torrentMock);
		cut.isDone();
		cut.onPhaseExit();

		verify(metadataMock).setFileSet(isA(MetadataFileSet.class));

		assertTrue(metadataFile.exists(), String.format("Metadata file %s should have been created", metadataFile));
		assertEquals(42, metadataFile.length(), "Incorrect metadata size");
	}

	@Test
	public void testGetChokingStrategy() {
		// No interaction expected.
		DiscoverMetadataSizePhase cut = new DiscoverMetadataSizePhase(torrentClientMock, torrentMock);
		IChokingStrategy result = cut.getChokingStrategy();

		assertNotNull(result, "Value can never be null");
	}

}
