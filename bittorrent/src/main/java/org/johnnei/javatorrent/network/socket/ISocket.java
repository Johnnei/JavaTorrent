package org.johnnei.javatorrent.network.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectableChannel;

/**
 * A facade to allow multiple channel implementations on the same type.
 *
 * @param <T> A channel which can be selected and transfer data.
 */
public interface ISocket<T extends SelectableChannel & ByteChannel> extends AutoCloseable {

	/**
	 * Connects the underlying Socket to the endpoint
	 * @param endpoint The Address to connect to
	 * @throws IOException When connection fails
	 */
	void connect(InetSocketAddress endpoint) throws IOException;

	/**
	 * Gets the inputstream from the underlaying socket
	 * @return An InputStream which allows to read data from it
	 * @throws IOException when the stream could not be created
	 * @deprecated
	 */
	@Deprecated
	InputStream getInputStream() throws IOException;

	/**
	 * Gets the outputstream from the underlaying socket
	 * @return An outputstream which allows to write data to the socket
	 * @throws IOException when the stream could not be created
	 * @deprecated
	 */
	@Deprecated
	OutputStream getOutputStream() throws IOException;

	T getChannel();

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

	/**
	 * Forces the socket to send all data
	 * @throws IOException When the flushing fails due to IO errors.
	 * @deprecated
	 */
	@Deprecated
	void flush() throws IOException;

}
