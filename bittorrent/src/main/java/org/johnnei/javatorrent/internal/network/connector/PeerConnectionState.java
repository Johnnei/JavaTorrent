package org.johnnei.javatorrent.internal.network.connector;

import java.time.Instant;

import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.network.socket.ISocket;

public class PeerConnectionState {

	private final PeerConnectInfo peer;

	private Instant startTime;

	private ISocket currentSocket;

	public PeerConnectionState(PeerConnectInfo peer) {
		this.peer = peer;
	}

	public void updateSocket(Instant startTime, ISocket currentSocket) {
		this.currentSocket = currentSocket;
		this.startTime = startTime;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public ISocket getCurrentSocket() {
		return currentSocket;
	}

	public PeerConnectInfo getPeer() {
		return peer;
	}
}
