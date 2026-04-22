package ctrmap.creativestudio.ngcs2d.project.yml;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;

/**
 * YAML record for one {@link Sprite2DMultiCell.MultiCellEntry}.
 */
public class MultiCellEntryYml {

	public int animIndex;
	public short x;
	public short y;

	public String layerName;
	public boolean visible = true;
	public float opacity = 1.0f;
	public boolean locked;

	public MultiCellEntryYml() {
	}

	public static MultiCellEntryYml from(Sprite2DMultiCell.MultiCellEntry e) {
		MultiCellEntryYml y = new MultiCellEntryYml();
		y.animIndex = e.animIndex;
		y.x = e.x;
		y.y = e.y;
		y.layerName = e.layerName;
		y.visible = e.visible;
		y.opacity = e.opacity;
		y.locked = e.locked;
		return y;
	}

	public Sprite2DMultiCell.MultiCellEntry toResource() {
		Sprite2DMultiCell.MultiCellEntry e = new Sprite2DMultiCell.MultiCellEntry();
		e.animIndex = animIndex;
		e.x = x;
		e.y = y;
		e.layerName = layerName;
		e.visible = visible;
		e.opacity = opacity;
		e.locked = locked;
		return e;
	}
}
