package org.johnnei.javatorrent.network.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;

/**
 * A facade to allow multiple channel implementations on the same type.
 *
 * @param <T> A channel which can be selected and transfer data.
 */
public interface ISocket<I extends SelectableChannel & ReadableByteChannel, O extends SelectableChannel & WritableByteChannel> extends AutoCloseable {

	/**
	 * Connects the underlying Socket to the endpoint. This event <em>must</em> happen asynchronously.
	 * To notify the socket is connected (or it failed) use {@link java.nio.channels.SelectionKey#OP_CONNECT} in combination with {@link #isConnected()}.
	 * @param endpoint The Address to connect to
	 * @throws IOException When connection fails
	 */
	void connect(InetSocketAddress endpoint) throws IOException;

	/**
	 * @return <code>true</code> if the connection is successfully established. Otherwise <code>false</code>.
	 */
	boolean isConnected();

	I getReadableChannel();

	O getWritableChannel();

	/**
	 * Formally closes the connection
	 * @throws IOException When the connection could not be closed
	 */
	@Override
	void close() throws IOException;

	/**
	 * Checks if the connection has been closed
	 * @return true if the connection is closing/closed
	 */
	boolean isClosed();

	/**
	 * Checks if there will be no more data incoming
	 * @return true if EOF has been found
	 * @deprecated
	 */
	@Deprecated
	boolean isInputShutdown();

	/**
	 * Checks if no more data can be send on this socket
	 * @return true if EOF has been send
	 * @deprecated
	 */
	@Deprecated
	boolean isOutputShutdown();

}
