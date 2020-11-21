package org.johnnei.javatorrent.tracker;

import java.util.Collections;
import java.util.List;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerException;
import org.johnnei.javatorrent.internal.tracker.udp.UdpSocketUtils;
import org.johnnei.javatorrent.internal.tracker.udp.UdpTrackerSocket;
import org.johnnei.javatorrent.module.IModule;
import org.johnnei.javatorrent.module.ModuleBuildException;
import org.johnnei.javatorrent.torrent.peer.Peer;

public final class UdpTrackerModule implements IModule {

	private UdpTrackerSocket socket;

	private UdpTrackerModule() {
	}

	@Override
	public void configureTorrentClient(TorrentClient.Builder builder) {
		builder.registerTrackerProtocol("udp", (trackerUrl, torrentClient) -> new UdpTracker.Builder()
				.setTorrentClient(torrentClient)
				.setSocket(socket)
				.setUrl(trackerUrl)
				.build());
	}

	@Override
	public int getRelatedBep() {
		return 15;
	}

	@Override
	public List<Class<IModule>> getDependsOn() {
		return Collections.emptyList();
	}

	@Override
	public void onPostHandshake(Peer peer) {
		/* UDP Tracker support is not announced in the handshake */
	}

	@Override
	public void onBuild(TorrentClient torrentClient) throws ModuleBuildException {
		try {
			socket = new UdpTrackerSocket.Builder()
					.setTorrentClient(torrentClient)
					.setSocketUtils(new UdpSocketUtils())
					.build();
			Thread thread = new Thread(socket, "UdpTracker Worker Thread");
			thread.setDaemon(true);
			thread.start();
		} catch (TrackerException e) {
			throw new ModuleBuildException("Failed to initialize tracker", e);
		}
	}

	@Override
	public void onShutdown() {
		socket.shutdown();
	}

	public static final class Builder {

		public UdpTrackerModule build() {
			return new UdpTrackerModule();
		}

	}

}
