package org.johnnei.javatorrent.bittorrent.module;

import java.io.IOException;
import java.util.List;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.download.peer.Peer;

public interface IModule {

	/**
	 * Registers all the mandatory components to the {@link TorrentClient.Builder}.
	 * @param builder The torrent client builder to which the module is being registered
	 */
	public void configureTorrentClient(TorrentClient.Builder builder);

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
	 * Called when the peer has successfully passed the handshake with us.
	 * @param peer The peer who passed the handshake
	 */
	public void onPostHandshake(Peer peer) throws IOException;

	/**
	 * Event called as the last method in the build process of the {@link TorrentClient.Builder#build()} call.
	 * @throws Exception When the module can not build itself correctly
	 */
	public void onBuild(TorrentClient torrentClient) throws Exception;

	/**
	 * Event called when the TorrentClient is being shutdown.
	 */
	public void onShutdown();

}
