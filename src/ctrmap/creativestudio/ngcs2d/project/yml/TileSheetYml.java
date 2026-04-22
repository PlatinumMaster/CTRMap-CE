package ctrmap.creativestudio.ngcs2d.project.yml;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import xstandard.formats.yaml.Yaml;
import xstandard.formats.yaml.YamlReflectUtil;
import xstandard.fs.FSFile;

/**
 * YAML record for a {@link Sprite2DTileSheet}'s metadata.
 * The tile pixel data is stored in a sibling indexed PNG —
 * {@link ctrmap.creativestudio.ngcs2d.project.IndexedPngCodec} handles
 * that side. Field naming mirrors the resource for round-trip clarity.
 */
public class TileSheetYml extends Yaml {

	public String name;
	public int format = 4;
	public int tileWidth = -1;
	public int tileHeight = -1;
	public boolean isLinearMapped;
	public int objTilesWide = 1;
	public int objTilesHigh = 1;
	public boolean rasterLayout;

	public TileSheetYml() {
		super();
	}

	public TileSheetYml(FSFile fsf) {
		super(fsf);
		YamlReflectUtil.deserializeToObject(root, this);
	}

	@Override
	public void write() {
		root.removeAllChildren();
		YamlReflectUtil.addFieldsToNode(root, this);
		super.write();
	}

	public static TileSheetYml from(Sprite2DTileSheet s) {
		TileSheetYml y = new TileSheetYml();
		y.name = s.name;
		y.format = s.format;
		y.tileWidth = s.tileWidth;
		y.tileHeight = s.tileHeight;
		y.isLinearMapped = s.isLinearMapped;
		y.objTilesWide = s.objTilesWide;
		y.objTilesHigh = s.objTilesHigh;
		y.rasterLayout = s.rasterLayout;
		return y;
	}

	/** Builds an empty {@link Sprite2DTileSheet} with the YAML fields applied.
	 *  Pixel data must be loaded separately and assigned to {@code tileData}. */
	public Sprite2DTileSheet toResourceShell() {
		Sprite2DTileSheet s = new Sprite2DTileSheet();
		s.name = name;
		s.format = format;
		s.tileWidth = tileWidth;
		s.tileHeight = tileHeight;
		s.isLinearMapped = isLinearMapped;
		s.objTilesWide = objTilesWide;
		s.objTilesHigh = objTilesHigh;
		s.rasterLayout = rasterLayout;
		return s;
	}
}
