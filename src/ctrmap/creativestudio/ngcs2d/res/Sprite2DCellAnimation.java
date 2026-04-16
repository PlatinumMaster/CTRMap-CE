package ctrmap.creativestudio.ngcs2d.res;

import java.util.ArrayList;
import java.util.List;
import xstandard.INamed;

/**
 * A game-agnostic cell animation. Contains a sequence of animation frames,
 * each referencing a cell by index, with duration and transform parameters.
 */
public class Sprite2DCellAnimation implements INamed {

	/**
	 * Display name of this cell animation.
	 */
	public String name;

	/**
	 * The list of animation frames in playback order.
	 */
	public List<Sprite2DAnimFrame> frames;

	/**
	 * Playback mode for this animation.
	 * 0 = invalid, 1 = forward, 2 = forward loop,
	 * 3 = reverse, 4 = reverse loop.
	 */
	public int playMode;

	/**
	 * Constructs a default cell animation named "CellAnimation"
	 * with forward playback and an empty frame list.
	 */
	public Sprite2DCellAnimation() {
		name = "CellAnimation";
		frames = new ArrayList<>();
		playMode = 1;
	}

	/**
	 * Constructs a cell animation with the given name, forward playback,
	 * and an empty frame list.
	 *
	 * @param name Display name of the animation.
	 */
	public Sprite2DCellAnimation(String name) {
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
		for (Sprite2DAnimFrame frame : frames) {
			total += frame.duration;
		}
		return total;
	}
}
