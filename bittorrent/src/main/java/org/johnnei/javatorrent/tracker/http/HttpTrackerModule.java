package org.johnnei.javatorrent.tracker.http;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.tracker.http.HttpTracker;
import org.johnnei.javatorrent.module.IModule;
import org.johnnei.javatorrent.module.ModuleBuildException;
import org.johnnei.javatorrent.torrent.peer.Peer;

/**
 * A {@link IModule} to add support for HTTP trackers.
 */
public class HttpTrackerModule implements IModule {

	@Override
	public void configureTorrentClient(TorrentClient.Builder builder) {
		builder.registerTrackerProtocol("http", (trackerUrl, torrentClient) -> new HttpTracker.Builder()
				.setTorrentClient(torrentClient)
				.setUrl(trackerUrl)
				.build());
	}

	@Override
	public int getRelatedBep() {
		return 3;
	}

	@Override
	public List<Class<IModule>> getDependsOn() {
		return Collections.emptyList();
	}

	@Override
	public void onPostHandshake(Peer peer) throws IOException {
		// HTTP tracker doesn't add anything to the handshake process.
	}

	@Override
	public void onBuild(TorrentClient torrentClient) throws ModuleBuildException {
		// HTTP tracker doesn't do anything on creation.
	}

	@Override
	public void onShutdown() {
		// HTTP tracker doesn't do anything on shutdown.
	}
}
