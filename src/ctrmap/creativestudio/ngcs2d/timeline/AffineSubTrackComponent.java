package ctrmap.creativestudio.ngcs2d.timeline;

import ctrmap.creativestudio.ngcs2d.canvas.undo.SetFrameAffineAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.SpriteUndoManager;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DAnimFrame;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCellAnimation;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * A single-row sub-track showing one diamond marker per cell-frame for
 * one affine property (rotation / scale / translate). Bound exclusively
 * to {@link Sprite2DCellAnimation} — NANR only;
 * {@code Sprite2DMultiCellAnimation} has no affine data.
 *
 * <p>Markers are positioned at each frame's start tick. Double-click a
 * marker pops a simple input dialog that edits the value; the change
 * goes through {@link SetFrameAffineAction} for undo/redo parity.</p>
 *
 * <p>No curve interpolation is drawn — NITRO uses step semantics, so
 * each frame's affine value is the value held throughout the frame. A
 * thin horizontal segment at the marker's y-position visualises that
 * constant.</p>
 */
public class AffineSubTrackComponent extends JPanel {

	public static final int SUBTRACK_HEIGHT = 22;

	private static final Color BG = new Color(30, 30, 34);
	private static final Color TRACK_LINE = new Color(70, 70, 80);
	private static final Color DIAMOND_FILL = new Color(220, 200, 90);
	private static final Color DIAMOND_FILL_IDENT = new Color(100, 100, 100);
	private static final Color DIAMOND_OUTLINE = new Color(30, 30, 30);
	private static final Color LABEL = new Color(200, 200, 200);

	private final TimelineGeometry geom;
	private final SetFrameAffineAction.Field field;
	private final float identityValue;

	private Sprite2DCellAnimation animation;
	private SpriteUndoManager undoManager;
	private Runnable repaintListener;

	public AffineSubTrackComponent(TimelineGeometry geom,
			SetFrameAffineAction.Field field, float identityValue) {
		this.geom = geom;
		this.field = field;
		this.identityValue = identityValue;
		setBackground(BG);
		setPreferredSize(new Dimension(200, SUBTRACK_HEIGHT));
		installMouse();
	}

	public void setAnimation(Sprite2DCellAnimation anim) {
		this.animation = anim;
		repaint();
	}

	public void setUndoManager(SpriteUndoManager um) { this.undoManager = um; }
	public void setRepaintListener(Runnable r) { this.repaintListener = r; }

	private float valueOf(Sprite2DAnimFrame f) {
		switch (field) {
			case ROTATION:    return f.rotation;
			case SCALE_X:     return f.scaleX;
			case SCALE_Y:     return f.scaleY;
			case TRANSLATE_X: return f.translateX;
			case TRANSLATE_Y: return f.translateY;
			default: return identityValue;
		}
	}

	/** @return true if any frame in the bound animation carries a non-
	 *          identity value for this sub-track's field. Used by
	 *          {@link TimelinePanel} to decide whether to show the row. */
	public boolean hasNonIdentityValues() {
		if (animation == null) return false;
		for (Sprite2DAnimFrame f : animation.frames) {
			if (Math.abs(valueOf(f) - identityValue) > 1e-6f) return true;
		}
		return false;
	}

	public String fieldLabel() {
		switch (field) {
			case ROTATION:    return "Rot";
			case SCALE_X:     return "SclX";
			case SCALE_Y:     return "SclY";
			case TRANSLATE_X: return "TrX";
			case TRANSLATE_Y: return "TrY";
			default: return "?";
		}
	}

	private long frameStartTick(int i) {
		long acc = 0;
		for (int k = 0; k < i; k++) {
			acc += Math.max(1, animation.frames.get(k).duration);
		}
		return acc;
	}

	private void installMouse() {
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() != 2) return;
				int frameIdx = hitTestDiamond(e.getX(), e.getY());
				if (frameIdx < 0) return;
				editFrameValue(frameIdx);
			}
		});
	}

	private int hitTestDiamond(int px, int py) {
		if (animation == null) return -1;
		int cy = getHeight() / 2;
		if (Math.abs(py - cy) > 7) return -1;
		for (int i = 0; i < animation.frames.size(); i++) {
			int cx = geom.tickToPx(frameStartTick(i));
			if (Math.abs(px - cx) <= 7) return i;
		}
		return -1;
	}

	private void editFrameValue(int frameIdx) {
		if (animation == null || undoManager == null) return;
		Sprite2DAnimFrame f = animation.frames.get(frameIdx);
		float oldV = valueOf(f);
		String prompt = fieldLabel() + " for frame " + frameIdx + ":";
		String input = JOptionPane.showInputDialog(
			this, prompt, String.valueOf(oldV));
		if (input == null) return;
		float newV;
		try {
			newV = Float.parseFloat(input.trim());
		} catch (NumberFormatException nfe) {
			JOptionPane.showMessageDialog(this,
				"Not a number: \"" + input + "\"",
				"Invalid value", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (Math.abs(newV - oldV) < 1e-6f) return;
		SetFrameAffineAction action = new SetFrameAffineAction(f, field, oldV, newV);
		action.execute();
		undoManager.perform(action);
		repaint();
		if (repaintListener != null) repaintListener.run();
	}

	@Override
	protected void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		Graphics2D g = (Graphics2D) gr;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int w = getWidth();
		int h = getHeight();
		g.setColor(BG);
		g.fillRect(0, 0, w, h);

		// Sub-track label on the far left, in the LEFT_PAD zone.
		g.setColor(LABEL);
		g.drawString(fieldLabel(), 2, h / 2 + 4);

		// Horizontal baseline.
		g.setColor(TRACK_LINE);
		g.drawLine(TimelineGeometry.LEFT_PAD, h / 2, w, h / 2);

		if (animation == null || animation.frames.isEmpty()) return;

		// One diamond per frame, positioned at its start tick.
		for (int i = 0; i < animation.frames.size(); i++) {
			Sprite2DAnimFrame f = animation.frames.get(i);
			float v = valueOf(f);
			int cx = geom.tickToPx(frameStartTick(i));
			int cy = h / 2;
			boolean nonIdentity = Math.abs(v - identityValue) > 1e-6f;
			Polygon diamond = makeDiamond(cx, cy, 5);
			g.setColor(nonIdentity ? DIAMOND_FILL : DIAMOND_FILL_IDENT);
			g.fillPolygon(diamond);
			g.setColor(DIAMOND_OUTLINE);
			g.drawPolygon(diamond);

			// Value label shown above non-identity markers only, to
			// keep the row readable when most frames are identity.
			if (nonIdentity) {
				String text = formatValue(v);
				g.setColor(LABEL);
				g.drawString(text, cx - g.getFontMetrics().stringWidth(text) / 2,
					cy - 7);
			}
		}
	}

	private static Polygon makeDiamond(int cx, int cy, int r) {
		Polygon p = new Polygon();
		p.addPoint(cx, cy - r);
		p.addPoint(cx + r, cy);
		p.addPoint(cx, cy + r);
		p.addPoint(cx - r, cy);
		return p;
	}

	/** Shorten float display: integers show without a decimal point. */
	private static String formatValue(float v) {
		if (v == (int) v) return Integer.toString((int) v);
		return String.format("%.2f", v);
	}
}
