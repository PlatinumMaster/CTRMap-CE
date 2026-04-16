package ctrmap.creativestudio.ngcs2d.editors;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell.MultiCellEntry;

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
 * Property editor panel for {@code Sprite2DMultiCell} resources.
 *
 * <p>Displays the multi-cell name in an editable text field, the entry count,
 * and a list of entry summaries showing the animation index and position
 * offset. Provides buttons to add and remove entries.</p>
 */
public class MultiCellEditor extends JPanel implements IEditor {

	private Sprite2DMultiCell multiCell;

	private final JTextField nameField;
	private final JLabel entryCountLabel;
	private final DefaultListModel<String> entryListModel;
	private final JList<String> entryList;

	/**
	 * Constructs the multi-cell editor panel with a name field, entry count
	 * label, entry list, and add/remove buttons.
	 */
	public MultiCellEditor() {
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
		headerPanel.add(new JLabel("Entries:"), gbc);

		gbc.gridx = 1;
		entryCountLabel = new JLabel("0");
		headerPanel.add(entryCountLabel, gbc);

		add(headerPanel, BorderLayout.NORTH);

		entryListModel = new DefaultListModel<>();
		entryList = new JList<>(entryListModel);
		entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane listScroll = new JScrollPane(entryList);
		add(listScroll, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JButton addButton = new JButton("Add Entry");
		JButton removeButton = new JButton("Remove Entry");

		addButton.addActionListener(e -> {
			if (multiCell != null) {
				multiCell.entries.add(new MultiCellEntry());
				refreshEntryList();
			}
		});

		removeButton.addActionListener(e -> {
			if (multiCell != null) {
				int idx = entryList.getSelectedIndex();
				if (idx >= 0 && idx < multiCell.entries.size()) {
					multiCell.entries.remove(idx);
					refreshEntryList();
				}
			}
		});

		buttonPanel.add(addButton);
		buttonPanel.add(removeButton);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	/**
	 * Loads a {@code Sprite2DMultiCell} into the editor, populating the name
	 * field, entry count, and entry list.
	 *
	 * @param o The {@code Sprite2DMultiCell} to edit, or {@code null} to clear the editor.
	 */
	@Override
	public void handleObject(Object o) {
		if (o instanceof Sprite2DMultiCell) {
			multiCell = (Sprite2DMultiCell) o;
		} else {
			multiCell = null;
		}

		if (multiCell != null) {
			nameField.setText(multiCell.name);
			refreshEntryList();
		} else {
			nameField.setText("");
			entryCountLabel.setText("0");
			entryListModel.clear();
		}
	}

	/**
	 * Writes the current editor state back to the loaded multi-cell resource.
	 * Saves the name field value to the multi-cell's name.
	 */
	@Override
	public void save() {
		if (multiCell != null) {
			multiCell.name = nameField.getText();
		}
	}

	/**
	 * Rebuilds the entry list model from the current multi-cell's entries.
	 */
	private void refreshEntryList() {
		entryListModel.clear();
		if (multiCell != null) {
			entryCountLabel.setText(String.valueOf(multiCell.getEntryCount()));
			for (int i = 0; i < multiCell.entries.size(); i++) {
				MultiCellEntry entry = multiCell.entries.get(i);
				entryListModel.addElement(
					"Entry " + i + ": anim=" + entry.animIndex + " @ (" + entry.x + ", " + entry.y + ")"
				);
			}
		}
	}
}
