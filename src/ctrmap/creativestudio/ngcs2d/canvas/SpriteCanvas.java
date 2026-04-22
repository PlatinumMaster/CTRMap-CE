package ctrmap.creativestudio.ngcs2d.canvas;

import ctrmap.creativestudio.ngcs2d.canvas.tools.Sprite2DCanvasEvent;
import ctrmap.creativestudio.ngcs2d.canvas.tools.Sprite2DTool;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 * 2D canvas for displaying rendered sprite content with zoom and pan.
 */
public class SpriteCanvas extends JPanel {

	private static final Color BG_DARK = new Color(64, 64, 64);
	private static final Color CHECK_LIGHT = new Color(200, 200, 200);
	private static final Color CHECK_DARK = new Color(160, 160, 160);

	/**
	 * What kind of resource the canvas is currently previewing.
	 */
	public enum DisplayMode {
		EMPTY,
		PALETTE,
		TILE_SHEET,
		CELL,
		CELL_ANIMATION,
		MULTI_CELL,
		MULTI_CELL_ANIMATION
	}

	private Sprite2DResource resource;
	private Sprite2DPalette activePalette;
	private Sprite2DTileSheet activeTileSheet;
	private Sprite2DCell activeCell;
	private Sprite2DCellAnimation activeCellAnim;
	private Sprite2DMultiCell activeMultiCell;
	private Sprite2DMultiCellAnimation activeMultiCellAnim;
	private int animFrameIndex = 0;
	/**
	 * Master tick counter forwarded to the multi-cell renderer so each
	 * entry's sub-NANR runs independently. Driven by
	 * {@link CS2DAnimControlPanel#getElapsedTicks()} and updated alongside
	 * {@link #animFrameIndex} via {@link #setAnimationFrame(int, long)}.
	 */
	private long animElapsedTicks = 0;
	private DisplayMode displayMode = DisplayMode.EMPTY;
	private BufferedImage renderedImage;
	// Cached layout produced by SpriteRenderer.layoutMultiCell whenever a
	// multi-cell preview is active. Drives hit-testing for the Move tool
	// so it can find which entry sits under a given pixel.
	private SpriteRenderer.MultiCellLayout multiCellLayout;

	/** If non-null, the canvas draws an outline box around this entry's
	 *  screen rectangle to indicate it's the Layers-panel-selected
	 *  layer. Identity comparison — the caller must pass the same entry
	 *  instance that's stored in {@code multiCellLayout.entries[*].entry}
	 *  or the outline won't render. */
	private Sprite2DMultiCell.MultiCellEntry highlightedEntry;

	/** Colours for the two-layer highlight outline. The outer dark ring
	 *  gives contrast on light sprites, the inner bright ring does the
	 *  same on dark ones — same idea as Photoshop's marching ants. */
	private static final Color HIGHLIGHT_OUTER = new Color(0, 0, 0, 220);
	private static final Color HIGHLIGHT_INNER = new Color(255, 208, 0, 255);

	private double zoom = 4.0;
	private int panX = 0;
	private int panY = 0;
	private boolean showGrid = false;
	private boolean showTileGrid = true;

	private int lastDragX, lastDragY;
	private boolean isPanning = false;
	private Sprite2DTool activeTool;

	public SpriteCanvas() {
		setBackground(BG_DARK);
		setPreferredSize(new Dimension(800, 600));
		setFocusable(true);

		MouseAdapter pressReleaseHandler = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				requestFocusInWindow();
				if (e.getButton() == MouseEvent.BUTTON2) {
					isPanning = true;
					lastDragX = e.getX();
					lastDragY = e.getY();
					return;
				}
				if (e.getButton() == MouseEvent.BUTTON1 && activeTool != null) {
					activeTool.onPressed(makeToolEvent(e));
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON2) {
					isPanning = false;
					return;
				}
				if (e.getButton() == MouseEvent.BUTTON1 && activeTool != null) {
					activeTool.onReleased(makeToolEvent(e));
				}
			}
		};

		MouseMotionAdapter motionHandler = new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (isPanning) {
					panX += e.getX() - lastDragX;
					panY += e.getY() - lastDragY;
					lastDragX = e.getX();
					lastDragY = e.getY();
					repaint();
					return;
				}
				if (activeTool != null) {
					activeTool.onDragged(makeToolEvent(e));
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				if (activeTool != null) {
					activeTool.onMoved(makeToolEvent(e));
				}
			}
		};

		MouseWheelListener wheelHandler = new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				double factor = e.getWheelRotation() < 0 ? 1.25 : 0.8;
				zoom = Math.max(0.25, Math.min(32.0, zoom * factor));
				repaint();
			}
		};

		addMouseListener(pressReleaseHandler);
		addMouseMotionListener(motionHandler);
		addMouseWheelListener(wheelHandler);
	}

	/**
	 * Sets the currently active drawing tool. Mouse events with the primary
	 * button are dispatched to this tool's pressed/dragged/released callbacks.
	 *
	 * @param tool The tool to activate, or null to disable tool dispatch.
	 */
	public void setActiveTool(Sprite2DTool tool) {
		this.activeTool = tool;
		if (tool != null) {
			Cursor c = tool.getCursor();
			if (c != null) {
				setCursor(c);
			} else {
				setCursor(Cursor.getDefaultCursor());
			}
		} else {
			setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * @return The currently active drawing tool, or null if none is set.
	 */
	public Sprite2DTool getActiveTool() {
		return activeTool;
	}

	private Sprite2DCanvasEvent makeToolEvent(MouseEvent src) {
		Point pix = canvasToPixel(src.getPoint());
		int tileIdx = -1;
		if (renderedImage != null && activeTileSheet != null
			&& pix.x >= 0 && pix.y >= 0
			&& pix.x < renderedImage.getWidth()
			&& pix.y < renderedImage.getHeight()) {
			tileIdx = activeTileSheet.pixelToTileIndex(pix.x, pix.y);
		}
		return new Sprite2DCanvasEvent(src, pix.x, pix.y, tileIdx, src.getButton());
	}

	@Override
	protected void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		Graphics2D g = (Graphics2D) gr;

		int w = getWidth();
		int h = getHeight();

		g.setColor(BG_DARK);
		g.fillRect(0, 0, w, h);

		if (renderedImage == null) {
			g.setColor(Color.GRAY);
			String msg = "No content selected";
			int sw = g.getFontMetrics().stringWidth(msg);
			g.drawString(msg, (w - sw) / 2, h / 2);
			return;
		}

		AffineTransform saved = g.getTransform();

		int imgW = renderedImage.getWidth();
		int imgH = renderedImage.getHeight();
		int ox = (w - (int) (imgW * zoom)) / 2 + panX;
		int oy = (h - (int) (imgH * zoom)) / 2 + panY;

		g.translate(ox, oy);
		g.scale(zoom, zoom);

		int checkSize = 8;
		for (int cy = 0; cy < imgH; cy += checkSize) {
			for (int cx = 0; cx < imgW; cx += checkSize) {
				boolean light = ((cx / checkSize) + (cy / checkSize)) % 2 == 0;
				g.setColor(light ? CHECK_LIGHT : CHECK_DARK);
				g.fillRect(cx, cy, checkSize, checkSize);
			}
		}

		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(renderedImage, 0, 0, null);

		if (showTileGrid && (displayMode == DisplayMode.TILE_SHEET || displayMode == DisplayMode.CELL)) {
			g.setColor(new Color(100, 100, 255, 80));
			for (int tx = 0; tx <= imgW; tx += 8) {
				g.drawLine(tx, 0, tx, imgH);
			}
			for (int ty = 0; ty <= imgH; ty += 8) {
				g.drawLine(0, ty, imgW, ty);
			}
		}

		if (showGrid && zoom >= 8.0) {
			g.setColor(new Color(255, 255, 255, 40));
			for (int px = 0; px <= imgW; px++) {
				g.drawLine(px, 0, px, imgH);
			}
			for (int py = 0; py <= imgH; py++) {
				g.drawLine(0, py, imgW, py);
			}
		}

		// Layers-panel selection overlay. Drawn in rendered-image pixel
		// coords inside the pan+scale transform so the box scales with
		// the sprite. Stroke width is scaled inversely so on-screen
		// thickness stays constant regardless of zoom.
		if (highlightedEntry != null && multiCellLayout != null) {
			SpriteRenderer.MultiCellEntryLayout slot = findLayoutFor(highlightedEntry);
			if (slot != null) {
				Stroke savedStroke = g.getStroke();
				float outerPx = Math.max(1.0f / (float) zoom, 1.0f / 32f);
				float innerPx = Math.max(0.6f / (float) zoom, 1.0f / 32f);
				// Outer ring for dark sprites
				g.setStroke(new BasicStroke(outerPx * 2f));
				g.setColor(HIGHLIGHT_OUTER);
				g.drawRect(slot.drawX, slot.drawY,
					Math.max(0, slot.width - 1),
					Math.max(0, slot.height - 1));
				// Inner bright ring
				g.setStroke(new BasicStroke(innerPx * 2f));
				g.setColor(HIGHLIGHT_INNER);
				g.drawRect(slot.drawX, slot.drawY,
					Math.max(0, slot.width - 1),
					Math.max(0, slot.height - 1));
				g.setStroke(savedStroke);
			}
		}

		// Capture the current pan+scale transform so the active tool can
		// project its overlay (selection rect, brush cursor, ...) into
		// pixel space if it wishes.
		AffineTransform canvasTransform = g.getTransform();
		g.setTransform(saved);

		if (activeTool != null) {
			activeTool.paintOverlay(g, canvasTransform);
		}
	}

	/**
	 * Sets the resource for this canvas.
	 */
	public void setResource(Sprite2DResource res) {
		this.resource = res;
		// Auto-select sensible defaults so that the very first imported
		// palette / tile sheet has a place to render against.
		if (res != null) {
			if (activePalette == null && !res.palettes.isEmpty()) {
				activePalette = res.palettes.get(0);
			}
			if (activeTileSheet == null && !res.tileSheets.isEmpty()) {
				activeTileSheet = res.tileSheets.get(0);
			}
		}
		refreshRender();
	}

	/**
	 * Sets the active content to render on the canvas. Used for cell display
	 * mode where the canvas needs all three: palette + tilesheet + cell.
	 */
	public void setActiveContent(Sprite2DPalette pal, Sprite2DTileSheet ts, Sprite2DCell cell) {
		this.activePalette = pal;
		this.activeTileSheet = ts;
		this.activeCell = cell;
		this.displayMode = DisplayMode.CELL;
		refreshRender();
	}

	/**
	 * Switches the canvas to palette preview mode.
	 */
	public void showPalette(Sprite2DPalette pal) {
		this.activePalette = pal;
		this.displayMode = DisplayMode.PALETTE;
		refreshRender();
	}

	/**
	 * Switches the canvas to tile sheet preview mode. The active palette is
	 * used to color the tiles; if none is active a grayscale ramp is used.
	 */
	public void showTileSheet(Sprite2DTileSheet ts) {
		this.activeTileSheet = ts;
		this.displayMode = DisplayMode.TILE_SHEET;
		refreshRender();
	}

	/**
	 * Switches the canvas to cell preview mode using the currently-active
	 * palette and tile sheet.
	 */
	public void showCell(Sprite2DCell cell) {
		this.activeCell = cell;
		this.displayMode = DisplayMode.CELL;
		refreshRender();
	}

	/**
	 * Switches the canvas to cell-animation preview mode. Resets the
	 * animation frame counter to 0; the {@link CS2DAnimControlPanel}
	 * (or any external driver) is expected to call
	 * {@link #setAnimationFrame(int)} to update the displayed frame.
	 */
	public void showCellAnimation(Sprite2DCellAnimation anim) {
		this.activeCellAnim = anim;
		this.animFrameIndex = 0;
		this.animElapsedTicks = 0;
		this.displayMode = DisplayMode.CELL_ANIMATION;
		refreshRender();
	}

	/**
	 * Switches the canvas to multi-cell preview mode (static frame 0).
	 */
	public void showMultiCell(Sprite2DMultiCell mc) {
		this.activeMultiCell = mc;
		this.animFrameIndex = 0;
		this.animElapsedTicks = 0;
		this.displayMode = DisplayMode.MULTI_CELL;
		refreshRender();
	}

	/**
	 * Switches the canvas to multi-cell-animation preview mode. As with
	 * {@link #showCellAnimation}, frame ticks are driven externally by
	 * the animation control panel.
	 */
	public void showMultiCellAnimation(Sprite2DMultiCellAnimation anim) {
		this.activeMultiCellAnim = anim;
		this.animFrameIndex = 0;
		this.animElapsedTicks = 0;
		this.displayMode = DisplayMode.MULTI_CELL_ANIMATION;
		refreshRender();
	}

	/**
	 * Sets the current animation frame index and re-renders. Used by the
	 * {@link CS2DAnimControlPanel} to drive playback. Has no effect when
	 * the canvas is not in an animation display mode.
	 *
	 * <p>Backward-compat overload; prefer {@link #setAnimationFrame(int, long)}
	 * so the multi-cell renderer can see the master tick count too.</p>
	 */
	public void setAnimationFrame(int idx) {
		setAnimationFrame(idx, idx);
	}

	/**
	 * Sets both the outer animation frame index and the master tick
	 * counter, then re-renders. The master tick counter drives each
	 * multi-cell entry's sub-NANR independently so entries with
	 * differing-length animations stay in real-time sync.
	 *
	 * @param idx          Outer frame index (for CELL_ANIMATION /
	 *                     MULTI_CELL_ANIMATION modes).
	 * @param elapsedTicks Monotonic tick count from the control panel.
	 */
	public void setAnimationFrame(int idx, long elapsedTicks) {
		this.animFrameIndex = idx;
		this.animElapsedTicks = elapsedTicks;
		if (displayMode == DisplayMode.CELL_ANIMATION
			|| displayMode == DisplayMode.MULTI_CELL_ANIMATION
			|| displayMode == DisplayMode.MULTI_CELL) {
			refreshRender();
		}
	}

	public int getAnimationFrame() {
		return animFrameIndex;
	}

	public long getAnimationElapsedTicks() {
		return animElapsedTicks;
	}

	public Sprite2DCellAnimation getActiveCellAnimation() {
		return activeCellAnim;
	}

	public Sprite2DMultiCell getActiveMultiCell() {
		return activeMultiCell;
	}

	public Sprite2DMultiCellAnimation getActiveMultiCellAnimation() {
		return activeMultiCellAnim;
	}

	/**
	 * Clears the canvas (no content selected).
	 */
	public void showNothing() {
		this.displayMode = DisplayMode.EMPTY;
		this.activeCell = null;
		this.activeCellAnim = null;
		this.activeMultiCell = null;
		this.activeMultiCellAnim = null;
		refreshRender();
	}

	public DisplayMode getDisplayMode() {
		return displayMode;
	}

	public Sprite2DPalette getActivePalette() {
		return activePalette;
	}

	public Sprite2DTileSheet getActiveTileSheet() {
		return activeTileSheet;
	}

	/**
	 * Re-renders the active content and repaints.
	 */
	public void refreshRender() {
		// Most display modes have no multi-cell entries to hit-test, so
		// drop the cached layout up-front. The MULTI_CELL / MULTI_CELL_ANIMATION
		// branches below repopulate it.
		if (displayMode != DisplayMode.MULTI_CELL
			&& displayMode != DisplayMode.MULTI_CELL_ANIMATION) {
			multiCellLayout = null;
		}
		switch (displayMode) {
			case PALETTE:
				renderedImage = SpriteRenderer.renderPalette(activePalette);
				break;
			case TILE_SHEET:
				renderedImage = SpriteRenderer.renderTileSheet(activeTileSheet, activePalette);
				break;
			case CELL:
				if (activeCell != null && activeTileSheet != null && activePalette != null) {
					int mode = (resource != null)
						? resource.mappingMode
						: Sprite2DResource.MAPPING_MODE_2D;
					renderedImage = SpriteRenderer.renderCell(activeCell, activeTileSheet, activePalette, mode);
				} else {
					renderedImage = null;
				}
				break;
			case CELL_ANIMATION:
				multiCellLayout = null;
				if (activeCellAnim != null && resource != null) {
					int idx = activeCellAnim.frames.isEmpty()
						? 0
						: Math.floorMod(animFrameIndex, activeCellAnim.frames.size());
					renderedImage = SpriteRenderer.renderAnimFrame(activeCellAnim, idx, resource);
				} else {
					renderedImage = null;
				}
				break;
			case MULTI_CELL:
				if (activeMultiCell != null && resource != null) {
					// Drive sub-NANRs off the master tick counter so each
					// entry's own animation advances in real time, even
					// though there is no outer NMAR frame list.
					multiCellLayout = SpriteRenderer.layoutMultiCell(activeMultiCell, resource, animElapsedTicks);
					renderedImage = multiCellLayout != null ? multiCellLayout.image : null;
				} else {
					multiCellLayout = null;
					renderedImage = null;
				}
				break;
			case MULTI_CELL_ANIMATION:
				if (activeMultiCellAnim != null && resource != null) {
					int idx = activeMultiCellAnim.frames.isEmpty()
						? 0
						: Math.floorMod(animFrameIndex, activeMultiCellAnim.frames.size());
					Sprite2DMultiCellAnimation.MultiCellAnimFrame f = activeMultiCellAnim.frames.isEmpty()
						? null
						: activeMultiCellAnim.frames.get(idx);
					if (f != null && f.multiCellIndex >= 0 && f.multiCellIndex < resource.multiCells.size()) {
						// Outer NMAR frame chooses which NMCR to render, but
						// each entry inside that NMCR still uses the master
						// tick counter for its own sub-NANR so slot-level
						// animations stay independent.
						multiCellLayout = SpriteRenderer.layoutMultiCell(
							resource.multiCells.get(f.multiCellIndex), resource, animElapsedTicks);
						renderedImage = multiCellLayout != null ? multiCellLayout.image : null;
					} else {
						multiCellLayout = null;
						renderedImage = null;
					}
				} else {
					multiCellLayout = null;
					renderedImage = null;
				}
				break;
			case EMPTY:
			default:
				renderedImage = null;
				break;
		}
		repaint();
	}

	public void setZoom(double z) {
		this.zoom = Math.max(0.25, Math.min(32.0, z));
		repaint();
	}

	public double getZoom() {
		return zoom;
	}

	public void setShowGrid(boolean show) {
		this.showGrid = show;
		repaint();
	}

	public void setShowTileGrid(boolean show) {
		this.showTileGrid = show;
		repaint();
	}

	/**
	 * @return The currently-cached multi-cell layout, or null if the canvas
	 *         is not in a multi-cell display mode. The layout's
	 *         {@code entries} list provides per-entry screen rectangles in
	 *         rendered-image pixel coordinates that the Move tool can
	 *         hit-test against.
	 */
	public SpriteRenderer.MultiCellLayout getMultiCellLayout() {
		return multiCellLayout;
	}

	/**
	 * Hit-tests a pixel against the cached multi-cell layout. Returns the
	 * topmost (lowest-index) entry whose bbox covers the point, or
	 * {@code null} if no entry matches or no layout is cached.
	 */
	public SpriteRenderer.MultiCellEntryLayout hitTestMultiCellEntry(int pixelX, int pixelY) {
		if (multiCellLayout == null) {
			return null;
		}
		// First entry in the list is on top — iterate forward and return
		// the first match so the user always grabs the front-most slot.
		for (SpriteRenderer.MultiCellEntryLayout slot : multiCellLayout.entries) {
			if (pixelX >= slot.drawX && pixelX < slot.drawX + slot.width
				&& pixelY >= slot.drawY && pixelY < slot.drawY + slot.height) {
				return slot;
			}
		}
		return null;
	}

	/**
	 * Sets the multi-cell entry that should be outlined on the canvas.
	 * Null clears the outline. Driven by the Layers panel — clicking a
	 * layer row pushes the matching entry here so it gets a visible box.
	 */
	public void setHighlightedEntry(Sprite2DMultiCell.MultiCellEntry entry) {
		if (this.highlightedEntry == entry) return;
		this.highlightedEntry = entry;
		repaint();
	}

	/** @return the entry currently outlined, or {@code null}. */
	public Sprite2DMultiCell.MultiCellEntry getHighlightedEntry() {
		return highlightedEntry;
	}

	/** Finds the layout slot whose {@code entry} matches the supplied one
	 *  by identity. Returns null if the layout is stale or the entry
	 *  isn't part of the currently-rendered multi-cell. */
	private SpriteRenderer.MultiCellEntryLayout findLayoutFor(Sprite2DMultiCell.MultiCellEntry entry) {
		if (multiCellLayout == null || entry == null) return null;
		for (SpriteRenderer.MultiCellEntryLayout slot : multiCellLayout.entries) {
			if (slot.entry == entry) return slot;
		}
		return null;
	}

	/**
	 * Converts a mouse position to pixel coordinates in the rendered image.
	 */
	public Point canvasToPixel(Point mousePoint) {
		if (renderedImage == null) {
			return new Point(0, 0);
		}
		int imgW = renderedImage.getWidth();
		int imgH = renderedImage.getHeight();
		int ox = (getWidth() - (int) (imgW * zoom)) / 2 + panX;
		int oy = (getHeight() - (int) (imgH * zoom)) / 2 + panY;
		int px = (int) ((mousePoint.x - ox) / zoom);
		int py = (int) ((mousePoint.y - oy) / zoom);
		return new Point(px, py);
	}

	/**
	 * Resets the viewport to default zoom and pan.
	 */
	public void resetView() {
		zoom = 4.0;
		panX = 0;
		panY = 0;
		repaint();
	}
}
