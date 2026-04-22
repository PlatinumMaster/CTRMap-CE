package ctrmap.creativestudio.ngcs2d.project;

import ctrmap.creativestudio.ngcs2d.project.yml.CellAnimYml;
import ctrmap.creativestudio.ngcs2d.project.yml.CellYml;
import ctrmap.creativestudio.ngcs2d.project.yml.MultiCellAnimYml;
import ctrmap.creativestudio.ngcs2d.project.yml.MultiCellYml;
import ctrmap.creativestudio.ngcs2d.project.yml.PaletteYml;
import ctrmap.creativestudio.ngcs2d.project.yml.ProjectMetadataYml;
import ctrmap.creativestudio.ngcs2d.project.yml.TileSheetYml;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import xstandard.fs.FSFile;

/**
 * Reads and writes a {@link Cs2dProject} as a directory tree:
 *
 * <pre>
 * project_root/
 * ├── project.yml
 * ├── palettes/{NN}_{name}.yml
 * ├── tilesheets/{NN}_{name}.yml + .png
 * ├── cells/{NN}_{name}.yml
 * ├── multicells/{NN}_{name}.yml
 * ├── cellanims/{NN}_{name}.yml
 * └── multicellanims/{NN}_{name}.yml
 * </pre>
 *
 * <p>The {@code NN_} prefix is a zero-padded list index for stable
 * lexicographic ordering on disk so git diffs stay clean.</p>
 */
public final class Cs2dProjectIO {

	private static final String DIR_PALETTES = "palettes";
	private static final String DIR_TILESHEETS = "tilesheets";
	private static final String DIR_CELLS = "cells";
	private static final String DIR_MULTICELLS = "multicells";
	private static final String DIR_CELLANIMS = "cellanims";
	private static final String DIR_MULTICELLANIMS = "multicellanims";
	private static final String FILE_PROJECT = "project.yml";

	private Cs2dProjectIO() {
	}

	// =====================================================================
	// SAVE
	// =====================================================================

	public static void save(Cs2dProject project, FSFile dir) throws IOException {
		Sprite2DResource res = project.resource;
		dir.mkdir();

		// project.yml
		ProjectMetadataYml meta = new ProjectMetadataYml();
		meta.formatVersion = project.formatVersion;
		meta.mappingMode = res.mappingMode;
		meta.writeToFile(dir.getChild(FILE_PROJECT));

		FSFile palDir = dir.getChild(DIR_PALETTES);
		clearDir(palDir);
		palDir.mkdir();
		for (int i = 0; i < res.palettes.size(); i++) {
			Sprite2DPalette p = res.palettes.get(i);
			PaletteYml.from(p).writeToFile(palDir.getChild(filename(i, p.name) + ".yml"));
		}

		FSFile tsDir = dir.getChild(DIR_TILESHEETS);
		clearDir(tsDir);
		tsDir.mkdir();
		Sprite2DPalette previewPalette = res.palettes.isEmpty() ? null : res.palettes.get(0);
		for (int i = 0; i < res.tileSheets.size(); i++) {
			Sprite2DTileSheet s = res.tileSheets.get(i);
			String base = filename(i, s.name);
			TileSheetYml.from(s).writeToFile(tsDir.getChild(base + ".yml"));
			IndexedPngCodec.write(s, previewPalette, tsDir.getChild(base + ".png"));
		}

		FSFile cellDir = dir.getChild(DIR_CELLS);
		clearDir(cellDir);
		cellDir.mkdir();
		for (int i = 0; i < res.cells.size(); i++) {
			Sprite2DCell c = res.cells.get(i);
			CellYml.from(c).writeToFile(cellDir.getChild(filename(i, c.name) + ".yml"));
		}

		FSFile mcDir = dir.getChild(DIR_MULTICELLS);
		clearDir(mcDir);
		mcDir.mkdir();
		for (int i = 0; i < res.multiCells.size(); i++) {
			Sprite2DMultiCell mc = res.multiCells.get(i);
			MultiCellYml.from(mc).writeToFile(mcDir.getChild(filename(i, mc.name) + ".yml"));
		}

		FSFile caDir = dir.getChild(DIR_CELLANIMS);
		clearDir(caDir);
		caDir.mkdir();
		for (int i = 0; i < res.cellAnimations.size(); i++) {
			Sprite2DCellAnimation a = res.cellAnimations.get(i);
			CellAnimYml.from(a).writeToFile(caDir.getChild(filename(i, a.name) + ".yml"));
		}

		FSFile mcaDir = dir.getChild(DIR_MULTICELLANIMS);
		clearDir(mcaDir);
		mcaDir.mkdir();
		for (int i = 0; i < res.multiCellAnimations.size(); i++) {
			Sprite2DMultiCellAnimation a = res.multiCellAnimations.get(i);
			MultiCellAnimYml.from(a).writeToFile(mcaDir.getChild(filename(i, a.name) + ".yml"));
		}

		project.rootDir = dir;
	}

	// =====================================================================
	// LOAD
	// =====================================================================

	public static Cs2dProject load(FSFile dir) throws IOException {
		Sprite2DResource res = new Sprite2DResource();
		Cs2dProject project = new Cs2dProject(res, dir);

		FSFile metaFile = dir.getChild(FILE_PROJECT);
		if (metaFile != null && metaFile.exists()) {
			ProjectMetadataYml meta = new ProjectMetadataYml(metaFile);
			project.formatVersion = meta.formatVersion;
			res.mappingMode = meta.mappingMode;
		}

		for (FSFile f : sortedYmlChildren(dir.getChild(DIR_PALETTES))) {
			PaletteYml y = new PaletteYml(f);
			res.palettes.add(y.toResource());
		}

		FSFile tsDir = dir.getChild(DIR_TILESHEETS);
		List<FSFile> tsYmls = sortedYmlChildren(tsDir);
		for (FSFile ymlFile : tsYmls) {
			TileSheetYml y = new TileSheetYml(ymlFile);
			Sprite2DTileSheet shell = y.toResourceShell();
			FSFile pngFile = tsDir.getChild(stripExt(ymlFile.getName()) + ".png");
			if (pngFile != null && pngFile.exists()) {
				shell.tileData = IndexedPngCodec.read(pngFile, shell);
			} else {
				shell.tileData = new byte[0];
			}
			res.tileSheets.add(shell);
		}

		for (FSFile f : sortedYmlChildren(dir.getChild(DIR_CELLS))) {
			res.cells.add(new CellYml(f).toResource());
		}
		for (FSFile f : sortedYmlChildren(dir.getChild(DIR_MULTICELLS))) {
			res.multiCells.add(new MultiCellYml(f).toResource());
		}
		for (FSFile f : sortedYmlChildren(dir.getChild(DIR_CELLANIMS))) {
			res.cellAnimations.add(new CellAnimYml(f).toResource());
		}
		for (FSFile f : sortedYmlChildren(dir.getChild(DIR_MULTICELLANIMS))) {
			res.multiCellAnimations.add(new MultiCellAnimYml(f).toResource());
		}

		return project;
	}

	// =====================================================================
	// helpers
	// =====================================================================

	private static String filename(int index, String name) {
		String safe = sanitize(name == null ? "unnamed" : name);
		return String.format("%02d_%s", index, safe);
	}

	private static String sanitize(String name) {
		// Replace anything that's not alphanumeric / dash / underscore with
		// an underscore so the file names work on every OS without
		// surprising the user's git client.
		StringBuilder sb = new StringBuilder(name.length());
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
				|| (c >= '0' && c <= '9') || c == '-' || c == '_') {
				sb.append(c);
			} else {
				sb.append('_');
			}
		}
		return sb.length() == 0 ? "unnamed" : sb.toString();
	}

	private static String stripExt(String name) {
		int dot = name.lastIndexOf('.');
		return dot < 0 ? name : name.substring(0, dot);
	}

	private static void clearDir(FSFile dir) {
		if (dir == null || !dir.exists() || !dir.isDirectory()) {
			return;
		}
		List<? extends FSFile> kids = dir.listFiles();
		if (kids == null) return;
		for (FSFile k : kids) {
			k.delete();
		}
	}

	private static List<FSFile> sortedYmlChildren(FSFile dir) {
		if (dir == null || !dir.exists() || !dir.isDirectory()) {
			return Collections.emptyList();
		}
		List<? extends FSFile> kids = dir.listFiles();
		if (kids == null || kids.isEmpty()) {
			return Collections.emptyList();
		}
		List<FSFile> out = new ArrayList<>(kids.size());
		for (FSFile k : kids) {
			String n = k.getName();
			if (n.endsWith(".yml") || n.endsWith(".yaml")) {
				out.add(k);
			}
		}
		out.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
		return out;
	}
}
