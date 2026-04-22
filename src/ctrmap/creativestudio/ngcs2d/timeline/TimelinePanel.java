package ctrmap.creativestudio.ngcs2d.timeline;

import ctrmap.creativestudio.ngcs2d.canvas.undo.SetFrameAffineAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.SpriteUndoManager;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Vegas-style timeline for NANR / NMAR animations.
 *
 * <p>Stack (top → bottom):</p>
 * <ul>
 *   <li>Header row: title label + affine-tracks toggle (NANR only).</li>
 *   <li>{@link RulerComponent}: tick marks every 30 / 60 / 120 ticks,
 *       labels at round numbers.</li>
 *   <li>{@link CellTrackComponent}: coloured bar per frame, width =
 *       duration. Click to scrub, drag bar edges to resize.</li>
 *   <li>Up to 5 {@link AffineSubTrackComponent}s (rotation, scaleX/Y,
 *       translateX/Y), NANR only, visible only when the toggle is on or
 *       when any frame holds a non-identity value.</li>
 *   <li>Toolbar: insert / delete / zoom buttons.</li>
 * </ul>
 *
 * <p>The playhead (vertical yellow line) overlays every row. Its
 * position is read from a supplied {@code LongSupplier} — usually
 * {@code CS2DAnimControlPanel::getElapsedTicks}.</p>
 */
public class TimelinePanel extends JPanel {

	private final TimelineGeometry geom = new TimelineGeometry();
	private final SpriteUndoManager undoManager;

	private final JLabel titleLabel;
	private final JCheckBox showAffineToggle;
	private final RulerComponent ruler;
	private final CellTrackComponent cellTrack;
	private final List<AffineSubTrackComponent> affineTracks = new ArrayList<>();
	private final JPanel affineContainer;
	private final JPanel trackStack;
	private final JPanel toolbar;

	private Object animation;
	/** Overlay covering the tracks so we can draw the playhead without
	 *  reinventing it in every sub-component. */
	private final TrackOverlay overlay;

	private LongConsumer externalScrubListener;
	private CellTrackComponent.LongSupplier playheadSource = () -> 0L;

	public TimelinePanel(SpriteUndoManager undoManager) {
		super(new BorderLayout());
		this.undoManager = undoManager;
		setBorder(BorderFactory.createTitledBorder("Timeline"));

		// --- Header row ---
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		titleLabel = new JLabel(" (no animation) ");
		titleLabel.setForeground(Color.DARK_GRAY);
		header.add(titleLabel, BorderLayout.WEST);

		showAffineToggle = new JCheckBox("Show affine tracks");
		showAffineToggle.setOpaque(false);
		showAffineToggle.setVisible(false);
		showAffineToggle.addActionListener(e -> rebuildAffineRows());
		JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		headerRight.setOpaque(false);
		headerRight.add(showAffineToggle);
		header.add(headerRight, BorderLayout.EAST);

		add(header, BorderLayout.NORTH);

		// --- Track stack (ruler + cell + affine) wrapped with overlay ---
		trackStack = new JPanel();
		trackStack.setLayout(new BoxLayout(trackStack, BoxLayout.Y_AXIS));

		ruler = new RulerComponent(geom);
		trackStack.add(ruler);

		cellTrack = new CellTrackComponent(geom);
		cellTrack.setUndoManager(undoManager);
		cellTrack.setRepaintListener(this::repaintAllTracks);
		trackStack.add(cellTrack);

		affineContainer = new JPanel();
		affineContainer.setLayout(new BoxLayout(affineContainer, BoxLayout.Y_AXIS));
		affineContainer.setOpaque(false);
		trackStack.add(affineContainer);

		// Overlay sits over trackStack in a JLayeredPane-equivalent via
		// an OverlayLayout-ish custom paint. Simpler: put trackStack and
		// a glass-pane overlay in a small JPanel with null layout.
		JPanel tracksWithOverlay = new JPanel(null) {
			@Override public Dimension getPreferredSize() {
				return trackStack.getPreferredSize();
			}
			@Override public void doLayout() {
				int w = getWidth(), h = getHeight();
				trackStack.setBounds(0, 0, w, h);
				if (overlay != null) overlay.setBounds(0, 0, w, h);
			}
		};
		tracksWithOverlay.add(trackStack);
		overlay = new TrackOverlay();
		tracksWithOverlay.add(overlay);
		tracksWithOverlay.setComponentZOrder(overlay, 0);
		add(tracksWithOverlay, BorderLayout.CENTER);

		// --- Toolbar ---
		toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JButton btnInsert = new JButton("\u2795 Insert");
		btnInsert.setToolTipText("Split the frame at the playhead into two");
		btnInsert.addActionListener(e -> cellTrack.insertFrameAtTick(playheadSource.getAsLong()));
		JButton btnDelete = new JButton("\uD83D\uDDD1 Delete");
		btnDelete.setToolTipText("Remove the frame at the playhead");
		btnDelete.addActionListener(e -> cellTrack.deleteFrameAtTick(playheadSource.getAsLong()));
		JButton btnZoomIn = new JButton("+");
		btnZoomIn.setToolTipText("Zoom in");
		btnZoomIn.setMargin(new java.awt.Insets(1, 6, 1, 6));
		btnZoomIn.addActionListener(e -> {
			geom.setPxPerTick(geom.getPxPerTick() * 1.5);
			refreshLayout();
		});
		JButton btnZoomOut = new JButton("\u2212");
		btnZoomOut.setToolTipText("Zoom out");
		btnZoomOut.setMargin(new java.awt.Insets(1, 6, 1, 6));
		btnZoomOut.addActionListener(e -> {
			geom.setPxPerTick(geom.getPxPerTick() / 1.5);
			refreshLayout();
		});
		JButton btnFit = new JButton("Fit");
		btnFit.setToolTipText("Zoom to fit whole animation");
		btnFit.setMargin(new java.awt.Insets(1, 6, 1, 6));
		btnFit.addActionListener(e -> {
			geom.fitToWidth(trackStack.getWidth());
			refreshLayout();
		});
		toolbar.add(btnInsert);
		toolbar.add(btnDelete);
		toolbar.add(Box.createHorizontalStrut(12));
		toolbar.add(btnZoomOut);
		toolbar.add(btnZoomIn);
		toolbar.add(btnFit);
		add(toolbar, BorderLayout.SOUTH);

		// --- Resize handling: auto-fit when no manual zoom ---
		addComponentListener(new ComponentAdapter() {
			@Override public void componentResized(ComponentEvent e) {
				if (geom.getTotalDuration() > 0) {
					geom.fitToWidth(trackStack.getWidth());
					refreshLayout();
				}
			}
		});

		// Cell track forwards scrub events; we relay to the outside
		// world and also trigger a repaint so the playhead follows.
		cellTrack.setScrubListener(tick -> {
			if (externalScrubListener != null) externalScrubListener.accept(tick);
			repaintAllTracks();
		});
		// Clicking anywhere on the ruler also scrubs.
		ruler.addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) {
				long tick = geom.pxToTick(e.getX());
				if (externalScrubListener != null) externalScrubListener.accept(tick);
				repaintAllTracks();
			}
		});

		// Start blank.
		clear();
	}

	/** Installs the listener that receives scrub/seek events. */
	public void setTickSeekListener(LongConsumer listener) {
		this.externalScrubListener = listener;
	}

	/** Installs the playhead position source (typically
	 *  {@code CS2DAnimControlPanel::getElapsedTicks}). */
	public void setPlayheadSource(CellTrackComponent.LongSupplier src) {
		this.playheadSource = (src != null) ? src : () -> 0L;
		cellTrack.setPlayheadSource(this.playheadSource);
	}

	/** Binds the timeline to an animation. Accepts either
	 *  {@link Sprite2DCellAnimation} or
	 *  {@link Sprite2DMultiCellAnimation}. */
	public void setAnimation(Object anim) {
		this.animation = anim;
		if (anim instanceof Sprite2DCellAnimation) {
			Sprite2DCellAnimation a = (Sprite2DCellAnimation) anim;
			geom.setTotalDuration(a.getTotalDuration());
			cellTrack.setAnimation(a);
			buildAffineTracksFor(a);
			titleLabel.setText(" " + safeLabel(a.getName()) + " (" + a.getFrameCount() + " frames) ");
			showAffineToggle.setVisible(true);
		} else if (anim instanceof Sprite2DMultiCellAnimation) {
			Sprite2DMultiCellAnimation a = (Sprite2DMultiCellAnimation) anim;
			geom.setTotalDuration(a.getTotalDuration());
			cellTrack.setAnimation(a);
			clearAffineTracks();
			titleLabel.setText(" " + safeLabel(a.getName()) + " (" + a.getFrameCount() + " frames) ");
			showAffineToggle.setVisible(false);
		} else {
			clear();
			return;
		}
		geom.fitToWidth(trackStack.getWidth());
		refreshLayout();
	}

	/** Collapses to the "no animation" placeholder. */
	public void clear() {
		this.animation = null;
		geom.setTotalDuration(0);
		cellTrack.setAnimation(null);
		clearAffineTracks();
		titleLabel.setText(" (no animation) ");
		showAffineToggle.setVisible(false);
		refreshLayout();
	}

	/** Rebuilds and re-measures all sub-components so a zoom or
	 *  animation change reshapes the strip. */
	private void refreshLayout() {
		int totalPx = geom.tickToPx(Math.max(0, geom.getTotalDuration()))
			+ TimelineGeometry.RIGHT_PAD;
		Dimension rulerSize = new Dimension(totalPx, RulerComponent.RULER_HEIGHT);
		ruler.setPreferredSize(rulerSize);
		Dimension cellSize = new Dimension(totalPx, CellTrackComponent.TRACK_HEIGHT);
		cellTrack.setPreferredSize(cellSize);
		for (AffineSubTrackComponent t : affineTracks) {
			t.setPreferredSize(new Dimension(totalPx, AffineSubTrackComponent.SUBTRACK_HEIGHT));
		}
		trackStack.revalidate();
		repaintAllTracks();
	}

	private void repaintAllTracks() {
		ruler.repaint();
		cellTrack.repaint();
		for (AffineSubTrackComponent t : affineTracks) t.repaint();
		overlay.repaint();
	}

	private void buildAffineTracksFor(Sprite2DCellAnimation anim) {
		clearAffineTracks();
		affineTracks.add(new AffineSubTrackComponent(geom, SetFrameAffineAction.Field.ROTATION, 0f));
		affineTracks.add(new AffineSubTrackComponent(geom, SetFrameAffineAction.Field.SCALE_X, 1f));
		affineTracks.add(new AffineSubTrackComponent(geom, SetFrameAffineAction.Field.SCALE_Y, 1f));
		affineTracks.add(new AffineSubTrackComponent(geom, SetFrameAffineAction.Field.TRANSLATE_X, 0f));
		affineTracks.add(new AffineSubTrackComponent(geom, SetFrameAffineAction.Field.TRANSLATE_Y, 0f));
		for (AffineSubTrackComponent t : affineTracks) {
			t.setAnimation(anim);
			t.setUndoManager(undoManager);
			t.setRepaintListener(this::repaintAllTracks);
		}
		rebuildAffineRows();
	}

	private void clearAffineTracks() {
		affineContainer.removeAll();
		affineTracks.clear();
	}

	/** Decides which affine sub-tracks to mount. The checkbox forces
	 *  them all visible (authoring mode); otherwise only non-identity
	 *  tracks appear so the common cell-only animation stays tidy. */
	private void rebuildAffineRows() {
		affineContainer.removeAll();
		if (affineTracks.isEmpty()) {
			affineContainer.revalidate();
			affineContainer.repaint();
			return;
		}
		boolean force = showAffineToggle.isSelected();
		for (AffineSubTrackComponent t : affineTracks) {
			if (force || t.hasNonIdentityValues()) {
				affineContainer.add(t);
			}
		}
		affineContainer.revalidate();
		affineContainer.repaint();
	}

	private static String safeLabel(String s) {
		if (s == null || s.isEmpty()) return "(unnamed)";
		return s.length() > 32 ? s.substring(0, 29) + "…" : s;
	}

	// -------- Inner components --------

	/** The tick ruler painted across the top of the timeline. */
	private static class RulerComponent extends JPanel {
		static final int RULER_HEIGHT = 20;
		private static final Color BG = new Color(28, 28, 32);
		private static final Color TICK_MINOR = new Color(80, 80, 90);
		private static final Color TICK_MAJOR = new Color(170, 170, 190);
		private static final Color TEXT = new Color(200, 200, 210);

		private final TimelineGeometry geom;

		RulerComponent(TimelineGeometry geom) {
			this.geom = geom;
			setBackground(BG);
			setPreferredSize(new Dimension(200, RULER_HEIGHT));
		}

		@Override
		protected void paintComponent(Graphics gr) {
			super.paintComponent(gr);
			Graphics2D g = (Graphics2D) gr;
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int w = getWidth(), h = getHeight();
			g.setColor(BG);
			g.fillRect(0, 0, w, h);

			int total = geom.getTotalDuration();
			if (total <= 0) return;

			// Pick a tick spacing that keeps labels readable.
			int spacing = pickMajorSpacing(geom.getPxPerTick());
			int minorSpacing = Math.max(1, spacing / 5);

			g.setColor(TICK_MINOR);
			for (int t = 0; t <= total; t += minorSpacing) {
				int x = geom.tickToPx(t);
				g.drawLine(x, h - 4, x, h - 1);
			}
			g.setColor(TICK_MAJOR);
			for (int t = 0; t <= total; t += spacing) {
				int x = geom.tickToPx(t);
				g.drawLine(x, h - 8, x, h - 1);
				String label = String.valueOf(t);
				g.setColor(TEXT);
				g.drawString(label, x + 2, h - 9);
				g.setColor(TICK_MAJOR);
			}
		}

		private static int pickMajorSpacing(double pxPerTick) {
			// Aim for ~60 px between major ticks.
			double target = 60.0 / Math.max(pxPerTick, 0.001);
			int[] candidates = { 1, 2, 5, 10, 15, 30, 60, 120, 300, 600 };
			for (int c : candidates) {
				if (c >= target) return c;
			}
			return candidates[candidates.length - 1];
		}
	}

	/** Glass-pane overlay painting the playhead line on top of all
	 *  tracks at once. Transparent background. */
	private class TrackOverlay extends JPanel {
		TrackOverlay() {
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics gr) {
			super.paintComponent(gr);
			if (geom.getTotalDuration() <= 0) return;
			long tick = playheadSource.getAsLong();
			int x = geom.tickToPx(tick);
			Graphics2D g = (Graphics2D) gr;
			// Draw the playhead spanning the full overlay height.
			g.setColor(new Color(255, 208, 0, 220));
			g.drawLine(x, 0, x, getHeight());
			// Small triangle handle on top so the user can see it
			// regardless of which track it's currently over.
			int[] xs = { x - 4, x + 4, x };
			int[] ys = { 0, 0, 6 };
			g.fillPolygon(xs, ys, 3);
			g.setColor(new Color(30, 30, 30, 220));
			g.drawPolygon(xs, ys, 3);
		}
	}
}
