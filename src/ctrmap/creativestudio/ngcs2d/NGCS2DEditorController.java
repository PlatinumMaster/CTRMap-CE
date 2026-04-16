package ctrmap.creativestudio.ngcs2d;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.editors.CellAnimEditor;
import ctrmap.creativestudio.ngcs2d.editors.CellEditor;
import ctrmap.creativestudio.ngcs2d.editors.MultiCellAnimEditor;
import ctrmap.creativestudio.ngcs2d.editors.MultiCellEditor;
import ctrmap.creativestudio.ngcs2d.editors.OAMEditor;
import ctrmap.creativestudio.ngcs2d.editors.PaletteEditor;
import ctrmap.creativestudio.ngcs2d.editors.TileSheetEditor;

import java.awt.BorderLayout;
import javax.swing.JPanel;

/**
 * Editor controller for the NGCS 2D sprite editing system. Manages the set of
 * property editors and handles switching between them as the user selects
 * different resource nodes in the tree.
 *
 * <p>Parallel to {@code NGEditorController} for 3D resources, this controller
 * owns one instance of each 2D editor type and swaps them into a container
 * panel on demand.</p>
 */
public class NGCS2DEditorController {

	/**
	 * Editor for {@code Sprite2DPalette} resources.
	 */
	public PaletteEditor paletteEditor = new PaletteEditor();

	/**
	 * Editor for {@code Sprite2DTileSheet} resources.
	 */
	public TileSheetEditor tileSheetEditor = new TileSheetEditor();

	/**
	 * Editor for {@code Sprite2DCell} resources.
	 */
	public CellEditor cellEditor = new CellEditor();

	/**
	 * Editor for {@code Sprite2DOAM} resources.
	 */
	public OAMEditor oamEditor = new OAMEditor();

	/**
	 * Editor for {@code Sprite2DCellAnimation} resources.
	 */
	public CellAnimEditor cellAnimEditor = new CellAnimEditor();

	/**
	 * Editor for {@code Sprite2DMultiCell} resources.
	 */
	public MultiCellEditor multiCellEditor = new MultiCellEditor();

	/**
	 * Editor for {@code Sprite2DMultiCellAnimation} resources.
	 */
	public MultiCellAnimEditor multiCellAnimEditor = new MultiCellAnimEditor();

	/**
	 * A no-op editor used when no valid editor is selected. Both
	 * {@code handleObject} and {@code save} do nothing.
	 */
	public IEditor defaultEditor = new IEditor() {
		@Override
		public void handleObject(Object o) {
		}

		@Override
		public void save() {
		}
	};

	/**
	 * The currently active editor. Defaults to the no-op editor.
	 */
	public IEditor currentEditor = defaultEditor;

	/**
	 * Switches the active editor, saves the previous editor's state, and
	 * installs the new editor panel into the given container.
	 *
	 * <p>The previous editor's {@code save()} method is called before the
	 * switch. The container is cleared, the new editor panel is added, and
	 * the editor is loaded with the given content via {@code handleObject}.</p>
	 *
	 * @param editor          The editor to switch to, or {@code null} for the default no-op editor.
	 * @param content         The resource object to load into the editor.
	 * @param editorContainer The container panel that hosts the active editor UI.
	 */
	public void switchEditor(IEditor editor, Object content, JPanel editorContainer) {
		if (currentEditor != null) {
			currentEditor.save();
		}

		if (editor != null) {
			currentEditor = editor;
		} else {
			currentEditor = defaultEditor;
		}

		editorContainer.removeAll();

		if (currentEditor instanceof JPanel) {
			editorContainer.setLayout(new BorderLayout());
			editorContainer.add((JPanel) currentEditor, BorderLayout.CENTER);
		}

		currentEditor.handleObject(content);

		editorContainer.revalidate();
		editorContainer.repaint();
	}
}
