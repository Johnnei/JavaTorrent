package torrent.frame;

import java.awt.Graphics;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.johnnei.utils.config.Config;

import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.download.peer.PeerDirection;
import torrent.frame.controls.TableBase;
import torrent.util.StringUtil;

public class TabPeers extends TableBase {

	public static final long serialVersionUID = 1L;
	private TorrentFrame torrentFrame;

	public TabPeers(TorrentFrame torrentFrame) {
		super(20);
		this.torrentFrame = torrentFrame;
	}

	protected void paintHeader(Graphics g) {
		g.drawString("IP", 5, getHeaderTextY());
		g.drawString("Client", 160, getHeaderTextY());
		g.drawString("Down Speed", 290, getHeaderTextY());
		g.drawString("Up Speed", 370, getHeaderTextY());
		g.drawString("Time idle", 440, getHeaderTextY());
		g.drawString("Pieces", 510, getHeaderTextY());
		g.drawString("Requests", 570, getHeaderTextY());
		g.drawString("Flags", 640, getHeaderTextY());
		g.drawString("State", 690, getHeaderTextY());
	}
	
	protected void paintData(Graphics g) {
		Torrent torrent = torrentFrame.getSelectedTorrent();
		
		if (torrent != null) {
			// Sort
			List<Peer> peers;
			synchronized (torrent) {
				peers = torrent.getPeers().stream().
						filter(p -> p.getBitTorrentSocket().getPassedHandshake() || Config.getConfig().getBoolean("general-show_all_peers")).
						collect(Collectors.toList());
			}
			Collections.sort(peers);
			setItemCount(peers.size());
			// Draw
			for (int i = peers.size() - 1; i >= 0; i--) {
				if (rowIsVisible()) {
					if (getSelectedIndex() == peers.size() - i - 1) {
						drawSelectedBackground(g);
					}
					Peer peer = peers.get(i);
					long duration = (System.currentTimeMillis() - peer.getLastActivity()) / 1000;
					g.drawString(peer.toString(), 5, getTextY());
					g.drawString(peer.getClientName(), 160, getTextY());
					g.drawString(StringUtil.compactByteSize(peer.getBitTorrentSocket().getDownloadRate()) + "/s", 290, getTextY());
					g.drawString(StringUtil.compactByteSize(peer.getBitTorrentSocket().getUploadRate()) + "/s", 370, getTextY());
					g.drawString(StringUtil.timeToString(duration), 440, getTextY());
					g.drawString("" + peer.countHavePieces(), 510, getTextY());
					g.drawString(peer.getWorkQueueSize(PeerDirection.Download) + "/" + peer.getRequestLimit() + " | " + peer.getWorkQueueSize(PeerDirection.Upload), 570, getTextY());
					g.drawString(peer.getFlags(), 640, getTextY());
					g.drawString(peer.getStatus(), 690, getTextY());
				}
				advanceLine();
			}
		}
	}
}
