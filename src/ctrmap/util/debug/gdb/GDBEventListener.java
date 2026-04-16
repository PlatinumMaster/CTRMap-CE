package ctrmap.util.debug.gdb;

/**
 * Callback interface for asynchronous GDB debug events.
 * All methods are called on the EDT (Event Dispatch Thread).
 */
public interface GDBEventListener {

	/**
	 * Called when the target stops execution (breakpoint hit, step completed, halt).
	 * @param signal The UNIX signal number (5 = SIGTRAP for breakpoints/single-step).
	 */
	void onStopped(int signal);

	/**
	 * Called when the GDB connection is lost unexpectedly or closed.
	 */
	void onDisconnected();

	/**
	 * Called when the target resumes execution (after continue or step command sent).
	 */
	void onResumed();
}
