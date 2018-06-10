package org.johnnei.javatorrent.utp;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.utp.UtpMultiplexer;
import org.johnnei.javatorrent.internal.utp.UtpPeerConnectionAcceptor;
import org.johnnei.javatorrent.internal.utp.UtpSocket;
import org.johnnei.javatorrent.internal.utp.stream.PacketReader;
import org.johnnei.javatorrent.module.IModule;
import org.johnnei.javatorrent.module.ModuleBuildException;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.utils.Argument;

/**
 * Module which allows for creating connections via uTP.
 *
 * Recommendations for: {@link TorrentClient.Builder#setExecutorService(ScheduledExecutorService)}. The module will use 1 'dedicated' task which is likely to
 * run constantly on high throughput, and 1 irregular task.
 */
public class UtpModule implements IModule {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpModule.class);

	private UtpMultiplexer multiplexer;

	private ScheduledFuture<?> socketProcessorTask;

	private int listeningPort;

	private UtpModule(Builder builder) {
		listeningPort = builder.listeningPort;
	}

	@Override
	public void configureTorrentClient(TorrentClient.Builder builder) {
		// uTP is configured on the ConnectionDegradation
	}

	@Override
	public int getRelatedBep() {
		return 29;
	}

	@Override
	public List<Class<IModule>> getDependsOn() {
		return Collections.emptyList();
	}

	@Override
	public void onPostHandshake(Peer peer) {
		// uTP doesn't do anything special with the BitTorrent protocol.
	}

	@Override
	public void onBuild(TorrentClient torrentClient) throws ModuleBuildException {
		try {
			multiplexer = new UtpMultiplexer(torrentClient, new UtpPeerConnectionAcceptor(torrentClient), new PacketReader(), listeningPort);
			socketProcessorTask = torrentClient.getExecutorService().scheduleAtFixedRate(multiplexer::updateSockets, 0, 1, TimeUnit.MILLISECONDS);
		} catch (IOException e) {
			throw new ModuleBuildException("Failed to create uTP Multiplexer.", e);
		}
	}

	@Override
	public void onShutdown() {
		try {
			multiplexer.close();
		} catch (IOException e) {
			LOGGER.warn("Failed to shutdown uTP Multiplexer", e);
		}
		socketProcessorTask.cancel(true);
	}

	/**
	 * @return The internal implementation of the uTP socket facade.
	 */
	@SuppressWarnings("unchecked")
	public Class<ISocket> getUtpSocketClass() {
		return (Class<ISocket>) (Class<? extends ISocket>) UtpSocket.class;
	}

	/**
	 * @return A supplier capable of creating new {@link UtpSocket}
	 */
	public Supplier<ISocket> createSocketFactory() {
		return () -> multiplexer.createUnconnectedSocket();
	}

	public static final class Builder {

		private int listeningPort;

		/**
		 * Configures on which UDP port uTP connections will be accepted.
		 * @param port The UDP port.
		 * @return The updated builder (this).
		 */
		public Builder listenOn(int port) {
			Argument.requireWithinBounds(port, 0, Short.MAX_VALUE + 1, () -> "Port must be a valid port (0 >= x < 2^16)");
			listeningPort = port;
			return this;
		}

		/**
		 * @return The newly created and configured UtpModule instance.
		 */
		public UtpModule build() {
			return new UtpModule(this);
		}

	}

}
