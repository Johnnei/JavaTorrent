package org.johnnei.javatorrent.torrent.files;

/**
 * The statuses which a {@link Block} can have which can be aggregated to create a status for a {@link Piece}
 */
public enum BlockStatus {

	/**
	 * This status indicates that we still need this block and have not yet assigned it to a peer.
	 */
	Needed,
	/**
	 * This status indicates that we still need this block and have requested it from a peer.
	 */
	Requested,
	/**
	 * This status indicates that we received and stored the bytes of this piece but the {@link Piece} as an
	 * entirety needs to be on this status before we can verify that the received information is correct.
	 */
	Stored,
	/**
	 * The block information we have is verified to match the expected data.
	 */
	Verified;

}
