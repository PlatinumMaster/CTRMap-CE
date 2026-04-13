package ctrmap.creativestudio.ngcs2d.canvas.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;

/**
 * Rectangular marquee selection tool.
 */
public class SelectTool extends Sprite2DBaseTool {

	private Rectangle selection = null;
	private int dragStartX, dragStartY;

	/**
	 * Gets the current selection rectangle, or null if none.
	 */
	public Rectangle getSelection() {
		return selection;
	}

	/**
	 * Clears the selection.
	 */
	public void clearSelection() {
		selection = null;
	}

	@Override
	public void onPressed(Sprite2DCanvasEvent e) {
		dragStartX = e.pixelX;
		dragStartY = e.pixelY;
		selection = new Rectangle(dragStartX, dragStartY, 0, 0);
	}

	@Override
	public void onDragged(Sprite2DCanvasEvent e) {
		int x = Math.min(dragStartX, e.pixelX);
		int y = Math.min(dragStartY, e.pixelY);
		int w = Math.abs(e.pixelX - dragStartX);
		int h = Math.abs(e.pixelY - dragStartY);
		selection = new Rectangle(x, y, w, h);
		if (canvas != null) {
			canvas.repaint();
		}
	}

	@Override
	public void onReleased(Sprite2DCanvasEvent e) {
		if (selection != null && (selection.width == 0 || selection.height == 0)) {
			selection = null;
		}
	}

	@Override
	public void paintOverlay(Graphics2D g, AffineTransform canvasTransform) {
		if (selection == null) {
			return;
		}
		AffineTransform saved = g.getTransform();
		g.setTransform(canvasTransform);

		Stroke dashedStroke = new BasicStroke(
			1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
			10.0f, new float[]{4.0f, 4.0f}, 0.0f
		);
		g.setStroke(dashedStroke);
		g.setColor(Color.WHITE);
		g.drawRect(selection.x, selection.y, selection.width, selection.height);
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke(
			1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
			10.0f, new float[]{4.0f, 4.0f}, 4.0f
		));
		g.drawRect(selection.x, selection.y, selection.width, selection.height);

		g.setTransform(saved);
	}

	@Override
	public Cursor getCursor() {
		return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	}

	@Override
	public String getToolName() {
		return "Select";
	}
}
