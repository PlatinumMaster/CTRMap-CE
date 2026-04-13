package ctrmap.creativestudio.ngcs2d.canvas.tools;

import ctrmap.creativestudio.ngcs2d.canvas.SpriteCanvas;
import ctrmap.creativestudio.ngcs2d.canvas.undo.SpriteUndoManager;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

/**
 * Abstract base class for sprite tools providing common fields and defaults.
 */
public abstract class Sprite2DBaseTool implements Sprite2DTool {

	protected SpriteCanvas canvas;
	protected Sprite2DResource resource;
	protected SpriteUndoManager undoManager;
	protected int foregroundColorIndex = 1;

	public void setCanvas(SpriteCanvas c) {
		this.canvas = c;
	}

	public void setResource(Sprite2DResource r) {
		this.resource = r;
	}

	public void setUndoManager(SpriteUndoManager u) {
		this.undoManager = u;
	}

	public void setForegroundColorIndex(int idx) {
		this.foregroundColorIndex = idx;
	}

	/**
	 * Returns the tile sheet that the user is currently editing — preferring
	 * the canvas's active tile sheet over the first one in the resource.
	 *
	 * @return The active tile sheet, or null if none is available.
	 */
	protected Sprite2DTileSheet getActiveTileSheet() {
		if (canvas != null && canvas.getActiveTileSheet() != null) {
			return canvas.getActiveTileSheet();
		}
		if (resource != null && !resource.tileSheets.isEmpty()) {
			return resource.tileSheets.get(0);
		}
		return null;
	}

	@Override
	public void onMoved(Sprite2DCanvasEvent e) {
	}

	@Override
	public void paintOverlay(Graphics2D g, AffineTransform canvasTransform) {
	}

	@Override
	public Cursor getCursor() {
		return Cursor.getDefaultCursor();
	}
}
