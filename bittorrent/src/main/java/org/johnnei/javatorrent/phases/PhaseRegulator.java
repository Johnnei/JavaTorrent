package org.johnnei.javatorrent.phases;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.Torrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The system which is being used to configure the transitions between {@link IDownloadPhase}s.
 */
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

	/**
	 * Creates an instance of the initial {@link IDownloadPhase} for the given {@link Torrent}.
	 * @param torrentClient The client on which the torrent is registered
	 * @param torrent The torrent for which the phase is created.
	 * @return The newly created {@link IDownloadPhase} instance.
	 */
	public IDownloadPhase createInitialPhase(TorrentClient torrentClient, Torrent torrent) {
		return phaseSupplier.get(initialPhase).apply(torrentClient, torrent);
	}


	/**
	 * Creates an instance of the next {@link IDownloadPhase} for the given {@link Torrent}.
	 * @param phase The phase which has been completed.
	 * @param torrentClient The client on which the torrent is registered
	 * @param torrent The torrent for which the phase is created.
	 * @return The newly created {@link IDownloadPhase} instance.
	 */
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

	/**
	 * A builder-pattern styled class to created the {@link PhaseRegulator} configuration.
	 */
	public static class Builder {

		private Class<? extends IDownloadPhase> initialPhase;

		private Map<Class<? extends IDownloadPhase>, Class<? extends IDownloadPhase>> downloadPhasesOrder;

		private Map<Class<? extends IDownloadPhase>, BiFunction<TorrentClient, Torrent, ? extends IDownloadPhase>> phaseSuppliers;

		/**
		 * Creates a new {@link Builder} instance without any {@link IDownloadPhase} configured.
		 */
		public Builder() {
			downloadPhasesOrder = new HashMap<>();
			phaseSuppliers = new HashMap<>();
		}

		/**
		 * Registers the initial {@link IDownloadPhase}
		 * @param phase The initial phase
		 * @param phaseSupplier The {@link BiFunction} which is capable of creating phase instances.
		 * @param <T> The type of the {@link IDownloadPhase}
		 * @return This {@link Builder} with updated configuration.
		 */
		public <T extends IDownloadPhase> Builder registerInitialPhase(Class<T> phase, BiFunction<TorrentClient, Torrent, T> phaseSupplier) {
			return registerInitialPhase(phase, phaseSupplier, null);
		}

		/**
		 * Registers the initial {@link IDownloadPhase}
		 * @param phase The initial phase
		 * @param phaseSupplier The {@link BiFunction} which is capable of creating phase instances.
		 * @param nextPhase The phase to transition to when <code>phase</code> has completed.
		 * @param <T> The type of the {@link IDownloadPhase}
		 * @return This {@link Builder} with updated configuration.
		 */
		public <T extends IDownloadPhase> Builder registerInitialPhase(
				Class<T> phase, BiFunction<TorrentClient, Torrent, T> phaseSupplier, Class<? extends IDownloadPhase> nextPhase) {
			if (initialPhase != null) {
				LOGGER.warn(String.format("Overriding initial download phase from %s to %s", initialPhase.getSimpleName(), phase.getSimpleName()));
			}

			initialPhase = phase;
			registerPhase(phase, phaseSupplier, nextPhase);
			return this;
		}

		/**
		 * Registers a new transition to the given {@link IDownloadPhase}
		 * @param phase The phase from which the transition happens.
		 * @param phaseSupplier The {@link BiFunction} which is capable of creating phase instances.
		 * @param <T> The type of the {@link IDownloadPhase}
		 * @return This {@link Builder} with updated configuration.
		 */
		public <T extends IDownloadPhase> Builder registerPhase(Class<T> phase, BiFunction<TorrentClient, Torrent, T> phaseSupplier) {
			return registerPhase(phase, phaseSupplier, null);
		}

		/**
		 * Registers a new transition to the given {@link IDownloadPhase}
		 * @param phase The phase from which the transition happens.
		 * @param phaseSupplier The {@link BiFunction} which is capable of creating phase instances.
		 * @param nextPhase The phase to transition to when <code>phase</code> has completed.
		 * @param <T> The type of the {@link IDownloadPhase}
		 * @return This {@link Builder} with updated configuration.
		 */
		public <T extends IDownloadPhase> Builder registerPhase(
				Class<T> phase, BiFunction<TorrentClient, Torrent, T> phaseSupplier, Class<? extends IDownloadPhase> nextPhase) {
			Objects.requireNonNull(phase, "Phase is required.");
			Objects.requireNonNull(phaseSupplier, "Phase supplier is required.");

			if (phaseSuppliers.containsKey(phase)) {
				throw new IllegalStateException(String.format("Phase %s is already mapped", phase.getSimpleName()));
			}

			if (nextPhase != null) {
				downloadPhasesOrder.put(phase, nextPhase);
			}

			this.phaseSuppliers.put(phase, phaseSupplier);
			return this;
		}

		/**
		 * @return The newly created configured instance of {@link PhaseRegulator}
		 */
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
