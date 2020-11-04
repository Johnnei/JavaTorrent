package org.johnnei.javatorrent.internal;

import org.johnnei.javatorrent.TorrentClientSettings;
import org.johnnei.javatorrent.utils.Argument;

public class TorrentClientSettingsImpl implements TorrentClientSettings {

	private final boolean acceptingConnections;

	private final int listeningPort;

	private TorrentClientSettingsImpl(Builder builder) {
		this.acceptingConnections = builder.acceptingConnections;
		this.listeningPort = builder.listeningPort;
	}

	@Override
	public boolean isAcceptingConnections() {
		return acceptingConnections;
	}

	@Override
	public int getAcceptingPort() {
		return listeningPort;
	}

	public static final class Builder {

		private boolean acceptingConnections;

		private int listeningPort = 6881;

		public Builder withAcceptingConnections(boolean acceptingConnections) {
			this.acceptingConnections = acceptingConnections;
			return this;
		}

		public Builder withAcceptingPort(int acceptingPort) {
			Argument.requireWithinBounds(acceptingPort, 1, 65535 + 1,
				() -> acceptingPort + " is not a valid port to listen on");
			this.listeningPort = acceptingPort;
			return this;
		}

		public TorrentClientSettings build() {
			return new TorrentClientSettingsImpl(this);
		}
	}

}
