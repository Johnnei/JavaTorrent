package org.johnnei.javatorrent.bittorrent.protocol;

import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBitfield;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageCancel;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageChoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageHave;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageInterested;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageRequest;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUnchoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUninterested;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link MessageFactory}
 */
public class MessageFactoryTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testBuildRegistersBitTorrentMessages() {
		MessageFactory cut = new MessageFactory.Builder().build();

		assertEquals("Incorrect message class for message ID 0", MessageChoke.class, cut.createById(BitTorrent.MESSAGE_CHOKE).getClass());
		assertEquals("Incorrect message class for message ID 1", MessageUnchoke.class, cut.createById(BitTorrent.MESSAGE_UNCHOKE).getClass());
		assertEquals("Incorrect message class for message ID 2", MessageInterested.class, cut.createById(BitTorrent.MESSAGE_INTERESTED).getClass());
		assertEquals("Incorrect message class for message ID 3", MessageUninterested.class, cut.createById(BitTorrent.MESSAGE_UNINTERESTED).getClass());
		assertEquals("Incorrect message class for message ID 4", MessageHave.class, cut.createById(BitTorrent.MESSAGE_HAVE).getClass());
		assertEquals("Incorrect message class for message ID 5", MessageBitfield.class, cut.createById(BitTorrent.MESSAGE_BITFIELD).getClass());
		assertEquals("Incorrect message class for message ID 6", MessageRequest.class, cut.createById(BitTorrent.MESSAGE_REQUEST).getClass());
		assertEquals("Incorrect message class for message ID 7", MessageBlock.class, cut.createById(BitTorrent.MESSAGE_PIECE).getClass());
		assertEquals("Incorrect message class for message ID 8", MessageCancel.class, cut.createById(BitTorrent.MESSAGE_CANCEL).getClass());
		// We can't assert port 9 as we don't support DHT and thus don't have a mapping to the message.
	}

	@Test
	public void testErrorOnInvalidId() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Message 9");

		MessageFactory cut = new MessageFactory.Builder().build();
		cut.createById(9);
	}

	@Test
	public void testErrorOnOverrideMessage() {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("Failed to add message");

		new MessageFactory.Builder()
				.registerMessage(4, MessageBlock::new);

	}

}