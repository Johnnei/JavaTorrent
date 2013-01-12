package torrent.protocol.messages.extention;

import java.util.HashMap;

import torrent.JavaTorrent;
import torrent.download.peer.Peer;
import torrent.encoding.Bencode;
import torrent.encoding.Bencoder;
import torrent.network.Stream;
import torrent.protocol.BitTorrent;
import torrent.protocol.IMessage;
import torrent.protocol.UTMetadata;

public class MessageHandshake implements IMessage {
	
	private String bencodedHandshake;
	
	public MessageHandshake() {
		Bencoder encoder = new Bencoder();
		encoder.dictionaryStart();
		encoder.string("m");
		encoder.dictionaryStart();
		encoder.string("ut_metadata");
		encoder.integer(UTMetadata.ID);
		encoder.dictionaryEnd();
		encoder.string("v");
		encoder.string(JavaTorrent.BUILD);
		encoder.dictionaryEnd();
		bencodedHandshake = encoder.getBencodedData();
	}

	@Override
	public void write(Stream outStream) {
		outStream.writeString(bencodedHandshake);
	}

	@Override
	public void read(Stream inStream) {
		bencodedHandshake = inStream.readString(inStream.available());
	}

	@Override
	public void process(Peer peer) {
		Bencode decoder = new Bencode(bencodedHandshake);
		try {
			HashMap<String, Object> dictionary = (HashMap<String, Object>)decoder.decodeDictionary();
			Object m = dictionary.get("m");
			if(m != null) {
				if(m instanceof HashMap<?, ?>) {
					HashMap<?, ?> extensionData = (HashMap<?, ?>)m;
					if(extensionData.containsKey(UTMetadata.NAME)) {
						peer.getClient().addExtentionID(UTMetadata.NAME, (Integer)extensionData.get(UTMetadata.NAME));
						if(dictionary.containsKey("metadata_size")) {
							peer.getTorrent().getFiles().setFilesize(peer.getTorrent().getHashArray(), (int)dictionary.get("metadata_size"));
						}
					}
				}
			}
			Object reqq = dictionary.get("reqq");
			if(reqq != null) {
				peer.getClient().setMaxRequests((int)reqq);
			}
		} catch (Exception e) {
			peer.log("Extension handshake error: " + e.getMessage());
			peer.close();
		}
	}

	@Override
	public int getLength() {
		return bencodedHandshake.length();
	}

	@Override
	public int getId() {
		return BitTorrent.EXTENDED_MESSAGE_HANDSHAKE;
	}
	
	@Override
	public void setReadDuration(int duration) {
	}
	
	@Override
	public String toString() {
		return "Handshake";
	}

}
