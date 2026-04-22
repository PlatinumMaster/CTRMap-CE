package ctrmap.creativestudio.ngcs2d.project.yml;

import xstandard.formats.yaml.Yaml;
import xstandard.formats.yaml.YamlReflectUtil;
import xstandard.fs.FSFile;

/**
 * Top-level {@code project.yml} — version + resource-wide settings.
 */
public class ProjectMetadataYml extends Yaml {

	public int formatVersion = 1;
	public int mappingMode;

	public ProjectMetadataYml() {
		super();
	}

	public ProjectMetadataYml(FSFile fsf) {
		super(fsf);
		YamlReflectUtil.deserializeToObject(root, this);
	}

	@Override
	public void write() {
		root.removeAllChildren();
		YamlReflectUtil.addFieldsToNode(root, this);
		super.write();
	}
}
