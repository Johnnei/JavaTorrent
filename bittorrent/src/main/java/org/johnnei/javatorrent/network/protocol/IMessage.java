package org.johnnei.javatorrent.network.protocol;

import java.time.Duration;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.download.peer.Peer;

public interface IMessage {

	/**
	 * Writes the message to the output stream
	 *
	 * @param outStream The buffered outputstream to write the message on
	 */
	void write(OutStream outStream);

	/***
	 * Read a message from the inputStream
	 *
	 * @param inStream The buffered inputstream to read the message from
	 */
	void read(InStream inStream);

	/**
	 * Process the message
	 *
	 * @param peer The client which should process this message
	 */
	void process(Peer peer);

	/**
	 * The length of the message (only needed on writing)<br/>
	 * The length should include the byte for the message id
	 *
	 * @return integer
	 */
	int getLength();

	/**
	 * The id of this message
	 *
	 * @return id
	 */
	int getId();

	/**
	 * Sets the read duration of this message
	 *
	 * @param duration The duration it took to read this message
	 */
	void setReadDuration(Duration duration);

}
