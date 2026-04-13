package ctrmap.creativestudio.ngcs2d.editors;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Property editor panel for {@code Sprite2DCell} resources.
 *
 * <p>Displays the cell name in an editable text field, the OAM count, and a
 * list of OAM summaries. Provides buttons to add and remove OAM entries.</p>
 */
public class CellEditor extends JPanel implements IEditor {

	private Sprite2DCell cell;

	private final JTextField nameField;
	private final JLabel oamCountLabel;
	private final DefaultListModel<String> oamListModel;
	private final JList<String> oamList;

	/**
	 * Constructs the cell editor panel with a name field, OAM count label,
	 * OAM list, and add/remove buttons.
	 */
	public CellEditor() {
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
		headerPanel.add(new JLabel("OAM Count:"), gbc);

		gbc.gridx = 1;
		oamCountLabel = new JLabel("0");
		headerPanel.add(oamCountLabel, gbc);

		add(headerPanel, BorderLayout.NORTH);

		oamListModel = new DefaultListModel<>();
		oamList = new JList<>(oamListModel);
		oamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane listScroll = new JScrollPane(oamList);
		add(listScroll, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JButton addButton = new JButton("Add OAM");
		JButton removeButton = new JButton("Remove OAM");

		addButton.addActionListener(e -> {
			if (cell != null) {
				cell.addOAM(new Sprite2DOAM());
				refreshOAMList();
			}
		});

		removeButton.addActionListener(e -> {
			if (cell != null) {
				int idx = oamList.getSelectedIndex();
				if (idx >= 0) {
					cell.removeOAM(idx);
					refreshOAMList();
				}
			}
		});

		buttonPanel.add(addButton);
		buttonPanel.add(removeButton);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	/**
	 * Loads a {@code Sprite2DCell} into the editor, populating the name field,
	 * OAM count, and OAM list.
	 *
	 * @param o The {@code Sprite2DCell} to edit, or {@code null} to clear the editor.
	 */
	@Override
	public void handleObject(Object o) {
		if (o instanceof Sprite2DCell) {
			cell = (Sprite2DCell) o;
		} else {
			cell = null;
		}

		if (cell != null) {
			nameField.setText(cell.name);
			refreshOAMList();
		} else {
			nameField.setText("");
			oamCountLabel.setText("0");
			oamListModel.clear();
		}
	}

	/**
	 * Writes the current editor state back to the loaded cell resource.
	 * Saves the name field value to the cell's name.
	 */
	@Override
	public void save() {
		if (cell != null) {
			cell.name = nameField.getText();
		}
	}

	/**
	 * Rebuilds the OAM list model from the current cell's OAM entries.
	 */
	private void refreshOAMList() {
		oamListModel.clear();
		if (cell != null) {
			oamCountLabel.setText(String.valueOf(cell.getOAMCount()));
			for (int i = 0; i < cell.oams.size(); i++) {
				Sprite2DOAM oam = cell.oams.get(i);
				oamListModel.addElement(
					"OAM " + i + ": " + oam.width + "x" + oam.height + " @ (" + oam.x + "," + oam.y + ")"
				);
			}
		}
	}
}
