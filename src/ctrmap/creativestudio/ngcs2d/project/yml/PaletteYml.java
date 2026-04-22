package ctrmap.creativestudio.ngcs2d.project.yml;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;
import java.util.ArrayList;
import java.util.List;
import xstandard.formats.yaml.Yaml;
import xstandard.formats.yaml.YamlReflectUtil;
import xstandard.fs.FSFile;

/**
 * YAML record for a {@link Sprite2DPalette}. Colors stored as
 * {@code "#AARRGGBB"} hex strings for git-diff readability.
 */
public class PaletteYml extends Yaml {

	public String name;
	public int format = 16;
	public List<String> colors = new ArrayList<>();

	public PaletteYml() {
		super();
	}

	public PaletteYml(FSFile fsf) {
		super(fsf);
		YamlReflectUtil.deserializeToObject(root, this);
	}

	@Override
	public void write() {
		root.removeAllChildren();
		YamlReflectUtil.addFieldsToNode(root, this);
		super.write();
	}

	public static PaletteYml from(Sprite2DPalette p) {
		PaletteYml y = new PaletteYml();
		y.name = p.name;
		y.format = p.format;
		y.colors = new ArrayList<>(p.colors.length);
		for (int argb : p.colors) {
			y.colors.add(String.format("#%08X", argb));
		}
		return y;
	}

	public Sprite2DPalette toResource() {
		Sprite2DPalette p = new Sprite2DPalette();
		p.name = name;
		p.format = format;
		p.colors = new int[colors.size()];
		for (int i = 0; i < colors.size(); i++) {
			p.colors[i] = parseHexArgb(colors.get(i));
		}
		return p;
	}

	private static int parseHexArgb(String s) {
		if (s == null || s.isEmpty()) return 0;
		String hex = s.startsWith("#") ? s.substring(1) : s;
		// Use parseUnsignedInt because 0xFFxxxxxx overflows signed int.
		return (int) Long.parseLong(hex, 16);
	}
}
