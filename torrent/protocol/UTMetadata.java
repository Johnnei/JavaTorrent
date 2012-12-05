package torrent.protocol;

public class UTMetadata {

	/**
	 * The ID that this client expects for the UT_METADATA extension
	 */
	public static final int EXTENDED_MESSAGE_UT_METADATA = 1;
	
	/**
	 * Requests a metadata piece<br/>
	 * dictionary value: "piece" => index
	 */
	public static final int UT_METADATA_REQUEST = 0;
	/**
	 * Data of a request piece<br/>
	 * dictionary value: "piece" => index<br/>
	 * x bytes piece data<br/>
	 * <br/>
	 * <i>x is 16KB or smaller incase of the last piece</i>
	 */
	public static final int UT_METADATA_DATA = 1;
	/**
	 * Rejects the send of a piece<br/>
	 * dictionary value: "piece" => index<br/>
	 */
	public static final int UT_METADATA_REJECT = 2;

}
