package ctrmap.creativestudio.ngcs2d.res;

import ctrmap.creativestudio.ngcs2d.layers.LayerItem;

/**
 * Game-agnostic OAM (Object Attribute Memory) entry representing one sprite
 * object within a cell. Each OAM maps a rectangular region of tiles from
 * the tile sheet onto the screen at a signed pixel position.
 */
public class Sprite2DOAM implements LayerItem {

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

	// ---------------------------------------------------------------------
	// Editor-only layer metadata. Not represented in NITRO on-disk formats;
	// purely decorative fields the CS 2D UI layers on top of OAMs so the
	// user can organise them like Photoshop layers. Persisted in the
	// .cs2dproj project file (future phase); dropped on NITRO export
	// (via {@link Sprite2DCell#getVisibleOAMs()} et al).
	// ---------------------------------------------------------------------

	/**
	 * User-facing name for this OAM when it's treated as a layer in the
	 * CS 2D layer panel. {@code null} means "use default" (the panel
	 * synthesises "Layer N" from the stack index).
	 */
	public String layerName;

	/**
	 * Whether this OAM participates in the rendered output. Hidden OAMs
	 * are skipped by the canvas preview <strong>and</strong> omitted from
	 * the NITRO export. Default: {@code true}.
	 */
	public boolean visible = true;

	/**
	 * Layer opacity, 0.0 (fully transparent) to 1.0 (fully opaque).
	 * The NITRO format cannot encode partial opacity, so on export a
	 * non-1.0 opacity is a lossy hint — the OAM is written as fully
	 * opaque. The canvas preview honours it. Default: 1.0.
	 */
	public float opacity = 1.0f;

	/**
	 * When {@code true}, the canvas tools and layer panel refuse to
	 * mutate this OAM. Purely a UI guard — still participates in render
	 * and export. Default: {@code false}.
	 */
	public boolean locked = false;

	/**
	 * Constructs a default OAM entry with an 8x8 size.
	 */
	public Sprite2DOAM() {
		width = 8;
		height = 8;
	}

	/**
	 * Copy constructor. Deep-copies all fields from the source OAM entry,
	 * including editor-only layer metadata.
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
		layerName = src.layerName;
		visible = src.visible;
		opacity = src.opacity;
		locked = src.locked;
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

	// --- LayerItem interface (field-backed, trivial) ---

	@Override public String getLayerName() { return layerName; }
	@Override public void setLayerName(String name) { this.layerName = name; }
	@Override public boolean isVisible() { return visible; }
	@Override public void setVisible(boolean visible) { this.visible = visible; }
	@Override public float getOpacity() { return opacity; }
	@Override public void setOpacity(float opacity) { this.opacity = opacity; }
	@Override public boolean isLocked() { return locked; }
	@Override public void setLocked(boolean locked) { this.locked = locked; }
}
