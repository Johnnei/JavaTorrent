package org.johnnei.javatorrent.mock;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.Set;

import static org.mockito.Mockito.mock;

public class MockDatagramChannel extends DatagramChannel {

	private final DatagramChannel underlyingMock;

	public MockDatagramChannel() {
		super(null);
		this.underlyingMock = mock(DatagramChannel.class);
	}

	@Override
	public DatagramChannel bind(SocketAddress local) throws IOException {
		underlyingMock.bind(local);
		return this;
	}

	@Override
	public <T> DatagramChannel setOption(SocketOption<T> name, T value) throws IOException {
		underlyingMock.setOption(name, value);
		return this;
	}

	@Override
	public <T> T getOption(SocketOption<T> name) throws IOException {
		return underlyingMock.getOption(name);
	}

	@Override
	public Set<SocketOption<?>> supportedOptions() {
		return null;
	}

	@Override
	public DatagramSocket socket() {
		return null;
	}

	@Override
	public boolean isConnected() {
		return false;
	}

	@Override
	public DatagramChannel connect(SocketAddress remote) {
		return this;
	}

	@Override
	public DatagramChannel disconnect() {
		return this;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return null;
	}

	@Override
	public SocketAddress receive(ByteBuffer dst) throws IOException {
		return underlyingMock.receive(dst);
	}

	@Override
	public int send(ByteBuffer src, SocketAddress target) {
		return 0;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		return underlyingMock.read(dst);
	}

	@Override
	public long read(ByteBuffer[] dsts, int offset, int length) {
		return 0;
	}

	@Override
	public int write(ByteBuffer src) {
		return 0;
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length) {
		return 0;
	}

	@Override
	public SocketAddress getLocalAddress() {
		return null;
	}

	@Override
	public MembershipKey join(InetAddress group, NetworkInterface interf) {
		return null;
	}

	@Override
	public MembershipKey join(InetAddress group, NetworkInterface interf, InetAddress source) {
		return null;
	}

	@Override
	protected void implCloseSelectableChannel() {

	}

	@Override
	protected void implConfigureBlocking(boolean block) {

	}

	public DatagramChannel getMockitoMock() {
		return underlyingMock;
	}
}
