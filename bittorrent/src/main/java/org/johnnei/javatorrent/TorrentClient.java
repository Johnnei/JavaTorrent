package org.johnnei.javatorrent;

import java.util.Optional;

import org.johnnei.javatorrent.network.protocol.ConnectionDegradation;
import org.johnnei.javatorrent.network.protocol.TcpSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Torrent Client is the main entry point for the configuration and initiation of downloads/uploads.
 *
 */
public class TorrentClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(TorrentClient.class);

	private ConnectionDegradation connectionDegradation;

	private TorrentClient(Builder builder) {
		connectionDegradation = builder.connectionDegradation;
		// TODO Configure client as indicated by the builder
	}

	/**
	 * Gets the configured connection degradation rules.
	 * @return The socket degradation rules.
	 */
	public ConnectionDegradation getConnectionDegradation() {
		return connectionDegradation;
	}

	public static class Builder {

		private ConnectionDegradation connectionDegradation;

		/**
		 * Creates a builder with all default modules configured.
		 */
		public static Builder createDefaultBuilder() {
			LOGGER.debug("Configuring connection degradation to only support TCP");
			Builder builder = new Builder()
					.setConnectionDegradation(new ConnectionDegradation.Builder()
							.registerDefaultConnectionType(TcpSocket.class, () -> new TcpSocket(), Optional.empty())
							.build());

			return builder;
		}

		public Builder setConnectionDegradation(ConnectionDegradation connectionDegradation) {
			this.connectionDegradation = connectionDegradation;
			return this;
		}

		public TorrentClient build() {
			return new TorrentClient(this);
		}

	}

}
