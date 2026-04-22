package ctrmap.creativestudio.ngcs2d.project.yml;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;

/**
 * YAML record for one {@link Sprite2DOAM}. Mirrors every field including
 * the editor-only layer metadata so a project round-trip preserves the
 * full editor state.
 */
public class OamYml {

	public int x;
	public int y;
	public int width = 8;
	public int height = 8;
	public int tileIndex;
	public int paletteIndex;
	public boolean flipH;
	public boolean flipV;
	public int priority;
	public boolean rotationScaling;
	public boolean doubleSize;
	public int rsParamIndex;

	public String layerName;
	public boolean visible = true;
	public float opacity = 1.0f;
	public boolean locked;

	public OamYml() {
	}

	public static OamYml from(Sprite2DOAM oam) {
		OamYml y = new OamYml();
		y.x = oam.x;
		y.y = oam.y;
		y.width = oam.width;
		y.height = oam.height;
		y.tileIndex = oam.tileIndex;
		y.paletteIndex = oam.paletteIndex;
		y.flipH = oam.flipH;
		y.flipV = oam.flipV;
		y.priority = oam.priority;
		y.rotationScaling = oam.rotationScaling;
		y.doubleSize = oam.doubleSize;
		y.rsParamIndex = oam.rsParamIndex;
		y.layerName = oam.layerName;
		y.visible = oam.visible;
		y.opacity = oam.opacity;
		y.locked = oam.locked;
		return y;
	}

	public Sprite2DOAM toResource() {
		Sprite2DOAM oam = new Sprite2DOAM();
		oam.x = x;
		oam.y = y;
		oam.width = width;
		oam.height = height;
		oam.tileIndex = tileIndex;
		oam.paletteIndex = paletteIndex;
		oam.flipH = flipH;
		oam.flipV = flipV;
		oam.priority = priority;
		oam.rotationScaling = rotationScaling;
		oam.doubleSize = doubleSize;
		oam.rsParamIndex = rsParamIndex;
		oam.layerName = layerName;
		oam.visible = visible;
		oam.opacity = opacity;
		oam.locked = locked;
		return oam;
	}
}
