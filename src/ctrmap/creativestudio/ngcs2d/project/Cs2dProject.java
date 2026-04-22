package ctrmap.creativestudio.ngcs2d.project;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import xstandard.fs.FSFile;

/**
 * In-memory representation of a CS2D project on disk: the loaded
 * {@link Sprite2DResource} plus the directory it lives in.
 *
 * <p>Saved as a directory tree (see {@link Cs2dProjectIO}). Git-friendly:
 * one YAML per resource, indexed PNGs for tile sheet pixel data.</p>
 */
public class Cs2dProject {

	public static final int FORMAT_VERSION = 1;

	public final Sprite2DResource resource;
	public FSFile rootDir;
	public int formatVersion = FORMAT_VERSION;

	public Cs2dProject(Sprite2DResource resource) {
		this.resource = resource;
	}

	public Cs2dProject(Sprite2DResource resource, FSFile rootDir) {
		this.resource = resource;
		this.rootDir = rootDir;
	}
}
