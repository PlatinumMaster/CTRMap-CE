package ctrmap.creativestudio.ngcs2d.canvas.undo;

/**
 * Interface for undoable sprite editing actions.
 */
public interface SpriteAction {

	/**
	 * Executes (or re-executes) this action.
	 */
	void execute();

	/**
	 * Reverses this action.
	 */
	void undo();

	/**
	 * Returns a human-readable description of this action.
	 */
	String getDescription();
}
