package org.johnnei.javatorrent.protocol.extension;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.johnnei.javatorrent.bittorrent.module.IModule;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.protocol.IExtension;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.protocol.messages.extension.MessageHandshake;
import org.johnnei.javatorrent.protocol.messages.extension.Protocol;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionModule implements IModule {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionModule.class);

	private final Map<Integer, IExtension> extensionsById;

	private ExtensionModule(Builder builder) {
		extensionsById = Collections.unmodifiableMap(builder.extensionsById);

		LOGGER.info(String.format("Configured Extension Protocols: %s", extensionsById.values().stream()
				.map(e -> e.getExtensionName())
				.reduce((a, b) -> a + ", " + b).orElse("")));
	}

	public MessageHandshake createHandshakeMessage() {
		return new MessageHandshake(extensionsById.values());
	}

	@Override
	public int getRelatedBep() {
		return 10;
	}

	@Override
	public List<Class<IModule>> getDependsOn() {
		return Collections.emptyList();
	}

	@Override
	public int[] getReservedBits() {
		return new int[] { 20 };
	}

	@Override
	public Map<Integer, Supplier<IMessage>> getMessages() {
		Map<Integer, Supplier<IMessage>> messages = new HashMap<>();
		messages.put(Protocol.MESSAGE_EXTENDED_MESSAGE, () -> new MessageExtension(this));
		return messages;
	}

	@Override
	public void onPostHandshake(Peer peer) throws IOException {
		if (peer.getExtensions().hasExtension(5, 0x10)) {
			// Extended Messages extension
			sendExtendedMessages(peer);
		}
	}

	public Optional<IExtension> getExtensionById(int extensionId) {
		if (!extensionsById.containsKey(extensionId)) {
			return Optional.empty();
		}

		return Optional.of(extensionsById.get(extensionId));
	}

	private void sendExtendedMessages(Peer peer) throws IOException {
		MessageExtension message = new MessageExtension(Protocol.EXTENDED_MESSAGE_HANDSHAKE, new MessageHandshake(peer, extensionsById));
		peer.getBitTorrentSocket().queueMessage(message);
	}

	public static class Builder {

		private Map<String, IExtension> extensionsByName;

		private Map<Integer, IExtension> extensionsById;

		private int nextExtensionId;

		public Builder() {
			extensionsById = new HashMap<>();
			extensionsByName = new HashMap<>();
			nextExtensionId = 1;
		}

		public Builder registerExtension(IExtension extension) {
			if (extensionsByName.containsKey(extension.getExtensionName())) {
				throw new IllegalStateException(String.format("Trying to register extension which is already defined: %s", extension.getExtensionName()));
			}

			int extensionId = nextExtensionId;
			nextExtensionId++;

			extensionsById.put(extensionId, extension);
			extensionsByName.put(extension.getExtensionName(), extension);
			return this;
		}

		public ExtensionModule build() {
			return new ExtensionModule(this);
		}
	}

}
