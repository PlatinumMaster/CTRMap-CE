package ctrmap.creativestudio.ngcs2d.canvas.tools;

import ctrmap.creativestudio.ngcs2d.canvas.SpriteCanvas;
import ctrmap.creativestudio.ngcs2d.canvas.SpriteRenderer;
import ctrmap.creativestudio.ngcs2d.canvas.undo.MoveMultiCellEntryAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.MoveOAMAction;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;
import java.awt.Cursor;

/**
 * Move tool for repositioning OAM entries within a cell, or multi-cell entries
 * when the canvas is showing a multi-cell preview.
 */
public class MoveTool extends Sprite2DBaseTool {

	private Sprite2DOAM draggedOAM;
	private Sprite2DMultiCell.MultiCellEntry draggedEntry;
	private int dragStartX, dragStartY;
	private int oamStartX, oamStartY;
	private short entryStartX, entryStartY;

	@Override
	public void onPressed(Sprite2DCanvasEvent e) {
		draggedOAM = null;
		draggedEntry = null;
		if (resource == null) {
			return;
		}

		// Multi-cell preview takes precedence: if the canvas is showing a
		// multi-cell, hit-test against its cached layout instead of walking
		// raw OAM lists. The cached layout's coordinates already live in
		// rendered-image pixel space, which is what makeToolEvent feeds us.
		if (canvas != null) {
			SpriteCanvas.DisplayMode mode = canvas.getDisplayMode();
			if (mode == SpriteCanvas.DisplayMode.MULTI_CELL
				|| mode == SpriteCanvas.DisplayMode.MULTI_CELL_ANIMATION) {
				SpriteRenderer.MultiCellEntryLayout slot = canvas.hitTestMultiCellEntry(e.pixelX, e.pixelY);
				if (slot != null) {
					draggedEntry = slot.entry;
					dragStartX = e.pixelX;
					dragStartY = e.pixelY;
					entryStartX = slot.entry.x;
					entryStartY = slot.entry.y;
				}
				return;
			}
		}

		if (resource.cells.isEmpty()) {
			return;
		}
		for (Sprite2DCell cell : resource.cells) {
			for (Sprite2DOAM oam : cell.oams) {
				if (e.pixelX >= oam.x && e.pixelX < oam.x + oam.width
					&& e.pixelY >= oam.y && e.pixelY < oam.y + oam.height) {
					draggedOAM = oam;
					dragStartX = e.pixelX;
					dragStartY = e.pixelY;
					oamStartX = oam.x;
					oamStartY = oam.y;
					return;
				}
			}
		}
	}

	@Override
	public void onDragged(Sprite2DCanvasEvent e) {
		if (draggedEntry != null) {
			draggedEntry.x = (short) (entryStartX + (e.pixelX - dragStartX));
			draggedEntry.y = (short) (entryStartY + (e.pixelY - dragStartY));
			if (canvas != null) {
				canvas.refreshRender();
			}
			return;
		}
		if (draggedOAM != null) {
			draggedOAM.x = oamStartX + (e.pixelX - dragStartX);
			draggedOAM.y = oamStartY + (e.pixelY - dragStartY);
			if (canvas != null) {
				canvas.refreshRender();
			}
		}
	}

	@Override
	public void onReleased(Sprite2DCanvasEvent e) {
		if (draggedEntry != null && undoManager != null) {
			short newX = draggedEntry.x;
			short newY = draggedEntry.y;
			if (newX != entryStartX || newY != entryStartY) {
				MoveMultiCellEntryAction action = new MoveMultiCellEntryAction(
					draggedEntry, entryStartX, entryStartY, newX, newY);
				undoManager.perform(action);
			}
		} else if (draggedOAM != null && undoManager != null) {
			int newX = draggedOAM.x;
			int newY = draggedOAM.y;
			if (newX != oamStartX || newY != oamStartY) {
				MoveOAMAction action = new MoveOAMAction(draggedOAM, oamStartX, oamStartY, newX, newY);
				undoManager.perform(action);
			}
		}
		draggedOAM = null;
		draggedEntry = null;
	}

	@Override
	public Cursor getCursor() {
		return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
	}

	@Override
	public String getToolName() {
		return "Move";
	}
}
