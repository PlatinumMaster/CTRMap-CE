package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.layers.LayerItem;

/**
 * Undoable action for toggling a layer's visibility.
 *
 * <p>Created when the user clicks the eye icon in the Layers panel.
 * Operates on any {@link LayerItem} — {@code MultiCellEntry} in the
 * primary case, {@code Sprite2DOAM} when editing a standalone cell.</p>
 */
public class LayerVisibilityAction implements SpriteAction {

	private final LayerItem item;
	private final boolean oldVisible;
	private final boolean newVisible;

	public LayerVisibilityAction(LayerItem item, boolean oldVisible, boolean newVisible) {
		this.item = item;
		this.oldVisible = oldVisible;
		this.newVisible = newVisible;
	}

	@Override
	public void execute() {
		item.setVisible(newVisible);
	}

	@Override
	public void undo() {
		item.setVisible(oldVisible);
	}

	@Override
	public String getDescription() {
		return (newVisible ? "Show" : "Hide") + " layer";
	}
}
