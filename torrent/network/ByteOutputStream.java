package torrent.network;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import torrent.network.utp.UtpSocket;

public class ByteOutputStream extends FilterOutputStream {

	private int speed;
	private UtpSocket socket;

	public ByteOutputStream(UtpSocket socket, OutputStream outStream) {
		super(outStream);
		this.socket = socket;
		speed = 0;
	}

	@Override
	public void write(int i) throws IOException {
		speed++;
		if(socket.isUTP()) {
			socket.write(i);
		} else 
			super.write(i);
	}

	@Override
	public void write(byte[] bytes, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			write(bytes[offset + i]);
		}
	}
	
	/**
     * Writes a <code>boolean</code> to the underlying output stream as
     * a 1-byte value. The value <code>true</code> is written out as the
     * value <code>(byte)1</code>; the value <code>false</code> is
     * written out as the value <code>(byte)0</code>. If no exception is
     * thrown, the counter <code>written</code> is incremented by
     * <code>1</code>.
     *
     * @param      v   a <code>boolean</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    /**
     * Writes out a <code>byte</code> to the underlying output stream as
     * a 1-byte value. If no exception is thrown, the counter
     * <code>written</code> is incremented by <code>1</code>.
     *
     * @param      v   a <code>byte</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeByte(int v) throws IOException {
        write(v);
    }

    /**
     * Writes a <code>short</code> to the underlying output stream as two
     * bytes, high byte first. If no exception is thrown, the counter
     * <code>written</code> is incremented by <code>2</code>.
     *
     * @param      v   a <code>short</code> to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeShort(int v) throws IOException {
        write((v >>> 8) & 0xFF);
        write((v >>> 0) & 0xFF);
    }

    /**
     * Writes an <code>int</code> to the underlying output stream as four
     * bytes, high byte first. If no exception is thrown, the counter
     * <code>written</code> is incremented by <code>4</code>.
     *
     * @param      v   an <code>int</code> to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeInt(int v) throws IOException {
        write((v >>> 24) & 0xFF);
        write((v >>> 16) & 0xFF);
        write((v >>>  8) & 0xFF);
        write((v >>>  0) & 0xFF);
    }

	public void writeString(String s) throws IOException {
		for (int i = 0; i < s.length(); i++) {
			write(s.charAt(i) & 0xFF);
		}
	}

	public int getSpeed() {
		return speed;
	}

	public void reset(int uploadRate) {
		speed -= uploadRate;
	}
	
	/**
	 * Returns the underlying socket for this connection
	 * @return the utpSocket on which this connection is based
	 */
	public UtpSocket getSocket() {
		return socket;
	}

}
