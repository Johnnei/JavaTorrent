package torrent.util;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger extends PrintStream {

	private DateFormat dateFormat = new SimpleDateFormat("dd/MM kk:mm:ss");
	private Date cachedDate = new Date();
	private long cacheTime = System.currentTimeMillis();
	private boolean hadBreak = true;

	public Logger(PrintStream out) {
		super(out);
	}

	@Override
	public void println(String str) {
		doPrint(str + "\r\n");
		hadBreak = true;
	}

	@Override
	public void print(String str) {
		doPrint(str);
		hadBreak = false;
	}

	public void doPrint(String str) {
		if (hadBreak)
			super.print(getPrefix() + " " + str);
		else
			super.print(str);
	}

	private String getPrefix() {
		if (System.currentTimeMillis() - cacheTime > 1000) {
			cachedDate = new Date();
			cacheTime = System.currentTimeMillis();
		}
		return dateFormat.format(cachedDate);
	}
}