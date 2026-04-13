package ctrmap.creativestudio.ngcs2d.canvas.tools;

import ctrmap.creativestudio.ngcs2d.canvas.undo.PixelEditAction;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Flood fill tool that fills connected pixels with the foreground color.
 */
public class FillTool extends Sprite2DBaseTool {

	@Override
	public void onPressed(Sprite2DCanvasEvent e) {
		Sprite2DTileSheet ts = getActiveTileSheet();
		if (ts == null) {
			return;
		}
		int totalTiles = ts.getTileCount();
		if (totalTiles == 0) {
			return;
		}

		int tilesWide = ts.getEffectiveTileWidth();
		int tilesHigh = ts.getEffectiveTileHeight();
		int imgW = tilesWide * 8;
		int imgH = tilesHigh * 8;

		int startX = e.pixelX;
		int startY = e.pixelY;
		if (startX < 0 || startY < 0 || startX >= imgW || startY >= imgH) {
			return;
		}

		int startTile = ts.pixelToTileIndex(startX, startY);
		if (startTile < 0) {
			return;
		}

		int targetColor = ts.getPixel(startTile, startX & 7, startY & 7);
		if (targetColor == foregroundColorIndex) {
			return;
		}

		PixelEditAction action = new PixelEditAction(ts);
		boolean[][] visited = new boolean[imgW][imgH];
		Deque<int[]> queue = new ArrayDeque<>();
		queue.add(new int[]{startX, startY});

		while (!queue.isEmpty()) {
			int[] pt = queue.poll();
			int px = pt[0];
			int py = pt[1];
			if (px < 0 || py < 0 || px >= imgW || py >= imgH) {
				continue;
			}
			if (visited[px][py]) {
				continue;
			}
			visited[px][py] = true;

			int tileIdx = ts.pixelToTileIndex(px, py);
			if (tileIdx < 0) {
				continue;
			}
			int localX = px & 7;
			int localY = py & 7;
			int curColor = ts.getPixel(tileIdx, localX, localY);
			if (curColor != targetColor) {
				continue;
			}

			ts.setPixel(tileIdx, localX, localY, foregroundColorIndex);
			action.addChange(tileIdx, localX, localY, curColor, foregroundColorIndex);

			queue.add(new int[]{px + 1, py});
			queue.add(new int[]{px - 1, py});
			queue.add(new int[]{px, py + 1});
			queue.add(new int[]{px, py - 1});
		}

		if (action.hasChanges() && undoManager != null) {
			undoManager.perform(action);
		}
		if (canvas != null) {
			canvas.refreshRender();
		}
	}

	@Override
	public void onDragged(Sprite2DCanvasEvent e) {
	}

	@Override
	public void onReleased(Sprite2DCanvasEvent e) {
	}

	@Override
	public String getToolName() {
		return "Fill";
	}
}
