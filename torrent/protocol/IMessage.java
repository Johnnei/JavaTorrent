package torrent.protocol;

import torrent.download.peer.Peer;
import torrent.network.Stream;

public interface IMessage {
	
	/**
	 * Writes the message to the output stream
	 * @param outStream
	 */
	public void write(Stream outStream);
	/***
	 * Read a message from the inputStream
	 * @param inStream
	 */
	public void read(Stream inStream);
	/**
	 * Process the message
	 * @param peer
	 * The client which should process this message
	 */
	public void process(Peer peer);
	/**
	 * The length of the message (only needed on writing)<br/>
	 * The length should include the byte for the message id
	 * @return
	 * integer
	 */
	public int getLength();
	/**
	 * The id of this message
	 * @return
	 * id
	 */
	public int getId();

}
