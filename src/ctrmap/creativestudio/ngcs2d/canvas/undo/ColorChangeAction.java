package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;

/**
 * Undoable action for changing a color in a palette.
 */
public class ColorChangeAction implements SpriteAction {

	private final Sprite2DPalette palette;
	private final int colorIndex;
	private final int oldColor;
	private final int newColor;

	public ColorChangeAction(Sprite2DPalette palette, int colorIndex, int oldColor, int newColor) {
		this.palette = palette;
		this.colorIndex = colorIndex;
		this.oldColor = oldColor;
		this.newColor = newColor;
	}

	@Override
	public void execute() {
		palette.colors[colorIndex] = newColor;
	}

	@Override
	public void undo() {
		palette.colors[colorIndex] = oldColor;
	}

	@Override
	public String getDescription() {
		return "Change color " + colorIndex;
	}
}
