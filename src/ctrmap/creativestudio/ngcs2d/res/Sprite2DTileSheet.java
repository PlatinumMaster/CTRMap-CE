package ctrmap.creativestudio.ngcs2d.res;

import xstandard.INamed;

/**
 * Game-agnostic tile sheet for 2D sprites. Stores 8x8 indexed pixel data
 * in either 4bpp (IDX4) or 8bpp (IDX8) format.
 */
public class Sprite2DTileSheet implements INamed {

	/**
	 * Display name of this tile sheet.
	 */
	public String name;

	/**
	 * Raw indexed pixel data. Layout depends on the format:
	 * IDX4 (format 3) packs two pixels per byte; IDX8 (format 4) uses one byte per pixel.
	 */
	public byte[] tileData;

	/**
	 * Pixel format: 3 = 4bpp/IDX4 (each byte stores 2 pixels),
	 * 4 = 8bpp/IDX8 (each byte is 1 pixel).
	 */
	public int format;

	/**
	 * Width of the tile sheet in 8x8 tiles, as recorded in the source file.
	 * A non-positive value (e.g. -1) indicates a 1D / "lineal" mapping where
	 * the natural layout is unknown; renderers should fall back to a default
	 * column count in that case.
	 */
	public int tileWidth = -1;

	/**
	 * Height of the tile sheet in 8x8 tiles, as recorded in the source file.
	 * A non-positive value (e.g. -1) indicates a 1D / "lineal" mapping where
	 * the natural layout is unknown.
	 */
	public int tileHeight = -1;

	/**
	 * True when the source character data was stored in 1D / "lineal" OBJ
	 * mapping (each OBJ's tiles laid out sequentially). When true, renderers
	 * must reorder by OBJ-size sub-blocks ({@link #objTilesWide} ×
	 * {@link #objTilesHigh}) to display the sheet coherently.
	 */
	public boolean isLinearMapped = false;

	/**
	 * Width of one OBJ in 8x8 tiles, used when {@link #isLinearMapped} is
	 * true. Defaults to 1 (each tile is its own OBJ → flat layout).
	 * Typically derived from cell OAM sizes after import.
	 */
	public int objTilesWide = 1;

	/**
	 * Height of one OBJ in 8x8 tiles, used when {@link #isLinearMapped} is
	 * true. Defaults to 1 (each tile is its own OBJ → flat layout).
	 * Typically derived from cell OAM sizes after import.
	 */
	public int objTilesHigh = 1;

	/**
	 * {@code true} when the source character data was stored as a linear
	 * raster (bitmap / "type == 1" mode in NitroPaint terminology) rather
	 * than a sequence of 8x8 tiles.
	 *
	 * <p>After import, the in-memory {@link #tileData} buffer is always
	 * tile-major — the raster was unswizzled during parsing so the rest of
	 * the editor can treat all sheets uniformly. HOWEVER, the unswizzle
	 * reorders tiles into a 2D grid whose row stride is the NCGR's
	 * {@code tileWidth}, not the OAM's {@code tilesWide}. So even when the
	 * paired NCER advertises 1D OBJ mapping, a bitmap-mode sheet must be
	 * walked with the 2D-style row stride (NCGR width) or tiles jump across
	 * raster rows and the sprite fragments into wrong pieces (the Pokemon
	 * B/W battle sprite case: Cyndaquil's Cell_0 rendered as a tiny head +
	 * a detached yellow paw).</p>
	 *
	 * <p>{@link SpriteRenderer#renderOAM} consults this flag and overrides
	 * the row stride to the NCGR's effective width when set, regardless of
	 * the NCER mapping mode.</p>
	 */
	public boolean rasterLayout = false;

	/**
	 * Constructs an empty tile sheet in 8bpp format.
	 */
	public Sprite2DTileSheet() {
		name = "TileSheet";
		format = 4;
		tileData = new byte[0];
	}

	/**
	 * Constructs a tile sheet with the given name, format, and tile count.
	 *
	 * @param name      Display name of the tile sheet.
	 * @param format    Pixel format (3 for IDX4, 4 for IDX8).
	 * @param tileCount Number of 8x8 tiles to allocate.
	 */
	public Sprite2DTileSheet(String name, int format, int tileCount) {
		this.name = name;
		this.format = format;
		this.tileData = new byte[tileCount * getBytesPerTile()];
	}

	/**
	 * Returns the natural rendering width (in tiles) for this tile sheet.
	 * Falls back to 32 columns when the source did not record a fixed
	 * dimension (1D / lineal mapping).
	 *
	 * @return Effective column count to use when rendering the tile sheet
	 *         as a flat tile dump.
	 */
	public int getEffectiveTileWidth() {
		if (tileWidth > 0) {
			return tileWidth;
		}
		int count = getTileCount();
		if (count <= 0) {
			return 1;
		}
		return Math.min(32, count);
	}

	/**
	 * Returns the effective rendering height (in tiles), computed from the
	 * recorded {@link #tileHeight} when available or from the tile count
	 * divided by {@link #getEffectiveTileWidth()}.
	 *
	 * @return Effective row count to use when rendering the tile sheet.
	 */
	public int getEffectiveTileHeight() {
		if (tileHeight > 0) {
			return tileHeight;
		}
		int count = getTileCount();
		if (count <= 0) {
			return 1;
		}
		int w = getEffectiveTileWidth();
		return (count + w - 1) / w;
	}

	/**
	 * Resolves a flat display tile coordinate (column, row in the rendered
	 * dump) to the underlying storage tile index. For 2D-mapped sheets this
	 * is the identity mapping {@code row * cols + col}. For 1D-mapped sheets
	 * with {@link #objTilesWide} > 1 or {@link #objTilesHigh} > 1, the
	 * coordinate is treated as lying inside an OBJ-size sub-block, and the
	 * result is the storage index of that tile within the corresponding OBJ.
	 *
	 * @param dispTileX Column in the rendered tile grid.
	 * @param dispTileY Row in the rendered tile grid.
	 * @return Storage tile index, or -1 if out of range.
	 */
	public int displayTileToStorageTile(int dispTileX, int dispTileY) {
		if (dispTileX < 0 || dispTileY < 0) {
			return -1;
		}
		int cols = getEffectiveTileWidth();
		int storageIdx;
		if (isLinearMapped && objTilesWide > 1 && objTilesHigh > 1
			&& cols >= objTilesWide && (cols % objTilesWide) == 0) {
			int objsPerRow = cols / objTilesWide;
			int objCol = dispTileX / objTilesWide;
			int objRow = dispTileY / objTilesHigh;
			int localX = dispTileX % objTilesWide;
			int localY = dispTileY % objTilesHigh;
			int objIdx = objRow * objsPerRow + objCol;
			int objTileCount = objTilesWide * objTilesHigh;
			storageIdx = objIdx * objTileCount + localY * objTilesWide + localX;
		} else {
			storageIdx = dispTileY * cols + dispTileX;
		}
		if (storageIdx < 0 || storageIdx >= getTileCount()) {
			return -1;
		}
		return storageIdx;
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
	 * Returns the number of bytes per 8x8 tile for the current format.
	 *
	 * @return 32 for IDX4 (format 3), 64 for IDX8 (format 4).
	 */
	public int getBytesPerTile() {
		return format == 3 ? 32 : 64;
	}

	/**
	 * Returns the number of 8x8 tiles in this tile sheet.
	 *
	 * @return The tile count, or 0 if the tile data is empty.
	 */
	public int getTileCount() {
		if (tileData.length == 0) {
			return 0;
		}
		return tileData.length / getBytesPerTile();
	}

	/**
	 * Resolves a flat pixel coordinate (in the rendered tile dump) to its
	 * 8x8 storage tile index. Honours the OBJ-size unfolding when the sheet
	 * is 1D-mapped (see {@link #displayTileToStorageTile}).
	 *
	 * @param px Pixel X in the rendered dump.
	 * @param py Pixel Y in the rendered dump.
	 * @return The storage tile index, or -1 if outside the bounds of the
	 *         tile data.
	 */
	public int pixelToTileIndex(int px, int py) {
		if (px < 0 || py < 0) {
			return -1;
		}
		return displayTileToStorageTile(px >> 3, py >> 3);
	}

	/**
	 * Returns the palette index of a pixel within a tile.
	 *
	 * @param tileIdx Index of the 8x8 tile.
	 * @param x       X coordinate within the tile (0-7).
	 * @param y       Y coordinate within the tile (0-7).
	 * @return The palette index at the given pixel, or 0 if out of bounds.
	 */
	public int getPixel(int tileIdx, int x, int y) {
		if (format == 3) {
			int offset = tileIdx * 32 + y * 4 + x / 2;
			if (offset < 0 || offset >= tileData.length) {
				return 0;
			}
			int b = tileData[offset] & 0xFF;
			if ((x & 1) == 0) {
				return b & 0x0F;
			} else {
				return (b >> 4) & 0x0F;
			}
		} else {
			int offset = tileIdx * 64 + y * 8 + x;
			if (offset < 0 || offset >= tileData.length) {
				return 0;
			}
			return tileData[offset] & 0xFF;
		}
	}

	/**
	 * Sets the palette index of a pixel within a tile.
	 * For IDX4 format, the other nibble in the byte is preserved.
	 *
	 * @param tileIdx    Index of the 8x8 tile.
	 * @param x          X coordinate within the tile (0-7).
	 * @param y          Y coordinate within the tile (0-7).
	 * @param paletteIdx The palette index to set.
	 */
	public void setPixel(int tileIdx, int x, int y, int paletteIdx) {
		if (format == 3) {
			int offset = tileIdx * 32 + y * 4 + x / 2;
			if (offset < 0 || offset >= tileData.length) {
				return;
			}
			int b = tileData[offset] & 0xFF;
			if ((x & 1) == 0) {
				b = (b & 0xF0) | (paletteIdx & 0x0F);
			} else {
				b = (b & 0x0F) | ((paletteIdx & 0x0F) << 4);
			}
			tileData[offset] = (byte) b;
		} else {
			int offset = tileIdx * 64 + y * 8 + x;
			if (offset < 0 || offset >= tileData.length) {
				return;
			}
			tileData[offset] = (byte) paletteIdx;
		}
	}
}
