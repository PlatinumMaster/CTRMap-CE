package ctrmap.creativestudio.ngcs2d.editors;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.layers.LayersPanel;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Property editor panel for {@code Sprite2DMultiCell} resources.
 *
 * <p>Previously this was a raw entry list. The multi-cell's entries are
 * now surfaced via the Photoshop-style {@link LayersPanel}, wired in
 * after construction by {@code NGCS2D} (the panel is shared across all
 * editor instances so its canvas-sync listeners only need one set of
 * wires). The header stays simple: just the editable name field and an
 * entry count readout.</p>
 */
public class MultiCellEditor extends JPanel implements IEditor {

	private Sprite2DMultiCell multiCell;

	private final JTextField nameField;
	private final JLabel entryCountLabel;
	private final JPanel bodyHost;
	private LayersPanel layersPanel;

	public MultiCellEditor() {
		setLayout(new BorderLayout(0, 8));

		// Header: name + entry count
		JPanel headerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.anchor = GridBagConstraints.WEST;

		gbc.gridx = 0; gbc.gridy = 0;
		headerPanel.add(new JLabel("Name:"), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		nameField = new JTextField(20);
		headerPanel.add(nameField, gbc);

		gbc.gridx = 0; gbc.gridy = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		headerPanel.add(new JLabel("Entries:"), gbc);

		gbc.gridx = 1;
		entryCountLabel = new JLabel("0");
		headerPanel.add(entryCountLabel, gbc);

		add(headerPanel, BorderLayout.NORTH);

		// Body host: LayersPanel mounts here once NGCS2D injects it.
		// Until then the placeholder label warns callers that wiring
		// wasn't completed, which would only happen if someone builds
		// NGCS2DEditorController without going through NGCS2D.
		bodyHost = new JPanel(new BorderLayout());
		JLabel placeholder = new JLabel(
			"<html><div style='text-align:center;color:#888;padding:24px;'>"
			+ "Layers panel not wired in.</div></html>",
			JLabel.CENTER);
		bodyHost.add(placeholder, BorderLayout.CENTER);
		add(bodyHost, BorderLayout.CENTER);
	}

	/**
	 * Installs the shared Photoshop-style layers panel. Called once by
	 * {@code NGCS2D} during initial wiring; the same {@link LayersPanel}
	 * instance is used by every multi-cell selection so its undo/canvas
	 * plumbing survives editor swaps.
	 */
	public void setLayersPanel(LayersPanel layersPanel) {
		this.layersPanel = layersPanel;
		bodyHost.removeAll();
		bodyHost.add(layersPanel, BorderLayout.CENTER);
		bodyHost.revalidate();
		bodyHost.repaint();
	}

	@Override
	public void handleObject(Object o) {
		if (o instanceof Sprite2DMultiCell) {
			multiCell = (Sprite2DMultiCell) o;
		} else {
			multiCell = null;
		}
		if (multiCell != null) {
			nameField.setText(multiCell.name != null ? multiCell.name : "");
			entryCountLabel.setText(String.valueOf(multiCell.getEntryCount()));
			if (layersPanel != null) {
				// Re-root the shared LayersPanel into our bodyHost — it
				// might currently live in CellEditor.bodyHost, which would
				// leave us empty otherwise.
				bodyHost.removeAll();
				bodyHost.add(layersPanel, BorderLayout.CENTER);
				bodyHost.revalidate();
				bodyHost.repaint();
				layersPanel.setMultiCell(multiCell);
			}
		} else {
			nameField.setText("");
			entryCountLabel.setText("0");
			if (layersPanel != null) layersPanel.clear();
		}
	}

	@Override
	public void save() {
		if (multiCell != null) {
			multiCell.name = nameField.getText();
		}
	}
}
