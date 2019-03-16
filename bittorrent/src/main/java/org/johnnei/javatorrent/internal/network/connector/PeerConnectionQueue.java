package org.johnnei.javatorrent.internal.network.connector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class PeerConnectionQueue {

	/**
	 * Queue mapping torrent hash to the queue
	 */
	private final Map<String, Queue<PeerConnectionState>> queue;

	private int count;

	public PeerConnectionQueue() {
		this.queue = new HashMap<>();
	}

	public void add(PeerConnectionState peerConnectionState) {
		Queue<PeerConnectionState> torrentQueue;
		synchronized (queue) {
			torrentQueue = queue.computeIfAbsent(
				peerConnectionState.getPeer().getTorrent().getMetadata().getHashString(),
				value -> new ConcurrentLinkedQueue<>()
			);
		}
		torrentQueue.add(peerConnectionState);
	}

	public synchronized PeerConnectionState poll() {
		count++;
		if (count < 0) {
			count = 0;
		}

		List<Queue<PeerConnectionState>> nonEmptyEntries = queue.entrySet().stream()
			.filter(entry -> !entry.getValue().isEmpty())
			.map(Map.Entry::getValue)
			.collect(Collectors.toList());

		if (nonEmptyEntries.isEmpty()) {
			return null;
		}

		return nonEmptyEntries.get(count % nonEmptyEntries.size()).poll();
	}

}
