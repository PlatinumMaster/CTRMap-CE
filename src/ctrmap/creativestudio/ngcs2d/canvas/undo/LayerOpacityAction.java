package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.layers.LayerItem;

/**
 * Undoable action for changing a layer's opacity.
 *
 * <p>The Layers panel coalesces continuous slider drags into a single
 * action: the drag's starting value is captured on slider press and the
 * final value is captured on release, producing one undoable step per
 * drag gesture instead of one per intermediate tick.</p>
 */
public class LayerOpacityAction implements SpriteAction {

	private final LayerItem item;
	private final float oldOpacity;
	private final float newOpacity;

	public LayerOpacityAction(LayerItem item, float oldOpacity, float newOpacity) {
		this.item = item;
		this.oldOpacity = oldOpacity;
		this.newOpacity = newOpacity;
	}

	@Override
	public void execute() {
		item.setOpacity(newOpacity);
	}

	@Override
	public void undo() {
		item.setOpacity(oldOpacity);
	}

	@Override
	public String getDescription() {
		return String.format("Opacity %.0f%% -> %.0f%%",
			oldOpacity * 100f, newOpacity * 100f);
	}
}
