package torrent.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import torrent.download.files.HashedPiece;
import torrent.download.files.Piece;
import torrent.download.files.PieceInfo;
import torrent.encoding.Bencode;
import torrent.network.ByteInputStream;

public class TorrentFiles implements IDownloadable {

	/**
	 * The pieces downloaded by this torrent
	 */
	private HashedPiece[] pieces;
	/**
	 * The pieces which are still being downloaded
	 */
	private ArrayList<PieceInfo> undownloaded;
	private String folderName;
	/**
	 * The size of a standard block
	 */
	private int pieceSize;
	/**
	 * Contains all needed file info to download all files
	 */
	private FileInfo[] fileInfo;
	private long totalSize;

	public TorrentFiles(File torrentFile) {
		undownloaded = new ArrayList<PieceInfo>();
		parseTorrentFileData(torrentFile);
	}

	private void parseTorrentFileData(File torrentFile) {
		try {
			ByteInputStream in = new ByteInputStream(null, new FileInputStream(torrentFile));
			String data = in.readString(in.available());
			in.close();
			Bencode decoder = new Bencode(data);
			parseDictionary(decoder.decodeDictionary());
		} catch (IOException e) {
			Torrent.sleep(10);
			parseTorrentFileData(torrentFile);
		}
	}

	private void parseDictionary(HashMap<String, Object> dictionary) throws IOException {
		folderName = (String) dictionary.get("name");
		new File("./" + folderName + "/").mkdirs();

		pieceSize = (int) dictionary.get("piece length");
		long remainingSize = 0L;

		if (dictionary.containsKey("files")) { // Multi-file torrent
			ArrayList<?> files = (ArrayList<?>) dictionary.get("files");
			fileInfo = new FileInfo[files.size()];
			for (int i = 0; i < fileInfo.length; i++) {
				HashMap<?, ?> file = (HashMap<?, ?>) files.get(i);
				long fileSize = 0L;
				Object o = file.get("length");
				if (o instanceof Integer) {
					fileSize = (long) ((int) o);
				} else {
					fileSize = (long) o;
				}
				remainingSize += fileSize;
				ArrayList<?> fileStructure = (ArrayList<?>) file.get("path");
				String fileName = "";
				if (fileStructure.size() > 1) {
					for (int j = 0; j < fileStructure.size(); j++) {
						fileName += "/" + fileStructure.get(j);
					}
				} else {
					fileName = (String) fileStructure.get(0);
				}
				FileInfo info = new FileInfo(i, fileName, fileSize, getFile(fileName));
				fileInfo[i] = info;
			}
		} else { // Single file torrent

		}
		totalSize = remainingSize;
		String pieceHashes = (String) dictionary.get("pieces");
		int pieceAmount = pieceHashes.length() / 20;
		pieces = new HashedPiece[pieceAmount];
		for (int index = 0; index < pieceAmount; index++) {
			int hashOffset = index * 20;
			int size = (remainingSize >= pieceSize) ? pieceSize : (int) remainingSize;
			byte[] sha1Hash = pieceHashes.substring(hashOffset, hashOffset + 20).getBytes();
			pieces[index] = new HashedPiece(index, size, sha1Hash);
			undownloaded.add(new PieceInfo(index, size));
			remainingSize -= size;
		}
	}

	private synchronized void removeUndownload(int index) {
		for (int i = 0; i < undownloaded.size(); i++) {
			PieceInfo pi = undownloaded.get(i);
			if (pi.getIndex() == index) {
				undownloaded.remove(i);
				return;
			}
		}
	}

	@Override
	public boolean hasAllPieces() {
		return undownloaded.size() == 0;
	}

	public PieceInfo getInfo(int index) {
		return undownloaded.get(index);
	}

	@Override
	public synchronized void requestedPiece(int index) {
		if (pieces[index].isRequestedAll()) {
			removeUndownload(index);
		}
	}

	@Override
	public synchronized void requestedPiece(int index, boolean requested) {
	}

	@Override
	public synchronized void fillPiece(int index, int offset, byte[] data) {
		if (pieces[index].getData().length != pieces[index].getSize()) {
			pieces[index].resetBuffer();
		}
		pieces[index].fill(data, offset);
	}

	private File getFile(String name) {
		return new File("./" + folderName + "/" + name);
	}

	@Override
	public synchronized void fillPiece(int index, byte[] data) {
		fillPiece(index, 0, data);
	}

	@Override
	public synchronized HashedPiece getPiece(int index) {
		return pieces[index];
	}

	public Piece getPieceReadOnly(int index) {
		return pieces[index];
	}
	
	private FileInfo getFileInfoByPiece(Piece p) {
		long fileOffset = p.getIndex() * pieceSize;
		for (int i = 0; i < fileInfo.length; i++) {
			FileInfo info = fileInfo[i];
			totalSize += info.getSize();
			if (fileOffset < totalSize) {
				return info;
			}
		}
		return null;
	}

	@Override
	public synchronized void save(int index) throws IOException {
		getFileInfoByPiece(pieces[index]).getPieceWriter().saveChunk(pieces[index], pieceSize);
		long fileOffset = index * pieceSize;
		long totalSize = 0L;
		for (int i = 0; i < fileInfo.length; i++) {
			FileInfo info = fileInfo[i];
			totalSize += info.getSize();
			if (fileOffset < totalSize) {
				info.addPiece();
				return;
			}
		}
	}

	@Override
	public void save(File dic) {
	}

	public File getFolderName() {
		return new File(folderName);
	}

	public int getPieceSize() {
		return pieceSize;
	}

	/**
	 * Used to check if we are interested in the peer
	 * 
	 * @return
	 */
	public ArrayList<PieceInfo> getNeededPieces() {
		return undownloaded;
	}

	public int getPieceCount() {
		return pieces.length;
	}

	public long getTotalSize() {
		return totalSize;
	}

	public FileInfo[] getFiles() {
		return fileInfo;
	}

	public ArrayList<PieceInfo> getUndownloadedPieces() {
		return undownloaded;
	}

	public int getBlockIndexByOffset(int offset) {
		return offset / pieceSize;
	}

}
