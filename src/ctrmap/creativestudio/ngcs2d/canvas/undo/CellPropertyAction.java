package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;

/**
 * Undoable action for editing a single scalar/boolean property of a
 * {@link Sprite2DCell}. Used by the NCER inspector so each property
 * commit (focus-lost / checkbox toggle) becomes one undo step.
 */
public class CellPropertyAction implements SpriteAction {

	public enum Field {
		NAME, HAS_VRAM_TRANSFER, VRAM_TRANSFER_SRC_ADDR, VRAM_TRANSFER_SIZE
	}

	private final Sprite2DCell target;
	private final Field field;
	private final Object oldValue;
	private final Object newValue;

	public CellPropertyAction(Sprite2DCell target, Field field,
			Object oldValue, Object newValue) {
		this.target = target;
		this.field = field;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	@Override
	public void execute() {
		apply(newValue);
	}

	@Override
	public void undo() {
		apply(oldValue);
	}

	private void apply(Object v) {
		switch (field) {
			case NAME: target.name = (String) v; break;
			case HAS_VRAM_TRANSFER: target.hasVramTransfer = (Boolean) v; break;
			case VRAM_TRANSFER_SRC_ADDR: target.vramTransferSrcAddr = (Integer) v; break;
			case VRAM_TRANSFER_SIZE: target.vramTransferSize = (Integer) v; break;
		}
	}

	@Override
	public String getDescription() {
		return "Cell " + field.name() + ": " + oldValue + " -> " + newValue;
	}
}
