package ctrmap.creativestudio.ngcs2d.canvas.undo;

import java.util.List;

/**
 * Undoable action inserting a frame into an animation's frame list at a
 * given index. Generic over the element type so the same class handles
 * both {@code Sprite2DAnimFrame} and
 * {@code Sprite2DMultiCellAnimation.MultiCellAnimFrame}.
 *
 * <p>{@link #undo()} removes the inserted frame. The frame reference is
 * retained so {@link #execute()} (i.e. redo) re-inserts the same
 * object at the same index.</p>
 */
public class InsertFrameAction<T> implements SpriteAction {

	private final List<T> list;
	private final int index;
	private final T frame;

	public InsertFrameAction(List<T> list, int index, T frame) {
		this.list = list;
		this.index = index;
		this.frame = frame;
	}

	@Override
	public void execute() {
		if (list == null) return;
		int clamped = Math.max(0, Math.min(index, list.size()));
		list.add(clamped, frame);
	}

	@Override
	public void undo() {
		if (list == null) return;
		// Remove by identity so concurrent edits that shifted our index
		// don't make us remove the wrong frame.
		list.remove(frame);
	}

	@Override
	public String getDescription() {
		return "Insert frame @ " + index;
	}
}
