package org.johnnei.javatorrent.internal.utp.protocol;

/**
 * The states the connection of a uTP socket can be in.
 */
public enum ConnectionState {

    /**
     * We sent the initial SYN packet and are awaiting confirmation to establish the connection.
     */
    SYN_SENT,
    /**
     * We received the initial SYN packet from the remote and have sent the confirmation that we want to establish a connection.
     */
    SYN_RECEIVED,
    /**
     * The connection has been established and application data is able to be sent over the line.
     */
    CONNECTED,
    /**
     * We've either received or sent a FIN packet indicating that the connection will be terminated.
     */
    CLOSING,
    /**
     * All application data has passed the line and has been confirmed. The connection is now stale.
     */
    CLOSED;
}
