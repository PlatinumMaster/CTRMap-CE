package ctrmap.creativestudio.ngcs2d.rtldr;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import xstandard.fs.FSFile;
import xstandard.util.ListenableList;

/**
 * Content accessor interface for the NGCS2D sprite editor. Provides access
 * to the active 2D sprite resource and its component lists. Mirrors
 * NGCSContentAccessor for the 3D system.
 */
public interface NGCS2DContentAccessor {

	/**
	 * Imports a file into the active 2D sprite resource.
	 *
	 * @param fsf The file to import.
	 */
	public void importFile(FSFile fsf);

	/**
	 * Imports a Sprite2DResource into the active resource, merging its contents.
	 *
	 * @param res The sprite resource to import.
	 */
	public void importResource(Sprite2DResource res);

	/**
	 * Gets the active 2D sprite resource.
	 *
	 * @return The current Sprite2DResource.
	 */
	public Sprite2DResource getResource();

	/**
	 * Gets the palette list from the active resource.
	 *
	 * @return The listenable list of palettes.
	 */
	public ListenableList<Sprite2DPalette> getPalettes();

	/**
	 * Gets the tile sheet list from the active resource.
	 *
	 * @return The listenable list of tile sheets.
	 */
	public ListenableList<Sprite2DTileSheet> getTileSheets();

	/**
	 * Gets the cell list from the active resource.
	 *
	 * @return The listenable list of cells.
	 */
	public ListenableList<Sprite2DCell> getCells();

	/**
	 * Gets the cell animation list from the active resource.
	 *
	 * @return The listenable list of cell animations.
	 */
	public ListenableList<Sprite2DCellAnimation> getCellAnimations();

	/**
	 * Gets the multi-cell list from the active resource.
	 *
	 * @return The listenable list of multi-cells.
	 */
	public ListenableList<Sprite2DMultiCell> getMultiCells();

	/**
	 * Gets the multi-cell animation list from the active resource.
	 *
	 * @return The listenable list of multi-cell animations.
	 */
	public ListenableList<Sprite2DMultiCellAnimation> getMultiCellAnimations();
}
