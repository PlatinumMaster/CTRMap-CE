package ctrmap.creativestudio.ngcs2d.canvas.tools;

import java.awt.event.MouseEvent;

/**
 * Wraps a MouseEvent with pixel-space coordinates for the sprite canvas.
 */
public class Sprite2DCanvasEvent {

	/** The originating mouse event. */
	public final MouseEvent sourceEvent;
	/** X coordinate in pixel space of the rendered content. */
	public final int pixelX;
	/** Y coordinate in pixel space of the rendered content. */
	public final int pixelY;
	/** Tile index at the cursor position, or -1 if unknown. */
	public final int tileIndex;
	/** Mouse button that triggered this event. */
	public final int button;

	public Sprite2DCanvasEvent(MouseEvent sourceEvent, int pixelX, int pixelY, int tileIndex, int button) {
		this.sourceEvent = sourceEvent;
		this.pixelX = pixelX;
		this.pixelY = pixelY;
		this.tileIndex = tileIndex;
		this.button = button;
	}
}
