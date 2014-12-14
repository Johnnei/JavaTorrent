package torrent.protocol;

import torrent.network.InStream;

public interface IExtension {

	/**
	 * Calculates which message should be returned
	 * 
	 * @param data The stream which remained after the extension base
	 * @return The Message
	 */
	public IMessage getMessage(InStream data) throws Exception;

}
