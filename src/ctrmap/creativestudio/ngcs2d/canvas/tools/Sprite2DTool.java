package ctrmap.creativestudio.ngcs2d.canvas.tools;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * Interface for 2D sprite drawing tools.
 */
public interface Sprite2DTool {

	void onPressed(Sprite2DCanvasEvent e);

	void onDragged(Sprite2DCanvasEvent e);

	void onReleased(Sprite2DCanvasEvent e);

	void onMoved(Sprite2DCanvasEvent e);

	/**
	 * Paints a tool overlay (selection rect, brush cursor, etc.).
	 */
	void paintOverlay(Graphics2D g, AffineTransform canvasTransform);

	/**
	 * Returns the cursor to use when this tool is active.
	 */
	Cursor getCursor();

	/**
	 * Returns the display name of this tool.
	 */
	String getToolName();
}
