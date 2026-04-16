package ctrmap.creativestudio.ngcs2d.canvas;

import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

/**
 * Status bar showing cursor position, zoom level, tile info, and color index.
 */
public class SpriteStatusBar extends JPanel {

	private final JLabel cursorLabel;
	private final JLabel zoomLabel;
	private final JLabel tileInfoLabel;
	private final JLabel colorLabel;

	public SpriteStatusBar() {
		setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));
		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

		cursorLabel = new JLabel("X: 0, Y: 0");
		zoomLabel = new JLabel("200%");
		tileInfoLabel = new JLabel("Tile: -");
		colorLabel = new JLabel("Color: 1");

		add(cursorLabel);
		add(new JSeparator(SwingConstants.VERTICAL));
		add(zoomLabel);
		add(new JSeparator(SwingConstants.VERTICAL));
		add(tileInfoLabel);
		add(new JSeparator(SwingConstants.VERTICAL));
		add(colorLabel);
	}

	/**
	 * Updates the cursor position display.
	 */
	public void updateCursor(int x, int y) {
		cursorLabel.setText("X: " + x + ", Y: " + y);
	}

	/**
	 * Updates the zoom percentage display.
	 */
	public void updateZoom(double zoomPercent) {
		zoomLabel.setText((int) (zoomPercent * 100) + "%");
	}

	/**
	 * Updates the tile index display.
	 */
	public void updateTileInfo(int tileIdx) {
		tileInfoLabel.setText("Tile: " + tileIdx);
	}

	/**
	 * Updates the foreground color index display.
	 */
	public void updateColor(int fgIndex) {
		colorLabel.setText("Color: " + fgIndex);
	}
}
