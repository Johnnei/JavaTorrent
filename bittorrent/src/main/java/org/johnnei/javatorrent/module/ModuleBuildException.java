package org.johnnei.javatorrent.module;

import org.johnnei.javatorrent.TorrentClient;

/**
 * An exception which gets thrown on the {@link IModule#onBuild(TorrentClient)} action when it can complete successfully.
 */
public class ModuleBuildException extends Exception {

	public ModuleBuildException(String message, Throwable cause) {
		super(message, cause);
	}

	public ModuleBuildException(String message) {
		super(message);
	}
}
