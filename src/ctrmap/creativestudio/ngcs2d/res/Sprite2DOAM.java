package ctrmap.creativestudio.ngcs2d.res;

/**
 * Game-agnostic OAM (Object Attribute Memory) entry representing one sprite
 * object within a cell. Each OAM maps a rectangular region of tiles from
 * the tile sheet onto the screen at a signed pixel position.
 */
public class Sprite2DOAM {

	/**
	 * Signed pixel X position of this object relative to the cell origin.
	 */
	public int x;

	/**
	 * Signed pixel Y position of this object relative to the cell origin.
	 */
	public int y;

	/**
	 * Pixel width of this object. Must be a valid OBJ size (8, 16, 32, or 64).
	 */
	public int width;

	/**
	 * Pixel height of this object. Must be a valid OBJ size (8, 16, 32, or 64).
	 */
	public int height;

	/**
	 * Starting tile index in the tile sheet.
	 */
	public int tileIndex;

	/**
	 * Sub-palette index for 16-color mode.
	 */
	public int paletteIndex;

	/**
	 * Horizontal flip flag.
	 */
	public boolean flipH;

	/**
	 * Vertical flip flag.
	 */
	public boolean flipV;

	/**
	 * OBJ priority (0-3). 0 is the highest priority.
	 */
	public int priority;

	/**
	 * Constructs a default OAM entry with an 8x8 size.
	 */
	public Sprite2DOAM() {
		width = 8;
		height = 8;
	}

	/**
	 * Copy constructor. Deep-copies all fields from the source OAM entry.
	 *
	 * @param src The source OAM entry to copy.
	 */
	public Sprite2DOAM(Sprite2DOAM src) {
		x = src.x;
		y = src.y;
		width = src.width;
		height = src.height;
		tileIndex = src.tileIndex;
		paletteIndex = src.paletteIndex;
		flipH = src.flipH;
		flipV = src.flipV;
		priority = src.priority;
	}
}
