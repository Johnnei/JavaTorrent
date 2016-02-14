package org.johnnei.javatorrent.bittorrent.phases;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.algos.IDownloadPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhaseRegulator {

	private static final Logger LOGGER = LoggerFactory.getLogger(PhaseRegulator.class);

	private Class<? extends IDownloadPhase> initialPhase;

	private Map<Class<? extends IDownloadPhase>, Class<? extends IDownloadPhase>> downloadPhasesOrder;

	private Map<Class<? extends IDownloadPhase>, BiFunction<TorrentClient, Torrent, ? extends IDownloadPhase>> phaseSupplier;

	private PhaseRegulator(Builder builder) {
		initialPhase = builder.initialPhase;
		downloadPhasesOrder = builder.downloadPhasesOrder;
		phaseSupplier = builder.phaseSuppliers;
	}

	public IDownloadPhase createInitialPhase(TorrentClient torrentClient, Torrent torrent) {
		return phaseSupplier.get(initialPhase).apply(torrentClient, torrent);
	}

	public Optional<IDownloadPhase> createNextPhase(IDownloadPhase phase, TorrentClient torrentClient, Torrent torrent) {
		if (!downloadPhasesOrder.containsKey(phase.getClass())) {
			return Optional.empty();
		}

		return Optional.of(phaseSupplier.get(downloadPhasesOrder.get(phase.getClass())).apply(torrentClient, torrent));
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder("PhaseRegulator[");
		Class<? extends IDownloadPhase> type = initialPhase;
		while (type != null) {
			stringBuilder.append(type.getSimpleName());

			type = downloadPhasesOrder.get(type);

			if (type != null) {
				stringBuilder.append(" -> ");
			}
		}
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	public static class Builder {

		private Class<? extends IDownloadPhase> initialPhase;

		private Map<Class<? extends IDownloadPhase>, Class<? extends IDownloadPhase>> downloadPhasesOrder;

		private Map<Class<? extends IDownloadPhase>, BiFunction<TorrentClient, Torrent, ? extends IDownloadPhase>> phaseSuppliers;

		public Builder() {
			downloadPhasesOrder = new HashMap<>();
			phaseSuppliers = new HashMap<>();
		}

		public <T extends IDownloadPhase> Builder registerInitialPhase(
				Class<T> phase, BiFunction<TorrentClient, Torrent, T> phaseSupplier, Optional<Class<? extends IDownloadPhase>> nextPhase) {
			if (initialPhase != null) {
				LOGGER.warn(String.format("Overriding initial download phase from %s to %s", initialPhase.getSimpleName(), phase.getSimpleName()));
			}

			initialPhase = phase;
			registerPhase(phase, phaseSupplier, nextPhase);
			return this;
		}

		public <T extends IDownloadPhase> Builder registerPhase(
				Class<T> phase, BiFunction<TorrentClient, Torrent, T> phaseSupplier, Optional<Class<? extends IDownloadPhase>> nextPhase) {
			Objects.requireNonNull(phase, "Phase is required.");
			Objects.requireNonNull(phaseSupplier, "Phase supplier is required.");
			Objects.requireNonNull(nextPhase, "Next phase should be Optional.empty() when not applicable.");

			if (phaseSuppliers.containsKey(phase)) {
				throw new IllegalStateException(String.format("Phase %s is already mapped", phase.getSimpleName()));
			}

			if (nextPhase.isPresent()) {
				downloadPhasesOrder.put(phase, nextPhase.get());
			}

			this.phaseSuppliers.put(phase, phaseSupplier);
			return this;
		}

		public PhaseRegulator build() {
			if (initialPhase == null) {
				throw new IllegalStateException("No initial state has been configured.");
			}

			verifyPhaseChain();
			return new PhaseRegulator(this);
		}

		private void verifyPhaseChain() {
			int phasesSeenCount = 0;
			Class<? extends IDownloadPhase> phase = initialPhase;

			while (phase != null) {
				phasesSeenCount++;
				phase = downloadPhasesOrder.get(phase);
			}

			if (phasesSeenCount != phaseSuppliers.size()) {
				LOGGER.warn(String.format(
					"Download phases chain does not contain all registered types. Registered: %d, in chain: %d.",
					phaseSuppliers.size(),
					phasesSeenCount));
			}
		}
	}

}
