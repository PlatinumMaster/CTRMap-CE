package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DAnimFrame;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;

/**
 * Undoable duration change for a cell-animation or multi-cell-animation
 * frame. The timeline coalesces continuous drag edits into a single
 * action: the initial duration is captured on mouse-press, the final
 * on mouse-release.
 *
 * <p>Single implementation handles both frame types via an
 * {@link IntSetter} adapter so we don't need two near-identical action
 * classes.</p>
 */
public class ResizeFrameDurationAction implements SpriteAction {

	/** Minimal "set this integer" indirection — lets us mutate either a
	 *  {@link Sprite2DAnimFrame} or a
	 *  {@link Sprite2DMultiCellAnimation.MultiCellAnimFrame} without
	 *  reflection or a shared interface. */
	public interface IntSetter {
		void set(int value);
		int get();
	}

	private final IntSetter target;
	private final int oldDuration;
	private final int newDuration;
	private final String label;

	public ResizeFrameDurationAction(IntSetter target, int oldDuration, int newDuration, String label) {
		this.target = target;
		this.oldDuration = oldDuration;
		this.newDuration = newDuration;
		this.label = label;
	}

	/** Convenience for a cell-animation frame. */
	public static ResizeFrameDurationAction forCellFrame(Sprite2DAnimFrame f,
			int oldDuration, int newDuration) {
		return new ResizeFrameDurationAction(new IntSetter() {
			@Override public void set(int value) { f.duration = value; }
			@Override public int get() { return f.duration; }
		}, oldDuration, newDuration, "Resize cell frame duration");
	}

	/** Convenience for a multi-cell-animation frame. */
	public static ResizeFrameDurationAction forMultiCellFrame(
			Sprite2DMultiCellAnimation.MultiCellAnimFrame f,
			int oldDuration, int newDuration) {
		return new ResizeFrameDurationAction(new IntSetter() {
			@Override public void set(int value) { f.duration = value; }
			@Override public int get() { return f.duration; }
		}, oldDuration, newDuration, "Resize multi-cell frame duration");
	}

	@Override
	public void execute() {
		target.set(newDuration);
	}

	@Override
	public void undo() {
		target.set(oldDuration);
	}

	@Override
	public String getDescription() {
		return label + " " + oldDuration + " -> " + newDuration;
	}
}
