package ctrmap.creativestudio.ngcs2d.canvas;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DAnimFrame;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Stateless utility class for rendering 2D sprite data to BufferedImages.
 */
public class SpriteRenderer {

	private static final BufferedImage EMPTY = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

	/**
	 * Renders a single OAM entry to a BufferedImage using a default
	 * 1D-style row stride (the OAM's own tile width). Prefer the overload
	 * that takes an explicit {@code mappingMode} when you know whether
	 * the OAM was authored against a 1D or 2D OBJ VRAM layout.
	 */
	public static BufferedImage renderOAM(Sprite2DOAM oam, Sprite2DTileSheet tileSheet, Sprite2DPalette palette) {
		return renderOAM(oam, tileSheet, palette, Sprite2DResource.MAPPING_MODE_1D_32K);
	}

	/**
	 * Renders a single OAM entry, picking the row stride based on the
	 * supplied mapping mode (matches NitroPaint's per-OAM tile walk):
	 *
	 * <ul>
	 *   <li>2D mode → row stride is the tile sheet's effective width (the
	 *       OAM addresses tiles in a 32-char-wide grid, so walking down a
	 *       row advances by the underlying NCGR's stride).</li>
	 *   <li>1D modes → row stride is the OAM's own tile width (each OAM's
	 *       tiles are stored sequentially).</li>
	 * </ul>
	 */
	public static BufferedImage renderOAM(Sprite2DOAM oam, Sprite2DTileSheet tileSheet, Sprite2DPalette palette, int mappingMode) {
		return renderOAM(oam, tileSheet, palette, mappingMode, null);
	}

	/**
	 * Full-power OAM renderer that honours an optional per-cell CHAR VRAM
	 * transfer (exact byte-level port of NitroPaint's {@code ChrGetChar} +
	 * {@code CellRenderOBJ}). When {@code ownerCell} has
	 * {@link Sprite2DCell#hasVramTransfer} set, each 8x8 character is
	 * fetched through {@link #chrGetChar} which checks the char's byte
	 * range against the transfer's destination window {@code [dstAddr,
	 * dstAddr+size)} and, per-pixel, either reads from the raw NCGR or
	 * remaps to {@code transfer->srcAddr}. For cells without a transfer,
	 * {@code ownerCell} may be {@code null}.
	 */
	public static BufferedImage renderOAM(Sprite2DOAM oam, Sprite2DTileSheet tileSheet, Sprite2DPalette palette, int mappingMode, Sprite2DCell ownerCell) {
		if (oam == null || tileSheet == null || palette == null) {
			return EMPTY;
		}
		int w = oam.width;
		int h = oam.height;
		if (w <= 0 || h <= 0) {
			return EMPTY;
		}
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int tilesWide = w >> 3;
		int tilesHigh = h >> 3;
		int colorsPerSubPal = palette.format;

		// Tile walk mirrors NitroPaint's CellRenderOBJ exactly:
		//
		//   2D mapping → ncx = x + ncgrStart % tilesX,
		//                ncy = y + ncgrStart / tilesX,
		//                index = ncx + tilesX * ncy
		//                (equivalent to ncgrStart + tx + ty * tilesX)
		//
		//   1D mapping → index = ncgrStart + tx + ty * tilesW
		//                (sequential, row pitch = OBJ tile width)
		//
		// Tile-walk stride — mirrors NitroPaint's CellRenderOBJ:
		//   2D mapping → step down by NCGR's 2D grid width.
		//   1D mapping → step down by the OAM's OWN tile width (each OAM's
		//                 tiles are stored sequentially in VRAM).
		// BITMAP OVERRIDE: when the NCGR was stored as a raster bitmap
		// (rasterLayout=true), the tile buffer is pre-arranged as a 2D
		// grid whose row stride is the NCGR's tileWidth regardless of
		// what the NCER claims. An OAM addressing such a sheet must use
		// that grid stride — using OAM stride would fragment the sprite
		// (Cyndaquil Cell_0 rendered as "head + paw").
		boolean is2D = (mappingMode == Sprite2DResource.MAPPING_MODE_2D);
		boolean useNcgrStride = is2D || tileSheet.rasterLayout;
		int rowStride;
		if (useNcgrStride) {
			rowStride = Math.max(1, tileSheet.getEffectiveTileWidth());
		} else {
			rowStride = Math.max(1, tilesWide);
		}

		// VRAM transfer state. NitroPaint loads dstAddr=0 from the file
		// and stores srcAddr/size verbatim — all three are BYTE values
		// into/through the NCGR tile stream. The per-tile remap is done
		// by chrGetChar() below.
		int nBits = (tileSheet.format == 3) ? 4 : 8;
		int chrSize = 8 * nBits; // bytes per char: 32 for 4bpp, 64 for 8bpp
		boolean useTransfer = (ownerCell != null && ownerCell.hasVramTransfer);
		int xferSrcByte = 0;
		int xferDstByte = 0;
		int xferSize = 0;
		if (useTransfer) {
			xferSrcByte = ownerCell.vramTransferSrcAddr;
			xferDstByte = 0; // NitroPaint hardcodes this
			xferSize = ownerCell.vramTransferSize;
		}

		int[] chrPixels = new int[64];

		for (int ty = 0; ty < tilesHigh; ty++) {
			for (int tx = 0; tx < tilesWide; tx++) {
				int computed = oam.tileIndex + ty * rowStride + tx;
				if (computed < 0) {
					continue;
				}

				chrGetChar(tileSheet, computed, useTransfer,
					xferSrcByte, xferDstByte, xferSize, nBits, chrSize, chrPixels);

				for (int i = 0; i < 64; i++) {
					int palIdx = chrPixels[i];
					if (palIdx == 0) {
						continue;
					}
					int absIdx;
					if (colorsPerSubPal <= 16) {
						absIdx = oam.paletteIndex * 16 + palIdx;
					} else {
						absIdx = palIdx;
					}
					// Skip pixels whose palette index falls outside the
					// palette entirely — leave them transparent instead of
					// forcing opaque black. The previous "argb | 0xFF000000"
					// fallback produced visible black rectangles whenever
					// an OAM referenced a sub-palette the NCLR didn't
					// contain, which hid rendering bugs by making them
					// look like geometry glitches.
					if (absIdx < 0 || absIdx >= palette.colors.length) {
						continue;
					}
					int argb = palette.getColor(absIdx) | 0xFF000000;
					int py = i >> 3;
					int px = i & 7;
					img.setRGB(tx * 8 + px, ty * 8 + py, argb);
				}
			}
		}

		if (oam.flipH || oam.flipV) {
			BufferedImage flipped = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = flipped.createGraphics();
			AffineTransform at = new AffineTransform();
			if (oam.flipH) {
				at.translate(w, 0);
				at.scale(-1, 1);
			}
			if (oam.flipV) {
				at.translate(0, oam.flipH ? h : 0);
				if (!oam.flipH) {
					at.translate(0, h);
				}
				at.scale(1, -1);
			}
			g.setTransform(at);
			g.drawImage(img, 0, 0, null);
			g.dispose();
			return flipped;
		}

		return img;
	}

	/**
	 * Exact port of NitroPaint's {@code ChrGetChar} function. Returns the
	 * 64 unpacked palette indices for a single 8x8 character, honouring an
	 * optional CHAR VRAM transfer. All address/size values are in BYTES.
	 *
	 * <p>Algorithm (matching ncgr.c verbatim):</p>
	 * <ol>
	 *   <li>If no transfer, copy {@code tileSheet.getPixel(chno, x, y)} for
	 *       all 64 pixels (or zero-fill if {@code chno} is out of range).</li>
	 *   <li>Compute this char's byte address {@code srcCharByte = chno *
	 *       chrSize}. Fast-reject when the char's whole byte range is
	 *       outside {@code [dstAddr, dstAddr+size)} and fall back to the
	 *       raw tile.</li>
	 *   <li>Per-pixel: compute the pixel's byte address inside the char
	 *       ({@code i>>1} for 4bpp, {@code i} for 8bpp). If that address
	 *       is inside the transfer window, remap via
	 *       {@code pxaddr = pxaddr - dstAddr + srcAddr}, split into
	 *       {@code (transferChr, transferChrPxOffset)} and recover the
	 *       linear pixel index (for 4bpp: {@code pxOff*2 + (i&1)}).</li>
	 *   <li>Out-of-window pixels fall through to the raw tile.</li>
	 * </ol>
	 *
	 * @param tileSheet the NCGR-equivalent tile store
	 * @param chno      the target character index (already post-CHNAME shift)
	 * @param useTransfer true if the owning cell has a VRAM transfer
	 * @param srcAddrByte transfer->srcAddr in bytes
	 * @param dstAddrByte transfer->dstAddr in bytes (0 for NCER)
	 * @param sizeByte    transfer->size in bytes
	 * @param nBits       4 or 8
	 * @param chrSize     bytes per char (pre-computed as 8*nBits)
	 * @param out         must be a 64-element array; receives palette indices
	 */
	private static void chrGetChar(Sprite2DTileSheet tileSheet, int chno, boolean useTransfer,
			int srcAddrByte, int dstAddrByte, int sizeByte, int nBits, int chrSize, int[] out) {
		int tileCount = tileSheet.getTileCount();

		if (!useTransfer) {
			if (chno >= 0 && chno < tileCount) {
				for (int i = 0; i < 64; i++) {
					out[i] = tileSheet.getPixel(chno, i & 7, i >> 3);
				}
			} else {
				for (int i = 0; i < 64; i++) {
					out[i] = 0;
				}
			}
			return;
		}

		// Fast reject: if this char's full byte range is fully outside
		// the destination window, return the raw tile. Mirrors NitroPaint's
		// ncgr.c line 773 — note the strict `<` on the left side (not `<=`),
		// so a char whose trailing byte equals dstAddr still goes through
		// the per-pixel path. In practice dstAddr == 0 for CEBK transfers,
		// so the left-side test only matters for hypothetical chno < 0.
		int srcCharByte = chno * chrSize;
		if ((srcCharByte + chrSize) < dstAddrByte || srcCharByte >= (dstAddrByte + sizeByte)) {
			if (chno >= 0 && chno < tileCount) {
				for (int i = 0; i < 64; i++) {
					out[i] = tileSheet.getPixel(chno, i & 7, i >> 3);
				}
			} else {
				for (int i = 0; i < 64; i++) {
					out[i] = 0;
				}
			}
			return;
		}

		// Per-pixel remap, matching NitroPaint's ChrGetChar inner loop.
		for (int i = 0; i < 64; i++) {
			int pxByteInChar = (nBits == 4) ? (i >> 1) : i;
			int pxaddr = srcCharByte + pxByteInChar;
			if (pxaddr >= dstAddrByte && pxaddr < (dstAddrByte + sizeByte)) {
				pxaddr = pxaddr - dstAddrByte + srcAddrByte;
				int transferChr = pxaddr / chrSize;
				int transferChrByteOff = pxaddr % chrSize;
				int pxno;
				if (nBits == 4) {
					pxno = transferChrByteOff * 2 + (i & 1);
				} else {
					pxno = transferChrByteOff;
				}
				int tpy = pxno >> 3;
				int tpx = pxno & 7;
				if (transferChr >= 0 && transferChr < tileCount) {
					out[i] = tileSheet.getPixel(transferChr, tpx, tpy);
				} else {
					out[i] = 0;
				}
			} else {
				if (chno >= 0 && chno < tileCount) {
					out[i] = tileSheet.getPixel(chno, i & 7, i >> 3);
				} else {
					out[i] = 0;
				}
			}
		}
	}

	/**
	 * Renders a complete cell (all OAMs composited) to a BufferedImage,
	 * defaulting to 1D-style OAM iteration. Prefer the overload that
	 * accepts an explicit mapping mode (or {@link #renderCell(Sprite2DCell,
	 * Sprite2DResource)}) so 2D-mode cells walk the tile sheet correctly.
	 */
	public static BufferedImage renderCell(Sprite2DCell cell, Sprite2DTileSheet tileSheet, Sprite2DPalette palette) {
		return renderCell(cell, tileSheet, palette, Sprite2DResource.MAPPING_MODE_1D_32K);
	}

	/**
	 * Renders a complete cell (all OAMs composited) to a BufferedImage,
	 * forwarding the supplied {@code mappingMode} to each OAM's renderer.
	 */
	public static BufferedImage renderCell(Sprite2DCell cell, Sprite2DTileSheet tileSheet, Sprite2DPalette palette, int mappingMode) {
		if (cell == null || cell.oams.isEmpty() || tileSheet == null || palette == null) {
			return EMPTY;
		}

		// Visible-OAM filter applied up front: hidden layers contribute
		// neither bbox pixels nor draw calls.
		java.util.List<Sprite2DOAM> visible = cell.getVisibleOAMs();
		if (visible.isEmpty()) {
			return EMPTY;
		}

		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
		for (Sprite2DOAM oam : visible) {
			// doubleSize OAMs store the top-left of their 2x area in (x,y),
			// content is centered (w/2, h/2) inside. Union the content bbox.
			int cx = oam.x + oam.getContentOffsetX();
			int cy = oam.y + oam.getContentOffsetY();
			minX = Math.min(minX, cx);
			minY = Math.min(minY, cy);
			maxX = Math.max(maxX, cx + oam.width);
			maxY = Math.max(maxY, cy + oam.height);
		}

		int imgW = maxX - minX;
		int imgH = maxY - minY;
		if (imgW <= 0 || imgH <= 0) {
			return EMPTY;
		}
		// Cell rendering: compose OAMs into a single image

		BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();

		for (int i = visible.size() - 1; i >= 0; i--) {
			Sprite2DOAM oam = visible.get(i);
			BufferedImage oamImg = renderOAM(oam, tileSheet, palette, mappingMode, cell);
			int dx = oam.x + oam.getContentOffsetX() - minX;
			int dy = oam.y + oam.getContentOffsetY() - minY;
			// Layer opacity: fold into the composite. NITRO can't encode
			// partial alpha, so this only affects the editor preview —
			// the NCER export emits full-opacity OAMs regardless.
			if (oam.opacity < 1.0f) {
				java.awt.Composite prev = g.getComposite();
				g.setComposite(java.awt.AlphaComposite.getInstance(
					java.awt.AlphaComposite.SRC_OVER, Math.max(0f, oam.opacity)));
				g.drawImage(oamImg, dx, dy, null);
				g.setComposite(prev);
			} else {
				g.drawImage(oamImg, dx, dy, null);
			}
		}

		g.dispose();
		return img;
	}

	/**
	 * Convenience: renders a cell using the first tile sheet and first
	 * palette in the resource, applying the resource's recorded
	 * {@code mappingMode}. Returns {@link #EMPTY} if either is missing.
	 */
	public static BufferedImage renderCell(Sprite2DCell cell, Sprite2DResource res) {
		if (cell == null || res == null || res.tileSheets.isEmpty() || res.palettes.isEmpty()) {
			return EMPTY;
		}
		// Use getActiveTileSheet so multi-NCGR resources (BW/BW2 Pokemon
		// battle sprites that ship both a raster bitmap and a tiled
		// fragment sheet) pick the raster one the NCER actually addresses.
		return renderCell(cell, res.getActiveTileSheet(), res.palettes.get(0), res.mappingMode);
	}

	/**
	 * Renders one frame of a cell animation. The animation's frame at the
	 * supplied index is resolved to a cell in the resource and rendered
	 * using the resource's mapping mode. Returns {@link #EMPTY} if the
	 * animation, frame or referenced cell is missing.
	 *
	 * <p>The frame's translate / scale / rotation transform fields are
	 * applied around the cell's centre so the on-screen result mirrors
	 * what the in-game OAM affine transform produces.</p>
	 */
	public static BufferedImage renderAnimFrame(Sprite2DCellAnimation anim, int frameIndex, Sprite2DResource res) {
		if (anim == null || res == null || anim.frames.isEmpty()) {
			return EMPTY;
		}
		if (frameIndex < 0 || frameIndex >= anim.frames.size()) {
			return EMPTY;
		}
		Sprite2DAnimFrame frame = anim.frames.get(frameIndex);
		if (frame.cellIndex < 0 || frame.cellIndex >= res.cells.size()) {
			return EMPTY;
		}
		Sprite2DCell cell = res.cells.get(frame.cellIndex);
		if (res.tileSheets.isEmpty() || res.palettes.isEmpty()) {
			return EMPTY;
		}
		BufferedImage cellImg = renderCell(cell, res.getActiveTileSheet(), res.palettes.get(0), res.mappingMode);
		if (cellImg == EMPTY) {
			return EMPTY;
		}
		// Skip the affine apply if the frame is identity (the common case
		// for plain frame-cycle animations).
		boolean identity = (frame.translateX == 0f && frame.translateY == 0f
			&& frame.scaleX == 1f && frame.scaleY == 1f && frame.rotation == 0f);
		if (identity) {
			return cellImg;
		}
		int srcW = cellImg.getWidth();
		int srcH = cellImg.getHeight();
		// Compute a destination canvas large enough to hold the transformed
		// cell. We oversize by the diagonal so rotated frames don't clip.
		int padW = (int) Math.ceil(Math.max(srcW, srcH) * Math.max(1.0, Math.max(Math.abs(frame.scaleX), Math.abs(frame.scaleY))) * 1.5);
		int dstW = padW * 2;
		int dstH = padW * 2;
		BufferedImage out = new BufferedImage(dstW, dstH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		AffineTransform at = new AffineTransform();
		at.translate(dstW / 2.0 + frame.translateX, dstH / 2.0 + frame.translateY);
		at.rotate(Math.toRadians(frame.rotation));
		at.scale(frame.scaleX, frame.scaleY);
		at.translate(-srcW / 2.0, -srcH / 2.0);
		g.drawImage(cellImg, at, null);
		g.dispose();
		return out;
	}

	/**
	 * Per-entry layout slot used by both the multi-cell renderer and the
	 * canvas hit-test for drag operations. Coordinates are in the rendered
	 * image's pixel space — i.e. (0,0) is the top-left corner of the
	 * BufferedImage that {@link #renderMultiCell} returns.
	 */
	public static class MultiCellEntryLayout {

		/** The original entry this slot represents. */
		public final Sprite2DMultiCell.MultiCellEntry entry;
		/** The rendered cell image (already cropped to its OAM bbox). */
		public final BufferedImage image;
		/** Top-left X of {@link #image} within the multi-cell render. */
		public final int drawX;
		/** Top-left Y of {@link #image} within the multi-cell render. */
		public final int drawY;
		/** Width of the entry's bbox in rendered-image pixels. */
		public final int width;
		/** Height of the entry's bbox in rendered-image pixels. */
		public final int height;
		/** Index of the entry within its parent multi-cell. */
		public final int entryIndex;

		public MultiCellEntryLayout(Sprite2DMultiCell.MultiCellEntry entry, BufferedImage image,
				int drawX, int drawY, int width, int height, int entryIndex) {
			this.entry = entry;
			this.image = image;
			this.drawX = drawX;
			this.drawY = drawY;
			this.width = width;
			this.height = height;
			this.entryIndex = entryIndex;
		}
	}

	/**
	 * Result of laying out a multi-cell: the rendered composite image plus
	 * the screen-space rectangle of every entry, plus the OAM-space offset
	 * (unionMinX/unionMinY) the renderer subtracted to crop the composite.
	 *
	 * <p>The {@code unionMin*} fields let callers convert between
	 * rendered-image pixel coordinates and the OAM coordinate frame the
	 * NMCR file actually stores: {@code oamX = pixelX + unionMinX}.</p>
	 */
	public static class MultiCellLayout {

		public final BufferedImage image;
		public final java.util.List<MultiCellEntryLayout> entries;
		public final int unionMinX;
		public final int unionMinY;

		public MultiCellLayout(BufferedImage image, java.util.List<MultiCellEntryLayout> entries,
				int unionMinX, int unionMinY) {
			this.image = image;
			this.entries = entries;
			this.unionMinX = unionMinX;
			this.unionMinY = unionMinY;
		}
	}

	/**
	 * Renders a single multi-cell as the union of all its referenced
	 * cell-animation frames composited at the cell-animation's first
	 * frame. Returns {@link #EMPTY} if the multi-cell or its referenced
	 * animations are missing.
	 *
	 * <p>This is a static (frame-zero) preview suitable for the tree
	 * "select multi-cell" view; for the animated view, see
	 * {@link #renderMultiCellAnimFrame}.</p>
	 */
	public static BufferedImage renderMultiCell(Sprite2DMultiCell mc, Sprite2DResource res) {
		MultiCellLayout layout = layoutMultiCell(mc, res, 0L);
		return layout != null ? layout.image : EMPTY;
	}

	/**
	 * Renders a single frame of a multi-cell animation by resolving the
	 * referenced multi-cell and forwarding to {@link #renderMultiCell}.
	 * Returns {@link #EMPTY} if any required resource is missing.
	 */
	public static BufferedImage renderMultiCellAnimFrame(Sprite2DMultiCellAnimation anim, int frameIndex, Sprite2DResource res) {
		if (anim == null || res == null || anim.frames.isEmpty()) {
			return EMPTY;
		}
		if (frameIndex < 0 || frameIndex >= anim.frames.size()) {
			return EMPTY;
		}
		Sprite2DMultiCellAnimation.MultiCellAnimFrame frame = anim.frames.get(frameIndex);
		if (frame.multiCellIndex < 0 || frame.multiCellIndex >= res.multiCells.size()) {
			return EMPTY;
		}
		return renderMultiCell(res.multiCells.get(frame.multiCellIndex), res);
	}

	/**
	 * Computes the layout of a multi-cell and renders it. Backward-compat
	 * overload that treats the legacy {@code subFrameIdx} argument as a
	 * one-tick-per-frame master clock and forwards to the tick-driven
	 * overload below. Prefer the {@code (mc, res, elapsedTicks)} variant
	 * when you want each multi-cell entry's sub-NANR to advance based on
	 * real elapsed time (the standard multi-cell playback model).
	 */
	public static MultiCellLayout layoutMultiCell(Sprite2DMultiCell mc, Sprite2DResource res, int subFrameIdx) {
		return layoutMultiCell(mc, res, (long) subFrameIdx);
	}

	/**
	 * Computes the layout of a multi-cell and renders it, driving every
	 * entry's sub-NANR from a single master tick counter so entries with
	 * different-length animations stay in real-time sync.
	 *
	 * <p>This mirrors NitroPaint's <code>RenderNmcrFrame</code> →
	 * <code>AnmRenderSequenceFrame</code> → <code>CellRender</code> chain.
	 * For each entry we:</p>
	 * <ol>
	 *   <li>Look up the referenced NANR sequence.</li>
	 *   <li>Convert {@code elapsedTicks} → per-entry frame index by
	 *       walking the cumulative per-frame durations modulo the
	 *       sequence's total length. This is the critical behaviour the
	 *       user asked for: entry 0 and entry 1 can point at NANRs of
	 *       very different lengths / durations, but because each entry
	 *       consumes the same master clock independently they all
	 *       advance in real time.</li>
	 *   <li>Fetch the cell that the computed frame names.</li>
	 *   <li>Composite each OAM at
	 *       {@code (oam.x + entry.x + frame.translateX,
	 *       oam.y + entry.y + frame.translateY)} in a unified OAM coord
	 *       space — i.e. the per-frame translate that NANR types 1/2
	 *       carry is folded into the entry offset, exactly the way
	 *       NitroPaint's {@code AnmCalcTransformMatrix} feeds it into
	 *       {@code CellRender}.</li>
	 * </ol>
	 *
	 * <p>Entries are drawn back-to-front (so entry 0 ends up on top) and
	 * within each entry the OAMs are also iterated in reverse so OAM 0
	 * wins.</p>
	 *
	 * <p>Returns {@code null} when the multi-cell is empty or no entry
	 * resolves to a non-empty cell.</p>
	 */
	public static MultiCellLayout layoutMultiCell(Sprite2DMultiCell mc, Sprite2DResource res, long elapsedTicks) {
		if (mc == null || res == null || mc.entries.isEmpty()) {
			return null;
		}
		if (res.tileSheets.isEmpty() || res.palettes.isEmpty()) {
			return null;
		}

		int n = mc.entries.size();
		ResolvedEntry[] resolved = new ResolvedEntry[n];
		for (int i = 0; i < n; i++) {
			resolved[i] = resolveEntryAtTick(mc.entries.get(i), res, elapsedTicks);
		}

		// First pass: compute the union of all OAMs across all entries in
		// the unified OAM coord space (oam.x + entry.x + frame.translateX,
		// oam.y + entry.y + frame.translateY). This is the bbox we need to
		// allocate for the output image.
		//
		// Entries and OAMs that are hidden (visible=false, set via the
		// layer panel) contribute nothing to the bbox and are skipped in
		// the composite pass below.
		int unionMinX = Integer.MAX_VALUE, unionMinY = Integer.MAX_VALUE;
		int unionMaxX = Integer.MIN_VALUE, unionMaxY = Integer.MIN_VALUE;
		boolean any = false;
		for (int i = 0; i < n; i++) {
			ResolvedEntry r = resolved[i];
			if (r == null || r.cell == null || r.cell.oams.isEmpty()) {
				continue;
			}
			Sprite2DMultiCell.MultiCellEntry entry = mc.entries.get(i);
			if (!entry.visible) {
				continue;
			}
			int baseX = entry.x + r.translateX;
			int baseY = entry.y + r.translateY;
			for (Sprite2DOAM oam : r.cell.oams) {
				if (!oam.visible) {
					continue;
				}
				// For doubleSize OAMs, oam.(x,y) is the top-left of the
				// DS 2x rendering area; the content sits (w/2, h/2) inside
				// that. Union the content bbox, not the outer box.
				int x0 = oam.x + baseX + oam.getContentOffsetX();
				int y0 = oam.y + baseY + oam.getContentOffsetY();
				int x1 = x0 + oam.width;
				int y1 = y0 + oam.height;
				if (x0 < unionMinX) unionMinX = x0;
				if (y0 < unionMinY) unionMinY = y0;
				if (x1 > unionMaxX) unionMaxX = x1;
				if (y1 > unionMaxY) unionMaxY = y1;
				any = true;
			}
		}
		if (!any) {
			return null;
		}
		int outW = unionMaxX - unionMinX;
		int outH = unionMaxY - unionMinY;
		if (outW <= 0 || outH <= 0) {
			return null;
		}

		BufferedImage out = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();

		// Second pass: composite the OAMs. Iterate entries in reverse so
		// the first multi-cell entry ends up drawn last (on top), and
		// within each entry iterate OAMs in reverse so OAM #0 ends up
		// drawn last (on top) — both matching NitroPaint's loops.
		Sprite2DTileSheet ts = res.getActiveTileSheet();
		Sprite2DPalette pal = res.palettes.get(0);
		int mappingMode = res.mappingMode;
		for (int i = n - 1; i >= 0; i--) {
			ResolvedEntry r = resolved[i];
			if (r == null || r.cell == null || r.cell.oams.isEmpty()) {
				continue;
			}
			Sprite2DMultiCell.MultiCellEntry entry = mc.entries.get(i);
			if (!entry.visible) {
				continue;
			}
			int baseX = entry.x + r.translateX;
			int baseY = entry.y + r.translateY;
			// Effective composite alpha = entry.opacity × oam.opacity.
			// Both default to 1.0 so the common path takes the else branch.
			for (int oi = r.cell.oams.size() - 1; oi >= 0; oi--) {
				Sprite2DOAM oam = r.cell.oams.get(oi);
				if (!oam.visible) {
					continue;
				}
				BufferedImage oamImg = renderOAM(oam, ts, pal, mappingMode, r.cell);
				// doubleSize: OAM position is top-left of 2x area; content
				// is centered inside, so shift by (w/2, h/2).
				int dx = oam.x + baseX + oam.getContentOffsetX() - unionMinX;
				int dy = oam.y + baseY + oam.getContentOffsetY() - unionMinY;
				float alpha = Math.max(0f, Math.min(1f, entry.opacity * oam.opacity));
				if (alpha < 1.0f) {
					java.awt.Composite prev = g.getComposite();
					g.setComposite(java.awt.AlphaComposite.getInstance(
						java.awt.AlphaComposite.SRC_OVER, alpha));
					g.drawImage(oamImg, dx, dy, null);
					g.setComposite(prev);
				} else {
					g.drawImage(oamImg, dx, dy, null);
				}
			}
		}
		g.dispose();

		// Third pass: build per-entry hit-test rectangles for the Move
		// tool. Each entry's bbox is its cell's OAM union, translated by
		// the entry's offset (plus per-frame translate) and reduced by
		// unionMin to get screen-space coords. Layout list is in entry
		// order (index 0 first) so the canvas can iterate forward and
		// pick up the topmost entry.
		java.util.List<MultiCellEntryLayout> layouts = new java.util.ArrayList<>();
		for (int i = 0; i < n; i++) {
			ResolvedEntry r = resolved[i];
			if (r == null || r.cell == null || r.cell.oams.isEmpty()) {
				continue;
			}
			Sprite2DMultiCell.MultiCellEntry entry = mc.entries.get(i);
			if (!entry.visible) {
				// Hidden entries don't contribute a hit-test rect — the
				// Move tool can't grab something the user can't see.
				continue;
			}
			int[] bbox = cellOAMBounds(r.cell);
			if (bbox == null) {
				continue;
			}
			int drawX = (entry.x + r.translateX + bbox[0]) - unionMinX;
			int drawY = (entry.y + r.translateY + bbox[1]) - unionMinY;
			int w = bbox[2] - bbox[0];
			int h = bbox[3] - bbox[1];
			// image is unused now (the Move tool only needs the rectangle),
			// so pass null instead of caching a per-entry BufferedImage.
			layouts.add(new MultiCellEntryLayout(entry, null, drawX, drawY, w, h, i));
		}

		return new MultiCellLayout(out, layouts, unionMinX, unionMinY);
	}

	/**
	 * Computes the (minX, minY, maxX, maxY) bounding box of a cell's OAMs
	 * in the cell's local coord space. Returns null if the cell has no
	 * OAMs (in which case it cannot be placed in a multi-cell layout).
	 */
	private static int[] cellOAMBounds(Sprite2DCell cell) {
		if (cell == null || cell.oams.isEmpty()) {
			return null;
		}
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
		boolean any = false;
		for (Sprite2DOAM oam : cell.oams) {
			if (!oam.visible) {
				continue; // hidden layers don't contribute to the hit-test bbox
			}
			// OAMs with doubleSize=true store the top-left of a 2x
			// rendering area in (x,y) while the actual content sits
			// centred at (x + width/2, y + height/2). cellOAMBounds
			// previously ignored this shift, so hit-test rects (and
			// now the Layers-panel selection highlight) were drawn a
			// half-OAM-width to the LEFT of where the content actually
			// rendered — visible as an off-target box around animated
			// body parts whose sub-cell used doubleSize OAMs. Apply
			// contentOffsetX/Y consistently with the render loop.
			int cx = oam.x + oam.getContentOffsetX();
			int cy = oam.y + oam.getContentOffsetY();
			if (cx < minX) minX = cx;
			if (cy < minY) minY = cy;
			if (cx + oam.width > maxX) maxX = cx + oam.width;
			if (cy + oam.height > maxY) maxY = cy + oam.height;
			any = true;
		}
		if (!any) {
			return null;
		}
		return new int[] { minX, minY, maxX, maxY };
	}

	/**
	 * The cell + per-frame translate that a multi-cell entry resolves to
	 * for a particular sub-frame. The per-frame translate is folded in
	 * because NANR sequence types 1 (affine) and 2 (translate) carry an
	 * extra (px,py) offset per frame that {@link Sprite2DAnimFrame}
	 * stores in {@code translateX}/{@code translateY} — see
	 * {@code AnmCalcTransformMatrix} in NitroPaint, which feeds those
	 * fields into {@code CellRender} as part of {@code xOffs}/{@code yOffs}.
	 */
	private static class ResolvedEntry {
		final Sprite2DCell cell;
		final int translateX;
		final int translateY;

		ResolvedEntry(Sprite2DCell cell, int translateX, int translateY) {
			this.cell = cell;
			this.translateX = translateX;
			this.translateY = translateY;
		}
	}

	/**
	 * Resolves a multi-cell entry to the underlying cell + per-frame
	 * translate using a FRAME-INDEX clock (legacy behaviour). Kept for
	 * external callers that still think in "sub-frame" terms; internal
	 * code uses {@link #resolveEntryAtTick} instead.
	 */
	private static ResolvedEntry resolveEntry(Sprite2DMultiCell.MultiCellEntry entry, Sprite2DResource res, int subFrameIdx) {
		if (entry == null) {
			return null;
		}
		if (entry.animIndex >= 0 && entry.animIndex < res.cellAnimations.size()) {
			Sprite2DCellAnimation anim = res.cellAnimations.get(entry.animIndex);
			if (!anim.frames.isEmpty()) {
				int idx = Math.floorMod(subFrameIdx, anim.frames.size());
				Sprite2DAnimFrame f = anim.frames.get(idx);
				if (f.cellIndex >= 0 && f.cellIndex < res.cells.size()) {
					return new ResolvedEntry(
						res.cells.get(f.cellIndex),
						(int) f.translateX,
						(int) f.translateY);
				}
			}
		}
		if (entry.animIndex >= 0 && entry.animIndex < res.cells.size()) {
			return new ResolvedEntry(res.cells.get(entry.animIndex), 0, 0);
		}
		return null;
	}

	/**
	 * Tick-driven version of {@link #resolveEntry}.
	 *
	 * <p>Converts a master tick count into the appropriate frame of the
	 * entry's referenced NANR by walking that NANR's cumulative per-frame
	 * durations, then wrapping around its total length. This is what
	 * gives each multi-cell entry its own independent clock: two entries
	 * pointing at NANRs of completely different lengths can share the
	 * same {@code elapsedTicks} and still land on the right frame each
	 * for their respective sub-animations.</p>
	 *
	 * <p>Falls through to a direct cell lookup if {@code animIndex} does
	 * not name a valid {@code Sprite2DCellAnimation}, matching the
	 * legacy {@link #resolveEntry} fallback semantics.</p>
	 */
	private static ResolvedEntry resolveEntryAtTick(Sprite2DMultiCell.MultiCellEntry entry, Sprite2DResource res, long elapsedTicks) {
		if (entry == null) {
			return null;
		}
		if (entry.animIndex >= 0 && entry.animIndex < res.cellAnimations.size()) {
			Sprite2DCellAnimation anim = res.cellAnimations.get(entry.animIndex);
			if (!anim.frames.isEmpty()) {
				// Compute this NANR's total duration in ticks. Frames
				// with zero-or-negative duration are clamped to 1 tick
				// so a badly-authored sequence still advances.
				long total = 0;
				for (int j = 0; j < anim.frames.size(); j++) {
					total += Math.max(1, anim.frames.get(j).duration);
				}
				long tick = Math.floorMod(elapsedTicks, total);
				int frameIdx = 0;
				long accum = 0;
				for (int j = 0; j < anim.frames.size(); j++) {
					int dur = Math.max(1, anim.frames.get(j).duration);
					if (tick < accum + dur) {
						frameIdx = j;
						break;
					}
					accum += dur;
				}
				Sprite2DAnimFrame f = anim.frames.get(frameIdx);
				if (f.cellIndex >= 0 && f.cellIndex < res.cells.size()) {
					return new ResolvedEntry(
						res.cells.get(f.cellIndex),
						(int) f.translateX,
						(int) f.translateY);
				}
			}
		}
		if (entry.animIndex >= 0 && entry.animIndex < res.cells.size()) {
			return new ResolvedEntry(res.cells.get(entry.animIndex), 0, 0);
		}
		return null;
	}

	/**
	 * Renders a palette as a grid of color swatches. Each swatch is
	 * {@code SWATCH_SIZE} pixels square. The grid is laid out 16 columns
	 * wide so a 16-color palette becomes 16x1 and a 256-color palette
	 * becomes 16x16.
	 */
	public static final int SWATCH_SIZE = 16;

	public static BufferedImage renderPalette(Sprite2DPalette palette) {
		if (palette == null || palette.colors == null || palette.colors.length == 0) {
			return EMPTY;
		}
		int colorCount = palette.colors.length;
		int cols = Math.min(16, colorCount);
		int rows = (colorCount + cols - 1) / cols;
		int imgW = cols * SWATCH_SIZE;
		int imgH = rows * SWATCH_SIZE;
		BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < colorCount; i++) {
			int cx = (i % cols) * SWATCH_SIZE;
			int cy = (i / cols) * SWATCH_SIZE;
			int argb = palette.colors[i];
			// Force opaque for visualization (index 0 is otherwise transparent)
			int opaque = 0xFF000000 | (argb & 0x00FFFFFF);
			for (int py = 0; py < SWATCH_SIZE; py++) {
				for (int px = 0; px < SWATCH_SIZE; px++) {
					// Draw a thin black border around each swatch
					if (px == 0 || py == 0 || px == SWATCH_SIZE - 1 || py == SWATCH_SIZE - 1) {
						img.setRGB(cx + px, cy + py, 0xFF000000);
					} else {
						img.setRGB(cx + px, cy + py, opaque);
					}
				}
			}
		}
		return img;
	}

	/**
	 * Renders a tile sheet as a flat dump of all 8x8 tiles laid out in a
	 * grid. The width follows {@link Sprite2DTileSheet#getEffectiveTileWidth()}
	 * so the canvas reproduces the on-disk layout (matching tools like
	 * Tinke). For 1D / lineal-mapped sheets the renderer reorders tiles
	 * by OBJ-size sub-blocks via
	 * {@link Sprite2DTileSheet#displayTileToStorageTile} so each cell sprite
	 * appears coherent. Each pixel uses the supplied palette (defaulting to
	 * a grayscale ramp if {@code palette} is {@code null}).
	 */
	public static BufferedImage renderTileSheet(Sprite2DTileSheet tileSheet, Sprite2DPalette palette) {
		if (tileSheet == null) {
			return EMPTY;
		}
		int tileCount = tileSheet.getTileCount();
		if (tileCount <= 0) {
			return EMPTY;
		}
		int cols = Math.max(1, tileSheet.getEffectiveTileWidth());
		int rows = (tileCount + cols - 1) / cols;
		int imgW = cols * 8;
		int imgH = rows * 8;
		BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);

		int colorsPerSubPal = (palette != null) ? palette.format : 256;
		boolean is4bpp = colorsPerSubPal <= 16;

		for (int dispTy = 0; dispTy < rows; dispTy++) {
			for (int dispTx = 0; dispTx < cols; dispTx++) {
				int storageIdx = tileSheet.displayTileToStorageTile(dispTx, dispTy);
				if (storageIdx < 0) {
					continue;
				}
				int outX = dispTx * 8;
				int outY = dispTy * 8;
				for (int py = 0; py < 8; py++) {
					for (int px = 0; px < 8; px++) {
						int palIdx = tileSheet.getPixel(storageIdx, px, py);
						int argb;
						if (palIdx == 0) {
							argb = 0x00000000; // transparent
						} else if (palette != null) {
							// 4bpp tile sheets render against the first sub-palette
							// by default; 8bpp uses the absolute index directly.
							int absIdx = is4bpp ? (palIdx & 0x0F) : palIdx;
							int rawArgb = palette.getColor(absIdx);
							argb = 0xFF000000 | (rawArgb & 0x00FFFFFF);
						} else {
							// Grayscale fallback when no palette is loaded
							int g = (palIdx * 255 / Math.max(1, colorsPerSubPal - 1)) & 0xFF;
							argb = 0xFF000000 | (g << 16) | (g << 8) | g;
						}
						img.setRGB(outX + px, outY + py, argb);
					}
				}
			}
		}
		return img;
	}
}
