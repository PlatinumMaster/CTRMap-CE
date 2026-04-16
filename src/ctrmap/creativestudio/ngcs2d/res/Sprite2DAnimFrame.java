package ctrmap.creativestudio.ngcs2d.res;

/**
 * A single frame in a cell animation. Stores a reference to a cell
 * along with duration and affine transform parameters.
 */
public class Sprite2DAnimFrame {

	/**
	 * Index into the cell list for this frame.
	 */
	public int cellIndex;

	/**
	 * Frame duration in animation ticks.
	 */
	public int duration;

	/**
	 * Pixel offset X applied to the cell for this frame.
	 */
	public float translateX;

	/**
	 * Pixel offset Y applied to the cell for this frame.
	 */
	public float translateY;

	/**
	 * Scale factor along the X axis.
	 */
	public float scaleX;

	/**
	 * Scale factor along the Y axis.
	 */
	public float scaleY;

	/**
	 * Rotation angle in degrees.
	 */
	public float rotation;

	/**
	 * Constructs a default animation frame with identity transforms
	 * and a duration of 1 tick.
	 */
	public Sprite2DAnimFrame() {
		scaleX = 1.0f;
		scaleY = 1.0f;
		duration = 1;
	}

	/**
	 * Constructs an animation frame with the given cell index and duration.
	 * Transform fields default to identity (no translation, unit scale, no rotation).
	 *
	 * @param cellIndex Index into the cell list.
	 * @param duration  Frame duration in animation ticks.
	 */
	public Sprite2DAnimFrame(int cellIndex, int duration) {
		this.cellIndex = cellIndex;
		this.duration = duration;
		scaleX = 1.0f;
		scaleY = 1.0f;
	}
}
