package org.johnnei.javatorrent.internal.utp;

import java.util.concurrent.ScheduledFuture;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.utils.Argument;

/**
 * A data-object connecting the {@link java.util.concurrent.ScheduledFuture} polling the socket state with the actual socket.
 */
public class UtpSocketRegistration {

	private final UtpSocketImpl socket;

	private final ScheduledFuture<?> pollingTask;

	public UtpSocketRegistration(UtpSocketImpl socket, ScheduledFuture<?> pollingTask) {
		this.socket = Argument.requireNonNull(socket, "Socket can not be null.");
		this.pollingTask = Argument.requireNonNull(pollingTask, "Polling task can not be null.");
	}

	public ScheduledFuture<?> getPollingTask() {
		return pollingTask;
	}

	public UtpSocketImpl getSocket() {
		return socket;
	}
}
