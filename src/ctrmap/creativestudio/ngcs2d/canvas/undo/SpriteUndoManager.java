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
	 * Performs an action and pushes it onto the undo stack.
	 * The action's execute() is NOT called here since it was already applied.
	 */
	public void perform(SpriteAction action) {
		undoStack.add(action);
		if (undoStack.size() > MAX_UNDO) {
			undoStack.remove(0);
		}
		redoStack.clear();
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
	 * Clears both undo and redo stacks.
	 */
	public void clear() {
		undoStack.clear();
		redoStack.clear();
	}
}
