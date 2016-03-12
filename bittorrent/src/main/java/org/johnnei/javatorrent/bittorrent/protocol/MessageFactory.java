package org.johnnei.javatorrent.bittorrent.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBitfield;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageCancel;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageChoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageHave;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageInterested;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageRequest;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUnchoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUninterested;

/**
 * The factory which maps the message IDs to the {@link IMessage} instances.
 *
 */
public class MessageFactory {

	private final Map<Integer, Supplier<IMessage>> messageSuppliers;

	private MessageFactory(Builder builder) {
		messageSuppliers = builder.messageSuppliers;
	}

	/**
	 * Creates a new message instance which maps to the given ID
	 * @param id The ID of the message
	 * @return New instance of the associated message
	 */
	public IMessage createById(int id) {
		if (!messageSuppliers.containsKey(id)) {
			throw new IllegalArgumentException(String.format("Message %d is not known.", id));
		}

		return messageSuppliers.get(id).get();
	}

	public static class Builder {

		private Map<Integer, Supplier<IMessage>> messageSuppliers;

		public Builder() {
			messageSuppliers = new HashMap<>();

			// Register BitTorrent messages
			registerMessage(BitTorrent.MESSAGE_BITFIELD, MessageBitfield::new);
			registerMessage(BitTorrent.MESSAGE_CANCEL, MessageCancel::new);
			registerMessage(BitTorrent.MESSAGE_CHOKE, MessageChoke::new);
			registerMessage(BitTorrent.MESSAGE_HAVE, MessageHave::new);
			registerMessage(BitTorrent.MESSAGE_INTERESTED, MessageInterested::new);
			registerMessage(BitTorrent.MESSAGE_PIECE, MessageBlock::new);
			registerMessage(BitTorrent.MESSAGE_REQUEST, MessageRequest::new);
			registerMessage(BitTorrent.MESSAGE_UNCHOKE, MessageUnchoke::new);
			registerMessage(BitTorrent.MESSAGE_UNINTERESTED, MessageUninterested::new);
		}

		public Builder registerMessage(int id, Supplier<IMessage> messageSupplier) {
			if (messageSuppliers.containsKey(id)) {
				throw new IllegalStateException(String.format("Failed to add message with id %d: Already taken.", id));
			}

			messageSuppliers.put(id, messageSupplier);
			return this;
		}

		public MessageFactory build() {
			return new MessageFactory(this);
		}

	}

}
