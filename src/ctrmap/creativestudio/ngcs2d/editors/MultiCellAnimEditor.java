package ctrmap.creativestudio.ngcs2d.editors;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation.MultiCellAnimFrame;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Property editor panel for {@code Sprite2DMultiCellAnimation} resources.
 *
 * <p>Displays the animation name, play mode selector, frame count with total
 * duration, and a list of frame summaries showing the multi-cell index and
 * duration. Provides buttons to add and remove animation frames.</p>
 */
public class MultiCellAnimEditor extends JPanel implements IEditor {

	private static final String[] PLAY_MODES = {
		"Invalid", "Forward", "Forward Loop", "Reverse", "Reverse Loop"
	};

	private Sprite2DMultiCellAnimation animation;

	private final JTextField nameField;
	private final JComboBox<String> playModeCombo;
	private final JLabel frameInfoLabel;
	private final DefaultListModel<String> frameListModel;
	private final JList<String> frameList;

	/**
	 * Constructs the multi-cell animation editor panel with header controls,
	 * frame list, and add/remove buttons.
	 */
	public MultiCellAnimEditor() {
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
		headerPanel.add(new JLabel("Play Mode:"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		playModeCombo = new JComboBox<>(PLAY_MODES);
		headerPanel.add(playModeCombo, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		headerPanel.add(new JLabel("Info:"), gbc);

		gbc.gridx = 1;
		frameInfoLabel = new JLabel();
		headerPanel.add(frameInfoLabel, gbc);

		add(headerPanel, BorderLayout.NORTH);

		frameListModel = new DefaultListModel<>();
		frameList = new JList<>(frameListModel);
		frameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane listScroll = new JScrollPane(frameList);
		add(listScroll, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JButton addButton = new JButton("Add Frame");
		JButton removeButton = new JButton("Remove Frame");

		addButton.addActionListener(e -> {
			if (animation != null) {
				animation.frames.add(new MultiCellAnimFrame());
				refreshFrameList();
			}
		});

		removeButton.addActionListener(e -> {
			if (animation != null) {
				int idx = frameList.getSelectedIndex();
				if (idx >= 0 && idx < animation.frames.size()) {
					animation.frames.remove(idx);
					refreshFrameList();
				}
			}
		});

		buttonPanel.add(addButton);
		buttonPanel.add(removeButton);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	/**
	 * Loads a {@code Sprite2DMultiCellAnimation} into the editor, populating
	 * the name field, play mode selector, frame info, and frame list.
	 *
	 * @param o The {@code Sprite2DMultiCellAnimation} to edit, or {@code null} to clear the editor.
	 */
	@Override
	public void handleObject(Object o) {
		if (o instanceof Sprite2DMultiCellAnimation) {
			animation = (Sprite2DMultiCellAnimation) o;
		} else {
			animation = null;
		}

		if (animation != null) {
			nameField.setText(animation.name);
			int modeIdx = animation.playMode;
			if (modeIdx >= 0 && modeIdx < PLAY_MODES.length) {
				playModeCombo.setSelectedIndex(modeIdx);
			} else {
				playModeCombo.setSelectedIndex(0);
			}
			refreshFrameList();
		} else {
			nameField.setText("");
			playModeCombo.setSelectedIndex(0);
			frameInfoLabel.setText("");
			frameListModel.clear();
		}
	}

	/**
	 * Writes the current editor state back to the loaded animation resource.
	 * Saves the name and play mode.
	 */
	@Override
	public void save() {
		if (animation != null) {
			animation.name = nameField.getText();
			animation.playMode = playModeCombo.getSelectedIndex();
		}
	}

	/**
	 * Rebuilds the frame list model and updates the frame info label
	 * from the current animation's frame data.
	 */
	private void refreshFrameList() {
		frameListModel.clear();
		if (animation != null) {
			frameInfoLabel.setText(
				animation.getFrameCount() + " frames, total duration: " + animation.getTotalDuration()
			);
			for (int i = 0; i < animation.frames.size(); i++) {
				MultiCellAnimFrame frame = animation.frames.get(i);
				frameListModel.addElement(
					"Frame " + i + ": multiCell=" + frame.multiCellIndex + ", dur=" + frame.duration
				);
			}
		}
	}
}
