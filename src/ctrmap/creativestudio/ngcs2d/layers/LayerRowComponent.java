package ctrmap.creativestudio.ngcs2d.layers;

import ctrmap.creativestudio.ngcs2d.canvas.undo.LayerLockAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.LayerNameAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.LayerOpacityAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.LayerVisibilityAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.SpriteUndoManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * One row in the Layers panel. Photoshop-style: left-to-right → eye
 * toggle, lock toggle, editable name, opacity slider, reorder arrows.
 *
 * <p>Operates on any {@link LayerItem} — usually a multi-cell entry
 * (the primary use case: body-part granularity for trainer / Pokemon
 * sprites), optionally an individual OAM when editing a cell directly.</p>
 *
 * <p>Each mutation produces a matching {@link
 * ctrmap.creativestudio.ngcs2d.canvas.undo.SpriteAction} pushed onto
 * {@link SpriteUndoManager} so the Ctrl-Z stack and the NGCS2D dirty
 * flag work uniformly across canvas edits and layer edits.</p>
 */
public class LayerRowComponent extends JPanel {

	/** Callback invoked after any row-driven mutation so the parent
	 *  panel can refresh the canvas and any sibling rows (e.g. after a
	 *  reorder button click). */
	public interface RowChangeListener {
		void onLayerChanged();
	}

	/** Reorder hooks fire when the user clicks ▲ / ▼. The parent panel
	 *  performs the actual reorder (it owns the backing list). */
	public interface RowReorderListener {
		void onMoveUp(int rowIndex);
		void onMoveDown(int rowIndex);
	}

	/** Fired when the row becomes the selected one — either the user
	 *  clicked somewhere on the row body or focused the name field.
	 *  Photoshop-style single-selection: the parent panel deselects any
	 *  previously-selected row in response. */
	public interface RowSelectionListener {
		void onRowSelected(int rowIndex);
	}

	private final LayerItem item;
	private final int stackIndex;
	private final SpriteUndoManager undoManager;
	private final RowChangeListener changeListener;
	private final RowSelectionListener selectionListener;

	private final JPanel westCluster;
	private final JPanel centerCluster;
	private final JPanel eastCluster;
	private final JButton eyeToggle;
	private final JButton lockToggle;
	private final JTextField nameField;
	private final JSlider opacitySlider;
	private final JButton upButton;
	private final JButton downButton;

	/** True when this row is currently the selected one (Photoshop-style
	 *  single selection owned by {@link LayersPanel}). Drives row
	 *  background / border — a selected row is painted like a JList
	 *  item under focus. */
	private boolean selected = false;

	/** Cached base/selected colours so {@link #syncFromModel()} and
	 *  {@link #setSelected(boolean)} compose in a predictable order. */
	private static final Color SELECTED_BG = new Color(214, 230, 248);
	private static final Color LOCKED_BG = new Color(248, 242, 224);
	private static final Color SELECTED_BORDER = new Color(48, 120, 200);

	/** Guard against feedback loops while reflecting model state into
	 *  widgets — stops DocumentListener/ChangeListener from firing
	 *  undo actions for programmatic updates. */
	private boolean suppressEvents = false;

	/** Captured at slider-press time; the undo action is pushed with
	 *  this as {@code oldOpacity} so a drag coalesces into one step. */
	private float opacityAtPressTime = 1.0f;

	public LayerRowComponent(LayerItem item, int stackIndex, int totalRows,
			SpriteUndoManager undoManager,
			RowChangeListener changeListener,
			RowReorderListener reorderListener,
			RowSelectionListener selectionListener) {
		this.item = item;
		this.stackIndex = stackIndex;
		this.undoManager = undoManager;
		this.changeListener = changeListener;
		this.selectionListener = selectionListener;

		setLayout(new BorderLayout(4, 0));
		setOpaque(true);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
			BorderFactory.createEmptyBorder(2, 4, 2, 4)));

		// --- Left cluster: eye + lock toggles ---
		westCluster = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
		westCluster.setOpaque(false);
		eyeToggle = new JButton();
		eyeToggle.setMargin(new java.awt.Insets(1, 4, 1, 4));
		eyeToggle.setToolTipText("Toggle layer visibility");
		eyeToggle.addActionListener(e -> { fireSelection(); toggleVisibility(); });
		westCluster.add(eyeToggle);

		lockToggle = new JButton();
		lockToggle.setMargin(new java.awt.Insets(1, 4, 1, 4));
		lockToggle.setToolTipText("Toggle edit lock");
		lockToggle.addActionListener(e -> { fireSelection(); toggleLock(); });
		westCluster.add(lockToggle);

		add(westCluster, BorderLayout.WEST);

		// --- Center: name field + opacity slider stacked vertically ---
		centerCluster = new JPanel(new BorderLayout(2, 2));
		centerCluster.setOpaque(false);

		nameField = new JTextField();
		nameField.setColumns(10);
		// Commit rename on focus lost or Enter keypress, not per-keystroke
		// (would flood the undo stack). Gaining focus selects the row so
		// clicking inside the text field is also a selection gesture.
		nameField.addActionListener(e -> commitRename());
		nameField.addFocusListener(new FocusAdapter() {
			@Override public void focusGained(FocusEvent e) { fireSelection(); }
			@Override public void focusLost(FocusEvent e) { commitRename(); }
		});
		centerCluster.add(nameField, BorderLayout.NORTH);

		opacitySlider = new JSlider(0, 100, 100);
		opacitySlider.setPreferredSize(new Dimension(120, 18));
		opacitySlider.setToolTipText("Layer opacity (editor preview only)");
		opacitySlider.setOpaque(false);
		// Capture drag-start opacity so the undo step spans the whole drag.
		opacitySlider.addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) {
				fireSelection();
				opacityAtPressTime = item.getOpacity();
			}
			@Override public void mouseReleased(MouseEvent e) {
				commitOpacity();
			}
		});
		// Live-update the model during drag so the canvas preview follows
		// the slider. We push the undo action only on mouse release above.
		opacitySlider.addChangeListener(new ChangeListener() {
			@Override public void stateChanged(ChangeEvent e) {
				if (suppressEvents) return;
				item.setOpacity(opacitySlider.getValue() / 100f);
				if (changeListener != null) changeListener.onLayerChanged();
			}
		});
		centerCluster.add(opacitySlider, BorderLayout.SOUTH);

		add(centerCluster, BorderLayout.CENTER);

		// --- Right cluster: up/down reorder buttons ---
		eastCluster = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
		eastCluster.setOpaque(false);
		upButton = new JButton("\u25B2");
		upButton.setMargin(new java.awt.Insets(1, 2, 1, 2));
		upButton.setToolTipText("Move layer up (closer to top)");
		upButton.setEnabled(stackIndex > 0);
		upButton.addActionListener(e -> {
			fireSelection();
			if (reorderListener != null) reorderListener.onMoveUp(stackIndex);
		});
		eastCluster.add(upButton);

		downButton = new JButton("\u25BC");
		downButton.setMargin(new java.awt.Insets(1, 2, 1, 2));
		downButton.setToolTipText("Move layer down");
		downButton.setEnabled(stackIndex < totalRows - 1);
		downButton.addActionListener(e -> {
			fireSelection();
			if (reorderListener != null) reorderListener.onMoveDown(stackIndex);
		});
		eastCluster.add(downButton);

		add(eastCluster, BorderLayout.EAST);

		// Row-level mouse listener: clicking any "dead" (non-widget) part
		// of the row fires selection. Child buttons / sliders / text field
		// consume their own clicks, so those only trigger their own actions
		// (but most also explicitly fire selection via the handlers above
		// so the row becomes selected on widget interaction too).
		MouseAdapter rowClick = new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) { fireSelection(); }
		};
		addMouseListener(rowClick);
		westCluster.addMouseListener(rowClick);
		centerCluster.addMouseListener(rowClick);
		eastCluster.addMouseListener(rowClick);

		syncFromModel();
	}

	/** Forwards the selection event to the parent panel. No-op if no
	 *  listener is installed or if the row is already selected. */
	private void fireSelection() {
		if (selectionListener != null) {
			selectionListener.onRowSelected(stackIndex);
		}
	}

	/** Toggles the "I am the selected row" visual state. Called by
	 *  {@link LayersPanel} as it maintains single-selection across the
	 *  whole row list. */
	public void setSelected(boolean selected) {
		if (this.selected == selected) return;
		this.selected = selected;
		syncFromModel();
	}

	/** Copies the item's current state into the widgets. Called at
	 *  construction, whenever the panel repopulates, and whenever the
	 *  selection state toggles (selection wins over lock colouring). */
	private void syncFromModel() {
		suppressEvents = true;
		try {
			eyeToggle.setText(item.isVisible() ? "\u25C9" : "\u25CB");
			eyeToggle.setForeground(item.isVisible() ? new Color(0, 120, 0) : Color.GRAY);
			lockToggle.setText(item.isLocked() ? "\uD83D\uDD12" : "\u2014"); // 🔒 or em-dash
			lockToggle.setForeground(item.isLocked() ? new Color(160, 110, 0) : Color.GRAY);
			nameField.setText(item.getLayerName() != null
				? item.getLayerName()
				: ("Layer " + stackIndex));
			opacitySlider.setValue(Math.round(item.getOpacity() * 100f));
			// Selection overrides lock shading so the user can always see
			// which row is active even when it's locked.
			if (selected) {
				setBackground(SELECTED_BG);
				setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(1, 1, 1, 1, SELECTED_BORDER),
					BorderFactory.createEmptyBorder(1, 3, 1, 3)));
			} else {
				setBackground(item.isLocked() ? LOCKED_BG : null);
				setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
					BorderFactory.createEmptyBorder(2, 4, 2, 4)));
			}
			repaint();
		} finally {
			suppressEvents = false;
		}
	}

	private void toggleVisibility() {
		if (suppressEvents) return;
		boolean oldVis = item.isVisible();
		boolean newVis = !oldVis;
		LayerVisibilityAction action = new LayerVisibilityAction(item, oldVis, newVis);
		action.execute();
		undoManager.perform(action);
		syncFromModel();
		if (changeListener != null) changeListener.onLayerChanged();
	}

	private void toggleLock() {
		if (suppressEvents) return;
		boolean oldLock = item.isLocked();
		boolean newLock = !oldLock;
		LayerLockAction action = new LayerLockAction(item, oldLock, newLock);
		action.execute();
		undoManager.perform(action);
		syncFromModel();
		if (changeListener != null) changeListener.onLayerChanged();
	}

	private void commitRename() {
		if (suppressEvents) return;
		String oldName = item.getLayerName();
		String newName = nameField.getText();
		if (newName != null && newName.isEmpty()) newName = null;
		if (java.util.Objects.equals(oldName, newName)) return; // no-op
		LayerNameAction action = new LayerNameAction(item, oldName, newName);
		action.execute();
		undoManager.perform(action);
		if (changeListener != null) changeListener.onLayerChanged();
	}

	private void commitOpacity() {
		if (suppressEvents) return;
		float newOp = opacitySlider.getValue() / 100f;
		if (Math.abs(newOp - opacityAtPressTime) < 1e-4f) return;
		// Model was live-updated during drag (so the preview followed);
		// restore so execute() runs a proper transition.
		item.setOpacity(opacityAtPressTime);
		LayerOpacityAction action = new LayerOpacityAction(item, opacityAtPressTime, newOp);
		action.execute();
		undoManager.perform(action);
		if (changeListener != null) changeListener.onLayerChanged();
	}

	public LayerItem getItem() {
		return item;
	}
}
