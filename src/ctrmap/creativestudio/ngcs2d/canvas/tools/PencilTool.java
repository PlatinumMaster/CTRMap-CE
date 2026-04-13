package ctrmap.creativestudio.ngcs2d.canvas.tools;

import ctrmap.creativestudio.ngcs2d.canvas.undo.PixelEditAction;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;

/**
 * Single-pixel pencil drawing tool.
 */
public class PencilTool extends Sprite2DBaseTool {

	private PixelEditAction currentAction;

	@Override
	public void onPressed(Sprite2DCanvasEvent e) {
		Sprite2DTileSheet ts = getActiveTileSheet();
		if (ts == null) {
			return;
		}
		currentAction = new PixelEditAction(ts);
		drawPixel(ts, e.pixelX, e.pixelY);
		if (canvas != null) {
			canvas.refreshRender();
		}
	}

	@Override
	public void onDragged(Sprite2DCanvasEvent e) {
		Sprite2DTileSheet ts = getActiveTileSheet();
		if (ts == null) {
			return;
		}
		drawPixel(ts, e.pixelX, e.pixelY);
		if (canvas != null) {
			canvas.refreshRender();
		}
	}

	@Override
	public void onReleased(Sprite2DCanvasEvent e) {
		if (currentAction != null && currentAction.hasChanges() && undoManager != null) {
			undoManager.perform(currentAction);
		}
		currentAction = null;
		if (canvas != null) {
			canvas.refreshRender();
		}
	}

	private void drawPixel(Sprite2DTileSheet ts, int px, int py) {
		int tileIdx = ts.pixelToTileIndex(px, py);
		if (tileIdx < 0) {
			return;
		}
		int localX = px & 7;
		int localY = py & 7;
		int oldVal = ts.getPixel(tileIdx, localX, localY);
		if (oldVal != foregroundColorIndex) {
			ts.setPixel(tileIdx, localX, localY, foregroundColorIndex);
			if (currentAction != null) {
				currentAction.addChange(tileIdx, localX, localY, oldVal, foregroundColorIndex);
			}
		}
	}

	@Override
	public String getToolName() {
		return "Pencil";
	}
}
