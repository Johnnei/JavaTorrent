package org.johnnei.javatorrent.protocol.messages.extension;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.protocol.extension.ExtensionModule;
import org.johnnei.javatorrent.protocol.extension.IExtension;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageExtension implements IMessage {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageExtension.class);

	private final ExtensionModule extensionModule;

	private int extensionId;

	private IMessage message;

	public MessageExtension(int extensionId, IMessage message) {
		this.extensionModule = null;
		this.extensionId = extensionId;
		this.message = message;
	}

	public MessageExtension(ExtensionModule extensionModule) {
		this.extensionModule = extensionModule;
	}

	@Override
	public void write(OutStream outStream) {
		outStream.writeByte(extensionId);
		message.write(outStream);
	}

	@Override
	public void read(InStream inStream) {
		extensionId = inStream.readByte();
		if (extensionId == Protocol.EXTENDED_MESSAGE_HANDSHAKE) {
			message = extensionModule.createHandshakeMessage();
		} else {
			IExtension extension = extensionModule.getExtensionById(extensionId)
					.orElseThrow(() -> new IllegalArgumentException(String.format("Unknown extension message with id %d", extensionId)));
			message = extension.getMessage(inStream);
		}
		message.read(inStream);
	}

	@Override
	public void process(Peer peer) {
		if (message == null) {
			LOGGER.error("Attemped to process extended message without message: {}", this);
			return;
		}
		message.process(peer);
	}

	@Override
	public int getLength() {
		return 2 + message.getLength();
	}

	@Override
	public int getId() {
		return Protocol.MESSAGE_EXTENDED_MESSAGE;
	}

	@Override
	public String toString() {
		return String.format("MessageExtension[extensionId=%s, message=%s]", extensionId, message);
	}

}
