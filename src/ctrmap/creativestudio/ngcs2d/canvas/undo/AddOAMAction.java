package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;

/**
 * Undoable action for inserting an OAM into a cell at a given index.
 * Holds the inserted instance so redo can reinsert the same object
 * (preserving identity for any selection / highlight references).
 */
public class AddOAMAction implements SpriteAction {

	private final Sprite2DCell cell;
	private final int index;
	private final Sprite2DOAM oam;

	public AddOAMAction(Sprite2DCell cell, int index, Sprite2DOAM oam) {
		this.cell = cell;
		this.index = index;
		this.oam = oam;
	}

	@Override
	public void execute() {
		int i = Math.max(0, Math.min(index, cell.oams.size()));
		cell.oams.add(i, oam);
	}

	@Override
	public void undo() {
		cell.oams.remove(oam);
	}

	@Override
	public String getDescription() {
		return "Add OAM @ " + index;
	}
}
