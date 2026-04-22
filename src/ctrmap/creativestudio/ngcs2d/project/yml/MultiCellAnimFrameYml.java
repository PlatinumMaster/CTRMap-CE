package ctrmap.creativestudio.ngcs2d.project.yml;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;

/**
 * YAML record for one {@link Sprite2DMultiCellAnimation.MultiCellAnimFrame}.
 */
public class MultiCellAnimFrameYml {

	public int multiCellIndex;
	public int duration = 1;

	public MultiCellAnimFrameYml() {
	}

	public static MultiCellAnimFrameYml from(Sprite2DMultiCellAnimation.MultiCellAnimFrame f) {
		MultiCellAnimFrameYml y = new MultiCellAnimFrameYml();
		y.multiCellIndex = f.multiCellIndex;
		y.duration = f.duration;
		return y;
	}

	public Sprite2DMultiCellAnimation.MultiCellAnimFrame toResource() {
		return new Sprite2DMultiCellAnimation.MultiCellAnimFrame(multiCellIndex, duration);
	}
}
