package org.johnnei.javatorrent.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.johnnei.javatorrent.internal.network.socket.ISocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls the degradation of socket types ordered from most preferred to least preferred.
 * @author johnn
 *
 */
public class ConnectionDegradation {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionDegradation.class);

	private final String preferredSocketIdentifier;
	private final Map<String, String> socketDegradation;
	private final Map<String, ISocketSupplier> socketSuppliers;

	private ConnectionDegradation(Builder builder) {
		preferredSocketIdentifier = builder.preferredSocketIdentifier;
		socketDegradation = builder.socketDegradation;
		socketSuppliers = builder.socketSuppliers;
	}

	/**
	 * Creates a new unconnected socket based on the most prefered connection type.
	 * @return An unconnected socket
	 */
	public ISocket createPreferredSocket() {
		return socketSuppliers.get(preferredSocketIdentifier).createSocket();
	}

	/**
	 * Degrades the given socket into the next type
	 * @param socket The socket type to degrade
	 * @return The degraded socket or {@link Optional#empty()} when no degradation is possible
	 */
	public Optional<ISocket> degradeSocket(ISocket socket) {
		String socketIdentifier = socket.getClass().getSimpleName();
		if (!socketSuppliers.containsKey(socketIdentifier)) {
			return Optional.empty();
		}

		String fallbackSocketIdentifier = socketDegradation.get(socketIdentifier);
		ISocketSupplier fallbackSocketSupplier = socketSuppliers.get(fallbackSocketIdentifier);
		return Optional.of(fallbackSocketSupplier.createSocket());
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder("ConnectionDegradation[");

		String socketIdentifier = preferredSocketIdentifier;
		while (socketDegradation.containsKey(socketIdentifier)) {
			stringBuilder.append(socketIdentifier);

			socketIdentifier = socketDegradation.get(socketIdentifier);

			if (socketIdentifier != null) {
				stringBuilder.append(" -> ");
			}
		}
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	/**
	 * A builder pattern styled class to create {@link ConnectionDegradation} instances.
	 */
	public static class Builder {

		private String preferredSocketIdentifier;
		private final Map<String, String> socketDegradation;
		private final Map<String, ISocketSupplier> socketSuppliers;

		/**
		 * Creates a new builder without configured defaults.
		 */
		public Builder() {
			socketDegradation = new HashMap<>();
			socketSuppliers = new HashMap<>();
		}

		/**
		 * Registers a new supported connection type. This type will be used first to connect with peers. The given fallback will be the used to connect when
		 * this type fails to connect to the peer. This will override any previously configured default socket types.
		 * @param supplier The socket supplier.
		 * @return The builder with updated configuration.
		 *
		 * @see #registerSocketSupplier(ISocketSupplier)
		 */
		public Builder registerDefaultSocketSupplier(ISocketSupplier supplier) {
			preferredSocketIdentifier = supplier.getSocketIdentifier();
			registerSocketSupplier(supplier, null);
			return this;
		}

		/**
		 * Registers a new supported connection type. The given fallback will be the used to connect when this type fails to connect to the peer. This will
		 * override any previously configured default socket types.
		 * @param supplier The {@link ISocketSupplier} supplier of the socket.
		 * @param fallbackSupplier The {@link ISocketSupplier} supplier of the fallback socket.
		 * @return The builder with updated configuration.
		 *
		 * @see #registerFallback(ISocketSupplier, ISocketSupplier)
		 */
		public Builder registerSocketSupplier(ISocketSupplier supplier, ISocketSupplier fallbackSupplier) {
			Objects.requireNonNull(supplier, "Socket supplier can not be null");

			socketSuppliers.put(supplier.getSocketIdentifier(), supplier);
			registerFallback(supplier, fallbackSupplier);
			return this;
		}

		private void registerFallback(ISocketSupplier from, ISocketSupplier to) {
			if (to == null) {
				return;
			}

			socketDegradation.put(from.getSocketIdentifier(), to.getSocketIdentifier());
		}

		/**
		 * @return The newly created configured {@link ConnectionDegradation} instance.
		 */
		public ConnectionDegradation build() {
			if (preferredSocketIdentifier == null) {
				throw new IllegalStateException("No preferred connection type has been configured.");
			}

			LOGGER.debug("Preferred Connection: {}", preferredSocketIdentifier);

			verifySocketChain();

			return new ConnectionDegradation(this);
		}

		/**
		 * Checks if all socket types are in the chain starting from the most preferred and test if a supplier has been set.
		 */
		private void verifySocketChain() {
			int typesSeen = 0;
			String socketIdentifier = preferredSocketIdentifier;
			while (socketIdentifier != null) {
				if (!socketSuppliers.containsKey(socketIdentifier)) {
					throw new IllegalStateException(String.format("Socket supplier for type %s has not been set.", socketIdentifier));
				}

				socketIdentifier = socketDegradation.get(socketIdentifier);
				typesSeen++;
			}

			if (typesSeen != socketSuppliers.size()) {
				LOGGER.warn("Socket chain does not contain all types. Chain contains {} types, whilst {} have been registered.",
						typesSeen,
						socketSuppliers.size());
			}
		}
	}

}
