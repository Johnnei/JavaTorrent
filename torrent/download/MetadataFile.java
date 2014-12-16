package torrent.download;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.johnnei.utils.JMath;
import org.johnnei.utils.config.Config;

import torrent.TorrentException;
import torrent.download.files.Piece;

public class MetadataFile extends AFiles {
	
	public static final int BLOCK_SIZE = 16384;
	
	/**
	 * The size of the metadata file in total
	 */
	private int fileSize;
	
	public MetadataFile(Torrent torrent, int fileSize) {
		fileInfos = new ArrayList<>();
		fileInfos.add(new FileInfo(fileSize, 0, Config.getConfig().getTorrentFileFor(torrent), JMath.ceilDivision(fileSize, BLOCK_SIZE)));
		this.fileSize = fileSize;
		pieces = new ArrayList<>(1);
		pieces.add(new Piece(this, torrent.getHashArray(), 0, fileSize, BLOCK_SIZE));
	}

	@Override
	public boolean hasPiece(int pieceIndex) throws NoSuchElementException {
		return pieces.get(pieceIndex).isDone();
	}

	@Override
	public void havePiece(int pieceIndex) throws NoSuchElementException {
		for (int i = 0; i < pieces.get(pieceIndex).getBlockCount(); i++) {
			pieces.get(pieceIndex).setDone(i);
		}
	}
	
	@Override
	public FileInfo getFileForBlock(int index, int blockIndex, int blockDataOffset) throws TorrentException {
		return fileInfos.get(0);
	}

	@Override
	public long getPieceSize() {
		return fileSize;
	}

	@Override
	public int getBlockSize() {
		return BLOCK_SIZE;
	}

	@Override
	public byte[] getBitfieldBytes() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("UT_METADATA does not support bitfields.");
	}
	
	 /**
	 * Gets a block from the metadata file
	 * 
	 * @param piece The block
	 * @return The 16384 (or less if it is the last block) bytes needed to answer the request
	 * @throws IOException When reading the file fails for whatever reason
	 */
	public byte[] getBlock(int piece) throws IOException {
		int blockOffset = piece * BLOCK_SIZE;
		synchronized (fileInfos.get(0).FILE_LOCK) {
			RandomAccessFile fileAccess = fileInfos.get(0).getFileAcces();
			int blockSize = Math.min(BLOCK_SIZE, (int)(fileAccess.length() - blockOffset));
			byte[] data = new byte[blockSize];
			fileAccess.seek(blockOffset);
			fileAccess.readFully(data);
			return data;
		}
	}
}
