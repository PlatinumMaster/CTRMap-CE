package ctrmap.creativestudio.ngcs2d.editors;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * Property editor panel for {@code Sprite2DTileSheet} resources.
 *
 * <p>Displays the tile sheet name in an editable text field, the pixel
 * format (4bpp or 8bpp), the tile count, and a preview panel that
 * renders each 8x8 tile as a grayscale block at 2x zoom.</p>
 */
public class TileSheetEditor extends JPanel implements IEditor {

	private static final int SCALE = 2;
	private static final int TILE_SIZE = 8;
	private static final int TILES_PER_ROW = 16;

	private Sprite2DTileSheet tileSheet;

	private final JTextField nameField;
	private final JLabel formatLabel;
	private final JLabel tileCountLabel;
	private final TilePreviewPanel previewPanel;

	/**
	 * Constructs the tile sheet editor panel with header fields and a
	 * scrollable tile preview area.
	 */
	public TileSheetEditor() {
		setLayout(new BorderLayout(0, 8));

		JPanel headerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;

		gbc.gridx = 0;
		gbc.gridy = 0;
		headerPanel.add(new JLabel("Name:"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		nameField = new JTextField(20);
		headerPanel.add(nameField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		headerPanel.add(new JLabel("Format:"), gbc);

		gbc.gridx = 1;
		formatLabel = new JLabel();
		headerPanel.add(formatLabel, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		headerPanel.add(new JLabel("Tiles:"), gbc);

		gbc.gridx = 1;
		tileCountLabel = new JLabel();
		headerPanel.add(tileCountLabel, gbc);

		add(headerPanel, BorderLayout.NORTH);

		previewPanel = new TilePreviewPanel();
		JScrollPane scrollPane = new JScrollPane(previewPanel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, BorderLayout.CENTER);
	}

	/**
	 * Loads a {@code Sprite2DTileSheet} into the editor, populating the
	 * name field, format label, tile count, and tile preview.
	 *
	 * @param o The {@code Sprite2DTileSheet} to edit, or {@code null} to clear the editor.
	 */
	@Override
	public void handleObject(Object o) {
		if (o instanceof Sprite2DTileSheet) {
			tileSheet = (Sprite2DTileSheet) o;
		} else {
			tileSheet = null;
		}

		if (tileSheet != null) {
			nameField.setText(tileSheet.name);
			formatLabel.setText(tileSheet.format == 3 ? "4bpp (IDX4)" : "8bpp (IDX8)");
			tileCountLabel.setText(String.valueOf(tileSheet.getTileCount()));
		} else {
			nameField.setText("");
			formatLabel.setText("");
			tileCountLabel.setText("");
		}

		previewPanel.repaint();
		previewPanel.revalidate();
	}

	/**
	 * Writes the current editor state back to the loaded tile sheet resource.
	 * Saves the name field value to the tile sheet's name.
	 */
	@Override
	public void save() {
		if (tileSheet != null) {
			tileSheet.name = nameField.getText();
		}
	}

	/**
	 * Inner panel that renders the tile sheet pixel data as a grid of
	 * grayscale 8x8 tiles at a fixed scale factor.
	 */
	private class TilePreviewPanel extends JPanel {

		/**
		 * Constructs the tile preview panel with a dark background.
		 */
		TilePreviewPanel() {
			setBackground(Color.BLACK);
		}

		@Override
		public Dimension getPreferredSize() {
			if (tileSheet == null || tileSheet.getTileCount() == 0) {
				return new Dimension(TILES_PER_ROW * TILE_SIZE * SCALE, TILE_SIZE * SCALE);
			}
			int tileCount = tileSheet.getTileCount();
			int rows = Math.max(1, (tileCount + TILES_PER_ROW - 1) / TILES_PER_ROW);
			return new Dimension(
				TILES_PER_ROW * TILE_SIZE * SCALE,
				rows * TILE_SIZE * SCALE
			);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			if (tileSheet == null) {
				return;
			}

			int tileCount = tileSheet.getTileCount();
			int maxPalIdx = tileSheet.format == 3 ? 15 : 255;

			for (int t = 0; t < tileCount; t++) {
				int col = t % TILES_PER_ROW;
				int row = t / TILES_PER_ROW;
				int baseX = col * TILE_SIZE * SCALE;
				int baseY = row * TILE_SIZE * SCALE;

				for (int py = 0; py < TILE_SIZE; py++) {
					for (int px = 0; px < TILE_SIZE; px++) {
						int palIdx = tileSheet.getPixel(t, px, py);
						int gray = maxPalIdx > 0 ? (palIdx * 255) / maxPalIdx : 0;
						g.setColor(new Color(gray, gray, gray));
						g.fillRect(baseX + px * SCALE, baseY + py * SCALE, SCALE, SCALE);
					}
				}
			}
		}
	}
}
