package org.johnnei.javatorrent.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


/**
 * A Formatter which is used for the {@link Logger} instances to log data to the console.
 * @author Johnnei
 *
 * @deprecated Use SLF4J instead
 */
@Deprecated
public class ConsoleLogger extends Formatter {

	/**
	 * Creates a logger based on the name.<br/>
	 * The logger will only output if the logged message is at least the given minLevel.<br/>
	 * As second request of the same logger will NOT update the minLevel
	 * @param name The name of the logger
	 * @param minLevel The minimum level to log
	 * @return A logger (possibly re-used) with the given name
	 */
	public static Logger createLogger(String name, Level minLevel) {
		Logger logger = Logger.getLogger(name);

		if (logger.getHandlers().length == 0) {
			// This logger has no handlers/formatters, Create the handler
			logger.setUseParentHandlers(false);
			logger.setLevel(minLevel);
			ConsoleHandler consoleHandler = new ConsoleHandler();
			consoleHandler.setFormatter(new ConsoleLogger());
			consoleHandler.setLevel(minLevel);
			logger.addHandler(consoleHandler);
		}
		return logger;
	}

	/**
	 * Defines the format of the console output
	 */
	@Override
	public String format(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		sb.append(generateDateTimeStamp());
		sb.append(" [");
		sb.append(record.getLevel());
		sb.append("] [");
		sb.append(record.getLoggerName());
		sb.append("] ");
		sb.append(record.getMessage());
		sb.append(System.lineSeparator());

		if (record.getThrown() != null) {
			sb.append("Stacktrace:" + System.lineSeparator());
			for (StackTraceElement stackTrace : record.getThrown().getStackTrace()) {
				sb.append(stackTrace.toString());
				sb.append(System.lineSeparator());
			}
		}

		return sb.toString();
	}

	/**
	 * Generates a datetime-stamp based on the current time
	 * @return
	 */
	private String generateDateTimeStamp() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		Date resultdate = new Date(System.currentTimeMillis());
		return dateFormat.format(resultdate);
	}

}

