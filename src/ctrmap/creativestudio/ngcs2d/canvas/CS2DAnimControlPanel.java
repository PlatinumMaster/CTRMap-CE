package ctrmap.creativestudio.ngcs2d.canvas;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

/**
 * Animation playback controls panel with play/pause/stop and frame slider.
 */
public class CS2DAnimControlPanel extends JPanel {

	private final JButton btnStepBack;
	private final JButton btnPlay;
	private final JButton btnPause;
	private final JButton btnStop;
	private final JButton btnStepForward;
	private final JSlider frameSlider;
	private final JLabel frameLabel;
	private final JSpinner speedSpinner;
	private final Timer animTimer;

	private boolean isPlaying = false;
	private int currentFrame = 0;
	private int totalFrames = 0;
	/**
	 * Per-frame duration list in NDS animation ticks (1/60 sec each).
	 * When present, playback honors each frame's duration rather than
	 * blindly advancing every timer tick. When null or empty, playback
	 * falls back to one-frame-per-tick (legacy behavior).
	 */
	private int[] frameDurations = null;
	/**
	 * Elapsed tick counter within the currently-displayed frame. Reset
	 * whenever we advance to a new frame or the user seeks.
	 */
	private int elapsedInFrame = 0;
	/**
	 * Monotonic master tick counter, driven by the playback timer and
	 * reset only when the user stops playback or loads a new animation.
	 * Used by the multi-cell renderer to drive each entry's sub-NANR
	 * independently: entries with different NANR lengths and per-frame
	 * durations share this one clock so every sub-animation advances in
	 * real time. Always non-negative; wraps via modulo at the renderer
	 * rather than overflowing here.
	 */
	private long elapsedTicks = 0;
	/**
	 * True when the panel should keep the master tick counter running
	 * even though there is no outer frame list (the multi-cell static
	 * preview case). In this mode {@link #totalFrames} is typically 0
	 * and the slider is frozen at 0, but the timer still fires so each
	 * multi-cell entry's sub-NANR can advance independently.
	 */
	private boolean continuousMode = false;
	private final List<Runnable> frameChangeListeners = new ArrayList<>();

	public CS2DAnimControlPanel() {
		setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
		setPreferredSize(new Dimension(800, 50));

		btnStepBack = new JButton("|<");
		btnStepBack.setToolTipText("Step Back");
		btnPlay = new JButton(">");
		btnPlay.setToolTipText("Play");
		btnPause = new JButton("||");
		btnPause.setToolTipText("Pause");
		btnStop = new JButton("[]");
		btnStop.setToolTipText("Stop");
		btnStepForward = new JButton(">|");
		btnStepForward.setToolTipText("Step Forward");

		frameSlider = new JSlider(0, 0, 0);
		frameSlider.setPreferredSize(new Dimension(300, 24));
		frameLabel = new JLabel("Frame: 0 / 0");
		speedSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));
		speedSpinner.setPreferredSize(new Dimension(60, 24));

		animTimer = new Timer(16, e -> advanceFrame());

		btnPlay.addActionListener(e -> {
			isPlaying = true;
			animTimer.start();
		});
		btnPause.addActionListener(e -> {
			isPlaying = false;
			animTimer.stop();
		});
		btnStop.addActionListener(e -> {
			isPlaying = false;
			animTimer.stop();
			currentFrame = 0;
			elapsedTicks = 0;
			elapsedInFrame = 0;
			updateAnimUI();
		});
		btnStepForward.addActionListener(e -> {
			if (totalFrames > 0) {
				currentFrame = (currentFrame + 1) % totalFrames;
				elapsedInFrame = 0;
				// Keep the master tick counter aligned with the stepped
				// frame so sub-NANRs in multi-cell mode jump to the same
				// logical point as the outer slider.
				if (frameDurations != null) {
					elapsedTicks = cumulativeTicksForFrame(currentFrame);
				} else {
					elapsedTicks = currentFrame;
				}
				updateAnimUI();
			}
		});
		btnStepBack.addActionListener(e -> {
			if (totalFrames > 0) {
				currentFrame = (currentFrame - 1 + totalFrames) % totalFrames;
				elapsedInFrame = 0;
				if (frameDurations != null) {
					elapsedTicks = cumulativeTicksForFrame(currentFrame);
				} else {
					elapsedTicks = currentFrame;
				}
				updateAnimUI();
			}
		});
		frameSlider.addChangeListener(e -> {
			if (!isPlaying) {
				currentFrame = frameSlider.getValue();
				elapsedInFrame = 0;
				if (frameDurations != null) {
					elapsedTicks = cumulativeTicksForFrame(currentFrame);
				} else {
					elapsedTicks = currentFrame;
				}
				frameLabel.setText("Frame: " + currentFrame + " / " + totalFrames);
				fireFrameChange();
			}
		});

		add(btnStepBack);
		add(btnPlay);
		add(btnPause);
		add(btnStop);
		add(btnStepForward);
		add(frameSlider);
		add(frameLabel);
		add(new JLabel("Speed:"));
		add(speedSpinner);
	}

	/**
	 * Sets the total frame count for the current animation without
	 * per-frame duration info. Each frame will advance every timer tick
	 * (legacy behavior). Prefer {@link #setFrameDurations(int[])} when
	 * the animation's NANR / NMAR durations are known.
	 */
	public void setTotalFrames(int total) {
		setFrameDurations(null);
		this.totalFrames = total;
		this.continuousMode = false;
		frameSlider.setMaximum(Math.max(0, total - 1));
		currentFrame = Math.min(currentFrame, Math.max(0, total - 1));
		elapsedInFrame = 0;
		elapsedTicks = 0;
		updateAnimUI();
	}

	/**
	 * Installs per-frame duration info (in NDS animation ticks, 1/60 sec
	 * each). The playback timer still fires at a fixed 60 Hz cadence, but
	 * instead of advancing every timer tick the current frame is held
	 * until its duration ticks have elapsed. This mirrors how NitroPaint
	 * and the NDS hardware play NANR / NMAR sequences so frames with
	 * duration > 1 (the common case) no longer rush past at 60 fps.
	 *
	 * @param durations Per-frame duration array, or null to fall back to
	 *                  the one-frame-per-tick legacy path.
	 */
	public void setFrameDurations(int[] durations) {
		this.frameDurations = (durations != null && durations.length > 0) ? durations.clone() : null;
		this.totalFrames = (this.frameDurations != null) ? this.frameDurations.length : 0;
		this.continuousMode = false;
		frameSlider.setMaximum(Math.max(0, totalFrames - 1));
		currentFrame = Math.min(currentFrame, Math.max(0, totalFrames - 1));
		elapsedInFrame = 0;
		elapsedTicks = 0;
		updateAnimUI();
	}

	/**
	 * Enables "continuous" master-tick playback with no outer frame list.
	 *
	 * <p>Used by the multi-cell static preview: there is no single NANR to
	 * advance but the multi-cell entries each have their own sub-NANR that
	 * must keep ticking in real time. When enabled, the slider and frame
	 * label are hidden from use (totalFrames is forced to 0) but the
	 * play/pause/stop buttons still drive the master {@link #elapsedTicks}
	 * counter that {@code SpriteRenderer.layoutMultiCell} consumes.</p>
	 *
	 * @param continuous {@code true} to enable continuous-tick mode.
	 */
	public void setContinuousPlayback(boolean continuous) {
		this.continuousMode = continuous;
		if (continuous) {
			this.frameDurations = null;
			this.totalFrames = 0;
			this.currentFrame = 0;
			this.elapsedInFrame = 0;
			this.elapsedTicks = 0;
			frameSlider.setMaximum(0);
			frameSlider.setValue(0);
		}
		updateAnimUI();
	}

	public int getCurrentFrame() {
		return currentFrame;
	}

	public void setCurrentFrame(int f) {
		this.currentFrame = f;
		this.elapsedInFrame = 0;
		if (frameDurations != null) {
			this.elapsedTicks = cumulativeTicksForFrame(f);
		} else {
			this.elapsedTicks = f;
		}
		updateAnimUI();
	}

	/**
	 * Returns the monotonic master tick counter. Used by the multi-cell
	 * renderer to drive independent sub-NANR playback across entries.
	 *
	 * @return Current elapsed tick count since the last reset / seek.
	 */
	public long getElapsedTicks() {
		return elapsedTicks;
	}

	/**
	 * Seeks the playhead to an absolute tick in the animation's
	 * timeline. Pauses playback (Vegas-style — scrubbing the timeline
	 * should stop runaway advance) and walks the per-frame duration
	 * table to translate tick → (frame, elapsedInFrame). Clamps to the
	 * final frame when {@code tick} exceeds total duration.
	 *
	 * <p>Used by the Vegas-style timeline panel: the user clicks on
	 * the ruler / cell track, and that tick gets delivered here.</p>
	 *
	 * <p>Falls back to the legacy "one frame per tick" model when no
	 * per-frame duration table has been installed, matching the rest
	 * of this class's behaviour for simple sliders.</p>
	 */
	public void seekToTick(long tick) {
		if (tick < 0) tick = 0;
		// Scrubbing pauses playback. The user is explicitly driving
		// the playhead; we shouldn't also be auto-advancing it.
		if (isPlaying) {
			isPlaying = false;
			animTimer.stop();
		}
		if (frameDurations == null || frameDurations.length == 0) {
			// Legacy tick == frame mapping.
			int clampedFrame = totalFrames > 0
				? (int) Math.min(tick, totalFrames - 1)
				: 0;
			currentFrame = clampedFrame;
			elapsedInFrame = 0;
			elapsedTicks = tick;
			updateAnimUI();
			return;
		}
		long acc = 0;
		for (int i = 0; i < frameDurations.length; i++) {
			long dur = Math.max(1, frameDurations[i]);
			if (tick < acc + dur) {
				currentFrame = i;
				elapsedInFrame = (int) (tick - acc);
				elapsedTicks = tick;
				updateAnimUI();
				return;
			}
			acc += dur;
		}
		// Past the end — clamp to last frame's final tick.
		currentFrame = frameDurations.length - 1;
		elapsedInFrame = Math.max(0, frameDurations[currentFrame] - 1);
		elapsedTicks = acc - 1;
		updateAnimUI();
	}

	/**
	 * @return sum of all frame durations in ticks. {@code 0} when no
	 *         duration table has been installed (including continuous
	 *         mode). Used by the timeline ruler.
	 */
	public long getTotalDurationTicks() {
		if (frameDurations == null) return 0;
		long acc = 0;
		for (int d : frameDurations) acc += Math.max(1, d);
		return acc;
	}

	/**
	 * Returns the cumulative tick count at which the given outer frame
	 * begins, so step/seek actions can realign the master clock with the
	 * slider position. Out-of-range frames clamp to 0.
	 */
	private long cumulativeTicksForFrame(int frameIdx) {
		if (frameDurations == null || frameDurations.length == 0) {
			return 0;
		}
		int clamped = Math.max(0, Math.min(frameIdx, frameDurations.length - 1));
		long acc = 0;
		for (int i = 0; i < clamped; i++) {
			acc += Math.max(1, frameDurations[i]);
		}
		return acc;
	}

	private void advanceFrame() {
		// In continuous mode there is no outer frame list, so the guard
		// `totalFrames <= 0` would block us. Force-run the master clock
		// instead and emit tick events.
		if (totalFrames <= 0 && !continuousMode) {
			return;
		}
		double speed = ((Number) speedSpinner.getValue()).doubleValue();
		// Each timer tick is 1 NDS tick (1/60 sec). The speed spinner
		// scales how many ticks we consume per real-time tick so 2.0
		// runs double-speed and 0.5 runs half-speed without touching
		// the timer period.
		int ticksToAdd = Math.max(1, (int) Math.round(speed));
		// Always advance the master clock so multi-cell entries with
		// independent NANRs can advance in real time regardless of what
		// the outer frame index is doing.
		elapsedTicks += ticksToAdd;

		if (continuousMode) {
			// No outer frame → just emit a tick event so the canvas
			// re-renders the multi-cell with the updated master clock.
			updateAnimUI();
			return;
		}

		if (frameDurations == null) {
			// Legacy path: advance one frame per tick, unchanged from the
			// original behavior. Keeps old callers that only know the
			// frame count working.
			currentFrame = (currentFrame + ticksToAdd) % totalFrames;
			updateAnimUI();
			return;
		}
		// Duration-aware path: accumulate ticks on the current frame and
		// roll over to the next frame when we meet its duration. Loop so
		// very-large speeds or zero-duration frames still make progress.
		// Always call updateAnimUI() so multi-cell entries' sub-NANRs get
		// a tick event even if the outer NMAR frame hasn't advanced yet.
		elapsedInFrame += ticksToAdd;
		int safety = totalFrames * 2 + 4;
		while (safety-- > 0) {
			int curFrame = Math.floorMod(currentFrame, totalFrames);
			int dur = Math.max(1, frameDurations[curFrame]);
			if (elapsedInFrame < dur) {
				break;
			}
			elapsedInFrame -= dur;
			currentFrame = (curFrame + 1) % totalFrames;
		}
		updateAnimUI();
	}

	private void updateAnimUI() {
		frameSlider.setValue(currentFrame);
		frameLabel.setText("Frame: " + currentFrame + " / " + totalFrames);
		fireFrameChange();
	}

	/**
	 * Adds a listener notified when the current frame changes.
	 */
	public void addFrameChangeListener(Runnable callback) {
		frameChangeListeners.add(callback);
	}

	private void fireFrameChange() {
		for (Runnable r : frameChangeListeners) {
			r.run();
		}
	}

	/**
	 * Resets the panel to its default state.
	 */
	public void reset() {
		isPlaying = false;
		animTimer.stop();
		currentFrame = 0;
		totalFrames = 0;
		frameDurations = null;
		elapsedInFrame = 0;
		elapsedTicks = 0;
		continuousMode = false;
		frameSlider.setMaximum(0);
		frameSlider.setValue(0);
		frameLabel.setText("Frame: 0 / 0");
	}
}
