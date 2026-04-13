package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;

/**
 * Undoable action for moving an OAM entry to a new position.
 */
public class MoveOAMAction implements SpriteAction {

	private final Sprite2DOAM oam;
	private final int oldX, oldY;
	private final int newX, newY;

	public MoveOAMAction(Sprite2DOAM oam, int oldX, int oldY, int newX, int newY) {
		this.oam = oam;
		this.oldX = oldX;
		this.oldY = oldY;
		this.newX = newX;
		this.newY = newY;
	}

	@Override
	public void execute() {
		oam.x = newX;
		oam.y = newY;
	}

	@Override
	public void undo() {
		oam.x = oldX;
		oam.y = oldY;
	}

	@Override
	public String getDescription() {
		return "Move OAM (" + oldX + "," + oldY + ") -> (" + newX + "," + newY + ")";
	}
}
