package org.johnnei.javatorrent.torrent.peer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;

import org.johnnei.javatorrent.test.DummyEntity;
import org.junit.Test;

public class PeerTest {

	@Test
	public void testAddModuleInfo() {
		Peer peer = new Peer(null, DummyEntity.createTorrent(), new byte[8]);

		Object o = new Object();
		peer.addModuleInfo(o);
		Object returnedO = peer.getModuleInfo(Object.class).get();

		assertEquals("Returned object is not equal to inserted", o, returnedO);
	}

	@Test(expected=IllegalStateException.class)
	public void testAddModuleInfoDuplicate() {
		Peer peer = new Peer(null, DummyEntity.createTorrent(), new byte[8]);

		Object o = new Object();
		Object o2 = new Object();
		peer.addModuleInfo(o);
		peer.addModuleInfo(o2);
	}

	@Test
	public void testAddModuleInfoNoElement() {
		Peer peer = new Peer(null, DummyEntity.createTorrent(), new byte[8]);

		Optional<Object> o = peer.getModuleInfo(Object.class);

		assertFalse("Expected empty result", o.isPresent());
	}

}
