package torrent.protocol;

import torrent.network.Stream;

public interface IExtension {

	/**
	 * Calculates which message should be returned
	 * 
	 * @param data The stream which remained after the extension base
	 * @return The Message
	 */
	public IMessage getMessage(Stream data) throws Exception;

}
