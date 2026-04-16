package ctrmap.util.debug.gdb;

/**
 * Low-level GDB Remote Serial Protocol packet encoding and decoding.
 *
 * Packet format: $data#XX where XX is a two-hex-digit checksum
 * (modular sum of all bytes in data, mod 256).
 */
public class GDBPacket {

	/**
	 * Encode a command string into a GDB RSP packet.
	 * @param data The command data (e.g. "g", "m2000000,100")
	 * @return The encoded packet bytes: $data#XX
	 */
	public static byte[] encode(String data) {
		String cs = checksum(data);
		String packet = "$" + data + "#" + cs;
		return packet.getBytes();
	}

	/**
	 * Compute the GDB RSP checksum for a data string.
	 * @param data The packet data (between $ and #)
	 * @return Two uppercase hex characters representing (sum of bytes) mod 256
	 */
	public static String checksum(String data) {
		int sum = 0;
		for (int i = 0; i < data.length(); i++) {
			sum += data.charAt(i) & 0xFF;
		}
		return String.format("%02x", sum & 0xFF);
	}

	/**
	 * Decode a received GDB RSP packet string.
	 * Strips the $ prefix and #XX checksum suffix, validates the checksum.
	 *
	 * @param raw The raw received string (may or may not include $ and #XX framing)
	 * @return The data payload if valid, or null if checksum mismatch
	 */
	public static String decode(String raw) {
		if (raw == null || raw.isEmpty()) {
			return null;
		}

		int start = raw.indexOf('$');
		if (start < 0) {
			// No framing — treat the whole thing as data (for simple responses)
			return raw;
		}

		int hashIdx = raw.indexOf('#', start);
		if (hashIdx < 0 || hashIdx + 2 >= raw.length()) {
			return null;
		}

		String data = raw.substring(start + 1, hashIdx);
		String expectedCS = raw.substring(hashIdx + 1, hashIdx + 3);

		String actualCS = checksum(data);
		if (!actualCS.equalsIgnoreCase(expectedCS)) {
			return null; // Checksum mismatch
		}

		return data;
	}

	/**
	 * Convert a byte array to a hex string (each byte → 2 hex chars).
	 */
	public static String bytesToHex(byte[] data) {
		StringBuilder sb = new StringBuilder(data.length * 2);
		for (byte b : data) {
			sb.append(String.format("%02x", b & 0xFF));
		}
		return sb.toString();
	}

	/**
	 * Convert a hex string back to a byte array.
	 */
	public static byte[] hexToBytes(String hex) {
		int len = hex.length() / 2;
		byte[] result = new byte[len];
		for (int i = 0; i < len; i++) {
			result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
		}
		return result;
	}

	/**
	 * Convert a 32-bit int to 8 hex characters in little-endian byte order,
	 * as GDB RSP uses for ARM register values.
	 * Example: 0x12345678 → "78563412"
	 */
	public static String intToHexLE(int value) {
		return String.format("%02x%02x%02x%02x",
			(value) & 0xFF,
			(value >>> 8) & 0xFF,
			(value >>> 16) & 0xFF,
			(value >>> 24) & 0xFF
		);
	}

	/**
	 * Convert 8 hex characters in little-endian byte order back to a 32-bit int.
	 * Example: "78563412" → 0x12345678
	 */
	public static int hexLEToInt(String hex) {
		if (hex.length() < 8) {
			hex = hex + "00000000".substring(hex.length()); // pad
		}
		int b0 = Integer.parseInt(hex.substring(0, 2), 16);
		int b1 = Integer.parseInt(hex.substring(2, 4), 16);
		int b2 = Integer.parseInt(hex.substring(4, 6), 16);
		int b3 = Integer.parseInt(hex.substring(6, 8), 16);
		return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
	}
}
