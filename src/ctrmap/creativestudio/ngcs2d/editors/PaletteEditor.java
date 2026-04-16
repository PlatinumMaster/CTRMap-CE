package ctrmap.creativestudio.ngcs2d.editors;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

/**
 * Property editor panel for {@code Sprite2DPalette} resources.
 *
 * <p>Displays the palette name in an editable text field, the palette format
 * (16 or 256 colors), and a grid of colored squares representing each color
 * entry. Clicking a color square opens a {@link JColorChooser} dialog to
 * edit that color in-place.</p>
 */
public class PaletteEditor extends JPanel implements IEditor {

	private Sprite2DPalette palette;

	private final JTextField nameField;
	private final JLabel formatLabel;
	private final JPanel colorGridPanel;

	/**
	 * Constructs the palette editor panel with a name field, format label,
	 * and a scrollable color grid.
	 */
	public PaletteEditor() {
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

		add(headerPanel, BorderLayout.NORTH);

		colorGridPanel = new JPanel();
		colorGridPanel.setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(), "Colors", TitledBorder.LEFT, TitledBorder.TOP
		));

		JScrollPane scrollPane = new JScrollPane(colorGridPanel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane, BorderLayout.CENTER);
	}

	/**
	 * Loads a {@code Sprite2DPalette} into the editor, populating the name
	 * field, format label, and color grid.
	 *
	 * @param o The {@code Sprite2DPalette} to edit, or {@code null} to clear the editor.
	 */
	@Override
	public void handleObject(Object o) {
		if (o instanceof Sprite2DPalette) {
			palette = (Sprite2DPalette) o;
		} else {
			palette = null;
		}

		if (palette != null) {
			nameField.setText(palette.name);
			formatLabel.setText(palette.format + " colors (" + (palette.format <= 16 ? "4bpp" : "8bpp") + ")");
			rebuildColorGrid();
		} else {
			nameField.setText("");
			formatLabel.setText("");
			colorGridPanel.removeAll();
			colorGridPanel.revalidate();
			colorGridPanel.repaint();
		}
	}

	/**
	 * Writes the current editor state back to the loaded palette resource.
	 * Saves the name field value to the palette's name.
	 */
	@Override
	public void save() {
		if (palette != null) {
			palette.name = nameField.getText();
		}
	}

	/**
	 * Rebuilds the color grid panel with one clickable cell per palette color.
	 */
	private void rebuildColorGrid() {
		colorGridPanel.removeAll();

		if (palette == null || palette.colors.length == 0) {
			colorGridPanel.revalidate();
			colorGridPanel.repaint();
			return;
		}

		int cols = 16;
		int rows = Math.max(1, (palette.colors.length + cols - 1) / cols);
		colorGridPanel.setLayout(new GridLayout(rows, cols, 1, 1));

		for (int i = 0; i < palette.colors.length; i++) {
			final int colorIndex = i;
			int argb = palette.colors[i];

			JPanel cell = new JPanel();
			cell.setBackground(new Color(argb, true));
			cell.setPreferredSize(new Dimension(16, 16));
			cell.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
			cell.setToolTipText("Index " + i + ": #" + String.format("%08X", argb));
			cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			cell.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (palette == null) {
						return;
					}
					Color current = new Color(palette.colors[colorIndex], true);
					Color chosen = JColorChooser.showDialog(
						PaletteEditor.this, "Edit Color " + colorIndex, current
					);
					if (chosen != null) {
						int newArgb = chosen.getRGB();
						palette.setColor(colorIndex, newArgb);
						cell.setBackground(chosen);
						cell.setToolTipText("Index " + colorIndex + ": #" + String.format("%08X", newArgb));
					}
				}
			});

			colorGridPanel.add(cell);
		}

		colorGridPanel.revalidate();
		colorGridPanel.repaint();
	}
}
