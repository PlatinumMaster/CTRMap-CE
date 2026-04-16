package ctrmap.util.debug.gdb;

/**
 * Constants and utilities for ARM register layout in the GDB Remote Serial Protocol.
 * Matches the register ordering used in GDB's 'g' packet response for ARM targets.
 */
public class GDBRegisters {

	// Register indices in the GDB 'g' packet response
	public static final int R0 = 0;
	public static final int R1 = 1;
	public static final int R2 = 2;
	public static final int R3 = 3;
	public static final int R4 = 4;
	public static final int R5 = 5;
	public static final int R6 = 6;
	public static final int R7 = 7;
	public static final int R8 = 8;
	public static final int R9 = 9;
	public static final int R10 = 10;
	public static final int R11 = 11;
	public static final int R12 = 12;
	public static final int SP = 13;
	public static final int LR = 14;
	public static final int PC = 15;
	public static final int CPSR = 16;

	/**
	 * Total number of registers in the GDB 'g' packet for ARM targets.
	 */
	public static final int NUM_REGISTERS = 17;

	// CPSR bit positions
	public static final int CPSR_T_BIT = 5;   // Thumb state
	public static final int CPSR_N_BIT = 31;  // Negative
	public static final int CPSR_Z_BIT = 30;  // Zero
	public static final int CPSR_C_BIT = 29;  // Carry
	public static final int CPSR_V_BIT = 28;  // Overflow
	public static final int CPSR_Q_BIT = 27;  // Saturation (DSP)
	public static final int CPSR_I_BIT = 7;   // IRQ disable
	public static final int CPSR_F_BIT = 6;   // FIQ disable

	private static final String[] REG_NAMES = {
		"R0", "R1", "R2", "R3", "R4", "R5", "R6", "R7",
		"R8", "R9", "R10", "R11", "R12", "SP", "LR", "PC", "CPSR"
	};

	/**
	 * Check if the processor is in Thumb mode from a CPSR value.
	 */
	public static boolean isThumbMode(int cpsr) {
		return ((cpsr >>> CPSR_T_BIT) & 1) != 0;
	}

	/**
	 * Get a human-readable name for a register index.
	 */
	public static String getName(int regIndex) {
		if (regIndex >= 0 && regIndex < REG_NAMES.length) {
			return REG_NAMES[regIndex];
		}
		return "R" + regIndex;
	}

	/**
	 * Format CPSR flags as a human-readable string (e.g. "NZcv T").
	 */
	public static String formatCPSRFlags(int cpsr) {
		StringBuilder sb = new StringBuilder();
		sb.append(((cpsr >>> CPSR_N_BIT) & 1) != 0 ? 'N' : 'n');
		sb.append(((cpsr >>> CPSR_Z_BIT) & 1) != 0 ? 'Z' : 'z');
		sb.append(((cpsr >>> CPSR_C_BIT) & 1) != 0 ? 'C' : 'c');
		sb.append(((cpsr >>> CPSR_V_BIT) & 1) != 0 ? 'V' : 'v');
		sb.append(' ');
		sb.append(isThumbMode(cpsr) ? 'T' : 'A');
		sb.append(' ');
		sb.append(((cpsr >>> CPSR_I_BIT) & 1) != 0 ? 'I' : 'i');
		sb.append(((cpsr >>> CPSR_F_BIT) & 1) != 0 ? 'F' : 'f');
		return sb.toString();
	}
}
