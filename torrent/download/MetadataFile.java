package torrent.download;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.johnnei.utils.JMath;
import org.johnnei.utils.config.Config;

import torrent.TorrentException;
import torrent.download.files.Piece;

public class MetadataFile extends AFiles {
	
	public static final int BLOCK_SIZE = 16384;
	
	private FileInfo metadata;
	
	/**
	 * The size of the metadata file in total
	 */
	private int fileSize;
	
	public MetadataFile(Torrent torrent, int fileSize) {
		metadata = new FileInfo(fileSize, 0, Config.getConfig().getTorrentFileFor(torrent), JMath.ceilDivision(fileSize, BLOCK_SIZE));
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
		return metadata;
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
	
	/*
	 * /**
	 * Gets a block from the metadata file
	 * 
	 * @param piece The block
	 * @return The 16384 bytes needed to answer the request
		public byte[] getMetadataBlock(int piece) {
			long blockSize = 16384L;
			byte[] data = new byte[(int) blockSize];
			long blockOffset = piece * blockSize;
			synchronized (metadata.FILE_LOCK) {
				RandomAccessFile fileAccess = metadata.getFileAcces();
				try {
					if (fileAccess.length() < blockOffset + data.length)
						data = new byte[(int) (fileAccess.length() - blockOffset)];
					int bytesRead = 0;
					while (bytesRead < data.length) {
						fileAccess.seek(piece * blockSize + bytesRead);
						int read = fileAccess.read(data, piece * (int) blockSize + bytesRead, data.length - bytesRead);
						if(read >= 0) {
							bytesRead += read;
						}
					}
				} catch (IOException e) {
				}
			}
			return data;
		}
	 */

}
