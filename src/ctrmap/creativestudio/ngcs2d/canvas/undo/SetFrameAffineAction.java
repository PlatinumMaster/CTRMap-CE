package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DAnimFrame;

/**
 * Undoable action for setting a single affine field on a
 * {@link Sprite2DAnimFrame}. Timeline affine sub-tracks use this when
 * the user edits rotation / scale / translate from a keyframe inspector.
 *
 * <p>Only NANR (cell) animations carry affine data;
 * {@code MultiCellAnimFrame} has no such fields.</p>
 */
public class SetFrameAffineAction implements SpriteAction {

	/** Which affine scalar this action writes to. */
	public enum Field {
		ROTATION, SCALE_X, SCALE_Y, TRANSLATE_X, TRANSLATE_Y
	}

	private final Sprite2DAnimFrame frame;
	private final Field field;
	private final float oldValue;
	private final float newValue;

	public SetFrameAffineAction(Sprite2DAnimFrame frame, Field field,
			float oldValue, float newValue) {
		this.frame = frame;
		this.field = field;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	@Override
	public void execute() {
		assign(newValue);
	}

	@Override
	public void undo() {
		assign(oldValue);
	}

	private void assign(float v) {
		if (frame == null) return;
		switch (field) {
			case ROTATION:    frame.rotation = v; break;
			case SCALE_X:     frame.scaleX = v; break;
			case SCALE_Y:     frame.scaleY = v; break;
			case TRANSLATE_X: frame.translateX = v; break;
			case TRANSLATE_Y: frame.translateY = v; break;
		}
	}

	@Override
	public String getDescription() {
		return field + " " + oldValue + " -> " + newValue;
	}
}
