package org.johnnei.javatorrent.protocol.extension;

import java.util.Map;

import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.torrent.peer.Peer;

public interface IExtension {

	/**
	 * Calculates which message should be returned
	 *
	 * @param data The stream which remained after the extension base
	 * @return The Message
	 */
	IMessage getMessage(InStream data);

	/**
	 * Add the information to the handshake which is needed for the extension to work.
	 * @param peer The peer for which the handshake is created
	 * @param bencoder The bencoder which is currently writing a dictionary
	 */
	void addHandshakeMetadata(Peer peer, BencodedMap bencoder);

	/**
	 * Process the information in the dictionary which is relevant to the torrent.
	 * @param peer The peer on which the handshake applies
	 * @param dictionary The handshake dictionary
	 * @param mEntry The 'm' entry of the handshake
	 */
	void processHandshakeMetadata(Peer peer, Map<String, Object> dictionary, Map<?, ?> mEntry);

	/**
	 * The name of this extension. The advised format for new extensions is: &ltclient-token&gt_&ltname&gt
	 * @return The extension name
	 */
	String getExtensionName();

}
