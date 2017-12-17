package org.johnnei.javatorrent.internal.network.connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;

public class NioConnectionAcceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(NioConnectionAcceptor.class);

	private final TorrentClient torrentClient;

	private final ServerSocketChannel serverChannel;
	private final ScheduledFuture<?> poller;

	public NioConnectionAcceptor(TorrentClient torrentClient) {
		this.torrentClient = torrentClient;
		try {
			serverChannel = ServerSocketChannel.open();
			serverChannel.bind(new InetSocketAddress(torrentClient.getDownloadPort()));
			serverChannel.configureBlocking(false);
			poller = torrentClient.getExecutorService().scheduleWithFixedDelay(this::pollConnections, 50, 100, TimeUnit.MILLISECONDS);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to set up Nio socket listener.", e);
		}

		LOGGER.info("Listening for incoming peer on TCP port {}", torrentClient.getDownloadPort());
	}

	public void stop() {
		poller.cancel(false);
	}

	private void pollConnections() {
		SocketChannel channel;
		try {
			while ((channel = serverChannel.accept()) != null) {
				torrentClient.getHandshakeHandler().onConnectionReceived(channel);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to accept connection", e);
		}

	}

}
