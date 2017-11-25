package org.johnnei.javatorrent.internal.utp;

import java.util.Objects;

public class Acknowledgement {

	private final short sequenceNumber;

	private int timesSeen;

	public Acknowledgement(short sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public void packetSeen() {
		timesSeen++;
	}

	public int getTimesSeen() {
		return timesSeen;
	}

	public short getSequenceNumber() {
		return sequenceNumber;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Acknowledgement that = (Acknowledgement) o;
		return sequenceNumber == that.sequenceNumber;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sequenceNumber);
	}

	@Override
	public String toString() {
		return String.format("Acknowledgement{sequenceNumber=%d, timesSeen=%d}", sequenceNumber, timesSeen);
	}
}
