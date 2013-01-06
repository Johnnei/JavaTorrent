package torrent.download;

import java.io.File;
import java.io.IOException;

import torrent.download.files.Piece;

public interface IDownloadable {

	/**
	 * Checks if all pieces are available and downloaded
	 * 
	 * @return
	 */
	public boolean hasAllPieces();

	public void fillPiece(int index, int offset, byte[] data);

	public void fillPiece(int index, byte[] data);

	public Piece getPiece(int index);

	public void save(int index) throws IOException;

	public void save(File file);

}
