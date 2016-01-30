package org.johnnei.javatorrent.torrent.network;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.johnnei.javatorrent.network.protocol.ISocket;

public class BitTorrentSocketException extends IOException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private Collection<String> connectionTypeErrors;

	public BitTorrentSocketException(String message, Throwable cause) {
		super(message, cause);
		connectionTypeErrors = new LinkedList<>();
	}

	public BitTorrentSocketException(String message) {
		this(message, null);
	}

	public void addConnectionFailure(ISocket socketType, IOException cause) {
		connectionTypeErrors.add(String.format("%s: %s", socketType.getClass().getSimpleName(), cause.getMessage()));
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder();

		sb.append(super.getMessage());
		sb.append(" Connection Stack: ");
		for (String s : connectionTypeErrors) {
			sb.append(s);
		}

		return sb.toString();
	}

}
