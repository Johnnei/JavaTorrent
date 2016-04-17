package org.johnnei.javatorrent.protocol.messages.extension;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.johnnei.javatorrent.Version;
import org.johnnei.javatorrent.bittorrent.encoding.Bencode;
import org.johnnei.javatorrent.bittorrent.encoding.Bencoder;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.protocol.extension.IExtension;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHandshake implements IMessage {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandshake.class);

	private String bencodedHandshake;

	/**
	 * The extensions we know, so we only need to check for these in the handshake dictionary.
	 */
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
				extensions.stream()
					.filter(extension -> extensionData.containsKey(extension.getExtensionName()))
					.forEach(extension -> {
						Optional<PeerExtensions> peerExtensions = peer.getModuleInfo(PeerExtensions.class);

						if (!peerExtensions.isPresent()) {
							LOGGER.warn("Received Extension handshake from peer but PeerExtensions aren't registed");
							return;
						}

						peerExtensions.get().registerExtension((Integer) extensionData.get(extension.getExtensionName()), extension.getExtensionName());
						extension.processHandshakeMetadata(peer, dictionary, extensionData);
						LOGGER.trace("Registered {}={} to {}", extensionData.get(extension.getExtensionName()), extension.getExtensionName(), peer);
					});
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
	public String toString() {
		return String.format("MessageHandshake[bencoded=%s]", bencodedHandshake);
	}

}
