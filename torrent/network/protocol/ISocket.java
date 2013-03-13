package torrent.network.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * A socket layout to get multiple socket protocols work on the some functions<br/>
 * @author Johnnei
 *
 */
public interface ISocket {
	
	/**
	 * Connects the underlaying Socket to the endpoint
	 * @param endpoint The Address to connect to
	 * @throws IOException When connection fails
	 */
	public void connect(InetSocketAddress endpoint) throws IOException;
	/**
	 * Gets the inputstream from the underlaying socket
	 * @return An InputStream which allows to read data from it
	 * @throws IOException when the stream could not be created
	 */
	public InputStream getInputStream() throws IOException;
	/**
	 * Gets the outputstream from the underlaying socket
	 * @return An outputstream which allows to write data to the socket
	 * @throws IOException when the stream could not be created
	 */
	public OutputStream getOutputStream() throws IOException;
	/**
	 * Formally closes the connection 
	 * @throws IOException When the connection could not be closed
	 */
	public void close() throws IOException;
	/**
	 * Creates a fallback socket based on the given information on this socket
	 * @return A new socket on which a connection might be established
	 */
	public ISocket getFallbackSocket();
	
	/**
	 * Check if this socket protocol has a fallback protocol<br/>
	 * Example: uTP may fallback to TCP
	 * @return If there is a fallback protocol
	 */
	public boolean canFallback();
	
	/**
	 * Checks if the connection has been closed
	 * @return true if the connection is closing/closed
	 */
	public boolean isClosed();
	
	/**
	 * Checks if there will be no more data incoming
	 * @return true if EOF has been found
	 */
	public boolean isInputShutdown();
	
	/**
	 * Checks if no more data can be send on this socket
	 * @return true if EOF has been send
	 */
	public boolean isOutputShutdown();

}
