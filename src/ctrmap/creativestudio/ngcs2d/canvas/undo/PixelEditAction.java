package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import java.util.ArrayList;
import java.util.List;

/**
 * Undoable action recording pixel changes in a tile sheet.
 * Supports coalescing multiple pixel changes from a single brush stroke.
 */
public class PixelEditAction implements SpriteAction {

	private final Sprite2DTileSheet tileSheet;
	private final List<PixelChange> changes = new ArrayList<>();

	public PixelEditAction(Sprite2DTileSheet tileSheet) {
		this.tileSheet = tileSheet;
	}

	/**
	 * Records a single pixel change.
	 */
	public void addChange(int tileIdx, int x, int y, int oldValue, int newValue) {
		changes.add(new PixelChange(tileIdx, x, y, oldValue, newValue));
	}

	/**
	 * Returns true if any changes were recorded.
	 */
	public boolean hasChanges() {
		return !changes.isEmpty();
	}

	@Override
	public void execute() {
		for (PixelChange c : changes) {
			tileSheet.setPixel(c.tileIdx, c.x, c.y, c.newValue);
		}
	}

	@Override
	public void undo() {
		for (int i = changes.size() - 1; i >= 0; i--) {
			PixelChange c = changes.get(i);
			tileSheet.setPixel(c.tileIdx, c.x, c.y, c.oldValue);
		}
	}

	@Override
	public String getDescription() {
		return "Edit " + changes.size() + " pixel(s)";
	}

	private static class PixelChange {

		final int tileIdx;
		final int x;
		final int y;
		final int oldValue;
		final int newValue;

		PixelChange(int tileIdx, int x, int y, int oldValue, int newValue) {
			this.tileIdx = tileIdx;
			this.x = x;
			this.y = y;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}
	}
}
