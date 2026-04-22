package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.layers.LayerItem;

/**
 * Undoable action for toggling a layer's lock state. Locked layers are a
 * UI-level guard only — canvas tools refuse to mutate them — so the
 * canvas renderer doesn't care about this flag.
 */
public class LayerLockAction implements SpriteAction {

	private final LayerItem item;
	private final boolean oldLocked;
	private final boolean newLocked;

	public LayerLockAction(LayerItem item, boolean oldLocked, boolean newLocked) {
		this.item = item;
		this.oldLocked = oldLocked;
		this.newLocked = newLocked;
	}

	@Override
	public void execute() {
		item.setLocked(newLocked);
	}

	@Override
	public void undo() {
		item.setLocked(oldLocked);
	}

	@Override
	public String getDescription() {
		return (newLocked ? "Lock" : "Unlock") + " layer";
	}
}
