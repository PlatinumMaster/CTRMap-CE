package ctrmap.creativestudio.ngcs2d.editors;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.canvas.SpriteRenderer;
import ctrmap.creativestudio.ngcs2d.canvas.undo.AddOAMAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.CellPropertyAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.DuplicateOAMAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.RemoveOAMAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.SpriteUndoManager;
import ctrmap.creativestudio.ngcs2d.editors.widgets.PalettePicker;
import ctrmap.creativestudio.ngcs2d.editors.widgets.ZoomSlider;
import ctrmap.creativestudio.ngcs2d.layers.LayerItem;
import ctrmap.creativestudio.ngcs2d.layers.LayersPanel;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

/**
 * NitroCharacter-style property editor for {@link Sprite2DCell} (NCER).
 *
 * <p>Top: editable property form (name, VRAM transfer toggles, palette
 * picker, zoom). Center: vertical split — live colored preview of the
 * composited cell on top, the shared {@link LayersPanel} (rebound to the
 * cell's OAM list) on the bottom. Bottom: Add / Duplicate / Delete OAM
 * buttons that operate on the layers-panel selection.</p>
 */
public class CellEditor extends JPanel implements IEditor {

	private final SpriteUndoManager undoManager;

	private Sprite2DResource resource;
	private Runnable canvasRefresh;
	private LayersPanel layersPanel;
	private Sprite2DCell cell;

	private boolean suppressEvents = false;

	private final JTextField nameField;
	private final JLabel oamCountLabel;
	private final JLabel visibleOamCountLabel;
	private final JLabel boundingBoxLabel;
	private final JCheckBox hasVramTransferCheck;
	private final JSpinner vramTransferSrcAddrSpinner;
	private final JSpinner vramTransferSizeSpinner;
	private final PalettePicker palettePicker;
	private final ZoomSlider zoomSlider;
	private final PreviewPanel previewPanel;
	private final JPanel bodyHost;
	private final JLabel bodyPlaceholder;
	private final JButton addButton;
	private final JButton dupButton;
	private final JButton delButton;

	public CellEditor(SpriteUndoManager undoManager) {
		this.undoManager = undoManager;
		setLayout(new BorderLayout(0, 8));

		// ---------- Header ----------
		JPanel header = new JPanel(new GridBagLayout());
		header.setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(), "NCER Properties",
			TitledBorder.LEFT, TitledBorder.TOP));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3, 4, 3, 4);
		gbc.anchor = GridBagConstraints.WEST;
		int row = 0;

		nameField = new JTextField(18);
		row = addRow(header, gbc, row, "Name:", nameField);

		oamCountLabel = new JLabel("0");
		row = addRow(header, gbc, row, "OAM count:", oamCountLabel);

		visibleOamCountLabel = new JLabel("0");
		row = addRow(header, gbc, row, "Visible OAMs:", visibleOamCountLabel);

		boundingBoxLabel = new JLabel("—");
		row = addRow(header, gbc, row, "Bounding box:", boundingBoxLabel);

		hasVramTransferCheck = new JCheckBox("Has VRAM transfer");
		row = addRow(header, gbc, row, "", hasVramTransferCheck);

		vramTransferSrcAddrSpinner = new JSpinner(
			new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 32));
		row = addRow(header, gbc, row, "VRAM src addr (bytes):", vramTransferSrcAddrSpinner);

		vramTransferSizeSpinner = new JSpinner(
			new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 32));
		row = addRow(header, gbc, row, "VRAM size (bytes):", vramTransferSizeSpinner);

		palettePicker = new PalettePicker();
		row = addRow(header, gbc, row, "Preview palette:", palettePicker);

		zoomSlider = new ZoomSlider();
		row = addRow(header, gbc, row, "Zoom:", zoomSlider);

		add(header, BorderLayout.NORTH);

		// ---------- Center: preview on top, layers-host on bottom ----------
		JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

		previewPanel = new PreviewPanel();
		JScrollPane previewScroll = new JScrollPane(previewPanel);
		previewScroll.setBorder(BorderFactory.createTitledBorder("Preview"));
		previewScroll.getVerticalScrollBar().setUnitIncrement(16);
		previewScroll.getHorizontalScrollBar().setUnitIncrement(16);
		previewScroll.setPreferredSize(new Dimension(280, 180));
		center.add(previewScroll);

		// bodyHost mirrors MultiCellEditor's pattern — the shared
		// LayersPanel is reparented in here on every handleObject so
		// switching MultiCell <-> Cell re-roots the singleton instance.
		bodyHost = new JPanel(new BorderLayout());
		bodyHost.setPreferredSize(new Dimension(280, 220));
		bodyPlaceholder = new JLabel(
			"<html><div style='text-align:center;color:#888;padding:24px;'>"
			+ "Layers panel not wired in.</div></html>",
			JLabel.CENTER);
		bodyHost.add(bodyPlaceholder, BorderLayout.CENTER);
		center.add(bodyHost);

		add(center, BorderLayout.CENTER);

		// ---------- South: OAM management buttons ----------
		addButton = new JButton("Add OAM");
		dupButton = new JButton("Duplicate OAM");
		delButton = new JButton("Delete OAM");
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
		buttons.add(addButton);
		buttons.add(dupButton);
		buttons.add(delButton);
		add(buttons, BorderLayout.SOUTH);

		installListeners();
		setControlsEnabled(false);
	}

	/** Mirrors {@link MultiCellEditor#setLayersPanel(LayersPanel)} —
	 *  called once at startup. The actual mount into {@link #bodyHost}
	 *  happens in {@link #handleObject(Object)} so the shared singleton
	 *  can be re-rooted between this editor and {@link MultiCellEditor}. */
	public void setLayersPanel(LayersPanel layersPanel) {
		this.layersPanel = layersPanel;
	}

	public void attachContext(Sprite2DResource resource, Runnable canvasRefresh) {
		this.resource = resource;
		this.canvasRefresh = canvasRefresh;
	}

	@Override
	public void handleObject(Object o) {
		if (o instanceof Sprite2DCell) {
			cell = (Sprite2DCell) o;
		} else {
			cell = null;
		}
		populateFromModel();
		// Rebind the shared LayersPanel to this cell's OAM list and re-root
		// it inside our bodyHost (it might currently live in MultiCellEditor).
		if (layersPanel != null) {
			bodyHost.removeAll();
			if (cell != null) {
				bodyHost.add(layersPanel, BorderLayout.CENTER);
				layersPanel.setCell(cell);
			} else {
				bodyHost.add(bodyPlaceholder, BorderLayout.CENTER);
				layersPanel.clear();
			}
			bodyHost.revalidate();
			bodyHost.repaint();
		}
	}

	@Override
	public void save() {
		if (cell != null && !nameField.getText().equals(cell.name)) {
			cell.name = nameField.getText();
		}
	}

	private static int addRow(JPanel host, GridBagConstraints gbc, int row,
			String label, java.awt.Component widget) {
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		host.add(new JLabel(label), gbc);

		gbc.gridx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		host.add(widget, gbc);
		return row + 1;
	}

	private void installListeners() {
		nameField.addActionListener(e -> commitName());
		nameField.addFocusListener(new FocusAdapter() {
			@Override public void focusLost(FocusEvent e) { commitName(); }
		});

		hasVramTransferCheck.addActionListener(e -> {
			if (suppressEvents || cell == null) return;
			pushCellProp(CellPropertyAction.Field.HAS_VRAM_TRANSFER,
				cell.hasVramTransfer, hasVramTransferCheck.isSelected());
			refreshVramSpinnersEnabled();
			refreshAfterMutation();
		});

		bindIntSpinner(vramTransferSrcAddrSpinner,
			CellPropertyAction.Field.VRAM_TRANSFER_SRC_ADDR,
			() -> cell.vramTransferSrcAddr);
		bindIntSpinner(vramTransferSizeSpinner,
			CellPropertyAction.Field.VRAM_TRANSFER_SIZE,
			() -> cell.vramTransferSize);

		palettePicker.setSelectionListener(p -> previewPanel.repaint());
		zoomSlider.addZoomListener(z -> {
			previewPanel.revalidate();
			previewPanel.repaint();
		});

		addButton.addActionListener(e -> {
			if (cell == null) return;
			Sprite2DOAM newOam = new Sprite2DOAM();
			int idx = cell.oams.size();
			AddOAMAction action = new AddOAMAction(cell, idx, newOam);
			action.execute();
			undoManager.perform(action);
			rebindLayersAndRepaint();
		});

		dupButton.addActionListener(e -> {
			if (cell == null || layersPanel == null) return;
			LayerItem sel = layersPanel.getSelectedItem();
			if (!(sel instanceof Sprite2DOAM)) return;
			Sprite2DOAM source = (Sprite2DOAM) sel;
			int srcIdx = cell.oams.indexOf(source);
			if (srcIdx < 0) return;
			Sprite2DOAM copy = new Sprite2DOAM(source);
			int insertIdx = srcIdx + 1;
			DuplicateOAMAction action = new DuplicateOAMAction(cell, insertIdx, copy);
			action.execute();
			undoManager.perform(action);
			rebindLayersAndRepaint();
		});

		delButton.addActionListener(e -> {
			if (cell == null || layersPanel == null) return;
			LayerItem sel = layersPanel.getSelectedItem();
			if (!(sel instanceof Sprite2DOAM)) return;
			Sprite2DOAM target = (Sprite2DOAM) sel;
			int idx = cell.oams.indexOf(target);
			if (idx < 0) return;
			RemoveOAMAction action = new RemoveOAMAction(cell, idx, target);
			action.execute();
			undoManager.perform(action);
			rebindLayersAndRepaint();
		});
	}

	private void rebindLayersAndRepaint() {
		if (layersPanel != null) {
			layersPanel.setCell(cell);
		}
		refreshDerivedReadouts();
		refreshAfterMutation();
	}

	private void bindIntSpinner(JSpinner spinner,
			CellPropertyAction.Field field,
			java.util.function.IntSupplier currentValue) {
		final int[] startValue = {0};
		spinner.addFocusListener(new FocusAdapter() {
			@Override public void focusGained(FocusEvent e) {
				if (cell != null) startValue[0] = currentValue.getAsInt();
			}
			@Override public void focusLost(FocusEvent e) {
				if (suppressEvents || cell == null) return;
				int now = (Integer) spinner.getValue();
				if (now == startValue[0]) return;
				pushCellProp(field, startValue[0], now);
				refreshAfterMutation();
			}
		});
	}

	private void commitName() {
		if (suppressEvents || cell == null) return;
		String now = nameField.getText();
		if (now == null) now = "";
		String old = cell.name == null ? "" : cell.name;
		if (now.equals(old)) return;
		pushCellProp(CellPropertyAction.Field.NAME, old, now);
		refreshAfterMutation();
	}

	private void pushCellProp(CellPropertyAction.Field f,
			Object oldVal, Object newVal) {
		CellPropertyAction action = new CellPropertyAction(cell, f, oldVal, newVal);
		action.execute();
		undoManager.perform(action);
	}

	private void refreshAfterMutation() {
		previewPanel.revalidate();
		previewPanel.repaint();
		if (canvasRefresh != null) canvasRefresh.run();
	}

	private void populateFromModel() {
		suppressEvents = true;
		try {
			if (cell == null) {
				nameField.setText("");
				oamCountLabel.setText("0");
				visibleOamCountLabel.setText("0");
				boundingBoxLabel.setText("—");
				hasVramTransferCheck.setSelected(false);
				vramTransferSrcAddrSpinner.setValue(0);
				vramTransferSizeSpinner.setValue(0);
				palettePicker.setPalettes(null);
				setControlsEnabled(false);
			} else {
				nameField.setText(cell.name == null ? "" : cell.name);
				refreshDerivedReadouts();
				hasVramTransferCheck.setSelected(cell.hasVramTransfer);
				vramTransferSrcAddrSpinner.setValue(cell.vramTransferSrcAddr);
				vramTransferSizeSpinner.setValue(cell.vramTransferSize);
				if (resource != null) {
					palettePicker.setPalettes(resource.palettes);
				}
				setControlsEnabled(true);
				refreshVramSpinnersEnabled();
			}
		} finally {
			suppressEvents = false;
		}
		previewPanel.revalidate();
		previewPanel.repaint();
	}

	private void refreshDerivedReadouts() {
		if (cell == null) return;
		oamCountLabel.setText(String.valueOf(cell.getOAMCount()));
		int vis = cell.getVisibleOAMs().size();
		visibleOamCountLabel.setText(String.valueOf(vis));
		if (vis == 0) {
			boundingBoxLabel.setText("—");
		} else {
			int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
			for (Sprite2DOAM oam : cell.getVisibleOAMs()) {
				minX = Math.min(minX, oam.x);
				minY = Math.min(minY, oam.y);
				maxX = Math.max(maxX, oam.x + oam.width);
				maxY = Math.max(maxY, oam.y + oam.height);
			}
			boundingBoxLabel.setText(
				"(" + minX + "," + minY + ") .. (" + maxX + "," + maxY + ")  "
				+ (maxX - minX) + "x" + (maxY - minY));
		}
	}

	private void refreshVramSpinnersEnabled() {
		boolean on = hasVramTransferCheck.isSelected() && cell != null;
		vramTransferSrcAddrSpinner.setEnabled(on);
		vramTransferSizeSpinner.setEnabled(on);
	}

	private void setControlsEnabled(boolean on) {
		nameField.setEnabled(on);
		hasVramTransferCheck.setEnabled(on);
		vramTransferSrcAddrSpinner.setEnabled(on);
		vramTransferSizeSpinner.setEnabled(on);
		palettePicker.setEnabled(on);
		zoomSlider.setEnabled(on);
		addButton.setEnabled(on);
		dupButton.setEnabled(on);
		delButton.setEnabled(on);
	}

	private class PreviewPanel extends JPanel {

		PreviewPanel() {
			setBackground(new Color(0x202020));
		}

		@Override
		public Dimension getPreferredSize() {
			BufferedImage img = renderImage();
			int z = zoomSlider.getZoomFactor();
			int w = Math.max(64, img.getWidth() * z);
			int h = Math.max(64, img.getHeight() * z);
			return new Dimension(w, h);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (cell == null || resource == null) return;
			BufferedImage img = renderImage();
			int z = zoomSlider.getZoomFactor();
			g.drawImage(img, 0, 0, img.getWidth() * z, img.getHeight() * z, this);
		}

		private BufferedImage renderImage() {
			if (cell == null || resource == null) {
				return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			}
			return SpriteRenderer.renderCell(cell,
				resource.getActiveTileSheet(),
				palettePicker.getSelectedPalette(),
				resource.mappingMode);
		}
	}
}
