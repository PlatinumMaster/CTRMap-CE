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
	 * DS OBJ rotation/scaling flag. When {@code true}, the NDS hardware
	 * applies an affine transform to this OAM via an {@link #rsParamIndex}
	 * entry in the CEBK's affine-parameter table. When {@code false}, the
	 * OAM renders without any affine transform (standard flipH/flipV apply).
	 */
	public boolean rotationScaling;

	/**
	 * When {@link #rotationScaling} is {@code true}, the DS "double-size"
	 * flag expands the OAM's on-screen bounding box to 2*width x 2*height
	 * so a 45deg rotation's corners don't get clipped. The actual sprite
	 * content still occupies width x height, centered inside the doubled
	 * area. Critical for positioning: {@link #x}/{@link #y} in this mode
	 * refer to the TOP-LEFT of the doubled area — the content itself is
	 * at (x + width/2, y + height/2). Ignoring this is what produced the
	 * "Bulbasaur body parts at wrong positions" bug.
	 */
	public boolean doubleSize;

	/**
	 * Index into the CEBK's OBJ-affine-parameter table, used only when
	 * {@link #rotationScaling} is {@code true}. The NDS OAM uses 5 bits
	 * here (0-31).
	 */
	public int rsParamIndex;

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
		rotationScaling = src.rotationScaling;
		doubleSize = src.doubleSize;
		rsParamIndex = src.rsParamIndex;
	}

	/**
	 * Returns the X offset of the actual content top-left within this
	 * OAM's on-screen area. For non-doubleSize OAMs this is 0; for
	 * doubleSize OAMs it's {@code width/2} because the content is
	 * centered inside the 2x-sized rendering area.
	 */
	public int getContentOffsetX() {
		return doubleSize ? (width / 2) : 0;
	}

	/** Y companion to {@link #getContentOffsetX}. */
	public int getContentOffsetY() {
		return doubleSize ? (height / 2) : 0;
	}
}
