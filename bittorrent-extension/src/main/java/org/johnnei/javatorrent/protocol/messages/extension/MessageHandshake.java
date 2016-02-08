package org.johnnei.javatorrent.protocol.messages.extension;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.johnnei.javatorrent.Version;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.protocol.IExtension;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.encoding.Bencode;
import org.johnnei.javatorrent.torrent.encoding.Bencoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHandshake implements IMessage {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandshake.class);

	private String bencodedHandshake;

	private Collection<IExtension> extensions;

	public MessageHandshake(Collection<IExtension> extensions) {
		this.extensions = extensions;
	}

	public MessageHandshake(Peer peer, Map<Integer, IExtension> extensionMap) {
		Bencoder encoder = new Bencoder();
		encoder.dictionaryStart();
		encoder.string("m");
		encoder.dictionaryStart();
		for (Entry<Integer, IExtension> extension : extensionMap.entrySet()) {
			encoder.string(extension.getValue().getExtensionName());
			encoder.integer(extension.getKey());
		}
		encoder.dictionaryEnd();
		encoder.string("v");
		encoder.string(Version.BUILD);

		for (IExtension extension : extensionMap.values()) {
			extension.addHandshakeMetadata(peer, encoder);
		}

		encoder.dictionaryEnd();
		bencodedHandshake = encoder.getBencodedData();
	}

	@Override
	public void write(OutStream outStream) {
		outStream.writeString(bencodedHandshake);
	}

	@Override
	public void read(InStream inStream) {
		bencodedHandshake = inStream.readString(inStream.available());
	}

	@Override
	public void process(Peer peer) {
		Bencode decoder = new Bencode(bencodedHandshake);
		try {
			Map<String, Object> dictionary = decoder.decodeDictionary();
			Object m = dictionary.get("m");
			if (m != null && m instanceof Map<?, ?>) {
				Map<?, ?> extensionData = (Map<?, ?>) m;
				extensions.forEach(e -> e.processHandshakeMetadata(peer, dictionary, extensionData));
			}
			Object reqq = dictionary.get("reqq");
			if (reqq != null) {
				peer.setAbsoluteRequestLimit((int) reqq);
			}
			Object v = dictionary.get("v");
			if (v != null) {
				peer.setClientName((String) v);
			}
		} catch (Exception e) {
			LOGGER.error("Extension handshake error", e);
			peer.getBitTorrentSocket().close();
		}
	}

	@Override
	public int getLength() {
		return bencodedHandshake.length();
	}

	@Override
	public int getId() {
		return Protocol.EXTENDED_MESSAGE_HANDSHAKE;
	}

	@Override
	public void setReadDuration(int duration) {
	}

	@Override
	public String toString() {
		return String.format("MessageHandshake[bencoded=%s]", bencodedHandshake);
	}

}
