package torrent.frame.controls;

import java.util.Observable;

public class ObservablePanel extends Observable {

	@Override
	public void notifyObservers(Object arg) {
		setChanged();
		super.notifyObservers(arg);
	}

}
