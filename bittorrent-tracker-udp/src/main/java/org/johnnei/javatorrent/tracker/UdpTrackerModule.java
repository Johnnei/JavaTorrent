package org.johnnei.javatorrent.tracker;

import java.io.IOException;
import java.time.Clock;
import java.util.Collections;
import java.util.List;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.module.IModule;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.tracker.udp.UdpTrackerSocket;

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
	public void onPostHandshake(Peer peer) throws IOException {
	}

	@Override
	public void onBuild(TorrentClient torrentClient) throws Exception {
		socket = new UdpTrackerSocket(torrentClient.getTrackerManager(), trackerPort, Clock.systemDefaultZone());
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
