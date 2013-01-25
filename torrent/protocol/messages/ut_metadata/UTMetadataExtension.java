package torrent.protocol.messages.ut_metadata;

import java.io.InvalidObjectException;
import java.util.HashMap;

import torrent.encoding.Bencode;
import torrent.network.Stream;
import torrent.protocol.IExtension;
import torrent.protocol.IMessage;
import torrent.protocol.UTMetadata;

public class UTMetadataExtension implements IExtension {

	@Override
	public IMessage getMessage(Stream inStream) throws InvalidObjectException {
		int moveBackLength = inStream.available();

		Bencode decoder = new Bencode(inStream.readString(inStream.available()));
		HashMap<String, Object> dictionary = (HashMap<String, Object>) decoder.decodeDictionary();
		int id = (int) dictionary.get("msg_type");
		IMessage message;
		switch (id) {
		case UTMetadata.DATA:
			message = new MessageData();
			break;

		case UTMetadata.REJECT:
			message = new MessageReject();
			break;

		case UTMetadata.REQUEST:
			message = new MessageRequest();
			break;

		default:
			message = null;
			break;
		}

		inStream.moveBack(moveBackLength);
		return message;
	}

}
