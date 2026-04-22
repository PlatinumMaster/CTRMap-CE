package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;

/**
 * Undoable action for changing the resource-level OAM mapping mode.
 * Lives on {@link Sprite2DResource} (not on a single tile sheet) because
 * the NCER mapping mode applies to every cell in the resource.
 */
public class MappingModeAction implements SpriteAction {

	private final Sprite2DResource target;
	private final int oldValue;
	private final int newValue;

	public MappingModeAction(Sprite2DResource target, int oldValue, int newValue) {
		this.target = target;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	@Override
	public void execute() {
		target.mappingMode = newValue;
	}

	@Override
	public void undo() {
		target.mappingMode = oldValue;
	}

	@Override
	public String getDescription() {
		return "Mapping mode: " + oldValue + " -> " + newValue;
	}
}
