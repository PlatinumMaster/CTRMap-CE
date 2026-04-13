package ctrmap.creativestudio.ngcs2d.res;

import java.util.ArrayList;
import java.util.List;
import xstandard.INamed;

/**
 * A game-agnostic multi-cell animation. Contains a sequence of frames,
 * each referencing a multi-cell by index with a duration.
 */
public class Sprite2DMultiCellAnimation implements INamed {

	/**
	 * Display name of this multi-cell animation.
	 */
	public String name;

	/**
	 * The list of animation frames in playback order.
	 */
	public List<MultiCellAnimFrame> frames;

	/**
	 * Playback mode for this animation.
	 * 0 = invalid, 1 = forward, 2 = forward loop,
	 * 3 = reverse, 4 = reverse loop.
	 */
	public int playMode;

	/**
	 * Constructs a default multi-cell animation named "MultiCellAnimation"
	 * with forward playback and an empty frame list.
	 */
	public Sprite2DMultiCellAnimation() {
		name = "MultiCellAnimation";
		frames = new ArrayList<>();
		playMode = 1;
	}

	/**
	 * Constructs a multi-cell animation with the given name, forward playback,
	 * and an empty frame list.
	 *
	 * @param name Display name of the animation.
	 */
	public Sprite2DMultiCellAnimation(String name) {
		this.name = name;
		frames = new ArrayList<>();
		playMode = 1;
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
	 * Returns the number of frames in this animation.
	 *
	 * @return The frame count.
	 */
	public int getFrameCount() {
		return frames.size();
	}

	/**
	 * Returns the total duration of this animation by summing
	 * the duration of all frames.
	 *
	 * @return The total duration in animation ticks.
	 */
	public int getTotalDuration() {
		int total = 0;
		for (MultiCellAnimFrame frame : frames) {
			total += frame.duration;
		}
		return total;
	}

	/**
	 * A single frame in a multi-cell animation, referencing a multi-cell
	 * by index with a duration.
	 */
	public static class MultiCellAnimFrame {

		/**
		 * Index into the multi-cell list for this frame.
		 */
		public int multiCellIndex;

		/**
		 * Frame duration in animation ticks.
		 */
		public int duration;

		/**
		 * Constructs a default multi-cell animation frame.
		 */
		public MultiCellAnimFrame() {
		}

		/**
		 * Constructs a multi-cell animation frame with the given
		 * multi-cell index and duration.
		 *
		 * @param multiCellIndex Index into the multi-cell list.
		 * @param duration       Frame duration in animation ticks.
		 */
		public MultiCellAnimFrame(int multiCellIndex, int duration) {
			this.multiCellIndex = multiCellIndex;
			this.duration = duration;
		}
	}
}
