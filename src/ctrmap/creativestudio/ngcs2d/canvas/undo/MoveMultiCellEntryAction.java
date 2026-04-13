package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;

/**
 * Undoable action for moving a multi-cell entry to a new position.
 */
public class MoveMultiCellEntryAction implements SpriteAction {

	private final Sprite2DMultiCell.MultiCellEntry entry;
	private final short oldX, oldY;
	private final short newX, newY;

	public MoveMultiCellEntryAction(Sprite2DMultiCell.MultiCellEntry entry, short oldX, short oldY, short newX, short newY) {
		this.entry = entry;
		this.oldX = oldX;
		this.oldY = oldY;
		this.newX = newX;
		this.newY = newY;
	}

	@Override
	public void execute() {
		entry.x = newX;
		entry.y = newY;
	}

	@Override
	public void undo() {
		entry.x = oldX;
		entry.y = oldY;
	}

	@Override
	public String getDescription() {
		return "Move MultiCell Entry (" + oldX + "," + oldY + ") -> (" + newX + "," + newY + ")";
	}
}
