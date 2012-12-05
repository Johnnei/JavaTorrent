package torrent.download.files;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class PieceWriter {

	/**
	 * The file to write the bytes in
	 */
	private RandomAccessFile outputFile;
	/**
	 * The output file
	 */
	private File file;

	public PieceWriter(File output) {
		this.file = output;
	}
	
	private void open() throws IOException {
		if(outputFile != null)
			return;
		if(!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}
		outputFile = new RandomAccessFile(file, "rw");
	}
	
	public void reserveDiskspace(long size) throws IOException {
		open();
		outputFile.setLength(size);
		close();
	}

	public void saveChunk(Piece p, int defaultPieceSize) throws IOException {
		open();
		close();
	}

	private void close() throws IOException {
		if(outputFile != null) {
			outputFile.close();
			outputFile = null;
		}
	}

}
