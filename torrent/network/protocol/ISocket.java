package torrent.network.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;


public interface ISocket {
	
	public void connect(InetSocketAddress endpoint, int port) throws IOException;
	
	public InputStream getInputStream() throws IOException;
	
	public OutputStream getOutputStream() throws IOException;
	
	public void close() throws IOException;
	
	public ISocket getFallbackSocket();
	
	public boolean canFallback();
	
	public boolean isClosed();
	
	public boolean isInputShutdown();
	
	public boolean isOutputShutdown();

}
