package ctrmap.creativestudio.ngcs2d.res;

import xstandard.INamed;

/**
 * Game-agnostic sprite palette. Stores colors in ARGB 8888 format
 * (standard Java Color format). Supports both 4bpp (16 color) and
 * 8bpp (256 color) configurations.
 */
public class Sprite2DPalette implements INamed {

	/**
	 * Display name of this palette.
	 */
	public String name;

	/**
	 * ARGB 8888 color values. Index 0 is typically treated as transparent.
	 */
	public int[] colors;

	/**
	 * Palette format: 16 for 4bpp/IDX4 palettes, 256 for 8bpp/IDX8 palettes.
	 */
	public int format;

	/**
	 * Constructs a default 16-color palette.
	 */
	public Sprite2DPalette() {
		name = "Palette";
		format = 16;
		colors = new int[16];
	}

	/**
	 * Constructs a palette with the given name and format.
	 *
	 * @param name   Display name of the palette.
	 * @param format Number of colors (16 for IDX4, 256 for IDX8).
	 */
	public Sprite2DPalette(String name, int format) {
		this.name = name;
		this.format = format;
		this.colors = new int[format];
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the number of colors in this palette.
	 *
	 * @return The color count.
	 */
	public int getColorCount() {
		return colors.length;
	}

	/**
	 * Returns the ARGB color at the given index.
	 *
	 * @param index The color index.
	 * @return The ARGB 8888 value, or 0 if the index is out of range.
	 */
	public int getColor(int index) {
		if (index < 0 || index >= colors.length) {
			return 0;
		}
		return colors[index];
	}

	/**
	 * Sets the ARGB color at the given index.
	 *
	 * @param index The color index.
	 * @param argb  The ARGB 8888 value to set.
	 */
	public void setColor(int index, int argb) {
		if (index >= 0 && index < colors.length) {
			colors[index] = argb;
		}
	}
}
