package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;

/**
 * Undoable action for removing an OAM from a cell. Stores the removed
 * instance + its original index so undo can restore the same object at
 * the same position.
 */
public class RemoveOAMAction implements SpriteAction {

	private final Sprite2DCell cell;
	private final int index;
	private final Sprite2DOAM oam;

	public RemoveOAMAction(Sprite2DCell cell, int index, Sprite2DOAM oam) {
		this.cell = cell;
		this.index = index;
		this.oam = oam;
	}

	@Override
	public void execute() {
		cell.oams.remove(oam);
	}

	@Override
	public void undo() {
		int i = Math.max(0, Math.min(index, cell.oams.size()));
		cell.oams.add(i, oam);
	}

	@Override
	public String getDescription() {
		return "Remove OAM @ " + index;
	}
}
