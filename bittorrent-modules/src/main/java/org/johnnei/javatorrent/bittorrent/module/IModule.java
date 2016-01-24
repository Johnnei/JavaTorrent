package org.johnnei.javatorrent.bittorrent.module;

public interface IModule {

	/**
	 * Returns the number of the BEP to which this module is related.
	 * This is used to inform the user/developer for what extension this module is being used.
	 * @return The related BEP number
	 */
	public int getRelatedBep();

	/**
	 * Returns the extensions which must be present to allow this module to work at all.
	 * @return The list of required modules
	 */
	public Class<IModule> getDependsOn();

	/**
	 * Returns the list of reserved bits to enable to indicate that we support this extension.
	 * The bit numbers are represented in the following order: Right to left, starting at zero.
	 * For reference see BEP 10 which indicates that bit 20 must be enabled.
	 * @return The bits to enable
	 */
	public int[] getReservedBits();

}
