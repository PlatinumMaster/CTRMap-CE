package ctrmap.creativestudio.ngcs2d.rtldr;

import xstandard.fs.FSFile;
import xstandard.gui.file.XFileDialog;
import xstandard.gui.file.ExtensionFilter;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 * Manages UI contributions (menu items) for the NGCS2D sprite editor.
 * Mirrors NGCSUIManager for the 3D system.
 */
public class NGCS2DUIManager {

	private final JMenuBar menuBar;

	/**
	 * Constructs a UI manager bound to the given menu bar.
	 *
	 * @param menuBar The menu bar to manage.
	 */
	public NGCS2DUIManager(JMenuBar menuBar) {
		this.menuBar = menuBar;
	}

	/**
	 * Adds a menu item to the menu with the given name.
	 *
	 * @param menuName The name of the parent menu.
	 * @param item     The menu item to add.
	 */
	public void addMenuItem(String menuName, JMenuItem item) {
		for (Component comp : menuBar.getComponents()) {
			if (comp instanceof JMenu) {
				JMenu menu = (JMenu) comp;
				if (Objects.equals(menu.getText(), menuName)) {
					menu.add(item);
					break;
				}
			}
		}
	}

	/**
	 * Removes a menu item from all menus in the menu bar.
	 *
	 * @param item The menu item to remove.
	 */
	public void removeMenuItem(JMenuItem item) {
		for (Component comp : menuBar.getComponents()) {
			if (comp instanceof JMenu) {
				JMenu menu = (JMenu) comp;
				menu.remove(item);
			}
		}
	}

	/**
	 * Creates a simple import menu item that opens a file dialog and imports
	 * the selected file into the content accessor.
	 *
	 * @param name            The display name of the menu item.
	 * @param filter          The file extension filter for the open dialog.
	 * @param contentAccessor The content accessor to import into.
	 * @return The created menu item.
	 */
	public static JMenuItem createSimpleImportMenuItem(String name, ExtensionFilter filter, NGCS2DContentAccessor contentAccessor) {
		JMenuItem i = new JMenuItem(name);
		i.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FSFile file = XFileDialog.openFileDialog(filter);

				contentAccessor.importFile(file);
			}
		});
		return i;
	}

	/**
	 * Creates an export menu item that opens a save dialog and delegates
	 * to the given callback to perform the export.
	 *
	 * @param name            The display name of the menu item.
	 * @param filter          The file extension filter for the save dialog.
	 * @param uiParent        The parent frame for dialogs.
	 * @param contentAccessor The content accessor providing the resource to export.
	 * @param callback        The export callback to invoke.
	 * @return The created menu item.
	 */
	public static JMenuItem createExportMenuItem(String name, ExtensionFilter filter, Frame uiParent, NGCS2DContentAccessor contentAccessor, ExportCallback callback) {
		JMenuItem i = new JMenuItem(name);
		i.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FSFile dest = XFileDialog.openSaveFileDialog(filter);

				if (dest != null) {
					callback.export(dest, uiParent, contentAccessor);
				}
			}
		});
		return i;
	}

	/**
	 * Callback interface for export operations from a menu item.
	 */
	public static interface ExportCallback {

		/**
		 * Performs the export operation.
		 *
		 * @param dest            The destination file.
		 * @param uiParent        The parent frame for dialogs.
		 * @param contentAccessor The content accessor providing the resource.
		 */
		public void export(FSFile dest, Frame uiParent, NGCS2DContentAccessor contentAccessor);
	}
}
