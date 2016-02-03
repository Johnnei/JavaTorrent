package org.johnnei.javatorrent.tracker.udp;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.tracker.TrackerAction;
import org.johnnei.javatorrent.torrent.tracker.TrackerException;
import org.johnnei.javatorrent.tracker.UdpTracker;

public interface IUdpTrackerPayload {

	/**
	 * Writes the message to the output stream
	 *
	 * @param outStream
	 */
	public void writeRequest(OutStream outStream);

	/***
	 * Read a message from the inputStream
	 *
	 * @param inStream
	 */
	public void readResponse(InStream inStream) throws TrackerException;

	/**
	 * Process the message
	 *
	 * @param tracker The tracker for which this packet has to be processed
	 */
	public void process(UdpTracker tracker);

	/**
	 * Gets the expected response action
	 * @return The tracker action
	 */
	public TrackerAction getAction();

	/**
	 * Gets the amount of bytes which the payload must at least be
	 * @return The minimal payload size
	 */
	public int getMinimalSize();

}
