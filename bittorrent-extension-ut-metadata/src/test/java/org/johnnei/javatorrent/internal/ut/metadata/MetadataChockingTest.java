package org.johnnei.javatorrent.internal.ut.metadata;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.module.MetadataInformation;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetadataChockingTest {

	@Test
	@DisplayName("Interested - Metadata size > 0")
	public void testShowsInterestOnHavingMetadataInformation() {
		Peer peer = mock(Peer.class);
		MetadataInformation info = new MetadataInformation();
		info.setMetadataSize(500);
		when(peer.getModuleInfo(MetadataInformation.class)).thenReturn(Optional.of(info));

		MetadataChocking cut = new MetadataChocking();
		cut.updateChoking(peer);
		verify(peer).setInterested(PeerDirection.Download, true);
	}

	@Test
	@DisplayName("Not Interested - Metadata = null")
	public void testDoesNotShowInterestOnNotHavingMetdataInformation() {
		Peer peer = mock(Peer.class);
		when(peer.getModuleInfo(MetadataInformation.class)).thenReturn(Optional.empty());

		MetadataChocking cut = new MetadataChocking();
		cut.updateChoking(peer);
		verify(peer, never()).setInterested(PeerDirection.Download, true);
	}

	@Test
	@DisplayName("Not Interested - Metadata Size = 0")
	public void testDoesNotShowInterestOnHavingMetadataInformationWithSize0() {
		Peer peer = mock(Peer.class);
		MetadataInformation info = new MetadataInformation();
		info.setMetadataSize(0);
		when(peer.getModuleInfo(MetadataInformation.class)).thenReturn(Optional.of(info));

		MetadataChocking cut = new MetadataChocking();
		cut.updateChoking(peer);
		verify(peer, never()).setInterested(PeerDirection.Download, true);
	}

}
