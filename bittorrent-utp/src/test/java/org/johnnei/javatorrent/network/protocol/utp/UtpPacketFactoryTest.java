package org.johnnei.javatorrent.network.protocol.utp;

import org.johnnei.javatorrent.network.network.protocol.utp.packet.PacketData;
import org.johnnei.javatorrent.network.network.protocol.utp.packet.PacketFin;
import org.johnnei.javatorrent.network.network.protocol.utp.packet.PacketReset;
import org.johnnei.javatorrent.network.network.protocol.utp.packet.PacketState;
import org.johnnei.javatorrent.network.network.protocol.utp.packet.PacketSyn;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;

/**
 * Tests {@link UtpPacketFactory}
 */
public class UtpPacketFactoryTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testGetFromId() {
		UtpPacketFactory cut = new UtpPacketFactory();

		assertTrue("Expected data packet for id 0", cut.getFromId(0) instanceof PacketData);
		assertTrue("Expected fin packet for id 1", cut.getFromId(1) instanceof PacketFin);
		assertTrue("Expected state packet for id 2", cut.getFromId(2) instanceof PacketState);
		assertTrue("Expected reset packet for id 3", cut.getFromId(3) instanceof PacketReset);
		assertTrue("Expected syn packet for id 4", cut.getFromId(4) instanceof PacketSyn);
	}

	@Test
	public void testGetFromIdIncorrectId() {
		thrown.expect(IllegalArgumentException.class);

		new UtpPacketFactory().getFromId(142412);
	}
}