package org.johnnei.javatorrent.tracker;

import java.util.Collections;
import java.util.List;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.module.IModule;
import org.johnnei.javatorrent.internal.tracker.udp.UdpTrackerSocket;
import org.johnnei.javatorrent.torrent.peer.Peer;

public class UdpTrackerModule implements IModule {

	private UdpTrackerSocket socket;

	private int trackerPort;

	public UdpTrackerModule(Builder builder) {
		trackerPort = builder.port;
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
	public void onBuild(TorrentClient torrentClient) throws Exception {
		socket = new UdpTrackerSocket.Builder()
				.setTrackerManager(torrentClient.getTrackerManager())
				.setSocketPort(trackerPort)
				.build();
		Thread thread = new Thread(socket, "UdpTracker Worker Thread");
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public void onShutdown() {
		socket.shutdown();
	}

	public static final class Builder {

		private int port;

		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		public UdpTrackerModule build() {
			return new UdpTrackerModule(this);
		}

	}

}
