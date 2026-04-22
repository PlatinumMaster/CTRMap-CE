package ctrmap.creativestudio.ngcs2d.project.yml;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;
import java.util.ArrayList;
import java.util.List;
import xstandard.formats.yaml.Yaml;
import xstandard.formats.yaml.YamlReflectUtil;
import xstandard.fs.FSFile;

/**
 * YAML record for a {@link Sprite2DCell}.
 */
public class CellYml extends Yaml {

	public String name;
	public boolean hasVramTransfer;
	public int vramTransferSrcAddr;
	public int vramTransferSize;
	public List<OamYml> oams = new ArrayList<>();

	public CellYml() {
		super();
	}

	public CellYml(FSFile fsf) {
		super(fsf);
		YamlReflectUtil.deserializeToObject(root, this);
	}

	@Override
	public void write() {
		root.removeAllChildren();
		YamlReflectUtil.addFieldsToNode(root, this);
		super.write();
	}

	public static CellYml from(Sprite2DCell c) {
		CellYml y = new CellYml();
		y.name = c.name;
		y.hasVramTransfer = c.hasVramTransfer;
		y.vramTransferSrcAddr = c.vramTransferSrcAddr;
		y.vramTransferSize = c.vramTransferSize;
		y.oams = new ArrayList<>(c.oams.size());
		for (Sprite2DOAM oam : c.oams) {
			y.oams.add(OamYml.from(oam));
		}
		return y;
	}

	public Sprite2DCell toResource() {
		Sprite2DCell c = new Sprite2DCell();
		c.name = name;
		c.hasVramTransfer = hasVramTransfer;
		c.vramTransferSrcAddr = vramTransferSrcAddr;
		c.vramTransferSize = vramTransferSize;
		c.oams = new ArrayList<>(oams.size());
		for (OamYml o : oams) {
			c.oams.add(o.toResource());
		}
		return c;
	}
}
