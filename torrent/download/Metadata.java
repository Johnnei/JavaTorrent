package torrent.download;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import torrent.download.files.Piece;
import torrent.encoding.SHA1;

public class Metadata implements IDownloadable {

	private Piece[] pieces;
	private int filesize;

	public synchronized void setFilesize(int size) {
		if (pieces != null)
			return;
		int pieceCount = size / 16384;
		int lastSize = 16384;
		if (size % 16384 != 0) {
			pieceCount++;
			lastSize = size % 16384;
		}
		pieces = new Piece[pieceCount];
		for (int i = 0; i < pieceCount - 1; i++) {
			pieces[i] = new Piece(i, 16384);
		}
		pieces[pieces.length - 1] = new Piece(pieces.length - 1, lastSize);
		filesize = size;
	}

	@Override
	public boolean hasAllPieces() {
		if (pieces == null)
			return false;
		for (int i = 0; i < pieces.length; i++) {
			if (!pieces[i].hasData())
				return false;
		}
		return true;
	}

	@Override
	public void fillPiece(int index, int offset, byte[] data) {

	}

	@Override
	public synchronized void fillPiece(int index, byte[] data) {
		if (!pieces[index].fill(data))
			pieces[index].setRequested(false);
	}

	public int getNextPieceIndex() {
		for (int i = 0; i < pieces.length; i++) {
			if (!pieces[i].hasData() && !pieces[i].isRequested())
				return i;
		}
		return -1;
	}

	@Override
	public void save(File file) {
		try {
			DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
			for (int i = 0; i < pieces.length; i++) {
				out.write(pieces[i].getData());
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			Torrent.sleep(10);
			save(file);
		}
	}

	public boolean checkHash(byte[] hash) {
		byte[] buffer = new byte[filesize];
		int offset = 0;
		for (int i = 0; i < pieces.length; i++) {
			byte[] data = pieces[i].getData();
			for (int j = 0; j < data.length; j++) {
				buffer[offset++] = data[j];
			}
		}
		return SHA1.match(SHA1.hash(buffer), hash);
	}

	public boolean hasMetainfo() {
		return filesize > 0;
	}

	public int getPieceSize(int index) {
		return pieces[index].getSize();
	}

	@Override
	public Piece getPiece(int index) {
		return pieces[index];
	}

	public int getTotalSize() {
		return filesize;
	}

	public void clear() {
		pieces = null;
		setFilesize(filesize);
	}

	@Override
	public void save(int index) throws IOException {
	}

	public int getPieceCount() {
		if (pieces == null)
			return 0;
		return pieces.length;
	}
	
	public long getRemainingBytes() {
		if (pieces == null)
			return 16384;
		long left = 0;
		for(int i = 0; i < pieces.length; i++) {
			left += pieces[i].getRemainingBytes();
		}
		return left;
	}

}
