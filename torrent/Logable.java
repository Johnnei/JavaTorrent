package torrent;

@Deprecated
public interface Logable {

	public void log(String s);

	public void log(String s, boolean isError);

	public String getStatus();

}
