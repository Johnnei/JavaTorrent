package org.johnnei.javatorrent.utp;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.async.LoopingRunnable;
import org.johnnei.javatorrent.internal.network.socket.ISocket;
import org.johnnei.javatorrent.internal.utp.UtpMultiplexer;
import org.johnnei.javatorrent.internal.utp.UtpSocket;
import org.johnnei.javatorrent.internal.utp.UtpSocketRegistry;
import org.johnnei.javatorrent.internal.utp.stream.PacketReader;
import org.johnnei.javatorrent.module.IModule;
import org.johnnei.javatorrent.module.ModuleBuildException;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.utils.Argument;

/**
 * Module which allows for creating connections via uTP.
 * <p>
 * It's advised to re-use instances of this module as each will create a dedicated thread to receive uTP messages.
 * Using more instances allows for more connections to be used as each instance is limited to 65536 (2^16) connections by protocol design.
 */
public class UtpModule implements IModule {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpModule.class);

	private UtpSocketRegistry socketRegistry;

	private UtpMultiplexer multiplexer;

	private Thread workerThread;

	private LoopingRunnable multiplexerRunner;

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
	public void onPostHandshake(Peer peer) throws IOException {
		// uTP doesn't do anything special with the BitTorrent protocol.
	}

	@Override
	public void onBuild(TorrentClient torrentClient) throws ModuleBuildException {
		socketRegistry = new UtpSocketRegistry();
		try {
			multiplexer = new UtpMultiplexer(new PacketReader(), socketRegistry, listeningPort);
			multiplexerRunner = new LoopingRunnable(multiplexer);
			workerThread = new Thread(multiplexerRunner, "uTP Packet Reader");
			workerThread.start();
			socketProcessorTask = torrentClient.getExecutorService().scheduleAtFixedRate(this::updateSockets, 0, 100, TimeUnit.MILLISECONDS);

		} catch (IOException e) {
			throw new ModuleBuildException("Failed to create uTP Multiplexer.", e);
		}
	}

	private void updateSockets() {
		try {
			for (UtpSocket socket : socketRegistry.getAllSockets()) {
				socket.processSendQueue();
			}
		} catch (Exception e) {
			LOGGER.warn("uTP socket caused exception", e);
		}
	}

	@Override
	public void onShutdown() {
		multiplexerRunner.stop();
		try {
			multiplexer.close();
		} catch (IOException e) {
			LOGGER.warn("Failed to shutdown uTP Multiplexer", e);
		}
		try {
			workerThread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.warn("Interrupted while waiting for uTP worker to exit.", e);
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
		return () -> socketRegistry.allocateSocket(connectionId -> {
			try {
				return UtpSocket.createInitiatingSocket(DatagramChannel.open(), connectionId);
			} catch (IOException e) {
				throw new IllegalStateException("Failed to open UDP channel.", e);
			}
		});
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
