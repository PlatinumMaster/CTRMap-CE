package ctrmap.creativestudio.ngcs2d.project.yml;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DAnimFrame;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCellAnimation;
import java.util.ArrayList;
import java.util.List;
import xstandard.formats.yaml.Yaml;
import xstandard.formats.yaml.YamlReflectUtil;
import xstandard.fs.FSFile;

/**
 * YAML record for a {@link Sprite2DCellAnimation}.
 */
public class CellAnimYml extends Yaml {

	public String name;
	public int playMode = 1;
	public List<CellAnimFrameYml> frames = new ArrayList<>();

	public CellAnimYml() {
		super();
	}

	public CellAnimYml(FSFile fsf) {
		super(fsf);
		YamlReflectUtil.deserializeToObject(root, this);
	}

	@Override
	public void write() {
		root.removeAllChildren();
		YamlReflectUtil.addFieldsToNode(root, this);
		super.write();
	}

	public static CellAnimYml from(Sprite2DCellAnimation a) {
		CellAnimYml y = new CellAnimYml();
		y.name = a.name;
		y.playMode = a.playMode;
		y.frames = new ArrayList<>(a.frames.size());
		for (Sprite2DAnimFrame f : a.frames) {
			y.frames.add(CellAnimFrameYml.from(f));
		}
		return y;
	}

	public Sprite2DCellAnimation toResource() {
		Sprite2DCellAnimation a = new Sprite2DCellAnimation();
		a.name = name;
		a.playMode = playMode;
		a.frames = new ArrayList<>(frames.size());
		for (CellAnimFrameYml f : frames) {
			a.frames.add(f.toResource());
		}
		return a;
	}
}
