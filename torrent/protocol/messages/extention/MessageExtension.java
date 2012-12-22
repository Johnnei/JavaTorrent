package torrent.protocol.messages.extention;

import torrent.download.peer.Peer;
import torrent.network.Stream;
import torrent.protocol.BitTorrent;
import torrent.protocol.ExtensionUtils;
import torrent.protocol.IExtension;
import torrent.protocol.IMessage;

public class MessageExtension implements IMessage {
	
	private int extensionId;
	private IMessage message;
	
	public MessageExtension(int extensionId, IMessage message) {
		this.extensionId = extensionId;
		this.message = message;
	}
	
	public MessageExtension() {
		
	}

	@Override
	public void write(Stream outStream) {
		outStream.writeByte(extensionId);
		message.write(outStream);
	}

	@Override
	public void read(Stream inStream) {
		extensionId = inStream.readByte();
		if(extensionId == BitTorrent.EXTENDED_MESSAGE_HANDSHAKE) {
			message = new MessageHandshake();
		} else {
			IExtension extension = ExtensionUtils.getUtils().getExtension(extensionId);
			try {
				message = extension.getMessage(inStream);
			} catch (Exception e) {
				return;
			}
		}
		message.read(inStream);
	}

	@Override
	public void process(Peer peer) {
		if (message == null) {
			peer.log("Extended Message Error (ID: " + extensionId + ")", true);
			peer.close();
		}
		message.process(peer);
	}

	@Override
	public int getLength() {
		return 2 + message.getLength();
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_EXTENDED_MESSAGE;
	}

}
