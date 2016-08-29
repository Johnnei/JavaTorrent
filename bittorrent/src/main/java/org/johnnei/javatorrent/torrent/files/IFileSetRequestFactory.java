package org.johnnei.javatorrent.torrent.files;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.torrent.peer.Peer;

/**
 * The interface which defines methods to construct {@link IMessage} to invoke requests for {@link Piece}
 */
public interface IFileSetRequestFactory {

	/**
	 * Creates a request to download the given piece block.
	 *
	 * @param peer The peer for which this request is being constructed.
	 * @param piece The piece to request.
	 * @param byteOffset The offset within the piece.
	 * @param length The length of the block to request.
	 * @return The {@link IMessage} which will request the block.
	 * @see #createCancelRequestFor(Piece, int, int)
	 */
	IMessage createRequestFor(Peer peer, Piece piece, int byteOffset, int length);

	/**
	 * Creates a cancel {@link IMessage} for a earlier send out request.
	 * @param piece The piece to cancel the request for.
	 * @param byteOffset The offset within the piece.
	 * @param length The length of the block which was requested.
	 * @return The {@link IMessage} to cancel the request, or <code>null</code> if {@link #supportsCancellation()} returns <code>false</code>.
	 * @see #createRequestFor(Peer, Piece, int, int)
	 */
	IMessage createCancelRequestFor(Piece piece, int byteOffset, int length);

	/**
	 * @return Returns <code>true</code> when a request created by {@link #createRequestFor(Peer, Piece, int, int)} can be cancelled.
	 * @see #createCancelRequestFor(Piece, int, int)
	 */
	boolean supportsCancellation();
}
