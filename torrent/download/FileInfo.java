package torrent.download;

import java.io.File;

import torrent.download.files.PieceWriter;

public class FileInfo {

	private String filename;
	private long filesize;
	private int index;
	private int pieceHaveCount;
	/**
	 * The link between the file on the harddrive
	 */
	private PieceWriter pieceWriter;

	public FileInfo(int index, String filename, long filesize, File file) {
		this.index = index;
		this.filename = filename;
		this.filesize = filesize;
		pieceHaveCount = 0;
		pieceWriter = new PieceWriter(file);
	}

	public void addPiece() {
		pieceHaveCount++;
	}

	public int getIndex() {
		return index;
	}

	public long getSize() {
		return filesize;
	}

	public int getPieceHaveCount() {
		return pieceHaveCount;
	}

	public String getFilename() {
		return filename;
	}

	public PieceWriter getPieceWriter() {
		return pieceWriter;
	}

}
