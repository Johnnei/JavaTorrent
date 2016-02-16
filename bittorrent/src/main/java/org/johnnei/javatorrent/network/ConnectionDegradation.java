package org.johnnei.javatorrent.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.johnnei.javatorrent.network.socket.ISocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls the degradation of socket types ordered from most preferred to least preferred.
 * @author johnn
 *
 */
public class ConnectionDegradation {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionDegradation.class);

	private final Class<? extends ISocket> preferedType;

	private final Map<Class<? extends ISocket>, Class<ISocket>> socketDegradation;

	private final Map<Class<? extends ISocket>, Supplier<? extends ISocket>> socketSuppliers;

	private ConnectionDegradation(Builder builder) {
		preferedType = builder.preferedType;
		socketDegradation = builder.socketDegradation;
		socketSuppliers = builder.socketSuppliers;
	}

	/**
	 * Creates a new unconnected socket based on the most prefered connection type.
	 * @return An unconnected socket
	 */
	public ISocket createPreferedSocket() {
		return socketSuppliers.get(preferedType).get();
	}

	/**
	 * Degrades the given socket into the next type
	 * @param socket The socket type to degrade
	 * @return The degraded socket or {@link Optional#empty()} when no degradation is possible
	 */
	public Optional<ISocket> degradeSocket(ISocket socket) {
		if (!socketDegradation.containsKey(socket.getClass())) {
			return Optional.empty();
		}

		Class<ISocket> fallbackType = socketDegradation.get(socket.getClass());
		return Optional.of(socketSuppliers.get(fallbackType).get());
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder("ConnectionDegradation[");
		Class<? extends ISocket> type = preferedType;
		while (type != null) {
			stringBuilder.append(type.getSimpleName());

			type = socketDegradation.get(type);

			if (type != null) {
				stringBuilder.append(" -> ");
			}
		}
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	public static class Builder {

		private Class<? extends ISocket> preferedType;

		private Map<Class<? extends ISocket>, Class<ISocket>> socketDegradation;

		private Map<Class<? extends ISocket>, Supplier<? extends ISocket>> socketSuppliers;

		public Builder() {
			socketDegradation = new HashMap<>();
			socketSuppliers = new HashMap<>();
		}

		public <T extends ISocket> Builder registerDefaultConnectionType(Class<T> socketType, Supplier<T> supplier, Optional<Class<ISocket>> fallbackType) {
			Objects.requireNonNull(socketType, "Socket type can not be null");
			if (preferedType != null) {
				LOGGER.warn(String.format("Overriding existing default connection type: %s.", preferedType.getSimpleName()));
			}

			preferedType = socketType;
			registerConnectionType(socketType, supplier, fallbackType);
			return this;
		}

		public <T extends ISocket> Builder registerConnectionType(Class<T> socketType, Supplier<T> supplier, Optional<Class<ISocket>> fallbackType) {
			Objects.requireNonNull(socketType, "Socket type can not be null");
			Objects.requireNonNull(supplier, "Socket supplier can not be null");

			socketSuppliers.put(socketType, supplier);
			registerFallback(socketType, fallbackType);
			return this;
		}

		private void registerFallback(Class<? extends ISocket> from, Optional<Class<ISocket>> to) {
			if (!to.isPresent()) {
				return;
			}

			socketDegradation.put(from, to.get());
		}

		public ConnectionDegradation build() {
			if (preferedType == null) {
				throw new IllegalStateException("No prefered connection type has been configured.");
			}

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Prefered Connection: %s", preferedType.getSimpleName()));
			}

			verifySocketChain();

			return new ConnectionDegradation(this);
		}

		/**
		 * Checks if all socket types are in the chain starting from the most preferred and test if a supplier has been set.
		 */
		private void verifySocketChain() {
			int typesSeen = 0;
			Class<? extends ISocket> type = preferedType;
			while (type != null) {
				if (!socketSuppliers.containsKey(type)) {
					throw new IllegalStateException(String.format("Socket supplier for type %s has not been set.", type.getSimpleName()));
				}

				type = socketDegradation.get(type);
				typesSeen++;
			}

			if (typesSeen != socketSuppliers.size()) {
				LOGGER.warn(String.format(
						"Socket chain does not contain all types. Chain contains %d types, whilst %d have been registered.",
						typesSeen,
						socketSuppliers.size()));
			}
		}
	}

}
