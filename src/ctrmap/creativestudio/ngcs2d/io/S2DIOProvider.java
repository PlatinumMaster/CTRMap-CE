package ctrmap.creativestudio.ngcs2d.io;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import java.awt.Frame;

/**
 * Provides supplementary data for 2D sprite I/O operations. Mirrors G3DIOProvider
 * for the NGCS2D subsystem.
 */
public interface S2DIOProvider {

	/**
	 * Gets the supplementary palette for import/export operations.
	 *
	 * @return The palette, or null if unavailable.
	 */
	public Sprite2DPalette getPalette();

	/**
	 * Gets the supplementary tile sheet for import/export operations.
	 *
	 * @return The tile sheet, or null if unavailable.
	 */
	public Sprite2DTileSheet getTileSheet();

	/**
	 * Gets the full 2D sprite resource.
	 *
	 * @return The complete Sprite2DResource.
	 */
	public Sprite2DResource getAll();

	/**
	 * Gets the parent frame for GUI dialogs.
	 *
	 * @return The parent AWT Frame.
	 */
	public Frame getGUIParent();
}
