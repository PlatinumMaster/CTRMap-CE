package ctrmap.creativestudio.ngcs2d.project.yml;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DAnimFrame;

/**
 * YAML record for one {@link Sprite2DAnimFrame}.
 */
public class CellAnimFrameYml {

	public int cellIndex;
	public int duration = 1;
	public float translateX;
	public float translateY;
	public float scaleX = 1.0f;
	public float scaleY = 1.0f;
	public float rotation;

	public CellAnimFrameYml() {
	}

	public static CellAnimFrameYml from(Sprite2DAnimFrame f) {
		CellAnimFrameYml y = new CellAnimFrameYml();
		y.cellIndex = f.cellIndex;
		y.duration = f.duration;
		y.translateX = f.translateX;
		y.translateY = f.translateY;
		y.scaleX = f.scaleX;
		y.scaleY = f.scaleY;
		y.rotation = f.rotation;
		return y;
	}

	public Sprite2DAnimFrame toResource() {
		Sprite2DAnimFrame f = new Sprite2DAnimFrame();
		f.cellIndex = cellIndex;
		f.duration = duration;
		f.translateX = translateX;
		f.translateY = translateY;
		f.scaleX = scaleX;
		f.scaleY = scaleY;
		f.rotation = rotation;
		return f;
	}
}
