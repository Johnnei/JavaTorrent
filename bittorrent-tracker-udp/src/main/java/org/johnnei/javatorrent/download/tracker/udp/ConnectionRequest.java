package org.johnnei.javatorrent.download.tracker.udp;

import java.time.Clock;

import org.johnnei.javatorrent.download.tracker.UdpTracker;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.download.tracker.TrackerAction;
import org.johnnei.javatorrent.torrent.download.tracker.TrackerException;

public class ConnectionRequest implements IUdpTrackerPayload {

	private final Clock clock;

	private long connectionId;

	public ConnectionRequest(final Clock clock) {
		this.clock = clock;
	}

	@Override
	public void writeRequest(OutStream outStream) {
		/* Message has no payload */
	}

	@Override
	public void readResponse(InStream inStream) throws TrackerException {
		/* Message has no payload */
	}

	@Override
	public void process(UdpTracker tracker) {
		tracker.setConnection(new Connection(connectionId, clock));
	}

	@Override
	public TrackerAction getAction() {
		return TrackerAction.CONNECT;
	}

	@Override
	public int getMinimalSize() {
		return 8;
	}

}
