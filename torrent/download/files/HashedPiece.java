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
	
	public boolean checkHash() throws TorrentException {
		byte[] pieceData = new byte[getSize()];
		int bytesCollected = 0;
		long pieceOffset = getIndex() * files.getPieceSize();
		while(bytesCollected < pieceData.length) {
			int blockIndex = bytesCollected / files.getBlockSize();
			int blockDataOffset = bytesCollected % files.getBlockSize();
			int bytesToRead = getSize() - bytesCollected;
			FileInfo file = files.getFileForBlock(getIndex(), blockIndex, blockDataOffset);
			if(file.getSize() < pieceOffset + bytesToRead) {
				bytesToRead = (int)(file.getSize() - pieceOffset);
			}
			synchronized (file.FILE_LOCK) {
				try {
					RandomAccessFile fileAccess = file.getFileAcces();
					fileAccess.seek(pieceOffset + bytesCollected); 
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
