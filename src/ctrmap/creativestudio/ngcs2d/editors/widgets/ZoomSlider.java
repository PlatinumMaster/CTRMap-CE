package ctrmap.creativestudio.ngcs2d.editors.widgets;

import java.util.function.IntConsumer;
import javax.swing.JSlider;

/**
 * Integer 1×–16× zoom slider for the inspector previews. Cosmetic only —
 * never produces an undo entry. Default value is 2× (matches the pre-
 * refactor TileSheetEditor's hard-coded SCALE).
 */
public class ZoomSlider extends JSlider {

	private IntConsumer zoomListener;

	public ZoomSlider() {
		super(1, 16, 2);
		setMajorTickSpacing(1);
		setPaintTicks(true);
		setSnapToTicks(true);
		addChangeListener(e -> {
			if (zoomListener != null) {
				zoomListener.accept(getValue());
			}
		});
	}

	/** Current zoom factor, always in [1, 16]. */
	public int getZoomFactor() {
		return Math.max(1, Math.min(16, getValue()));
	}

	/** Installs a callback fired on any value change. */
	public void addZoomListener(IntConsumer listener) {
		this.zoomListener = listener;
	}
}
