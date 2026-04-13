package ctrmap.creativestudio.ngcs2d.canvas.tools;

import java.awt.Cursor;

/**
 * Pan tool for scrolling the canvas viewport.
 */
public class PanTool extends Sprite2DBaseTool {

	private int lastMouseX, lastMouseY;

	@Override
	public void onPressed(Sprite2DCanvasEvent e) {
		lastMouseX = e.sourceEvent.getX();
		lastMouseY = e.sourceEvent.getY();
	}

	@Override
	public void onDragged(Sprite2DCanvasEvent e) {
		// Pan is handled at canvas level via middle mouse, but this tool
		// enables panning with left mouse button too
		if (canvas != null) {
			// Not directly settable on SpriteCanvas yet; this is a placeholder
		}
		lastMouseX = e.sourceEvent.getX();
		lastMouseY = e.sourceEvent.getY();
	}

	@Override
	public void onReleased(Sprite2DCanvasEvent e) {
	}

	@Override
	public Cursor getCursor() {
		return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
	}

	@Override
	public String getToolName() {
		return "Pan";
	}
}
