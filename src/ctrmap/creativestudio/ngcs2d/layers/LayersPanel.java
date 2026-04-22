package ctrmap.creativestudio.ngcs2d.layers;

import ctrmap.creativestudio.ngcs2d.canvas.undo.LayerReorderAction;
import ctrmap.creativestudio.ngcs2d.canvas.undo.SpriteUndoManager;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

/**
 * Photoshop-style layer panel. The primary binding is
 * {@link #setMultiCell(Sprite2DMultiCell)} — each multi-cell entry
 * becomes one layer row, which is the user-facing granularity for
 * trainer / Pokemon sprites (one entry per body part: head, torso,
 * arm, etc.).
 *
 * <p>A secondary binding {@link #setCell(Sprite2DCell)} shows one row
 * per OAM inside a standalone cell. That's useful when authoring a
 * cell in isolation, but OAMs aren't the natural layer granularity for
 * multi-cell-based sprites — multi-cells are.</p>
 *
 * <p>Selection binding: NGCS2D calls the appropriate {@code setXxx()}
 * whenever the resource tree selection changes. Passing {@code null} to
 * either method (or calling {@link #clear()}) collapses the panel to
 * its placeholder.</p>
 *
 * <p>Canvas refresh: every mutation the rows perform calls the
 * listener installed via {@link #setCanvasRefresh(Runnable)}
 * (typically {@code spriteCanvas::refreshRender}).</p>
 */
public class LayersPanel extends JPanel {

	private final SpriteUndoManager undoManager;
	private final JPanel rowContainer;
	private final JScrollPane scrollPane;
	private final JLabel placeholderLabel;
	private final TitledBorder titledBorder;

	/** The list currently feeding the rows. Either
	 *  {@code Sprite2DMultiCell.entries} or {@code Sprite2DCell.oams}.
	 *  Generic-wildcarded so reorder edits go through
	 *  {@link LayerReorderAction} parameterised on the concrete element
	 *  type. */
	private List<? extends LayerItem> activeSource;

	/** Live reference to the currently-mounted row components, in the
	 *  same order as {@link #activeSource}. Rebuilt on every
	 *  {@link #rebuildRows()}; used to propagate single-selection
	 *  state (deselect the old row, select the new one). */
	private final List<LayerRowComponent> rowComponents = new ArrayList<>();

	/** Index into {@link #rowComponents} of the currently-selected row,
	 *  or -1 for no selection. Maintained by
	 *  {@link #setSelectedIndex(int)}. */
	private int selectedIndex = -1;

	private Runnable canvasRefresh;

	/** External callback fired when the selected layer changes. Receives
	 *  the newly-selected {@link LayerItem}, or {@code null} when
	 *  selection clears. NGCS2D uses this to drive the canvas's
	 *  highlighted-entry box. */
	private Consumer<LayerItem> selectionListener;

	public LayersPanel(SpriteUndoManager undoManager) {
		super(new BorderLayout());
		this.undoManager = undoManager;
		titledBorder = BorderFactory.createTitledBorder("Layers");
		setBorder(titledBorder);

		rowContainer = new JPanel();
		rowContainer.setLayout(new BoxLayout(rowContainer, BoxLayout.Y_AXIS));

		scrollPane = new JScrollPane(rowContainer,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.setBorder(null);

		placeholderLabel = new JLabel(
			"<html><div style='text-align:center;color:#888;'>"
			+ "Select a Multi-Cell in the tree to view its layers."
			+ "</div></html>", SwingConstants.CENTER);
		placeholderLabel.setBorder(BorderFactory.createEmptyBorder(24, 12, 24, 12));

		add(placeholderLabel, BorderLayout.CENTER);
		setPreferredSize(new Dimension(300, 220));
	}

	/** Installs the canvas refresh hook. Called once at construction. */
	public void setCanvasRefresh(Runnable canvasRefresh) {
		this.canvasRefresh = canvasRefresh;
	}

	/**
	 * Installs the selection-change listener. NGCS2D wires this to push
	 * the selected multi-cell entry into the canvas, which then draws an
	 * outline box around that entry's screen rectangle.
	 *
	 * <p>The listener receives {@code null} whenever selection clears
	 * (tree moves off multi-cell, source list rebuild, explicit
	 * {@link #clear()}).</p>
	 */
	public void setSelectionListener(Consumer<LayerItem> listener) {
		this.selectionListener = listener;
	}

	/** @return the currently-selected layer item, or {@code null}. */
	public LayerItem getSelectedItem() {
		if (selectedIndex < 0 || activeSource == null
			|| selectedIndex >= activeSource.size()) {
			return null;
		}
		return activeSource.get(selectedIndex);
	}

	/**
	 * Programmatically sets which row is selected. Deselects any other
	 * row, fires {@link #selectionListener}. Pass {@code -1} to clear.
	 */
	public void setSelectedIndex(int index) {
		if (activeSource == null) {
			index = -1;
		} else if (index >= 0 && index >= activeSource.size()) {
			index = -1;
		}
		if (index == selectedIndex) return;
		selectedIndex = index;
		for (int i = 0; i < rowComponents.size(); i++) {
			rowComponents.get(i).setSelected(i == selectedIndex);
		}
		if (selectionListener != null) {
			selectionListener.accept(getSelectedItem());
		}
	}

	/**
	 * Binds the panel to a multi-cell's entries (the primary use case).
	 * Pass {@code null} to clear.
	 */
	public void setMultiCell(Sprite2DMultiCell mc) {
		if (mc == null) {
			clear();
			return;
		}
		this.activeSource = mc.entries;
		titledBorder.setTitle("Layers — " + safeName(mc.name, "MultiCell"));
		rebuildRows();
	}

	/**
	 * Binds the panel to a cell's OAMs. Secondary use case: editing a
	 * standalone cell. Pass {@code null} to clear.
	 */
	public void setCell(Sprite2DCell cell) {
		if (cell == null) {
			clear();
			return;
		}
		this.activeSource = cell.oams;
		titledBorder.setTitle("Layers — " + safeName(cell.name, "Cell"));
		rebuildRows();
	}

	/** Collapses the panel to the "nothing selected" placeholder. */
	public void clear() {
		this.activeSource = null;
		titledBorder.setTitle("Layers");
		rebuildRows();
	}

	private void rebuildRows() {
		rowContainer.removeAll();
		rowComponents.clear();
		removeAll();

		// Rebuild implies a fresh selection state. Don't fire the external
		// listener from here — clear() calls this path and also needs to
		// fire null; setMultiCell/setCell just wipe any stale selection
		// and callers should re-pick the first row themselves if they want
		// a default selection.
		int oldSelection = selectedIndex;
		selectedIndex = -1;

		if (activeSource == null) {
			placeholderLabel.setText(
				"<html><div style='text-align:center;color:#888;'>"
				+ "Select a Multi-Cell in the tree to view its layers."
				+ "</div></html>");
			add(placeholderLabel, BorderLayout.CENTER);
			revalidate();
			repaint();
			if (oldSelection != -1 && selectionListener != null) {
				selectionListener.accept(null);
			}
			return;
		}
		if (activeSource.isEmpty()) {
			placeholderLabel.setText(
				"<html><div style='text-align:center;color:#888;'>"
				+ "No layers to display."
				+ "</div></html>");
			add(placeholderLabel, BorderLayout.CENTER);
			revalidate();
			repaint();
			if (oldSelection != -1 && selectionListener != null) {
				selectionListener.accept(null);
			}
			return;
		}

		int total = activeSource.size();
		for (int i = 0; i < total; i++) {
			LayerItem it = activeSource.get(i);
			final int idx = i;
			LayerRowComponent row = new LayerRowComponent(
				it, idx, total, undoManager,
				this::fireCanvasRefresh,
				new LayerRowComponent.RowReorderListener() {
					@Override public void onMoveUp(int rowIndex) {
						performReorder(rowIndex, rowIndex - 1);
					}
					@Override public void onMoveDown(int rowIndex) {
						performReorder(rowIndex, rowIndex + 1);
					}
				},
				this::setSelectedIndex // RowSelectionListener
			);
			rowContainer.add(row);
			rowComponents.add(row);
		}
		// Push rows to the top; remaining space stays empty.
		rowContainer.add(Box.createVerticalGlue());

		add(scrollPane, BorderLayout.CENTER);
		revalidate();
		repaint();

		if (oldSelection != -1 && selectionListener != null) {
			selectionListener.accept(null);
		}
	}

	/**
	 * Reorders within {@link #activeSource}. Uses the concrete element
	 * type of the bound list via the generic {@link LayerReorderAction};
	 * the wildcard capture goes through a helper so the unchecked cast
	 * is isolated to one spot.
	 */
	private void performReorder(int from, int to) {
		if (activeSource == null) return;
		int n = activeSource.size();
		if (from < 0 || from >= n || to < 0 || to >= n || from == to) return;
		doReorder(activeSource, from, to);
		rebuildRows();
		// Rebuild cleared selection; restore to the moved item's new
		// index so the user doesn't lose context after pressing ▲/▼.
		setSelectedIndex(to);
		fireCanvasRefresh();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void doReorder(List<?> list, int from, int to) {
		LayerReorderAction action = new LayerReorderAction((List) list, from, to);
		action.execute();
		undoManager.perform(action);
	}

	private void fireCanvasRefresh() {
		if (canvasRefresh != null) {
			canvasRefresh.run();
		}
	}

	private static String safeName(String s, String fallback) {
		if (s == null || s.isEmpty()) return fallback;
		return s.length() > 32 ? s.substring(0, 29) + "..." : s;
	}
}
