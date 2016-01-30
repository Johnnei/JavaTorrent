package org.johnnei.javatorrent.bittorrent.module;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.torrent.download.peer.Peer;

public interface IModule {

	/**
	 * Returns the number of the BEP to which this module is related.
	 * This is used to inform the user/developer for what extension this module is being used.
	 * @return The related BEP number
	 */
	public int getRelatedBep();

	/**
	 * Returns the extensions which must be present to allow this module to work at all.
	 * @return The list of required modules
	 */
	public List<Class<IModule>> getDependsOn();

	/**
	 * Returns the list of reserved bits to enable to indicate that we support this extension.
	 * The bit numbers are represented in the following order: Right to left, starting at zero.
	 * For reference see BEP 10 which indicates that bit 20 must be enabled.
	 * @return The bits to enable
	 */
	public int[] getReservedBits();

	/**
	 * Returns newly introduced messages are being added directly to the BitTorrent Protocol.
	 * @return The list of new messages
	 */
	public Map<Integer, Supplier<IMessage>> getMessages();

	/**
	 * Called when the peer has successfully passed the handshake with us.
	 * @param peer The peer who passed the handshake
	 */
	public void onPostHandshake(Peer peer) throws IOException;

}
