package torrent.network.protocol.utp;

public class UtpClient {

	private short connectionId;
	
	public void setConnectionId(int connectionId) {
		this.connectionId = (short)connectionId;
	}
	
	public short getConnectionId() {
		return connectionId;
	}
}
