package ctrmap.util.debug.gdb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;

/**
 * GDB Remote Serial Protocol client for connecting to a GDB stub (e.g. melonDS).
 * Manages a TCP socket with a background receiver thread and provides high-level
 * methods for register/memory access, execution control, and breakpoints.
 *
 * <p>Thread safety: methods that perform I/O are synchronized via cmdLock and
 * must not be called from the EDT. Event listener callbacks are dispatched on
 * the EDT via SwingUtilities.invokeLater.</p>
 */
public class GDBClient {

	public enum State {
		DISCONNECTED,
		CONNECTING,
		STOPPED,
		RUNNING
	}

	private Socket socket;
	private InputStream in;
	private OutputStream out;
	private volatile State state = State.DISCONNECTED;
	private Thread recvThread;
	private final List<GDBEventListener> listeners = new CopyOnWriteArrayList<>();

	// Synchronization for command/response pairs.
	// cmdLock guards the send side; responseLatch bridges receiver → sender.
	private final Object cmdLock = new Object();
	private final Object writeLock = new Object(); // guards all out.write() calls
	private final AtomicReference<CountDownLatch> responseLatch = new AtomicReference<>();
	private volatile String pendingResponse;

	private static final int SOCKET_TIMEOUT_MS = 5000;
	private static final int RESPONSE_TIMEOUT_MS = 10000;

	private static final boolean DEBUG = true;

	// --- Connection management ---

	/**
	 * Connect to a GDB stub at the given host and port.
	 * Sends initial handshake and starts the receiver thread.
	 * Must NOT be called from the EDT.
	 */
	public void connect(String host, int port) throws IOException {
		if (state != State.DISCONNECTED) {
			disconnect();
		}

		state = State.CONNECTING;
		log("Connecting to " + host + ":" + port + "...");

		try {
			socket = new Socket(host, port);
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(SOCKET_TIMEOUT_MS);
			log("TCP socket connected.");

			in = socket.getInputStream();
			out = socket.getOutputStream();

			// melonDS handshake: the stub expects a bare '+' immediately after
			// TCP connect.  It blocks for up to 1 s waiting for this byte and
			// closes the connection if it never arrives.  This is non-standard
			// (real GDB doesn't do it) but we must comply.
			log("Sending initial '+' handshake...");
			out.write('+');
			out.flush();

			// Read the '+' melonDS sends back (blocking, with socket timeout)
			int ack = in.read();
			log("Received initial response: " + (ack >= 0 ? ("'" + (char) ack + "' (0x" + Integer.toHexString(ack) + ")") : "EOF"));
			if (ack < 0) {
				throw new IOException("GDB stub closed connection during handshake");
			}

			// Now start the receiver thread for normal packet I/O
			recvThread = new Thread(this::receiverLoop, "GDB-Receiver");
			recvThread.setDaemon(true);
			recvThread.start();

			// Small delay to let the receiver thread start its read loop
			try {
				Thread.sleep(50);
			} catch (InterruptedException ignored) {
			}

			// Feature negotiation (best-effort — some stubs don't support it)
			log("Sending qSupported...");
			String supported = sendCommand("qSupported:swbreak+;hwbreak+");
			log("qSupported response: " + (supported != null ? supported : "<timeout>"));

			// Query halt reason — this is what tells us the target state
			log("Sending ?...");
			String haltReply = sendCommand("?");
			log("Halt reply: " + (haltReply != null ? haltReply : "<timeout>"));

			if (haltReply != null && (haltReply.startsWith("S") || haltReply.startsWith("T"))) {
				state = State.STOPPED;
				int signal = parseStopSignal(haltReply);
				log("Target is stopped, signal=" + signal);
				// Don't fire onStopped here — the receiver thread already
				// fired it when it saw the stop reply packet. Only fire if
				// the receiver didn't handle it (i.e. state wasn't already STOPPED).
			} else if (haltReply != null && haltReply.isEmpty()) {
				// Empty response — stub might not support '?'. Try halting.
				log("Empty halt reply, sending break to halt target...");
				halt();
				// Wait for the stop reply from the receiver
				try {
					Thread.sleep(500);
				} catch (InterruptedException ignored) {
				}
			} else if (haltReply == null) {
				// Timeout — target might be running. Try sending break.
				log("No halt reply (timeout), sending break...");
				halt();
				try {
					Thread.sleep(500);
				} catch (InterruptedException ignored) {
				}
			}

			// If we're still not stopped, at least mark as connected
			if (state == State.CONNECTING) {
				state = State.STOPPED;
				fireOnStopped(0);
			}

			log("Connection established. State: " + state);
		} catch (IOException e) {
			log("Connection failed: " + e.getMessage());
			disconnect();
			throw e;
		}
	}

	/**
	 * Disconnect from the GDB stub and clean up resources.
	 */
	public void disconnect() {
		State prevState = state;
		state = State.DISCONNECTED;

		if (recvThread != null) {
			recvThread.interrupt();
			recvThread = null;
		}

		if (socket != null) {
			try {
				socket.close();
			} catch (IOException ignored) {
			}
			socket = null;
		}

		in = null;
		out = null;

		// Release any waiting command
		CountDownLatch latch = responseLatch.getAndSet(null);
		if (latch != null) {
			pendingResponse = null;
			latch.countDown();
		}

		if (prevState != State.DISCONNECTED) {
			fireOnDisconnected();
		}
	}

	public State getState() {
		return state;
	}

	public boolean isConnected() {
		return state != State.DISCONNECTED;
	}

	// --- Event listeners ---

	public void addListener(GDBEventListener listener) {
		listeners.add(listener);
	}

	public void removeListener(GDBEventListener listener) {
		listeners.remove(listener);
	}

	// --- Register access ---

	/**
	 * Read all ARM registers (r0-r15 + CPSR).
	 * @return int[17] with register values, or null on failure
	 */
	public int[] readRegisters() throws IOException {
		String reply = sendCommand("g");
		if (reply == null || reply.isEmpty() || reply.startsWith("E")) {
			log("readRegisters failed: " + reply);
			return null;
		}

		int numRegs = Math.min(GDBRegisters.NUM_REGISTERS, reply.length() / 8);
		int[] regs = new int[GDBRegisters.NUM_REGISTERS];
		for (int i = 0; i < numRegs; i++) {
			int offset = i * 8;
			regs[i] = GDBPacket.hexLEToInt(reply.substring(offset, offset + 8));
		}
		return regs;
	}

	/**
	 * Read a single register.
	 * @param regNum Register number (0-16 for r0-CPSR)
	 */
	public int readRegister(int regNum) throws IOException {
		String reply = sendCommand("p" + Integer.toHexString(regNum));
		if (reply == null || reply.isEmpty() || reply.startsWith("E")) {
			return 0;
		}
		return GDBPacket.hexLEToInt(reply);
	}

	/**
	 * Write a single register.
	 */
	public void writeRegister(int regNum, int value) throws IOException {
		sendCommand("P" + Integer.toHexString(regNum) + "=" + GDBPacket.intToHexLE(value));
	}

	// --- Memory access ---

	/**
	 * Read memory from the target.
	 * @param address Start address
	 * @param length  Number of bytes to read
	 * @return The memory contents, or null on error
	 */
	public byte[] readMemory(int address, int length) throws IOException {
		// Clamp to reasonable chunk to avoid protocol issues
		length = Math.min(length, 4096);
		String addrHex = Long.toHexString(address & 0xFFFFFFFFL);
		String reply = sendCommand("m" + addrHex + "," + Integer.toHexString(length));
		if (reply == null || reply.isEmpty() || reply.startsWith("E")) {
			return null;
		}
		try {
			return GDBPacket.hexToBytes(reply);
		} catch (Exception e) {
			log("readMemory hex decode error: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Write memory on the target.
	 */
	public void writeMemory(int address, byte[] data) throws IOException {
		String addrHex = Long.toHexString(address & 0xFFFFFFFFL);
		String cmd = "M" + addrHex + "," +
			Integer.toHexString(data.length) + ":" + GDBPacket.bytesToHex(data);
		sendCommand(cmd);
	}

	// --- Execution control ---

	/**
	 * Continue execution. The target will run until it hits a breakpoint or is halted.
	 */
	public void continueExecution() throws IOException {
		state = State.RUNNING;
		fireOnResumed();
		sendCommandAsync("c");
	}

	/**
	 * Single-step one instruction.
	 */
	public void step() throws IOException {
		state = State.RUNNING;
		fireOnResumed();
		sendCommandAsync("s");
	}

	/**
	 * Halt the running target by sending the break character (0x03 / Ctrl-C).
	 */
	public void halt() throws IOException {
		synchronized (writeLock) {
			if (out != null) {
				out.write(0x03);
				out.flush();
				log("Sent break (0x03)");
			}
		}
	}

	// --- Breakpoints ---

	/**
	 * Set a software breakpoint.
	 * @param address The breakpoint address
	 * @param kind    Breakpoint kind: 2 for Thumb, 4 for ARM
	 */
	public void setBreakpoint(int address, int kind) throws IOException {
		String addrHex = Long.toHexString(address & 0xFFFFFFFFL);
		sendCommand("Z0," + addrHex + "," + Integer.toHexString(kind));
	}

	/**
	 * Remove a software breakpoint.
	 */
	public void removeBreakpoint(int address, int kind) throws IOException {
		String addrHex = Long.toHexString(address & 0xFFFFFFFFL);
		sendCommand("z0," + addrHex + "," + Integer.toHexString(kind));
	}

	/**
	 * Set a hardware breakpoint.
	 */
	public void setHardwareBreakpoint(int address, int kind) throws IOException {
		String addrHex = Long.toHexString(address & 0xFFFFFFFFL);
		sendCommand("Z1," + addrHex + "," + Integer.toHexString(kind));
	}

	/**
	 * Remove a hardware breakpoint.
	 */
	public void removeHardwareBreakpoint(int address, int kind) throws IOException {
		String addrHex = Long.toHexString(address & 0xFFFFFFFFL);
		sendCommand("z1," + addrHex + "," + Integer.toHexString(kind));
	}

	// --- Internal transport ---

	/**
	 * Send a command and wait for the response.
	 */
	private String sendCommand(String command) throws IOException {
		synchronized (cmdLock) {
			if (out == null) {
				throw new IOException("Not connected");
			}

			// Set up response latch
			CountDownLatch latch = new CountDownLatch(1);
			pendingResponse = null;
			responseLatch.set(latch);

			// Send the packet
			byte[] packet = GDBPacket.encode(command);
			synchronized (writeLock) {
				out.write(packet);
				out.flush();
			}
			log("TX: $" + command + "#..");

			// Wait for response
			try {
				if (!latch.await(RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
					log("Response timeout for: " + command);
					responseLatch.set(null);
					return null;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				responseLatch.set(null);
				return null;
			}

			String resp = pendingResponse;
			log("RX: " + (resp != null ? (resp.length() > 80 ? resp.substring(0, 80) + "..." : resp) : "<null>"));
			return resp;
		}
	}

	/**
	 * Send a command without waiting for an immediate response.
	 * Used for 'c' (continue) and 's' (step) where the response is an async stop reply.
	 */
	private void sendCommandAsync(String command) throws IOException {
		synchronized (cmdLock) {
			if (out == null) {
				throw new IOException("Not connected");
			}

			// Clear any pending response state — the stop reply will be
			// handled asynchronously by the receiver thread
			responseLatch.set(null);
			pendingResponse = null;

			byte[] packet = GDBPacket.encode(command);
			synchronized (writeLock) {
				out.write(packet);
				out.flush();
			}
			log("TX (async): $" + command + "#..");
		}
	}

	/**
	 * Background receiver thread loop. Reads from the socket, assembles packets,
	 * handles ack/nack, and dispatches responses or stop notifications.
	 */
	private void receiverLoop() {
		log("Receiver thread started.");
		StringBuilder buffer = new StringBuilder();
		boolean inPacket = false;

		try {
			while (!Thread.currentThread().isInterrupted() && socket != null && !socket.isClosed()) {
				int b;
				try {
					b = in.read();
				} catch (SocketTimeoutException e) {
					continue; // Read timeout — just loop and retry
				}

				if (b < 0) {
					log("Receiver: EOF");
					break;
				}

				char c = (char) (b & 0xFF);

				// Ack / Nack — only skip when NOT inside packet data,
				// since '+' and '-' can appear in packet payloads
				// (e.g. qSupported response: "swbreak-;hwbreak+")
				if (!inPacket && (c == '+' || c == '-')) {
					continue;
				}

				if (c == '$') {
					// Start of a new packet
					buffer.setLength(0);
					inPacket = true;
					continue;
				}

				if (c == '#' && inPacket) {
					// End of packet data — read 2 checksum characters
					// Must handle SocketTimeoutException here specifically
					int cs1 = readByteBlocking();
					int cs2 = readByteBlocking();
					if (cs1 < 0 || cs2 < 0) {
						log("Receiver: EOF reading checksum");
						break;
					}

					String data = buffer.toString();
					String expectedCS = "" + (char) cs1 + (char) cs2;
					String actualCS = GDBPacket.checksum(data);

					if (actualCS.equalsIgnoreCase(expectedCS)) {
						sendAck(true);
						handleReceivedPacket(data);
					} else {
						log("Receiver: checksum mismatch, expected=" + expectedCS + " actual=" + actualCS);
						sendAck(false);
					}

					inPacket = false;
					buffer.setLength(0);
					continue;
				}

				if (inPacket) {
					buffer.append(c);
				}
			}
		} catch (SocketException e) {
			// Socket closed — expected during disconnect
			if (state != State.DISCONNECTED) {
				log("Receiver: socket closed unexpectedly: " + e.getMessage());
			}
		} catch (IOException e) {
			if (state != State.DISCONNECTED) {
				log("Receiver error: " + e.getMessage());
			}
		}

		log("Receiver thread exiting. State=" + state);

		// If we got here and weren't intentionally disconnected, fire disconnected event
		if (state != State.DISCONNECTED) {
			state = State.DISCONNECTED;

			// Release any waiting sendCommand
			CountDownLatch latch = responseLatch.getAndSet(null);
			if (latch != null) {
				pendingResponse = null;
				latch.countDown();
			}

			fireOnDisconnected();
		}
	}

	/**
	 * Read a single byte from the input, retrying on SocketTimeoutException.
	 * Returns -1 on EOF or if the socket is closed.
	 */
	private int readByteBlocking() throws IOException {
		while (!Thread.currentThread().isInterrupted() && socket != null && !socket.isClosed()) {
			try {
				return in.read();
			} catch (SocketTimeoutException e) {
				// Retry — we MUST get this byte to complete the packet
				continue;
			}
		}
		return -1;
	}

	/**
	 * Send ack (+) or nack (-) to the stub.
	 */
	private void sendAck(boolean positive) {
		try {
			synchronized (writeLock) {
				if (out != null) {
					out.write(positive ? '+' : '-');
					out.flush();
				}
			}
		} catch (IOException e) {
			log("Failed to send ack: " + e.getMessage());
		}
	}

	/**
	 * Process a fully received and validated packet.
	 */
	private void handleReceivedPacket(String data) {
		// Check if this is a stop reply
		if (data.startsWith("S") || data.startsWith("T")) {
			state = State.STOPPED;
			int signal = parseStopSignal(data);
			log("Received stop reply: " + data + " (signal=" + signal + ")");

			// Fulfill any waiting sendCommand latch
			CountDownLatch latch = responseLatch.getAndSet(null);
			if (latch != null) {
				pendingResponse = data;
				latch.countDown();
			}

			fireOnStopped(signal);
			return;
		}

		// Regular response to a pending command
		CountDownLatch latch = responseLatch.getAndSet(null);
		if (latch != null) {
			pendingResponse = data;
			latch.countDown();
		} else {
			log("Received unsolicited packet (no latch): " + (data.length() > 60 ? data.substring(0, 60) + "..." : data));
		}
	}

	/**
	 * Parse the signal number from a stop reply packet.
	 * S05 → 5, T05thread:01; → 5
	 */
	private int parseStopSignal(String reply) {
		if (reply == null || reply.length() < 3) {
			return 0;
		}
		try {
			return Integer.parseInt(reply.substring(1, 3), 16);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	// --- Event dispatching (always on EDT) ---

	private void fireOnStopped(int signal) {
		SwingUtilities.invokeLater(() -> {
			for (GDBEventListener l : listeners) {
				l.onStopped(signal);
			}
		});
	}

	private void fireOnDisconnected() {
		SwingUtilities.invokeLater(() -> {
			for (GDBEventListener l : listeners) {
				l.onDisconnected();
			}
		});
	}

	private void fireOnResumed() {
		SwingUtilities.invokeLater(() -> {
			for (GDBEventListener l : listeners) {
				l.onResumed();
			}
		});
	}

	private static void log(String msg) {
		if (DEBUG) {
			System.out.println("[GDBClient] " + msg);
		}
	}
}
