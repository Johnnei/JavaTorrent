package org.johnnei.javatorrent.protocol.messages.extension;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.johnnei.javatorrent.Version;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedInteger;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedString;
import org.johnnei.javatorrent.bittorrent.encoding.Bencoding;
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

	private Bencoding bencoding = new Bencoding();

	private byte[] bencodedHandshake;

	/**
	 * The extensions we know, so we only need to check for these in the handshake dictionary.
	 */
	private Collection<IExtension> extensions;

	public MessageHandshake(Collection<IExtension> extensions) {
		this.extensions = extensions;
		bencodedHandshake = new byte[0];
	}

	public MessageHandshake(Peer peer, Map<Integer, IExtension> extensionMap) {
		BencodedMap extensionHandshake = new BencodedMap();
		BencodedMap extensionMessages = new BencodedMap();

		extensionHandshake.put("m", extensionMessages);

		for (Entry<Integer, IExtension> extension : extensionMap.entrySet()) {
			extensionMessages.put(extension.getValue().getExtensionName(), new BencodedInteger(extension.getKey()));
		}

		extensionHandshake.put("v", new BencodedString(Version.BUILD));

		for (IExtension extension : extensionMap.values()) {
			extension.addHandshakeMetadata(peer, extensionHandshake);
		}

		bencodedHandshake = extensionHandshake.serialize().getBytes(Charset.forName("UTF-8"));
	}

	@Override
	public void write(OutStream outStream) {
		outStream.write(bencodedHandshake);
	}

	@Override
	public void read(InStream inStream) {
		bencodedHandshake = inStream.readFully(inStream.available());
	}

	@Override
	public void process(Peer peer) {
		try {
			BencodedMap handshakeMap = (BencodedMap) bencoding.decode(new InStream(bencodedHandshake));
			BencodedMap messageMap = (BencodedMap) handshakeMap.get("m").orElse(new BencodedMap());
			extensions.stream()
				.filter(extension -> messageMap.get(extension.getExtensionName()).isPresent())
				.forEach(extension -> {
					Optional<PeerExtensions> peerExtensions = peer.getModuleInfo(PeerExtensions.class);

					if (!peerExtensions.isPresent()) {
						LOGGER.warn("Received Extension handshake from peer but PeerExtensions aren't registed");
						return;
					}

					peerExtensions.get().registerExtension((int) messageMap.get(extension.getExtensionName()).get().asLong(), extension.getExtensionName());
					extension.processHandshakeMetadata(peer, handshakeMap, messageMap);
					LOGGER.trace("Registered {}={} to {}", messageMap.get(extension.getExtensionName()), extension.getExtensionName(), peer);
				});

			handshakeMap.get("reqq").ifPresent(reqq -> peer.setAbsoluteRequestLimit((int) reqq.asLong()));
			handshakeMap.get("v").ifPresent(v -> peer.setClientName(v.asString()));
		} catch (Exception e) {
			LOGGER.error("Extension handshake error. Bencoded handshake: " + Arrays.toString(bencodedHandshake), e);
			peer.getBitTorrentSocket().close();
		}
	}

	@Override
	public int getLength() {
		return bencodedHandshake.length;
	}

	@Override
	public int getId() {
		return Protocol.EXTENDED_MESSAGE_HANDSHAKE;
	}

	@Override
	public String toString() {
		return String.format("MessageHandshake[bencoded=%s]", new String(bencodedHandshake, Charset.forName("UTF-8")));
	}

}
