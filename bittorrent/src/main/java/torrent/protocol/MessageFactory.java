package torrent.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.johnnei.javatorrent.network.protocol.IMessage;

public class MessageFactory {

	private Map<Integer, Supplier<IMessage>> messageSuppliers;

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
