package ctrmap.creativestudio.ngcs2d.canvas.undo;

import java.util.List;

/**
 * Undoable action removing a frame from an animation's frame list.
 * Generic so one class handles both cell and multi-cell animation
 * frames.
 *
 * <p>Captures the removed frame's original index at execute-time. Undo
 * re-inserts at that index; redo removes again (by identity so the
 * right frame is taken even if the list has shifted meanwhile).</p>
 */
public class RemoveFrameAction<T> implements SpriteAction {

	private final List<T> list;
	private final T frame;
	private int originalIndex;

	public RemoveFrameAction(List<T> list, T frame) {
		this.list = list;
		this.frame = frame;
	}

	@Override
	public void execute() {
		if (list == null || frame == null) return;
		int idx = list.indexOf(frame);
		if (idx < 0) return;
		originalIndex = idx;
		list.remove(idx);
	}

	@Override
	public void undo() {
		if (list == null) return;
		int clamped = Math.max(0, Math.min(originalIndex, list.size()));
		list.add(clamped, frame);
	}

	@Override
	public String getDescription() {
		return "Remove frame @ " + originalIndex;
	}
}
