package ctrmap.creativestudio.ngcs2d.res;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import xstandard.INamed;
import xstandard.util.ListenableList;

/**
 * Main document container for 2D sprite resources. Parallel to G3DResource
 * for 3D models, this class aggregates all sprite data (palettes, tile sheets,
 * cells, animations) into a single resource container with observable lists.
 */
public class Sprite2DResource {

	/** OBJ VRAM mapping mode index 0 = 1D 32K (32-byte char boundary). */
	public static final int MAPPING_MODE_1D_32K = 0;
	/** OBJ VRAM mapping mode index 1 = 1D 64K (64-byte char boundary). */
	public static final int MAPPING_MODE_1D_64K = 1;
	/** OBJ VRAM mapping mode index 2 = 1D 128K (128-byte char boundary). */
	public static final int MAPPING_MODE_1D_128K = 2;
	/** OBJ VRAM mapping mode index 3 = 1D 256K (256-byte char boundary). */
	public static final int MAPPING_MODE_1D_256K = 3;
	/** OBJ VRAM mapping mode index 4 = 2D (32-char-wide grid). */
	public static final int MAPPING_MODE_2D = 4;

	/**
	 * OAM addressing mode used by this resource (mirrors the NCER mapping
	 * mode field). Defaults to {@link #MAPPING_MODE_2D} which matches the
	 * common bitmap-mode pairing used in modern (DPPt/HGSS/BW/BW2) sprite
	 * NARCs. Renderers consult this to choose how to walk an OAM's tiles
	 * through the underlying tile sheet.
	 */
	public int mappingMode = MAPPING_MODE_2D;

	/**
	 * Palettes used by the sprites in this resource.
	 */
	public final ListenableList<Sprite2DPalette> palettes = new ListenableList<>();

	/**
	 * Tile sheets containing pixel data for the sprites.
	 */
	public final ListenableList<Sprite2DTileSheet> tileSheets = new ListenableList<>();

	/**
	 * Cells composing OAM entries into displayable sprite images.
	 */
	public final ListenableList<Sprite2DCell> cells = new ListenableList<>();

	/**
	 * Cell animations defining frame sequences over cells.
	 */
	public final ListenableList<Sprite2DCellAnimation> cellAnimations = new ListenableList<>();

	/**
	 * Multi-cells composing cell animations at position offsets.
	 */
	public final ListenableList<Sprite2DMultiCell> multiCells = new ListenableList<>();

	/**
	 * Multi-cell animations defining frame sequences over multi-cells.
	 */
	public final ListenableList<Sprite2DMultiCellAnimation> multiCellAnimations = new ListenableList<>();

	/**
	 * Constructs an empty sprite resource.
	 */
	public Sprite2DResource() {
	}

	/**
	 * Adds a palette to this resource, replacing any existing palette with the same name.
	 *
	 * @param p The palette to add. Ignored if null.
	 */
	public void addPalette(Sprite2DPalette p) {
		if (p != null) {
			removeOldINamed(p, palettes);
			palettes.add(p);
		}
	}

	/**
	 * Adds a tile sheet to this resource, replacing any existing tile sheet with the same name.
	 *
	 * @param t The tile sheet to add. Ignored if null.
	 */
	public void addTileSheet(Sprite2DTileSheet t) {
		if (t != null) {
			removeOldINamed(t, tileSheets);
			tileSheets.add(t);
		}
	}

	/**
	 * Adds a cell to this resource, replacing any existing cell with the same name.
	 *
	 * @param c The cell to add. Ignored if null.
	 */
	public void addCell(Sprite2DCell c) {
		if (c != null) {
			removeOldINamed(c, cells);
			cells.add(c);
		}
	}

	/**
	 * Adds a cell animation to this resource, replacing any existing animation with the same name.
	 *
	 * @param a The cell animation to add. Ignored if null.
	 */
	public void addCellAnimation(Sprite2DCellAnimation a) {
		if (a != null) {
			removeOldINamed(a, cellAnimations);
			cellAnimations.add(a);
		}
	}

	/**
	 * Adds a multi-cell to this resource, replacing any existing multi-cell with the same name.
	 *
	 * @param mc The multi-cell to add. Ignored if null.
	 */
	public void addMultiCell(Sprite2DMultiCell mc) {
		if (mc != null) {
			removeOldINamed(mc, multiCells);
			multiCells.add(mc);
		}
	}

	/**
	 * Adds a multi-cell animation to this resource, replacing any existing animation with the same name.
	 *
	 * @param mca The multi-cell animation to add. Ignored if null.
	 */
	public void addMultiCellAnimation(Sprite2DMultiCellAnimation mca) {
		if (mca != null) {
			removeOldINamed(mca, multiCellAnimations);
			multiCellAnimations.add(mca);
		}
	}

	/**
	 * Merges all resources from another Sprite2DResource into this one.
	 * Existing entries with matching names are replaced.
	 *
	 * @param res The source resource to merge from. Ignored if null.
	 */
	public void merge(Sprite2DResource res) {
		if (res != null) {
			// Inherit mappingMode from incoming NCER imports so the renderer
			// picks up 1D vs 2D OAM addressing for the just-loaded data.
			if (!res.cells.isEmpty()) {
				this.mappingMode = res.mappingMode;
			}
			for (Sprite2DPalette p : res.palettes) {
				addPalette(p);
			}
			for (Sprite2DTileSheet t : res.tileSheets) {
				addTileSheet(t);
			}
			for (Sprite2DCell c : res.cells) {
				addCell(c);
			}
			for (Sprite2DCellAnimation a : res.cellAnimations) {
				addCellAnimation(a);
			}
			for (Sprite2DMultiCell mc : res.multiCells) {
				addMultiCell(mc);
			}
			for (Sprite2DMultiCellAnimation mca : res.multiCellAnimations) {
				addMultiCellAnimation(mca);
			}
		}
	}

	/**
	 * Merges all resources from another Sprite2DResource into this one,
	 * automatically deduplicating names by appending numeric suffixes.
	 *
	 * @param res The source resource to merge from. Ignored if null.
	 */
	public void mergeFull(Sprite2DResource res) {
		if (res != null) {
			if (!res.cells.isEmpty()) {
				this.mappingMode = res.mappingMode;
			}
			addListPrededupe(palettes, res.palettes, "Palette");
			addListPrededupe(tileSheets, res.tileSheets, "TileSheet");
			addListPrededupe(cells, res.cells, "Cell");
			addListPrededupe(cellAnimations, res.cellAnimations, "CellAnimation");
			addListPrededupe(multiCells, res.multiCells, "MultiCell");
			addListPrededupe(multiCellAnimations, res.multiCellAnimations, "MultiCellAnimation");
		}
	}

	/**
	 * Removes all resources present in the given Sprite2DResource from this one.
	 *
	 * @param res The resource whose entries should be removed. Ignored if null.
	 */
	public void unmerge(Sprite2DResource res) {
		if (res != null) {
			palettes.removeAll(res.palettes);
			tileSheets.removeAll(res.tileSheets);
			cells.removeAll(res.cells);
			cellAnimations.removeAll(res.cellAnimations);
			multiCells.removeAll(res.multiCells);
			multiCellAnimations.removeAll(res.multiCellAnimations);
		}
	}

	/**
	 * Picks the tile sheet that the loaded {@link #cells} (and therefore the
	 * loaded NCER / NMCR) are actually designed to address.
	 *
	 * <p>BW / BW2 Pokemon battle sprite NARCs ship <em>two</em> NCGRs per
	 * species: a <b>bitmap / raster-layout</b> NCGR (e.g. 16 KB, 512 tiles,
	 * {@code rasterLayout = true}) and a smaller tiled NCGR (e.g. 4 KB, 144
	 * tiles, {@code rasterLayout = false}) used as body-part fragments for
	 * specific animations. The NCER's OAM {@code tileIndex} values address
	 * the raster-layout sheet — rendering a cell against the fragment sheet
	 * produces the "scattered pieces" artifact (Bulbasaur rendered as a
	 * small broken fragment instead of a full Pokemon).</p>
	 *
	 * <p>Selection rule: when the NCER requested 2D OAM mapping (the BW+
	 * Pokemon battle case) and a raster-layout sheet is present, prefer it.
	 * Otherwise fall back to {@code tileSheets.get(0)} so single-sheet
	 * resources (trainers, icons, classic 1D NCGRs) keep working.</p>
	 *
	 * @return the preferred tile sheet for rendering, or {@code null} if
	 *         the resource has no tile sheets.
	 */
	public Sprite2DTileSheet getActiveTileSheet() {
		if (tileSheets.isEmpty()) {
			return null;
		}
		// 2D-mapped NCERs (BW/BW2 Pokemon) address the unswizzled raster
		// NCGR. When we loaded both the raster and the tiled fragment sheet,
		// only the raster one will produce a coherent composed sprite.
		if (mappingMode == MAPPING_MODE_2D) {
			for (Sprite2DTileSheet ts : tileSheets) {
				if (ts.rasterLayout) {
					return ts;
				}
			}
		}
		return tileSheets.get(0);
	}

	/**
	 * Derives a sensible OBJ-size for every 1D / lineal-mapped tile sheet
	 * by inspecting the most common OAM size in the cell list.
	 *
	 * <p>1D-mapped NCGR data stores each OBJ's tiles sequentially, so a
	 * flat tile dump fragments each sprite. Once the cell list is loaded
	 * we know what shape the OBJs actually are (e.g. 4×4 tiles for Pokemon
	 * B/W2 icons) and the renderer can reorder the storage tiles by
	 * OBJ-size sub-blocks to display them coherently. Sheets that are not
	 * lineal-mapped are left untouched.</p>
	 */
	public void linkTileSheetsToCells() {
		if (tileSheets.isEmpty() || cells.isEmpty()) {
			return;
		}
		// In 2D mapping mode every OAM addresses tiles by 2D coordinate, so
		// the storage layout already matches what the renderer expects;
		// there's nothing to "unfold". The OBJ-size vote is only relevant
		// for genuinely 1D-mapped tile sheets.
		if (mappingMode == MAPPING_MODE_2D) {
			return;
		}
		// Tally OAM tile dimensions across every cell. Vote for the most
		// common (width, height) pair so a single odd OAM doesn't bias the
		// result for an entire sheet.
		Map<Long, Integer> votes = new HashMap<>();
		for (Sprite2DCell cell : cells) {
			for (Sprite2DOAM oam : cell.oams) {
				int tw = oam.width >> 3;
				int th = oam.height >> 3;
				if (tw <= 0 || th <= 0) {
					continue;
				}
				long key = ((long) tw << 32) | (th & 0xFFFFFFFFL);
				votes.merge(key, 1, Integer::sum);
			}
		}
		if (votes.isEmpty()) {
			return;
		}
		long bestKey = 0;
		int bestVotes = -1;
		for (Map.Entry<Long, Integer> e : votes.entrySet()) {
			if (e.getValue() > bestVotes) {
				bestVotes = e.getValue();
				bestKey = e.getKey();
			}
		}
		int objW = (int) (bestKey >>> 32);
		int objH = (int) (bestKey & 0xFFFFFFFFL);
		for (Sprite2DTileSheet ts : tileSheets) {
			if (ts.isLinearMapped) {
				ts.objTilesWide = objW;
				ts.objTilesHigh = objH;
			}
		}
	}

	/**
	 * Clears all resource lists.
	 */
	public void clear() {
		palettes.clear();
		tileSheets.clear();
		cells.clear();
		cellAnimations.clear();
		multiCells.clear();
		multiCellAnimations.clear();
	}

	/**
	 * Adds all elements from a source list to a destination list, ensuring
	 * unique names by appending _1, _2, etc. to duplicates.
	 *
	 * @param <T>         The element type, which must implement INamed.
	 * @param dest        The destination list.
	 * @param source      The source list whose elements are added.
	 * @param defaultName The default name to assign if an element's name is null.
	 */
	public static <T extends INamed> void addListPrededupe(List<T> dest, List<T> source, String defaultName) {
		HashSet<String> usedNames = new HashSet<>();
		for (T exist : dest) {
			usedNames.add(exist.getName());
		}
		for (T elem : source) {
			if (elem.getName() == null) {
				elem.setName(defaultName);
			}

			String base = elem.getName();

			String name = base;

			int index = 1;
			while (usedNames.contains(name)) {
				name = base + "_" + index;
				index++;
			}
			if (!base.equals(name)) {
				elem.setName(name);
			}

			usedNames.add(name);
			dest.add(elem);
		}
	}

	/**
	 * Removes any existing element with the same name as the new element
	 * from the given list. Used before adding to ensure name uniqueness.
	 *
	 * @param <T>     The element type, which must implement INamed.
	 * @param newElem The element about to be added.
	 * @param list    The list to remove duplicates from.
	 */
	public static <T extends INamed> void removeOldINamed(T newElem, List<T> list) {
		Iterator<T> it = list.iterator();
		while (it.hasNext()) {
			T exist = it.next();
			if (exist.getName() != null && exist.getName().equals(newElem.getName())) {
				it.remove();
			}
		}
	}
}
