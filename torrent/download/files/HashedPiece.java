package torrent.download.files;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.johnnei.utils.ThreadUtils;

import torrent.TorrentException;
import torrent.download.FileInfo;
import torrent.download.Files;
import torrent.encoding.SHA1;

public class HashedPiece extends Piece {

	private byte[] shaHash;
	
	public HashedPiece(byte[] shaHash, Files files, int index, int pieceSize, int blockSize) {
		super(files, index, pieceSize, blockSize);
		this.shaHash = shaHash;
	}
	
	/**
	 * Checks if the received bytes hash matches with the hash which was given in the metadata
	 * @return hashMatched ? true : false
	 * @throws TorrentException If the piece is not within any of the files in this torrent (Shouldn't occur)
	 */
	public boolean checkHash() throws TorrentException {
		byte[] pieceData = new byte[getSize()];
		int bytesCollected = 0;
		long pieceOffset = getIndex() * files.getPieceSize();
		while(bytesCollected < pieceData.length) {
			int blockIndex = bytesCollected / files.getBlockSize();
			int blockOffset = blockIndex * files.getBlockSize();
			int blockDataOffset = bytesCollected % files.getBlockSize();
			int bytesToRead = getSize() - bytesCollected;
			FileInfo file = files.getFileForBlock(getIndex(), blockIndex, blockDataOffset);
			long offsetInFile = pieceOffset + blockOffset + bytesCollected - file.getFirstByteOffset();
			if(file.getSize() < offsetInFile + bytesToRead) {
				bytesToRead = (int)(file.getSize() - pieceOffset);
			}
			synchronized (file.FILE_LOCK) {
				try {
					RandomAccessFile fileAccess = file.getFileAcces();
					fileAccess.seek(offsetInFile); 
					int read = fileAccess.read(pieceData, bytesCollected, bytesToRead);
					if(read >= 0)
						bytesCollected += read;
				} catch (IOException e) {
					ThreadUtils.sleep(10);
					return checkHash();
				}
			}
		}
		return SHA1.match(shaHash, SHA1.hash(pieceData));
	}
}
