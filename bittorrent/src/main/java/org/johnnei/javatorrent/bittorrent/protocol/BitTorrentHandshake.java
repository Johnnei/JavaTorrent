package org.johnnei.javatorrent.bittorrent.protocol;

public class BitTorrentHandshake {
	
	/**
	 * The SHA-1 hash of the torrent
	 */
	private byte[] torrentHash;
	
	/**
	 * The bytes which define which extension the peer supports
	 */
	private byte[] extensionBytes;
	
	/**
	 * The peer ID
	 */
	private byte[] peerId;
	
	public BitTorrentHandshake(byte[] torrentHash, byte[] extensionBytes, byte[] peerId) {
		this.torrentHash = torrentHash;
		this.extensionBytes = extensionBytes;
		this.peerId = peerId;
	}
	
	public byte[] getPeerId() {
		return peerId;
	}

	public byte[] getTorrentHash() {
		return torrentHash;
	}
	
	public byte[] getPeerExtensionBytes() {
		return extensionBytes;
	}
}
