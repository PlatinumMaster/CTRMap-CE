package ctrmap.creativestudio.ngcs2d.editors;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * Property editor panel for {@code Sprite2DOAM} resources.
 *
 * <p>Provides spinner controls for X/Y position, tile index, palette index,
 * and priority; combo boxes for width and height selection; and check boxes
 * for horizontal and vertical flip flags. Uses {@link GridBagLayout} with
 * labels on the left and controls on the right.</p>
 */
public class OAMEditor extends JPanel implements IEditor {

	private static final Integer[] OBJ_SIZES = {8, 16, 32, 64};

	private Sprite2DOAM oam;

	private final JSpinner xSpinner;
	private final JSpinner ySpinner;
	private final JComboBox<Integer> widthCombo;
	private final JComboBox<Integer> heightCombo;
	private final JSpinner tileIndexSpinner;
	private final JSpinner paletteIndexSpinner;
	private final JSpinner prioritySpinner;
	private final JCheckBox flipHCheck;
	private final JCheckBox flipVCheck;

	/**
	 * Constructs the OAM editor panel with labeled controls for all
	 * OAM attribute fields.
	 */
	public OAMEditor() {
		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;

		int row = 0;

		// X position
		gbc.gridx = 0;
		gbc.gridy = row;
		add(new JLabel("X Position:"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		xSpinner = new JSpinner(new SpinnerNumberModel(0, -256, 255, 1));
		add(xSpinner, gbc);
		row++;

		// Y position
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		add(new JLabel("Y Position:"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		ySpinner = new JSpinner(new SpinnerNumberModel(0, -128, 127, 1));
		add(ySpinner, gbc);
		row++;

		// Width
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		add(new JLabel("Width:"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		widthCombo = new JComboBox<>(OBJ_SIZES);
		add(widthCombo, gbc);
		row++;

		// Height
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		add(new JLabel("Height:"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		heightCombo = new JComboBox<>(OBJ_SIZES);
		add(heightCombo, gbc);
		row++;

		// Tile index
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		add(new JLabel("Tile Index:"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		tileIndexSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1023, 1));
		add(tileIndexSpinner, gbc);
		row++;

		// Palette index
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		add(new JLabel("Palette Index:"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		paletteIndexSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 15, 1));
		add(paletteIndexSpinner, gbc);
		row++;

		// Priority
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		add(new JLabel("Priority:"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		prioritySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));
		add(prioritySpinner, gbc);
		row++;

		// Flip H
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		add(new JLabel("Flip H:"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		flipHCheck = new JCheckBox();
		add(flipHCheck, gbc);
		row++;

		// Flip V
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		add(new JLabel("Flip V:"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		flipVCheck = new JCheckBox();
		add(flipVCheck, gbc);

		// Bottom spacer to push everything to the top
		gbc.gridx = 0;
		gbc.gridy = row + 1;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.VERTICAL;
		add(new JLabel(), gbc);
	}

	/**
	 * Loads a {@code Sprite2DOAM} into the editor, setting all spinner,
	 * combo box, and check box values from the OAM fields.
	 *
	 * @param o The {@code Sprite2DOAM} to edit, or {@code null} to clear the editor.
	 */
	@Override
	public void handleObject(Object o) {
		if (o instanceof Sprite2DOAM) {
			oam = (Sprite2DOAM) o;
		} else {
			oam = null;
		}

		if (oam != null) {
			xSpinner.setValue(oam.x);
			ySpinner.setValue(oam.y);
			widthCombo.setSelectedItem(oam.width);
			heightCombo.setSelectedItem(oam.height);
			tileIndexSpinner.setValue(oam.tileIndex);
			paletteIndexSpinner.setValue(oam.paletteIndex);
			prioritySpinner.setValue(oam.priority);
			flipHCheck.setSelected(oam.flipH);
			flipVCheck.setSelected(oam.flipV);
		} else {
			xSpinner.setValue(0);
			ySpinner.setValue(0);
			widthCombo.setSelectedIndex(0);
			heightCombo.setSelectedIndex(0);
			tileIndexSpinner.setValue(0);
			paletteIndexSpinner.setValue(0);
			prioritySpinner.setValue(0);
			flipHCheck.setSelected(false);
			flipVCheck.setSelected(false);
		}
	}

	/**
	 * Writes the current editor state back to the loaded OAM resource.
	 * All spinner, combo box, and check box values are written to the
	 * corresponding OAM fields.
	 */
	@Override
	public void save() {
		if (oam != null) {
			oam.x = (Integer) xSpinner.getValue();
			oam.y = (Integer) ySpinner.getValue();
			oam.width = (Integer) widthCombo.getSelectedItem();
			oam.height = (Integer) heightCombo.getSelectedItem();
			oam.tileIndex = (Integer) tileIndexSpinner.getValue();
			oam.paletteIndex = (Integer) paletteIndexSpinner.getValue();
			oam.priority = (Integer) prioritySpinner.getValue();
			oam.flipH = flipHCheck.isSelected();
			oam.flipV = flipVCheck.isSelected();
		}
	}
}
