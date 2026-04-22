package ctrmap.creativestudio.ngcs2d.canvas.undo;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;

/**
 * Undoable action for editing a single scalar/boolean property of a
 * {@link Sprite2DTileSheet}. Targeted by the NCGR property inspector
 * so each focus-lost / checkbox toggle becomes one undo step.
 */
public class TileSheetPropertyAction implements SpriteAction {

	public enum Field {
		NAME, TILE_WIDTH, TILE_HEIGHT,
		IS_LINEAR_MAPPED, OBJ_TILES_WIDE, OBJ_TILES_HIGH, RASTER_LAYOUT
	}

	private final Sprite2DTileSheet target;
	private final Field field;
	private final Object oldValue;
	private final Object newValue;

	public TileSheetPropertyAction(Sprite2DTileSheet target, Field field,
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
			case TILE_WIDTH: target.tileWidth = (Integer) v; break;
			case TILE_HEIGHT: target.tileHeight = (Integer) v; break;
			case IS_LINEAR_MAPPED: target.isLinearMapped = (Boolean) v; break;
			case OBJ_TILES_WIDE: target.objTilesWide = (Integer) v; break;
			case OBJ_TILES_HIGH: target.objTilesHigh = (Integer) v; break;
			case RASTER_LAYOUT: target.rasterLayout = (Boolean) v; break;
		}
	}

	@Override
	public String getDescription() {
		return "TileSheet " + field.name() + ": " + oldValue + " -> " + newValue;
	}
}
