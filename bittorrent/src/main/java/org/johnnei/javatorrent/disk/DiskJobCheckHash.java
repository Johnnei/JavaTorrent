package org.johnnei.javatorrent.disk;

import java.io.IOException;
import java.util.function.Consumer;

import org.johnnei.javatorrent.internal.disk.DiskJobPriority;
import org.johnnei.javatorrent.torrent.files.Piece;

/**
 * A job to check the hash of a piece for a given torrent
 *
 * @author Johnnei
 *
 */
public class DiskJobCheckHash implements IDiskJob {

	/**
	 * The piece to check the has for
	 */
	private final Piece piece;

	private final Consumer<DiskJobCheckHash> callback;

	private boolean matchingHash;

	public DiskJobCheckHash(Piece piece, Consumer<DiskJobCheckHash> callback) {
		this.callback = callback;
		this.piece = piece;
	}

	@Override
	public void process() throws IOException {
		matchingHash = piece.checkHash();
		callback.accept(this);
	}

	@Override
	public int getPriority() {
		return DiskJobPriority.LOCAL_ACTION.getPriority();
	}

	/**
	 * This method returns the result of the {@link #process()} call.
	 * @return <code>true</code> when the hash verification passed, otherwise <code>false</code>.
	 *
	 * @see #process()
	 */
	public boolean isMatchingHash() {
		return matchingHash;
	}

	/**
	 * Gets the piece for which the hash has been verified.
	 * @return The piece which is affected by this job.
	 */
	public Piece getPiece() {
		return piece;
	}

}
