package ctrmap.creativestudio.ngcs2d.canvas.undo;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages an undo/redo stack for sprite editing actions.
 */
public class SpriteUndoManager {

	private static final int MAX_UNDO = 50;

	private final List<SpriteAction> undoStack = new ArrayList<>();
	private final List<SpriteAction> redoStack = new ArrayList<>();

	/**
	 * Monotonic counter bumped on every {@link #perform(SpriteAction)}.
	 * Held constant by {@link #undo()} / {@link #redo()} / {@link #clear()}.
	 * Callers that need a "has the user made any edits since point X?"
	 * check can snapshot {@link #getModCount()} at X and compare later.
	 */
	private int modCount = 0;

	/** Subscribers fired after every modCount bump — used by NGCS2D to
	 *  refresh its dirty title indicator without polling. */
	private final List<Runnable> modCountListeners = new ArrayList<>();

	/**
	 * Performs an action and pushes it onto the undo stack.
	 * The action's execute() is NOT called here since it was already applied.
	 */
	public void perform(SpriteAction action) {
		undoStack.add(action);
		if (undoStack.size() > MAX_UNDO) {
			undoStack.remove(0);
		}
		redoStack.clear();
		modCount++;
		fireModCountChanged();
	}

	/**
	 * Undoes the most recent action.
	 */
	public void undo() {
		if (!undoStack.isEmpty()) {
			SpriteAction action = undoStack.remove(undoStack.size() - 1);
			action.undo();
			redoStack.add(action);
		}
	}

	/**
	 * Redoes the most recently undone action.
	 */
	public void redo() {
		if (!redoStack.isEmpty()) {
			SpriteAction action = redoStack.remove(redoStack.size() - 1);
			action.execute();
			undoStack.add(action);
		}
	}

	public boolean canUndo() {
		return !undoStack.isEmpty();
	}

	public boolean canRedo() {
		return !redoStack.isEmpty();
	}

	/**
	 * Clears both undo and redo stacks. Does NOT reset the modCount — the
	 * fact that edits were made is independent of whether they're
	 * currently undoable.
	 */
	public void clear() {
		undoStack.clear();
		redoStack.clear();
	}

	/**
	 * Returns the current modification counter. Increments on each
	 * {@link #perform(SpriteAction)}; unchanged on undo/redo/clear. Used
	 * by NGCS2D's dirty-tracking: snapshot on apply, compare on close.
	 */
	public int getModCount() {
		return modCount;
	}

	/**
	 * Registers a listener that fires after each {@link #perform} call.
	 * Used by NGCS2D to refresh its dirty title indicator as soon as the
	 * user makes an edit.
	 */
	public void addModCountListener(Runnable listener) {
		if (listener != null) {
			modCountListeners.add(listener);
		}
	}

	private void fireModCountChanged() {
		// Copy-on-iterate so listeners that remove themselves during the
		// notification don't throw ConcurrentModificationException.
		for (Runnable r : new ArrayList<>(modCountListeners)) {
			try {
				r.run();
			} catch (RuntimeException ex) {
				// Never let a bad listener take down the edit path.
				ex.printStackTrace();
			}
		}
	}
}
