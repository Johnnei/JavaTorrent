package org.johnnei.javatorrent.protocol;

public class UTMetadata {

	/**
	 * The ID that this client expects for the UT_METADATA extension
	 */
	public static final int ID = 1;

	public static final String NAME = "ut_metadata";

	/**
	 * Requests a metadata piece<br/>
	 * dictionary value: "piece" => index
	 */
	public static final int REQUEST = 0;
	/**
	 * Data of a request piece<br/>
	 * dictionary value: "piece" => index<br/>
	 * x bytes piece data<br/>
	 * <br/>
	 * <i>x is 16KB or smaller incase of the last piece</i>
	 */
	public static final int DATA = 1;
	/**
	 * Rejects the send of a piece<br/>
	 * dictionary value: "piece" => index<br/>
	 */
	public static final int REJECT = 2;

}
