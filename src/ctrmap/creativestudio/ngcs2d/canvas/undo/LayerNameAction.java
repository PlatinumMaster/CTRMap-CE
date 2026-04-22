package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.layers.LayerItem;

/**
 * Undoable action for renaming a layer.
 *
 * <p>The Layers panel commits a single rename action on field blur or
 * Enter press, not on every keystroke, to keep the undo stack readable.</p>
 */
public class LayerNameAction implements SpriteAction {

	private final LayerItem item;
	private final String oldName;
	private final String newName;

	public LayerNameAction(LayerItem item, String oldName, String newName) {
		this.item = item;
		this.oldName = oldName;
		this.newName = newName;
	}

	@Override
	public void execute() {
		item.setLayerName(newName);
	}

	@Override
	public void undo() {
		item.setLayerName(oldName);
	}

	@Override
	public String getDescription() {
		return "Rename layer \"" + (oldName == null ? "" : oldName)
			+ "\" -> \"" + (newName == null ? "" : newName) + "\"";
	}
}
