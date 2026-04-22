package ctrmap.creativestudio.ngcs2d.project.yml;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;
import java.util.ArrayList;
import java.util.List;
import xstandard.formats.yaml.Yaml;
import xstandard.formats.yaml.YamlReflectUtil;
import xstandard.fs.FSFile;

/**
 * YAML record for a {@link Sprite2DMultiCell}.
 */
public class MultiCellYml extends Yaml {

	public String name;
	public List<MultiCellEntryYml> entries = new ArrayList<>();

	public MultiCellYml() {
		super();
	}

	public MultiCellYml(FSFile fsf) {
		super(fsf);
		YamlReflectUtil.deserializeToObject(root, this);
	}

	@Override
	public void write() {
		root.removeAllChildren();
		YamlReflectUtil.addFieldsToNode(root, this);
		super.write();
	}

	public static MultiCellYml from(Sprite2DMultiCell mc) {
		MultiCellYml y = new MultiCellYml();
		y.name = mc.name;
		y.entries = new ArrayList<>(mc.entries.size());
		for (Sprite2DMultiCell.MultiCellEntry e : mc.entries) {
			y.entries.add(MultiCellEntryYml.from(e));
		}
		return y;
	}

	public Sprite2DMultiCell toResource() {
		Sprite2DMultiCell mc = new Sprite2DMultiCell();
		mc.name = name;
		mc.entries = new ArrayList<>(entries.size());
		for (MultiCellEntryYml e : entries) {
			mc.entries.add(e.toResource());
		}
		return mc;
	}
}
