package ctrmap.creativestudio.ngcs2d.editors;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.canvas.SpriteRenderer;
import ctrmap.creativestudio.ngcs2d.canvas.undo.MappingModeAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.SpriteUndoManager;
import ctrmap.creativestudio.ngcs2d.canvas.undo.TileSheetPropertyAction;
import ctrmap.creativestudio.ngcs2d.editors.widgets.PalettePicker;
import ctrmap.creativestudio.ngcs2d.editors.widgets.ZoomSlider;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

/**
 * NitroCharacter-style property editor for {@link Sprite2DTileSheet} (NCGR).
 *
 * <p>Top: editable property form (name, dimensions, mapping flags, palette
 * picker, zoom). Center: live colored preview rendered via
 * {@link SpriteRenderer#renderTileSheet}. All scalar/boolean field edits
 * push a {@link TileSheetPropertyAction} (or {@link MappingModeAction} for
 * the resource-wide mapping mode) so the dirty-tracker and undo stack
 * stay coherent.</p>
 */
public class TileSheetEditor extends JPanel implements IEditor {

	private static final String[] MAPPING_MODE_LABELS = {
		"1D 32K", "1D 64K", "1D 128K", "1D 256K", "2D"
	};

	private final SpriteUndoManager undoManager;

	private Sprite2DResource resource;
	private Runnable canvasRefresh;
	private Sprite2DTileSheet tileSheet;

	private boolean suppressEvents = false;

	private final JTextField nameField;
	private final JLabel formatLabel;
	private final JLabel tileCountLabel;
	private final JLabel totalBytesLabel;
	private final JCheckBox useLinealLayoutCheck;
	private final JSpinner tileWidthSpinner;
	private final JSpinner tileHeightSpinner;
	private final JCheckBox isLinearMappedCheck;
	private final JSpinner objTilesWideSpinner;
	private final JSpinner objTilesHighSpinner;
	private final JCheckBox rasterLayoutCheck;
	private final JComboBox<String> mappingModeCombo;
	private final PalettePicker palettePicker;
	private final ZoomSlider zoomSlider;
	private final PreviewPanel previewPanel;

	public TileSheetEditor(SpriteUndoManager undoManager) {
		this.undoManager = undoManager;
		setLayout(new BorderLayout(0, 8));

		JPanel header = new JPanel(new GridBagLayout());
		header.setBorder(BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(), "NCGR Properties",
			TitledBorder.LEFT, TitledBorder.TOP));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3, 4, 3, 4);
		gbc.anchor = GridBagConstraints.WEST;

		int row = 0;

		nameField = new JTextField(18);
		row = addRow(header, gbc, row, "Name:", nameField);

		formatLabel = new JLabel();
		row = addRow(header, gbc, row, "Format:", formatLabel);

		tileCountLabel = new JLabel();
		row = addRow(header, gbc, row, "Tile count:", tileCountLabel);

		totalBytesLabel = new JLabel();
		row = addRow(header, gbc, row, "Total bytes:", totalBytesLabel);

		useLinealLayoutCheck = new JCheckBox(
			"Use 1D / lineal layout (no fixed width/height)");
		row = addRow(header, gbc, row, "", useLinealLayoutCheck);

		tileWidthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4096, 1));
		row = addRow(header, gbc, row, "Tile width:", tileWidthSpinner);

		tileHeightSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4096, 1));
		row = addRow(header, gbc, row, "Tile height:", tileHeightSpinner);

		isLinearMappedCheck = new JCheckBox("Is linear mapped (1D OBJ layout)");
		row = addRow(header, gbc, row, "", isLinearMappedCheck);

		objTilesWideSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 64, 1));
		row = addRow(header, gbc, row, "OBJ tiles wide:", objTilesWideSpinner);

		objTilesHighSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 64, 1));
		row = addRow(header, gbc, row, "OBJ tiles high:", objTilesHighSpinner);

		rasterLayoutCheck = new JCheckBox("Raster layout (bitmap-mode source)");
		row = addRow(header, gbc, row, "", rasterLayoutCheck);

		mappingModeCombo = new JComboBox<>(MAPPING_MODE_LABELS);
		row = addRow(header, gbc, row, "Mapping mode (resource-wide):", mappingModeCombo);

		palettePicker = new PalettePicker();
		row = addRow(header, gbc, row, "Preview palette:", palettePicker);

		zoomSlider = new ZoomSlider();
		row = addRow(header, gbc, row, "Zoom:", zoomSlider);

		add(header, BorderLayout.NORTH);

		previewPanel = new PreviewPanel();
		JScrollPane previewScroll = new JScrollPane(previewPanel);
		previewScroll.setBorder(BorderFactory.createTitledBorder("Preview"));
		previewScroll.getVerticalScrollBar().setUnitIncrement(16);
		previewScroll.getHorizontalScrollBar().setUnitIncrement(16);
		add(previewScroll, BorderLayout.CENTER);

		installListeners();
		setControlsEnabled(false);
	}

	/** Mirrors {@link MultiCellEditor#setLayersPanel(Object)} — called once
	 *  after construction by NGCS2D. */
	public void attachContext(Sprite2DResource resource, Runnable canvasRefresh) {
		this.resource = resource;
		this.canvasRefresh = canvasRefresh;
	}

	@Override
	public void handleObject(Object o) {
		if (o instanceof Sprite2DTileSheet) {
			tileSheet = (Sprite2DTileSheet) o;
		} else {
			tileSheet = null;
		}
		populateFromModel();
	}

	@Override
	public void save() {
		// Listener-driven primary path handles undoable commits. This is
		// only the safety net for the focus-lost-fires-after-swap case on
		// the name field — everything else commits immediately.
		if (tileSheet != null && !nameField.getText().equals(tileSheet.name)) {
			tileSheet.name = nameField.getText();
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
		// Name: commit on focus lost / Enter.
		nameField.addActionListener(e -> commitName());
		nameField.addFocusListener(new FocusAdapter() {
			@Override public void focusLost(FocusEvent e) { commitName(); }
		});

		// Spinners: capture on focusGained, commit on focusLost.
		bindSpinner(tileWidthSpinner, TileSheetPropertyAction.Field.TILE_WIDTH,
			() -> tileSheet.tileWidth);
		bindSpinner(tileHeightSpinner, TileSheetPropertyAction.Field.TILE_HEIGHT,
			() -> tileSheet.tileHeight);
		bindSpinner(objTilesWideSpinner, TileSheetPropertyAction.Field.OBJ_TILES_WIDE,
			() -> tileSheet.objTilesWide);
		bindSpinner(objTilesHighSpinner, TileSheetPropertyAction.Field.OBJ_TILES_HIGH,
			() -> tileSheet.objTilesHigh);

		// Checkboxes: commit immediately.
		useLinealLayoutCheck.addActionListener(e -> {
			if (suppressEvents || tileSheet == null) return;
			boolean lineal = useLinealLayoutCheck.isSelected();
			suppressEvents = true;
			try {
				if (lineal) {
					// Force both width/height to the -1 sentinel via two atomic
					// undo steps so each can be reverted individually.
					if (tileSheet.tileWidth != -1) {
						pushTileSheetProp(TileSheetPropertyAction.Field.TILE_WIDTH,
							tileSheet.tileWidth, -1);
					}
					if (tileSheet.tileHeight != -1) {
						pushTileSheetProp(TileSheetPropertyAction.Field.TILE_HEIGHT,
							tileSheet.tileHeight, -1);
					}
				} else {
					int fallback = tileSheet.getEffectiveTileWidth();
					int rows = tileSheet.getEffectiveTileHeight();
					if (tileSheet.tileWidth != fallback) {
						pushTileSheetProp(TileSheetPropertyAction.Field.TILE_WIDTH,
							tileSheet.tileWidth, fallback);
					}
					if (tileSheet.tileHeight != rows) {
						pushTileSheetProp(TileSheetPropertyAction.Field.TILE_HEIGHT,
							tileSheet.tileHeight, rows);
					}
					tileWidthSpinner.setValue(Math.max(1, tileSheet.tileWidth));
					tileHeightSpinner.setValue(Math.max(1, tileSheet.tileHeight));
				}
			} finally {
				suppressEvents = false;
			}
			refreshDimensionSpinners();
			refreshDerivedReadouts();
			refreshAfterMutation();
		});

		isLinearMappedCheck.addActionListener(e -> {
			if (suppressEvents || tileSheet == null) return;
			pushTileSheetProp(TileSheetPropertyAction.Field.IS_LINEAR_MAPPED,
				tileSheet.isLinearMapped, isLinearMappedCheck.isSelected());
			refreshObjTileSpinners();
			refreshAfterMutation();
		});

		rasterLayoutCheck.addActionListener(e -> {
			if (suppressEvents || tileSheet == null) return;
			pushTileSheetProp(TileSheetPropertyAction.Field.RASTER_LAYOUT,
				tileSheet.rasterLayout, rasterLayoutCheck.isSelected());
			refreshAfterMutation();
		});

		mappingModeCombo.addActionListener(e -> {
			if (suppressEvents || resource == null) return;
			int newMode = mappingModeCombo.getSelectedIndex();
			if (newMode < 0 || newMode == resource.mappingMode) return;
			MappingModeAction action = new MappingModeAction(
				resource, resource.mappingMode, newMode);
			action.execute();
			undoManager.perform(action);
			refreshAfterMutation();
		});

		palettePicker.setSelectionListener(p -> {
			previewPanel.repaint();
		});

		zoomSlider.addZoomListener(z -> {
			previewPanel.revalidate();
			previewPanel.repaint();
		});
	}

	private void bindSpinner(JSpinner spinner,
			TileSheetPropertyAction.Field field,
			java.util.function.IntSupplier currentValue) {
		final int[] startValue = {0};
		spinner.addFocusListener(new FocusAdapter() {
			@Override public void focusGained(FocusEvent e) {
				if (tileSheet != null) startValue[0] = currentValue.getAsInt();
			}
			@Override public void focusLost(FocusEvent e) {
				if (suppressEvents || tileSheet == null) return;
				int now = (Integer) spinner.getValue();
				if (now == startValue[0]) return;
				pushTileSheetProp(field, startValue[0], now);
				refreshAfterMutation();
			}
		});
	}

	private void commitName() {
		if (suppressEvents || tileSheet == null) return;
		String now = nameField.getText();
		if (now == null) now = "";
		String old = tileSheet.name == null ? "" : tileSheet.name;
		if (now.equals(old)) return;
		pushTileSheetProp(TileSheetPropertyAction.Field.NAME, old, now);
		refreshAfterMutation();
	}

	private void pushTileSheetProp(TileSheetPropertyAction.Field f,
			Object oldVal, Object newVal) {
		TileSheetPropertyAction action = new TileSheetPropertyAction(
			tileSheet, f, oldVal, newVal);
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
			if (tileSheet == null) {
				nameField.setText("");
				formatLabel.setText("");
				tileCountLabel.setText("");
				totalBytesLabel.setText("");
				useLinealLayoutCheck.setSelected(false);
				tileWidthSpinner.setValue(1);
				tileHeightSpinner.setValue(1);
				isLinearMappedCheck.setSelected(false);
				objTilesWideSpinner.setValue(1);
				objTilesHighSpinner.setValue(1);
				rasterLayoutCheck.setSelected(false);
				mappingModeCombo.setSelectedIndex(0);
				palettePicker.setPalettes(null);
				setControlsEnabled(false);
			} else {
				nameField.setText(tileSheet.name == null ? "" : tileSheet.name);
				formatLabel.setText(tileSheet.format == 3
					? "4bpp (IDX4) — read-only"
					: "8bpp (IDX8) — read-only");
				refreshDerivedReadouts();
				boolean lineal = (tileSheet.tileWidth <= 0 || tileSheet.tileHeight <= 0);
				useLinealLayoutCheck.setSelected(lineal);
				tileWidthSpinner.setValue(Math.max(1, tileSheet.tileWidth));
				tileHeightSpinner.setValue(Math.max(1, tileSheet.tileHeight));
				isLinearMappedCheck.setSelected(tileSheet.isLinearMapped);
				objTilesWideSpinner.setValue(Math.max(1, tileSheet.objTilesWide));
				objTilesHighSpinner.setValue(Math.max(1, tileSheet.objTilesHigh));
				rasterLayoutCheck.setSelected(tileSheet.rasterLayout);
				if (resource != null) {
					int mode = resource.mappingMode;
					if (mode < 0 || mode >= MAPPING_MODE_LABELS.length) {
						mode = 0;
					}
					mappingModeCombo.setSelectedIndex(mode);
					palettePicker.setPalettes(resource.palettes);
				}
				setControlsEnabled(true);
				refreshDimensionSpinners();
				refreshObjTileSpinners();
			}
		} finally {
			suppressEvents = false;
		}
		previewPanel.revalidate();
		previewPanel.repaint();
	}

	private void refreshDerivedReadouts() {
		if (tileSheet == null) return;
		tileCountLabel.setText(String.valueOf(tileSheet.getTileCount()));
		totalBytesLabel.setText(String.valueOf(
			tileSheet.tileData == null ? 0 : tileSheet.tileData.length));
	}

	private void refreshDimensionSpinners() {
		boolean lineal = useLinealLayoutCheck.isSelected();
		tileWidthSpinner.setEnabled(!lineal);
		tileHeightSpinner.setEnabled(!lineal);
	}

	private void refreshObjTileSpinners() {
		boolean linear = isLinearMappedCheck.isSelected();
		objTilesWideSpinner.setEnabled(linear);
		objTilesHighSpinner.setEnabled(linear);
	}

	private void setControlsEnabled(boolean on) {
		nameField.setEnabled(on);
		useLinealLayoutCheck.setEnabled(on);
		tileWidthSpinner.setEnabled(on);
		tileHeightSpinner.setEnabled(on);
		isLinearMappedCheck.setEnabled(on);
		objTilesWideSpinner.setEnabled(on);
		objTilesHighSpinner.setEnabled(on);
		rasterLayoutCheck.setEnabled(on);
		mappingModeCombo.setEnabled(on);
		palettePicker.setEnabled(on);
		zoomSlider.setEnabled(on);
	}

	private class PreviewPanel extends JPanel {

		PreviewPanel() {
			setBackground(new Color(0x202020));
		}

		@Override
		public Dimension getPreferredSize() {
			if (tileSheet == null) {
				return new Dimension(64, 64);
			}
			BufferedImage img = SpriteRenderer.renderTileSheet(
				tileSheet, palettePicker.getSelectedPalette());
			int z = zoomSlider.getZoomFactor();
			int w = Math.max(1, img.getWidth() * z);
			int h = Math.max(1, img.getHeight() * z);
			return new Dimension(w, h);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (tileSheet == null) return;
			BufferedImage img = SpriteRenderer.renderTileSheet(
				tileSheet, palettePicker.getSelectedPalette());
			int z = zoomSlider.getZoomFactor();
			g.drawImage(img, 0, 0, img.getWidth() * z, img.getHeight() * z, this);
		}
	}
}
