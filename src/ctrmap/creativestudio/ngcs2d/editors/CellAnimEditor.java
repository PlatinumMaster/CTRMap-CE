package ctrmap.creativestudio.ngcs2d.editors;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.canvas.CS2DAnimControlPanel;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCellAnimation;
import ctrmap.creativestudio.ngcs2d.timeline.TimelinePanel;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Property editor for {@code Sprite2DCellAnimation} resources.
 *
 * <p>The body embeds the shared {@link TimelinePanel} plus the shared
 * {@link CS2DAnimControlPanel} (play / pause / stop / step / speed).
 * Everything the user needs for animation editing lives in this one
 * pane — no separate "main window" timeline.</p>
 *
 * <p>The timeline and anim-control panels are SHARED singletons owned
 * by {@code NGCS2D}; editors mount them via {@link #attachComponents(TimelinePanel,
 * CS2DAnimControlPanel)} after construction. Since only one animation
 * can be selected at a time, swapping editors physically reparents
 * those shared panels to whichever animation editor is currently
 * visible.</p>
 */
public class CellAnimEditor extends JPanel implements IEditor {

	private static final String[] PLAY_MODES = {
		"Invalid", "Forward", "Forward Loop", "Reverse", "Reverse Loop"
	};

	private Sprite2DCellAnimation animation;

	private final JTextField nameField;
	private final JComboBox<String> playModeCombo;
	private final JLabel frameInfoLabel;
	private final JPanel bodyHost;

	private TimelinePanel timelinePanel;
	private CS2DAnimControlPanel animControlPanel;

	public CellAnimEditor() {
		setLayout(new BorderLayout(0, 4));

		// --- Header: name, play mode, frame info ---
		JPanel header = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;

		gbc.gridx = 0; gbc.gridy = 0;
		header.add(new JLabel("Name:"), gbc);
		gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
		nameField = new JTextField(20);
		header.add(nameField, gbc);

		gbc.gridx = 0; gbc.gridy = 1;
		gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
		header.add(new JLabel("Play mode:"), gbc);
		gbc.gridx = 1;
		playModeCombo = new JComboBox<>(PLAY_MODES);
		header.add(playModeCombo, gbc);

		gbc.gridx = 0; gbc.gridy = 2;
		header.add(new JLabel("Frames:"), gbc);
		gbc.gridx = 1;
		frameInfoLabel = new JLabel("0 (0 ticks)");
		header.add(frameInfoLabel, gbc);

		add(header, BorderLayout.NORTH);

		// --- Body host: holds timeline + anim controls once attached ---
		bodyHost = new JPanel();
		bodyHost.setLayout(new BoxLayout(bodyHost, BoxLayout.Y_AXIS));
		JLabel placeholder = new JLabel(
			"<html><div style='text-align:center;color:#888;padding:24px;'>"
			+ "Timeline not wired in.</div></html>",
			JLabel.CENTER);
		bodyHost.add(placeholder);
		add(bodyHost, BorderLayout.CENTER);
	}

	/**
	 * Mounts the shared timeline and play-control panels into this
	 * editor's body. Called by {@code NGCS2D} during initial wiring.
	 * Safe to call multiple times — the shared components get
	 * reparented to this editor each time.
	 */
	public void attachComponents(TimelinePanel timelinePanel, CS2DAnimControlPanel animControlPanel) {
		this.timelinePanel = timelinePanel;
		this.animControlPanel = animControlPanel;
		reparentBody();
	}

	/** Re-adds the timeline + controls to the body host. Called
	 *  whenever this editor becomes active via {@link #handleObject}
	 *  so the shared components land in the right place after being
	 *  parented to some other editor. */
	private void reparentBody() {
		if (timelinePanel == null || animControlPanel == null) return;
		bodyHost.removeAll();
		bodyHost.add(timelinePanel);
		bodyHost.add(animControlPanel);
		bodyHost.revalidate();
		bodyHost.repaint();
	}

	@Override
	public void handleObject(Object o) {
		if (o instanceof Sprite2DCellAnimation) {
			animation = (Sprite2DCellAnimation) o;
		} else {
			animation = null;
		}
		if (animation != null) {
			nameField.setText(animation.name != null ? animation.name : "");
			int mode = Math.max(0, Math.min(animation.playMode, PLAY_MODES.length - 1));
			playModeCombo.setSelectedIndex(mode);
			int nFrames = animation.getFrameCount();
			int total = animation.getTotalDuration();
			frameInfoLabel.setText(nFrames + " (" + total + " ticks)");

			if (timelinePanel != null) timelinePanel.setAnimation(animation);
			if (animControlPanel != null) {
				int[] durs = new int[nFrames];
				for (int i = 0; i < nFrames; i++) {
					durs[i] = Math.max(1, animation.frames.get(i).duration);
				}
				animControlPanel.setFrameDurations(durs);
				animControlPanel.setCurrentFrame(0);
			}
			reparentBody();
		} else {
			nameField.setText("");
			playModeCombo.setSelectedIndex(0);
			frameInfoLabel.setText("0 (0 ticks)");
			if (timelinePanel != null) timelinePanel.clear();
		}
	}

	@Override
	public void save() {
		if (animation != null) {
			animation.name = nameField.getText();
			animation.playMode = playModeCombo.getSelectedIndex();
		}
	}
}
