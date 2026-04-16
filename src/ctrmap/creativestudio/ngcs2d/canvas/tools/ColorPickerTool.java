package ctrmap.creativestudio.ngcs2d.canvas.tools;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import java.awt.Cursor;

/**
 * Color picker (eyedropper) tool that reads the palette index at the cursor.
 */
public class ColorPickerTool extends Sprite2DBaseTool {

	@Override
	public void onPressed(Sprite2DCanvasEvent e) {
		Sprite2DTileSheet ts = getActiveTileSheet();
		if (ts == null) {
			return;
		}
		int tileIdx = ts.pixelToTileIndex(e.pixelX, e.pixelY);
		if (tileIdx < 0) {
			return;
		}
		int localX = e.pixelX & 7;
		int localY = e.pixelY & 7;
		foregroundColorIndex = ts.getPixel(tileIdx, localX, localY);
	}

	@Override
	public void onDragged(Sprite2DCanvasEvent e) {
	}

	@Override
	public void onReleased(Sprite2DCanvasEvent e) {
	}

	@Override
	public Cursor getCursor() {
		return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}

	@Override
	public String getToolName() {
		return "Color Picker";
	}
}
