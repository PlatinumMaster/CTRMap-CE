package ctrmap.creativestudio.ngcs2d.canvas.tools;

import ctrmap.creativestudio.ngcs2d.canvas.undo.PixelEditAction;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;

/**
 * Brush tool that paints in a square area.
 */
public class BrushTool extends Sprite2DBaseTool {

	private int brushSize = 3;
	private PixelEditAction currentAction;

	public void setBrushSize(int size) {
		this.brushSize = Math.max(1, Math.min(16, size));
	}

	public int getBrushSize() {
		return brushSize;
	}

	@Override
	public void onPressed(Sprite2DCanvasEvent e) {
		Sprite2DTileSheet ts = getActiveTileSheet();
		if (ts == null) {
			return;
		}
		currentAction = new PixelEditAction(ts);
		paintArea(ts, e.pixelX, e.pixelY);
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
		paintArea(ts, e.pixelX, e.pixelY);
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

	protected int getPaintColorIndex() {
		return foregroundColorIndex;
	}

	private void paintArea(Sprite2DTileSheet ts, int cx, int cy) {
		int half = brushSize / 2;
		int colorIdx = getPaintColorIndex();
		for (int dy = -half; dy < brushSize - half; dy++) {
			for (int dx = -half; dx < brushSize - half; dx++) {
				int px = cx + dx;
				int py = cy + dy;
				int tileIdx = ts.pixelToTileIndex(px, py);
				if (tileIdx < 0) {
					continue;
				}
				int localX = px & 7;
				int localY = py & 7;
				int oldVal = ts.getPixel(tileIdx, localX, localY);
				if (oldVal != colorIdx) {
					ts.setPixel(tileIdx, localX, localY, colorIdx);
					if (currentAction != null) {
						currentAction.addChange(tileIdx, localX, localY, oldVal, colorIdx);
					}
				}
			}
		}
	}

	@Override
	public String getToolName() {
		return "Brush";
	}
}
