package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;

/**
 * Undoable action for adding or removing an OAM from a cell.
 */
public class CellStructureAction implements SpriteAction {

	private final Sprite2DCell cell;
	private final Sprite2DOAM oam;
	private final boolean isAdd;
	private final int index;

	/**
	 * @param cell  The cell being modified.
	 * @param oam   The OAM entry.
	 * @param isAdd True if this action adds the OAM, false if it removes.
	 * @param index The index position for add/remove.
	 */
	public CellStructureAction(Sprite2DCell cell, Sprite2DOAM oam, boolean isAdd, int index) {
		this.cell = cell;
		this.oam = oam;
		this.isAdd = isAdd;
		this.index = index;
	}

	@Override
	public void execute() {
		if (isAdd) {
			if (index >= 0 && index <= cell.oams.size()) {
				cell.oams.add(index, oam);
			} else {
				cell.oams.add(oam);
			}
		} else {
			cell.oams.remove(index);
		}
	}

	@Override
	public void undo() {
		if (isAdd) {
			cell.oams.remove(index);
		} else {
			if (index >= 0 && index <= cell.oams.size()) {
				cell.oams.add(index, oam);
			} else {
				cell.oams.add(oam);
			}
		}
	}

	@Override
	public String getDescription() {
		return (isAdd ? "Add" : "Remove") + " OAM at index " + index;
	}
}
