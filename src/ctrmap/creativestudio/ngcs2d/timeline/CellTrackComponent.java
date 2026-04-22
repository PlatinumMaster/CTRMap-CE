package ctrmap.creativestudio.ngcs2d.timeline;

import ctrmap.creativestudio.ngcs2d.canvas.undo.InsertFrameAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.RemoveFrameAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.ResizeFrameDurationAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.SpriteUndoManager;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DAnimFrame;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.function.LongConsumer;
import javax.swing.JPanel;

/**
 * The main horizontal track in the timeline: one coloured bar per
 * animation frame. Bars are laid out in playback order; each one's width
 * is {@code frame.duration * pxPerTick}. The active frame (the one the
 * playhead sits inside) is rendered with a brighter fill to show the
 * user where "now" is.
 *
 * <p>Interactions:</p>
 * <ul>
 *   <li>Click anywhere on the track → seeks the playhead to that tick
 *       via {@code scrubListener}.</li>
 *   <li>Click and drag on a bar's right edge (3px hot zone) → resizes
 *       the frame's duration, pushes a single
 *       {@link ResizeFrameDurationAction} on release.</li>
 * </ul>
 *
 * <p>Bound either to a {@link Sprite2DCellAnimation} (NANR) or a
 * {@link Sprite2DMultiCellAnimation} (NMAR) via
 * {@link #setAnimation(Object)}; the bar labels and edit actions adapt
 * to which kind is active.</p>
 */
public class CellTrackComponent extends JPanel {

	public static final int TRACK_HEIGHT = 36;

	private static final int EDGE_HIT_PX = 4;
	private static final Color BG = new Color(40, 40, 44);
	private static final Color BAR_FILL_A = new Color(90, 130, 180);
	private static final Color BAR_FILL_B = new Color(70, 105, 150);
	private static final Color BAR_FILL_ACTIVE = new Color(220, 160, 60);
	private static final Color BAR_BORDER = new Color(20, 20, 20);
	private static final Color BAR_TEXT = Color.WHITE;

	private final TimelineGeometry geom;
	private SpriteUndoManager undoManager;
	private LongConsumer scrubListener;
	private Runnable repaintListener;
	private LongSupplier playheadSource = () -> 0L;

	/** One of {@link Sprite2DCellAnimation} or
	 *  {@link Sprite2DMultiCellAnimation}; {@code null} when nothing is
	 *  bound. Held as a plain Object and distinguished via instanceof so
	 *  the component avoids duplicating every bar-handling method per
	 *  type. */
	private Object animation;

	/** Drag state: which frame's right edge is being dragged and its
	 *  starting duration. Non-null only during an active drag. */
	private Integer draggingFrameIndex = null;
	private int dragStartDuration = 0;
	private int dragStartPixelX = 0;

	public CellTrackComponent(TimelineGeometry geom) {
		this.geom = geom;
		setBackground(BG);
		setPreferredSize(new Dimension(200, TRACK_HEIGHT));
		installMouseHandlers();
	}

	public void setUndoManager(SpriteUndoManager um) { this.undoManager = um; }
	public void setScrubListener(LongConsumer l) { this.scrubListener = l; }
	public void setRepaintListener(Runnable r) { this.repaintListener = r; }
	public void setPlayheadSource(LongSupplier src) {
		this.playheadSource = (src != null) ? src : () -> 0L;
	}

	public void setAnimation(Object anim) {
		if (anim != null && !(anim instanceof Sprite2DCellAnimation)
			&& !(anim instanceof Sprite2DMultiCellAnimation)) {
			throw new IllegalArgumentException(
				"CellTrackComponent only accepts Sprite2DCellAnimation or "
				+ "Sprite2DMultiCellAnimation, got " + anim.getClass());
		}
		this.animation = anim;
		repaint();
	}

	/** @return the current frame count, or 0 if nothing is bound. */
	public int getFrameCount() {
		if (animation instanceof Sprite2DCellAnimation) {
			return ((Sprite2DCellAnimation) animation).frames.size();
		}
		if (animation instanceof Sprite2DMultiCellAnimation) {
			return ((Sprite2DMultiCellAnimation) animation).frames.size();
		}
		return 0;
	}

	/** @return the duration of frame {@code i} (minimum 1 for display). */
	private int frameDuration(int i) {
		if (animation instanceof Sprite2DCellAnimation) {
			return Math.max(1, ((Sprite2DCellAnimation) animation).frames.get(i).duration);
		}
		if (animation instanceof Sprite2DMultiCellAnimation) {
			return Math.max(1, ((Sprite2DMultiCellAnimation) animation).frames.get(i).duration);
		}
		return 1;
	}

	/** @return label for frame {@code i}: "Cell N" for NANR, "MC N" for NMAR. */
	private String frameLabel(int i) {
		if (animation instanceof Sprite2DCellAnimation) {
			Sprite2DAnimFrame f = ((Sprite2DCellAnimation) animation).frames.get(i);
			return "Cell " + f.cellIndex;
		}
		if (animation instanceof Sprite2DMultiCellAnimation) {
			Sprite2DMultiCellAnimation.MultiCellAnimFrame f =
				((Sprite2DMultiCellAnimation) animation).frames.get(i);
			return "MC " + f.multiCellIndex;
		}
		return "?";
	}

	/** @return cumulative tick at which frame {@code i} starts. */
	private long frameStartTick(int i) {
		long acc = 0;
		for (int k = 0; k < i; k++) {
			acc += frameDuration(k);
		}
		return acc;
	}

	/** @return the frame index whose [startTick, startTick+duration)
	 *          contains the supplied tick, or -1 if outside. */
	public int frameIndexAtTick(long tick) {
		long acc = 0;
		int n = getFrameCount();
		for (int i = 0; i < n; i++) {
			int dur = frameDuration(i);
			if (tick >= acc && tick < acc + dur) return i;
			acc += dur;
		}
		return -1;
	}

	/**
	 * Inserts a frame at the playhead tick by splitting whichever frame
	 * currently holds the playhead. The left half keeps the original
	 * cellIndex and gets whatever duration lies before the playhead;
	 * the right half (a fresh frame, a copy of the original for NANR
	 * or a new {@code MultiCellAnimFrame} for NMAR) gets the remainder.
	 *
	 * <p>No-op if the playhead sits outside any frame (animation
	 * empty) or the split would produce a zero-duration half.</p>
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void insertFrameAtTick(long tick) {
		int idx = frameIndexAtTick(tick);
		if (idx < 0 || undoManager == null) return;
		long startTick = frameStartTick(idx);
		int origDur = frameDuration(idx);
		int leftDur = (int)(tick - startTick);
		int rightDur = origDur - leftDur;
		if (leftDur <= 0 || rightDur <= 0) return;

		if (animation instanceof Sprite2DCellAnimation) {
			Sprite2DCellAnimation a = (Sprite2DCellAnimation) animation;
			Sprite2DAnimFrame left = a.frames.get(idx);
			Sprite2DAnimFrame right = copyAnimFrame(left);
			right.duration = rightDur;
			// Shrink the left half to the pre-playhead portion via an
			// undoable action, then insert the new right half at idx+1.
			ResizeFrameDurationAction resize =
				ResizeFrameDurationAction.forCellFrame(left, origDur, leftDur);
			resize.execute();
			undoManager.perform(resize);
			InsertFrameAction<Sprite2DAnimFrame> insert =
				new InsertFrameAction<>(a.frames, idx + 1, right);
			insert.execute();
			undoManager.perform(insert);
		} else if (animation instanceof Sprite2DMultiCellAnimation) {
			Sprite2DMultiCellAnimation a = (Sprite2DMultiCellAnimation) animation;
			Sprite2DMultiCellAnimation.MultiCellAnimFrame left = a.frames.get(idx);
			Sprite2DMultiCellAnimation.MultiCellAnimFrame right =
				new Sprite2DMultiCellAnimation.MultiCellAnimFrame(left.multiCellIndex, rightDur);
			ResizeFrameDurationAction resize =
				ResizeFrameDurationAction.forMultiCellFrame(left, origDur, leftDur);
			resize.execute();
			undoManager.perform(resize);
			InsertFrameAction<Sprite2DMultiCellAnimation.MultiCellAnimFrame> insert =
				new InsertFrameAction<>(a.frames, idx + 1, right);
			insert.execute();
			undoManager.perform(insert);
		}
		fireRepaint();
	}

	/**
	 * Removes the frame currently under the playhead tick. No-op when
	 * the tick is outside any frame or the animation is empty.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void deleteFrameAtTick(long tick) {
		int idx = frameIndexAtTick(tick);
		if (idx < 0 || undoManager == null) return;
		if (animation instanceof Sprite2DCellAnimation) {
			Sprite2DCellAnimation a = (Sprite2DCellAnimation) animation;
			if (a.frames.size() <= 1) return; // keep at least one frame
			RemoveFrameAction<Sprite2DAnimFrame> remove =
				new RemoveFrameAction<>(a.frames, a.frames.get(idx));
			remove.execute();
			undoManager.perform(remove);
		} else if (animation instanceof Sprite2DMultiCellAnimation) {
			Sprite2DMultiCellAnimation a = (Sprite2DMultiCellAnimation) animation;
			if (a.frames.size() <= 1) return;
			RemoveFrameAction<Sprite2DMultiCellAnimation.MultiCellAnimFrame> remove =
				new RemoveFrameAction<>(a.frames, a.frames.get(idx));
			remove.execute();
			undoManager.perform(remove);
		}
		fireRepaint();
	}

	private static Sprite2DAnimFrame copyAnimFrame(Sprite2DAnimFrame src) {
		Sprite2DAnimFrame c = new Sprite2DAnimFrame(src.cellIndex, src.duration);
		c.translateX = src.translateX;
		c.translateY = src.translateY;
		c.scaleX = src.scaleX;
		c.scaleY = src.scaleY;
		c.rotation = src.rotation;
		return c;
	}

	private void installMouseHandlers() {
		MouseAdapter mouse = new MouseAdapter() {
			@Override public void mouseMoved(MouseEvent e) {
				int hit = findEdgeFrame(e.getX());
				setCursor(hit >= 0
					? Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
					: Cursor.getDefaultCursor());
			}

			@Override public void mousePressed(MouseEvent e) {
				int hit = findEdgeFrame(e.getX());
				if (hit >= 0) {
					draggingFrameIndex = hit;
					dragStartDuration = frameDuration(hit);
					dragStartPixelX = e.getX();
				} else {
					// Click elsewhere on track = scrub to that tick.
					long tick = geom.pxToTick(e.getX());
					if (scrubListener != null) scrubListener.accept(tick);
				}
			}

			@Override public void mouseReleased(MouseEvent e) {
				if (draggingFrameIndex != null) {
					commitDragDurationChange();
				}
				draggingFrameIndex = null;
			}
		};
		MouseMotionAdapter motion = new MouseMotionAdapter() {
			@Override public void mouseMoved(MouseEvent e) {
				int hit = findEdgeFrame(e.getX());
				setCursor(hit >= 0
					? Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
					: Cursor.getDefaultCursor());
			}

			@Override public void mouseDragged(MouseEvent e) {
				if (draggingFrameIndex != null) {
					applyLiveDurationDrag(e.getX());
				} else {
					long tick = geom.pxToTick(e.getX());
					if (scrubListener != null) scrubListener.accept(tick);
				}
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(motion);
	}

	/** @return the frame whose right-edge is within {@link #EDGE_HIT_PX}
	 *          of {@code pixelX}, or -1. */
	private int findEdgeFrame(int pixelX) {
		int n = getFrameCount();
		long acc = 0;
		for (int i = 0; i < n; i++) {
			acc += frameDuration(i);
			int edgeX = geom.tickToPx(acc);
			if (Math.abs(pixelX - edgeX) <= EDGE_HIT_PX) return i;
		}
		return -1;
	}

	/** Live-updates the dragged frame's duration. No undo action yet —
	 *  that gets pushed on mouse release via
	 *  {@link #commitDragDurationChange()}. */
	private void applyLiveDurationDrag(int pixelX) {
		if (draggingFrameIndex == null) return;
		int idx = draggingFrameIndex;
		int deltaPx = pixelX - dragStartPixelX;
		// Convert px delta to tick delta via the current zoom.
		int deltaTicks = (int) Math.round(deltaPx / geom.getPxPerTick());
		int newDur = Math.max(1, dragStartDuration + deltaTicks);
		setFrameDurationLive(idx, newDur);
		fireRepaint();
	}

	private void setFrameDurationLive(int idx, int newDur) {
		if (animation instanceof Sprite2DCellAnimation) {
			((Sprite2DCellAnimation) animation).frames.get(idx).duration = newDur;
		} else if (animation instanceof Sprite2DMultiCellAnimation) {
			((Sprite2DMultiCellAnimation) animation).frames.get(idx).duration = newDur;
		}
	}

	private void commitDragDurationChange() {
		if (draggingFrameIndex == null || undoManager == null) return;
		int idx = draggingFrameIndex;
		int finalDur = frameDuration(idx);
		if (finalDur == dragStartDuration) return; // no change
		// The live drag already mutated the model; restore to pre-drag so
		// the action.execute() can do a clean transition.
		setFrameDurationLive(idx, dragStartDuration);
		ResizeFrameDurationAction action;
		if (animation instanceof Sprite2DCellAnimation) {
			action = ResizeFrameDurationAction.forCellFrame(
				((Sprite2DCellAnimation) animation).frames.get(idx),
				dragStartDuration, finalDur);
		} else if (animation instanceof Sprite2DMultiCellAnimation) {
			action = ResizeFrameDurationAction.forMultiCellFrame(
				((Sprite2DMultiCellAnimation) animation).frames.get(idx),
				dragStartDuration, finalDur);
		} else {
			return;
		}
		action.execute();
		undoManager.perform(action);
	}

	private void fireRepaint() {
		repaint();
		if (repaintListener != null) repaintListener.run();
	}

	@Override
	protected void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		Graphics2D g = (Graphics2D) gr;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int w = getWidth();
		int h = getHeight();
		g.setColor(BG);
		g.fillRect(0, 0, w, h);

		int n = getFrameCount();
		if (n == 0) {
			g.setColor(Color.GRAY);
			String msg = "(no frames)";
			int sw = g.getFontMetrics().stringWidth(msg);
			g.drawString(msg, (w - sw) / 2, h / 2 + 4);
			return;
		}

		long playhead = playheadSource.getAsLong();
		int activeFrame = frameIndexAtTick(playhead);

		long acc = 0;
		Stroke savedStroke = g.getStroke();
		g.setStroke(new BasicStroke(1f));
		for (int i = 0; i < n; i++) {
			int dur = frameDuration(i);
			int x0 = geom.tickToPx(acc);
			int x1 = geom.tickToPx(acc + dur);
			int bw = Math.max(2, x1 - x0);

			Color fill = (i == activeFrame) ? BAR_FILL_ACTIVE
				: ((i & 1) == 0 ? BAR_FILL_A : BAR_FILL_B);
			g.setColor(fill);
			g.fillRect(x0, 2, bw, h - 4);

			g.setColor(BAR_BORDER);
			g.drawRect(x0, 2, bw - 1, h - 5);

			// Label if there's enough horizontal room
			if (bw > 36) {
				String text = frameLabel(i) + "  (" + dur + "t)";
				g.setColor(BAR_TEXT);
				java.awt.FontMetrics fm = g.getFontMetrics();
				int maxTextW = bw - 8;
				String clipped = clipToWidth(text, fm, maxTextW);
				g.drawString(clipped, x0 + 4, h / 2 + fm.getAscent() / 2 - 2);
			}

			acc += dur;
		}
		g.setStroke(savedStroke);
	}

	private static String clipToWidth(String s, java.awt.FontMetrics fm, int maxW) {
		if (fm.stringWidth(s) <= maxW) return s;
		String ell = "…";
		int ellW = fm.stringWidth(ell);
		int n = s.length();
		while (n > 0 && fm.stringWidth(s.substring(0, n)) + ellW > maxW) n--;
		return s.substring(0, n) + ell;
	}

	// Java 8 doesn't have LongSupplier in java.util.function? Yes it does.
	// Keep our own tiny interface alias anyway to avoid importing the util
	// flavour in every caller; same shape.
	public interface LongSupplier {
		long getAsLong();
	}
}
