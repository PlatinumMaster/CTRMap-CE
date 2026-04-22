package ctrmap.creativestudio.ngcs2d.project;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import javax.imageio.ImageIO;
import xstandard.fs.FSFile;

/**
 * Reads/writes a {@link Sprite2DTileSheet}'s pixel indices as an
 * 8bpp-indexed PNG. The PNG is laid out in the tile sheet's natural
 * <em>display</em> orientation — same column count as
 * {@link SpriteRenderer#renderTileSheet} and the in-editor preview, with
 * 1D-mapped sheets unfolded by OBJ-size sub-block via
 * {@link Sprite2DTileSheet#displayTileToStorageTile} so the on-disk
 * image looks like the sprite (not a scrambled tile dump).
 *
 * <p>The metadata YAML alongside records the original
 * {@code tileWidth}/{@code tileHeight}/{@code isLinearMapped}/
 * {@code objTilesWide}/{@code objTilesHigh}/{@code rasterLayout} flags
 * so the read path can rebuild the same display→storage mapping and
 * pack pixels back into the NCGR storage layout.</p>
 *
 * <p>The PLTE chunk gets the supplied palette so the PNG previews with
 * meaningful colors in any image viewer. Pixel indices, not colors, are
 * the source of truth — re-rendering with a different palette in the
 * editor yields a different look without touching the PNG.</p>
 */
public final class IndexedPngCodec {

	private static final int TILE_PX = 8;

	private IndexedPngCodec() {
	}

	/**
	 * Writes the tile sheet to {@code pngFile} as an 8bpp indexed PNG in
	 * the natural display orientation. If {@code paletteForPlte} is
	 * {@code null} or empty, a grayscale ramp palette is synthesised so
	 * the PNG still renders as something recognisable.
	 */
	public static void write(Sprite2DTileSheet sheet, Sprite2DPalette paletteForPlte, FSFile pngFile) throws IOException {
		int tileCount = sheet.getTileCount();
		int cols = Math.max(1, sheet.getEffectiveTileWidth());
		int rows = Math.max(1, (tileCount + cols - 1) / cols);
		int w = cols * TILE_PX;
		int h = rows * TILE_PX;

		IndexColorModel icm = buildColorModel(paletteForPlte);
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, icm);
		WritableRaster raster = img.getRaster();

		// Walk display positions; resolve each to its storage tile so
		// 1D-mapped sheets unfold by OBJ-size sub-block (matches
		// SpriteRenderer.renderTileSheet exactly).
		for (int dispTy = 0; dispTy < rows; dispTy++) {
			for (int dispTx = 0; dispTx < cols; dispTx++) {
				int storageIdx = sheet.displayTileToStorageTile(dispTx, dispTy);
				if (storageIdx < 0) {
					continue; // trailing empty cell; leaves index 0
				}
				int baseX = dispTx * TILE_PX;
				int baseY = dispTy * TILE_PX;
				for (int py = 0; py < TILE_PX; py++) {
					for (int px = 0; px < TILE_PX; px++) {
						int idx = sheet.getPixel(storageIdx, px, py);
						raster.setSample(baseX + px, baseY + py, 0, idx & 0xFF);
					}
				}
			}
		}

		ImageIO.write(img, "png", pngFile.getNativeOutputStream());
	}

	/**
	 * Reads {@code pngFile} as an indexed image and packs the pixel
	 * indices into a {@code byte[]} using the NCGR layout described by
	 * {@code metaShell} (format, isLinearMapped, objTilesWide/Hide,
	 * tileWidth/Height). The shell's metadata fields are read; its
	 * {@code tileData} is replaced by the returned array.
	 *
	 * <p>The PNG's intrinsic dimensions drive the display grid. If the
	 * shell's {@code tileWidth}/{@code tileHeight} are unset (-1) they
	 * are populated from the PNG's dimensions on return so a
	 * freshly-imported tile sheet picks up sensible values.</p>
	 */
	public static byte[] read(FSFile pngFile, Sprite2DTileSheet metaShell) throws IOException {
		BufferedImage img = ImageIO.read(pngFile.getNativeInputStream());
		if (img == null) {
			throw new IOException("Could not decode PNG: " + pngFile.getName());
		}
		WritableRaster raster = img.getRaster();
		int w = img.getWidth();
		int h = img.getHeight();
		int pngCols = Math.max(1, w / TILE_PX);
		int pngRows = Math.max(1, h / TILE_PX);
		int totalTiles = pngCols * pngRows;
		int format = metaShell.format;
		int bytesPerTile = (format == 3) ? 32 : 64;
		byte[] out = new byte[totalTiles * bytesPerTile];

		// Build a transient shell whose displayTileToStorageTile uses the
		// PNG's actual layout. tileWidth/Height come from the PNG (the
		// authoritative source after a manual edit); the OBJ-size and
		// linear-mapped flags come from the YAML metadata.
		Sprite2DTileSheet readShell = new Sprite2DTileSheet();
		readShell.format = format;
		readShell.tileData = out;
		readShell.tileWidth = pngCols;
		readShell.tileHeight = pngRows;
		readShell.isLinearMapped = metaShell.isLinearMapped;
		readShell.objTilesWide = metaShell.objTilesWide;
		readShell.objTilesHigh = metaShell.objTilesHigh;

		for (int dispTy = 0; dispTy < pngRows; dispTy++) {
			for (int dispTx = 0; dispTx < pngCols; dispTx++) {
				int storageIdx = readShell.displayTileToStorageTile(dispTx, dispTy);
				if (storageIdx < 0) {
					continue;
				}
				int baseX = dispTx * TILE_PX;
				int baseY = dispTy * TILE_PX;
				for (int py = 0; py < TILE_PX; py++) {
					for (int px = 0; px < TILE_PX; px++) {
						int idx = raster.getSample(baseX + px, baseY + py, 0) & 0xFF;
						readShell.setPixel(storageIdx, px, py, idx);
					}
				}
			}
		}

		// Sync back the dimensions the PNG dictated so the loaded sheet
		// matches what's on disk if the user resized it externally.
		if (metaShell.tileWidth <= 0) metaShell.tileWidth = pngCols;
		if (metaShell.tileHeight <= 0) metaShell.tileHeight = pngRows;

		return out;
	}

	private static IndexColorModel buildColorModel(Sprite2DPalette palette) {
		int n;
		int[] cmap;
		if (palette == null || palette.colors == null || palette.colors.length == 0) {
			n = 256;
			cmap = new int[n];
			cmap[0] = 0x00000000;
			for (int i = 1; i < n; i++) {
				int g = i & 0xFF;
				cmap[i] = 0xFF000000 | (g << 16) | (g << 8) | g;
			}
		} else {
			n = Math.min(256, palette.colors.length);
			cmap = new int[n];
			System.arraycopy(palette.colors, 0, cmap, 0, n);
		}
		// hasAlpha=true so the int[] cmap is interpreted as ARGB; index 0
		// flagged as transparent so the PNG renders with a proper alpha
		// channel for the typical NDS "index 0 = transparent" convention.
		return new IndexColorModel(8, n, cmap, 0, true, 0, DataBuffer.TYPE_BYTE);
	}
}
