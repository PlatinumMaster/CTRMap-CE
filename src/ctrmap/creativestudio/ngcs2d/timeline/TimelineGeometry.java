package ctrmap.creativestudio.ngcs2d.timeline;

/**
 * Bidirectional tick ↔ pixel conversion for the timeline.
 *
 * <p>All timeline components share one instance so the ruler, the cell
 * track, the affine sub-tracks, and the playhead all agree on where
 * tick N lives in screen space. The conversion is linear:
 * {@code pixelX = tick * pxPerTick + leftPad} / inverse.</p>
 *
 * <p>{@link #pxPerTick} is mutable so a future zoom control can bump it
 * without recreating the timeline. {@link #totalDuration} is re-set
 * whenever the bound animation changes, and the timeline calls
 * {@link #fitToWidth(int)} on resize to keep the whole clip visible
 * when no explicit zoom has been set.</p>
 */
public class TimelineGeometry {

	/** Fixed left padding in pixels — leaves room for row labels and
	 *  stops the playhead from clipping off the left edge at tick 0. */
	public static final int LEFT_PAD = 8;

	/** Fixed right padding in pixels — keeps a visual margin past the
	 *  end of the last frame. */
	public static final int RIGHT_PAD = 16;

	/** Minimum on-screen px per tick. Anything below this collapses the
	 *  ruler labels into unreadable mush, so we clamp. */
	public static final double MIN_PX_PER_TICK = 0.25;

	/** Maximum on-screen px per tick. Above this, even single ticks
	 *  would be wider than a typical bar — not useful, clamp. */
	public static final double MAX_PX_PER_TICK = 32.0;

	private double pxPerTick = 4.0;
	private int totalDuration = 0;

	public TimelineGeometry() {
	}

	public TimelineGeometry(int totalDuration) {
		this.totalDuration = totalDuration;
	}

	/** @return the current horizontal zoom. */
	public double getPxPerTick() {
		return pxPerTick;
	}

	/** Clamped to [{@link #MIN_PX_PER_TICK}, {@link #MAX_PX_PER_TICK}]. */
	public void setPxPerTick(double px) {
		this.pxPerTick = Math.max(MIN_PX_PER_TICK, Math.min(MAX_PX_PER_TICK, px));
	}

	public int getTotalDuration() {
		return totalDuration;
	}

	public void setTotalDuration(int totalDuration) {
		this.totalDuration = Math.max(0, totalDuration);
	}

	/**
	 * Recomputes {@link #pxPerTick} so the whole animation occupies the
	 * supplied viewport width minus the padding. No-op if total duration
	 * is 0 or the viewport is too narrow. Stays within the zoom clamp.
	 */
	public void fitToWidth(int viewportPx) {
		int usable = viewportPx - LEFT_PAD - RIGHT_PAD;
		if (totalDuration <= 0 || usable <= 0) return;
		setPxPerTick(usable / (double) totalDuration);
	}

	/** Tick → pixel X. */
	public int tickToPx(long tick) {
		return LEFT_PAD + (int) Math.round(tick * pxPerTick);
	}

	/** Pixel X → tick, clamped to {@code [0, totalDuration]}. */
	public long pxToTick(int pixelX) {
		long raw = Math.round((pixelX - LEFT_PAD) / pxPerTick);
		if (raw < 0) return 0;
		if (raw > totalDuration) return totalDuration;
		return raw;
	}

	/** Pixel width of a span covering {@code ticks} ticks. */
	public int ticksToWidth(int ticks) {
		return Math.max(0, (int) Math.round(ticks * pxPerTick));
	}
}
