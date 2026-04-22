package ctrmap.creativestudio.ngcs2d.project.yml;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;
import java.util.ArrayList;
import java.util.List;
import xstandard.formats.yaml.Yaml;
import xstandard.formats.yaml.YamlReflectUtil;
import xstandard.fs.FSFile;

/**
 * YAML record for a {@link Sprite2DMultiCellAnimation}.
 */
public class MultiCellAnimYml extends Yaml {

	public String name;
	public int playMode = 1;
	public List<MultiCellAnimFrameYml> frames = new ArrayList<>();

	public MultiCellAnimYml() {
		super();
	}

	public MultiCellAnimYml(FSFile fsf) {
		super(fsf);
		YamlReflectUtil.deserializeToObject(root, this);
	}

	@Override
	public void write() {
		root.removeAllChildren();
		YamlReflectUtil.addFieldsToNode(root, this);
		super.write();
	}

	public static MultiCellAnimYml from(Sprite2DMultiCellAnimation a) {
		MultiCellAnimYml y = new MultiCellAnimYml();
		y.name = a.name;
		y.playMode = a.playMode;
		y.frames = new ArrayList<>(a.frames.size());
		for (Sprite2DMultiCellAnimation.MultiCellAnimFrame f : a.frames) {
			y.frames.add(MultiCellAnimFrameYml.from(f));
		}
		return y;
	}

	public Sprite2DMultiCellAnimation toResource() {
		Sprite2DMultiCellAnimation a = new Sprite2DMultiCellAnimation();
		a.name = name;
		a.playMode = playMode;
		a.frames = new ArrayList<>(frames.size());
		for (MultiCellAnimFrameYml f : frames) {
			a.frames.add(f.toResource());
		}
		return a;
	}
}
