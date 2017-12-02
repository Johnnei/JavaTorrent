package org.johnnei.javatorrent.bittorrent.protocol;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBitfield;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageCancel;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageChoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageHave;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageInterested;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageRequest;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUnchoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUninterested;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link MessageFactory}
 */
public class MessageFactoryTest {

	@Test
	public void testBuildRegistersBitTorrentMessages() {
		MessageFactory cut = new MessageFactory.Builder().build();

		assertEquals(MessageChoke.class, cut.createById(BitTorrent.MESSAGE_CHOKE).getClass(), "Incorrect message class for message ID 0");
		assertEquals(MessageUnchoke.class, cut.createById(BitTorrent.MESSAGE_UNCHOKE).getClass(), "Incorrect message class for message ID 1");
		assertEquals(MessageInterested.class, cut.createById(BitTorrent.MESSAGE_INTERESTED).getClass(), "Incorrect message class for message ID 2");
		assertEquals(MessageUninterested.class, cut.createById(BitTorrent.MESSAGE_UNINTERESTED).getClass(), "Incorrect message class for message ID 3");
		assertEquals(MessageHave.class, cut.createById(BitTorrent.MESSAGE_HAVE).getClass(), "Incorrect message class for message ID 4");
		assertEquals(MessageBitfield.class, cut.createById(BitTorrent.MESSAGE_BITFIELD).getClass(), "Incorrect message class for message ID 5");
		assertEquals(MessageRequest.class, cut.createById(BitTorrent.MESSAGE_REQUEST).getClass(), "Incorrect message class for message ID 6");
		assertEquals(MessageBlock.class, cut.createById(BitTorrent.MESSAGE_PIECE).getClass(), "Incorrect message class for message ID 7");
		assertEquals(MessageCancel.class, cut.createById(BitTorrent.MESSAGE_CANCEL).getClass(), "Incorrect message class for message ID 8");
		// We can't assert port 9 as we don't support DHT and thus don't have a mapping to the message.
	}

	@Test
	public void testErrorOnInvalidId() {
		MessageFactory cut = new MessageFactory.Builder().build();
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> cut.createById(9));
		assertThat(exception.getMessage(), containsString("Message 9"));
	}

	@Test
	public void testErrorOnOverrideMessage() {
		IllegalStateException exception =
			assertThrows(IllegalStateException.class, () -> new MessageFactory.Builder().registerMessage(4, MessageBlock::new));
		assertThat(exception.getMessage(), containsString("Failed to add message"));

	}

}
