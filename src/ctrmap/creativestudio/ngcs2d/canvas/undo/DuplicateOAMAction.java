package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;

/**
 * Undoable action for duplicating an OAM. Inserts {@code copy}
 * immediately after {@code sourceIndex} on execute; removes it on undo.
 * The caller deep-copies the source via {@link Sprite2DOAM#Sprite2DOAM(Sprite2DOAM)}
 * so the new instance has its own identity.
 */
public class DuplicateOAMAction implements SpriteAction {

	private final Sprite2DCell cell;
	private final int insertIndex;
	private final Sprite2DOAM copy;

	public DuplicateOAMAction(Sprite2DCell cell, int insertIndex, Sprite2DOAM copy) {
		this.cell = cell;
		this.insertIndex = insertIndex;
		this.copy = copy;
	}

	@Override
	public void execute() {
		int i = Math.max(0, Math.min(insertIndex, cell.oams.size()));
		cell.oams.add(i, copy);
	}

	@Override
	public void undo() {
		cell.oams.remove(copy);
	}

	@Override
	public String getDescription() {
		return "Duplicate OAM @ " + insertIndex;
	}
}
