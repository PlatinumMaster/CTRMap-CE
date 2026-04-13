package ctrmap.creativestudio.ngcs2d;

import ctrmap.creativestudio.ngcs2d.canvas.CS2DAnimControlPanel;
import ctrmap.creativestudio.ngcs2d.canvas.SpriteCanvas;
import ctrmap.creativestudio.ngcs2d.canvas.SpriteToolStrip;
import ctrmap.creativestudio.ngcs2d.canvas.tools.BrushTool;
import ctrmap.creativestudio.ngcs2d.canvas.tools.ColorPickerTool;
import ctrmap.creativestudio.ngcs2d.canvas.tools.EraserTool;
import ctrmap.creativestudio.ngcs2d.canvas.tools.FillTool;
import ctrmap.creativestudio.ngcs2d.canvas.tools.MoveTool;
import ctrmap.creativestudio.ngcs2d.canvas.tools.PanTool;
import ctrmap.creativestudio.ngcs2d.canvas.tools.PencilTool;
import ctrmap.creativestudio.ngcs2d.canvas.tools.SelectTool;
import ctrmap.creativestudio.ngcs2d.canvas.tools.Sprite2DBaseTool;
import ctrmap.creativestudio.ngcs2d.canvas.tools.Sprite2DTool;
import ctrmap.creativestudio.ngcs2d.canvas.undo.SpriteUndoManager;
import ctrmap.creativestudio.ngcs2d.io.I2DFormatHandler;
import ctrmap.creativestudio.ngcs2d.io.NGCS2DImporter;
import ctrmap.creativestudio.ngcs2d.plugins.NGCS2DStandardIOPlugin;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import ctrmap.creativestudio.ngcs2d.rtldr.NGCS2DContentAccessor;
import ctrmap.creativestudio.ngcs2d.rtldr.NGCS2DIOManager;
import ctrmap.creativestudio.ngcs2d.rtldr.NGCS2DJulietHelper;
import ctrmap.creativestudio.ngcs2d.rtldr.NGCS2DJulietIface;
import ctrmap.creativestudio.ngcs2d.rtldr.NGCS2DUIManager;
import ctrmap.creativestudio.ngcs2d.tree.CS2DNode;
import ctrmap.creativestudio.ngcs2d.tree.CS2DTree;
import xstandard.fs.FSFile;
import xstandard.fs.accessors.DiskFile;
import xstandard.gui.DnDHelper;
import xstandard.gui.components.ComponentUtils;
import xstandard.gui.file.ExtensionFilter;
import xstandard.gui.file.XFileDialog;
import xstandard.util.ListenableList;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import rtldr.JRTLDRCore;

/**
 * Main JFrame window for the NGCS 2D CreativeStudio sprite editor.
 *
 * <p>This is the central window that ties together the data tree, editor panels,
 * canvas placeholder, and plugin-contributed menus. It mirrors the structure of
 * {@link ctrmap.creativestudio.ngcs.NGCS} but is simplified for 2D sprite
 * resources -- no 3D viewport, cameras, lights, or shaders.</p>
 *
 * <p>The layout is built programmatically using {@link JSplitPane} dividers:
 * the left side hosts a canvas placeholder (to be replaced by a real renderer
 * in Phase 5), while the right side is split vertically between the resource
 * tree and the property editor panel.</p>
 *
 * <p>Implements {@link NGCS2DContentAccessor} to provide uniform access to the
 * active {@link Sprite2DResource} for plugins and I/O handlers.</p>
 */
public class NGCS2D extends JFrame implements NGCS2DContentAccessor {

	static {
		//Default NGCS2D plugins
		JRTLDRCore.loadExtensions(NGCS2DJulietIface.getInstance(),
			new NGCS2DStandardIOPlugin()
		);
	}

	/**
	 * Callback interface for embedded mode. When the editor is launched in
	 * embedded mode from another tool, the callback receives the edited
	 * resource on window close.
	 */
	public static interface NGCS2DEmbeddedCallback {

		/**
		 * Called when the embedded editor is closed and the resource should
		 * be saved back to the caller.
		 *
		 * @param resource The edited sprite resource.
		 * @return True if the save was successful.
		 */
		boolean onSave(Sprite2DResource resource);
	}

	private Sprite2DResource resource;
	private CS2DTree dataTree;
	private SpriteCanvas canvas;
	private SpriteToolStrip toolStrip;
	private CS2DAnimControlPanel animControlPanel;
	private JPanel editorContainer;
	private JScrollPane editorScrollPane;
	private NGCS2DEditorController editors;
	private NGCS2DIOManager ioMgr = NGCS2DIOManager.getInstance();
	private NGCS2DUIManager uiMgr;
	private JMenuBar menuBar;
	private CS2DNode lastSelectedNode;
	private SpriteUndoManager undoManager = new SpriteUndoManager();

	private NGCS2DEmbeddedCallback embeddedCallback;
	private JLabel statusBar;

	/**
	 * Creates the CreativeStudio 2D window in standalone mode.
	 *
	 * <p>Initializes the look-and-feel, builds the UI layout, loads plugins,
	 * creates the editor controller, and sets up tree selection and drag-and-drop
	 * listeners.</p>
	 */
	public NGCS2D() {
		super();
		setTitle("CTRMap Creative Studio 2D");
		ComponentUtils.setSystemNativeLookAndFeel();

		resource = new Sprite2DResource();

		initComponents();

		uiMgr = new NGCS2DUIManager(menuBar);
		NGCS2DJulietHelper.onCSWindowLoad(this);

		editorScrollPane.getVerticalScrollBar().setUnitIncrement(20);

		editors = new NGCS2DEditorController();

		dataTree.initTree(this, resource);

		dataTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		dataTree.addTreeSelectionListener((TreeSelectionEvent e) -> {
			CS2DNode node = getSelectedNode();
			if (node != null) {
				editors.switchEditor(node.getEditor(), node.getContent(), editorContainer);
				updateCanvasFromNode(node);
			} else {
				editors.switchEditor(null, null, editorContainer);
				canvas.showNothing();
			}
			lastSelectedNode = node;
		});
		editors.switchEditor(null, null, editorContainer);
		dataTree.expandRow(0);

		dataTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				handleTreePopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				handleTreePopup(e);
			}
		});

		DnDHelper.addFileDropTarget(canvas, new DnDHelper.FileDropListener() {
			@Override
			public void acceptDrop(List<File> files) {
				List<DiskFile> diskFiles = new ArrayList<>();
				for (File f : files) {
					diskFiles.add(new DiskFile(f));
				}
				importFiles(diskFiles);
			}
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				NGCS2DJulietHelper.onCSWindowClose(NGCS2D.this);
			}
		});

		setSize(1200, 800);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	/**
	 * Creates the CreativeStudio 2D window in embedded mode. Merges the given
	 * source resource into the editor and invokes the callback with the final
	 * resource when the window is closed.
	 *
	 * @param source   The initial sprite resource to edit.
	 * @param callback The callback to invoke on save/close.
	 */
	public NGCS2D(Sprite2DResource source, NGCS2DEmbeddedCallback callback) {
		this();
		this.embeddedCallback = callback;
		merge(source);
		setTitle("CTRMap Creative Studio 2D - Embedded Mode");
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (embeddedCallback != null) {
					embeddedCallback.onSave(resource);
				}
			}
		});
	}

	// -------------------------------------------------------------------------
	// UI construction
	// -------------------------------------------------------------------------

	/**
	 * Builds the Swing component hierarchy programmatically.
	 *
	 * <p>Layout structure:</p>
	 * <pre>
	 *  BorderLayout(frame)
	 *  +-- NORTH: JMenuBar
	 *  +-- CENTER: mainSplit (HORIZONTAL_SPLIT)
	 *  |   +-- Left: canvasPlaceholder
	 *  |   +-- Right: rightSplit (VERTICAL_SPLIT)
	 *  |       +-- Top: treeScrollPane (CS2DTree)
	 *  |       +-- Bottom: editorScrollPane (editorContainer)
	 *  +-- SOUTH: statusBar
	 * </pre>
	 */
	private void initComponents() {
		//Menu bar
		menuBar = new JMenuBar();
		buildMenuBar();
		setJMenuBar(menuBar);

		//Sprite canvas (real 2D viewport)
		canvas = new SpriteCanvas();
		canvas.setMinimumSize(new Dimension(200, 200));
		canvas.setPreferredSize(new Dimension(600, 500));

		//Tool strip (left edge)
		toolStrip = new SpriteToolStrip();
		toolStrip.addToolChangeListener((ActionEvent e) -> activateSelectedTool());

		//Animation playback controls (south of canvas). Drives the canvas
		//frame index when previewing a cell / multi-cell animation, so the
		//play / pause / step buttons actually advance the displayed frame.
		animControlPanel = new CS2DAnimControlPanel();
		animControlPanel.addFrameChangeListener(() -> {
			if (canvas != null) {
				// Forward both the outer frame index and the master tick
				// counter. The multi-cell renderer uses the tick counter to
				// drive each entry's sub-NANR independently so slots with
				// different animation lengths stay in real-time sync.
				canvas.setAnimationFrame(
					animControlPanel.getCurrentFrame(),
					animControlPanel.getElapsedTicks());
			}
		});

		//Compose canvas + tool strip into a single left panel
		JPanel canvasPane = new JPanel(new BorderLayout());
		canvasPane.add(toolStrip, BorderLayout.WEST);
		canvasPane.add(canvas, BorderLayout.CENTER);
		canvasPane.add(animControlPanel, BorderLayout.SOUTH);
		canvasPane.setMinimumSize(new Dimension(320, 200));
		canvasPane.setPreferredSize(new Dimension(720, 550));

		// Activate the default tool (pencil) so the canvas is editable
		// straight after launch.
		activateSelectedTool();

		//Data tree
		dataTree = new CS2DTree();
		JScrollPane treeScrollPane = new JScrollPane(dataTree);
		treeScrollPane.setMinimumSize(new Dimension(200, 150));
		treeScrollPane.setPreferredSize(new Dimension(300, 350));

		//Editor container
		editorContainer = new JPanel(new BorderLayout());
		editorContainer.setMinimumSize(new Dimension(200, 100));
		editorScrollPane = new JScrollPane(editorContainer);
		editorScrollPane.setMinimumSize(new Dimension(200, 100));
		editorScrollPane.setPreferredSize(new Dimension(300, 350));

		//Right split: tree on top, editor on bottom
		JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeScrollPane, editorScrollPane);
		rightSplit.setDividerLocation(350);
		rightSplit.setResizeWeight(0.5);

		//Main split: canvas+tool strip on left, right panel on right
		JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvasPane, rightSplit);
		mainSplit.setDividerLocation(680);
		mainSplit.setResizeWeight(0.6);

		//Status bar
		statusBar = new JLabel(" Ready");
		statusBar.setBorder(BorderFactory.createEtchedBorder());

		//Frame layout
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(mainSplit, BorderLayout.CENTER);
		getContentPane().add(statusBar, BorderLayout.SOUTH);
	}

	/**
	 * Constructs the menu bar with Project, Import, Export, and View menus.
	 */
	private void buildMenuBar() {
		JMenu projectMenu = new JMenu("Project");
		JMenuItem newItem = new JMenuItem("New");
		newItem.addActionListener((ActionEvent e) -> {
			clear();
		});
		JMenuItem openSpriteItem = new JMenuItem("Open Sprite...");
		openSpriteItem.setToolTipText("Open one or more sprite files (NCLR + NCGR + NCER, etc.) at once");
		openSpriteItem.addActionListener((ActionEvent e) -> {
			ExtensionFilter[] filters = collectImportExtensionFilters();
			List<DiskFile> files = XFileDialog.openMultiFileDialog(filters);
			if (files != null && !files.isEmpty()) {
				importFiles(files);
			}
		});
		JMenuItem importGenericItem = new JMenuItem("Import...");
		importGenericItem.addActionListener((ActionEvent e) -> {
			List<DiskFile> files = XFileDialog.openMultiFileDialog();
			if (!files.isEmpty()) {
				importFiles(files);
			}
		});
		JMenuItem saveItem = new JMenuItem("Save");
		saveItem.addActionListener((ActionEvent e) -> {
			editors.currentEditor.save();
		});
		projectMenu.add(newItem);
		projectMenu.addSeparator();
		projectMenu.add(openSpriteItem);
		projectMenu.add(importGenericItem);
		projectMenu.add(saveItem);

		JMenu importMenu = new JMenu("Import");
		JMenu exportMenu = new JMenu("Export");
		JMenu viewMenu = new JMenu("View");

		menuBar.add(projectMenu);
		menuBar.add(importMenu);
		menuBar.add(exportMenu);
		menuBar.add(viewMenu);
	}

	// -------------------------------------------------------------------------
	// Tree interaction
	// -------------------------------------------------------------------------

	/**
	 * Returns the currently selected node in the data tree.
	 *
	 * @return The selected {@link CS2DNode}, or null if nothing is selected.
	 */
	public CS2DNode getSelectedNode() {
		Object node = dataTree.getLastSelectedPathComponent();
		if (node instanceof CS2DNode) {
			return (CS2DNode) node;
		}
		return null;
	}

	/**
	 * Handles right-click popup menus on tree nodes. Displays the node's
	 * registered actions as menu items.
	 *
	 * @param e The mouse event to evaluate.
	 */
	private void handleTreePopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			int row = dataTree.getClosestRowForLocation(e.getX(), e.getY());
			if (row >= 0) {
				dataTree.setSelectionRow(row);
				CS2DNode node = getSelectedNode();
				if (node != null) {
					List<CS2DNode.CS2DNodeAction> actions = node.getActions();
					if (!actions.isEmpty()) {
						JPopupMenu popup = new JPopupMenu();
						for (CS2DNode.CS2DNodeAction action : actions) {
							JMenuItem mi = new JMenuItem(action.name);
							mi.addActionListener((ActionEvent ae) -> {
								action.callback.run();
							});
							popup.add(mi);
						}
						popup.show(dataTree, e.getX(), e.getY());
					}
				}
			}
		}
	}

	/**
	 * Reinitializes the data tree from the current resource. Called after
	 * import, merge, or clear operations to refresh the tree display.
	 */
	private void reinitTree() {
		dataTree.initTree(this, resource);
	}

	// -------------------------------------------------------------------------
	// Import / merge / clear
	// -------------------------------------------------------------------------

	/**
	 * Imports a list of files into the active resource using the registered
	 * format handlers.
	 *
	 * @param files The files to import.
	 */
	public void importFiles(List<? extends FSFile> files) {
		Sprite2DResource res = NGCS2DImporter.importFiles(this, files.toArray(new FSFile[files.size()]));
		merge(res);
	}

	@Override
	public void importFile(FSFile fsf) {
		if (fsf != null) {
			Sprite2DResource res = NGCS2DImporter.importFiles(this, fsf);
			merge(res);
		}
	}

	@Override
	public void importResource(Sprite2DResource res) {
		if (res != null) {
			resource.merge(res);
			reinitTree();
		}
	}

	/**
	 * Merges a sprite resource into the current resource and refreshes
	 * the tree.
	 *
	 * @param rsc The resource to merge.
	 */
	public void merge(Sprite2DResource rsc) {
		if (rsc != null) {
			resource.merge(rsc);
			// After merge, derive OBJ-size for lineal-mapped tile sheets
			// from any newly-imported cells so the tile sheet view shows
			// coherent sprites instead of fragmented OBJ strips.
			resource.linkTileSheetsToCells();
			reinitTree();
			refreshCanvasResource();
			refreshActiveToolResource();
		}
	}

	/**
	 * Clears all data from the current resource and reinitializes the tree
	 * to an empty state.
	 */
	public void clear() {
		resource.clear();
		reinitTree();
		canvas.showNothing();
		canvas.setResource(resource);
		undoManager.clear();
		refreshActiveToolResource();
	}

	/**
	 * Pushes the current sprite resource into the active tool so that
	 * tools created before any data was loaded see the latest tile sheets,
	 * cells, and palettes.
	 */
	private void refreshActiveToolResource() {
		if (canvas == null) {
			return;
		}
		Sprite2DTool t = canvas.getActiveTool();
		if (t instanceof Sprite2DBaseTool) {
			((Sprite2DBaseTool) t).setResource(resource);
		}
	}

	/**
	 * Pushes the current resource into the canvas and previews the most
	 * fully-rendered view available. Priority is cell &gt; tile sheet &gt; palette
	 * so that loading a complete sprite (NCLR + NCGR + NCER) immediately
	 * shows the rendered cell rather than just the palette grid.
	 */
	private void refreshCanvasResource() {
		if (canvas == null) {
			return;
		}
		canvas.setResource(resource);
		// Always pick the richest preview after an import so the canvas
		// upgrades from palette/tilesheet to cell as more data arrives.
		if (!resource.cells.isEmpty()
			&& !resource.tileSheets.isEmpty()
			&& !resource.palettes.isEmpty()) {
			canvas.showCell(resource.cells.get(0));
		} else if (!resource.tileSheets.isEmpty()) {
			canvas.showTileSheet(resource.tileSheets.get(0));
		} else if (!resource.palettes.isEmpty()) {
			canvas.showPalette(resource.palettes.get(0));
		} else {
			canvas.showNothing();
		}
	}

	/**
	 * Collects all import-capable extension filters from registered format
	 * handlers, plus an "All Sprite Files" combined filter at the front.
	 *
	 * @return An array of extension filters suitable for a file dialog.
	 */
	private ExtensionFilter[] collectImportExtensionFilters() {
		I2DFormatHandler[] handlers = ioMgr.getAllFormatHandlers();
		List<ExtensionFilter> perHandler = new ArrayList<>();
		List<String> allExtensions = new ArrayList<>();
		for (I2DFormatHandler h : handlers) {
			if (!h.canImport()) {
				continue;
			}
			ExtensionFilter f = h.getExtensionFilter();
			if (f == null) {
				continue;
			}
			perHandler.add(f);
			for (String ext : f.getExtensions()) {
				if (!allExtensions.contains(ext)) {
					allExtensions.add(ext);
				}
			}
		}
		List<ExtensionFilter> result = new ArrayList<>();
		if (!allExtensions.isEmpty()) {
			result.add(new ExtensionFilter("All Sprite Files",
				allExtensions.toArray(new String[allExtensions.size()])));
		}
		result.addAll(perHandler);
		return result.toArray(new ExtensionFilter[result.size()]);
	}

	/**
	 * Instantiates the correct {@link Sprite2DTool} for the currently
	 * selected tool strip button, binds it to the canvas + resource +
	 * undo manager, and sets it as active.
	 */
	private void activateSelectedTool() {
		if (toolStrip == null || canvas == null) {
			return;
		}
		Sprite2DTool tool;
		switch (toolStrip.getSelectedTool()) {
			case PENCIL:       tool = new PencilTool(); break;
			case BRUSH:        tool = new BrushTool(); break;
			case ERASER:       tool = new EraserTool(); break;
			case FILL:         tool = new FillTool(); break;
			case COLOR_PICKER: tool = new ColorPickerTool(); break;
			case SELECT:       tool = new SelectTool(); break;
			case MOVE:         tool = new MoveTool(); break;
			case PAN:          tool = new PanTool(); break;
			default:           tool = new PencilTool(); break;
		}
		if (tool instanceof Sprite2DBaseTool) {
			Sprite2DBaseTool bt = (Sprite2DBaseTool) tool;
			bt.setCanvas(canvas);
			bt.setResource(resource);
			bt.setUndoManager(undoManager);
			bt.setForegroundColorIndex(1);
		}
		canvas.setActiveTool(tool);
	}

	/**
	 * Updates the canvas to display the content of the given tree node.
	 * Decides which display mode (palette / tile sheet / cell / animation
	 * / multi-cell / multi-cell-animation) to use based on the node's
	 * content type. Also resets / updates the animation control panel so
	 * playback buttons drive the new content.
	 */
	private void updateCanvasFromNode(CS2DNode node) {
		if (canvas == null || node == null) {
			return;
		}
		Object content = node.getContent();
		if (content instanceof Sprite2DPalette) {
			canvas.showPalette((Sprite2DPalette) content);
			resetAnimControl();
		} else if (content instanceof Sprite2DTileSheet) {
			canvas.showTileSheet((Sprite2DTileSheet) content);
			resetAnimControl();
		} else if (content instanceof Sprite2DCell) {
			canvas.showCell((Sprite2DCell) content);
			resetAnimControl();
		} else if (content instanceof Sprite2DOAM) {
			// Selecting an OAM displays its parent cell (if available).
			TreeNode parent = node.getParent();
			if (parent instanceof CS2DNode) {
				Object pContent = ((CS2DNode) parent).getContent();
				if (pContent instanceof Sprite2DCell) {
					canvas.showCell((Sprite2DCell) pContent);
					resetAnimControl();
					return;
				}
			}
			canvas.showNothing();
			resetAnimControl();
		} else if (content instanceof Sprite2DCellAnimation) {
			Sprite2DCellAnimation anim = (Sprite2DCellAnimation) content;
			canvas.showCellAnimation(anim);
			if (animControlPanel != null) {
				int n = anim.getFrameCount();
				int[] durs = new int[n];
				for (int i = 0; i < n; i++) {
					durs[i] = Math.max(1, anim.frames.get(i).duration);
				}
				animControlPanel.setFrameDurations(durs);
				animControlPanel.setCurrentFrame(0);
			}
		} else if (content instanceof Sprite2DMultiCell) {
			canvas.showMultiCell((Sprite2DMultiCell) content);
			if (animControlPanel != null) {
				// Multi-cell preview has no outer NMAR frame list, but each
				// entry may still reference a NANR that needs to tick in real
				// time. Enable continuous-playback mode so the control panel
				// runs the master tick counter off the play button without
				// requiring the user to load an outer animation first.
				animControlPanel.setContinuousPlayback(true);
			}
		} else if (content instanceof Sprite2DMultiCellAnimation) {
			Sprite2DMultiCellAnimation anim = (Sprite2DMultiCellAnimation) content;
			canvas.showMultiCellAnimation(anim);
			if (animControlPanel != null) {
				int n = anim.getFrameCount();
				int[] durs = new int[n];
				for (int i = 0; i < n; i++) {
					durs[i] = Math.max(1, anim.frames.get(i).duration);
				}
				animControlPanel.setFrameDurations(durs);
				animControlPanel.setCurrentFrame(0);
			}
		}
	}

	/**
	 * Resets the animation control panel back to its idle state. Called
	 * whenever the user selects a non-animated node so the play / pause
	 * buttons stop affecting whatever the canvas is now showing.
	 */
	private void resetAnimControl() {
		if (animControlPanel != null) {
			animControlPanel.reset();
		}
	}

	/**
	 * Re-fires the current tree selection to refresh the active editor.
	 * Useful after external modifications to the selected node's content.
	 */
	public void reloadEditor() {
		CS2DNode node = getSelectedNode();
		if (node != null) {
			editors.switchEditor(node.getEditor(), node.getContent(), editorContainer);
		}
	}

	/**
	 * Saves the current editor state.
	 */
	public void save() {
		editors.currentEditor.save();
	}

	// -------------------------------------------------------------------------
	// Accessors
	// -------------------------------------------------------------------------

	/**
	 * Returns the I/O manager for this editor session.
	 *
	 * @return The I/O manager singleton.
	 */
	public NGCS2DIOManager getIOManager() {
		return ioMgr;
	}

	/**
	 * Returns the UI manager for this editor session.
	 *
	 * @return The UI manager.
	 */
	public NGCS2DUIManager getUIManager() {
		return uiMgr;
	}

	@Override
	public Sprite2DResource getResource() {
		return resource;
	}

	@Override
	public ListenableList<Sprite2DPalette> getPalettes() {
		return resource.palettes;
	}

	@Override
	public ListenableList<Sprite2DTileSheet> getTileSheets() {
		return resource.tileSheets;
	}

	@Override
	public ListenableList<Sprite2DCell> getCells() {
		return resource.cells;
	}

	@Override
	public ListenableList<Sprite2DCellAnimation> getCellAnimations() {
		return resource.cellAnimations;
	}

	@Override
	public ListenableList<Sprite2DMultiCell> getMultiCells() {
		return resource.multiCells;
	}

	@Override
	public ListenableList<Sprite2DMultiCellAnimation> getMultiCellAnimations() {
		return resource.multiCellAnimations;
	}

	/**
	 * Sets the status bar text.
	 *
	 * @param text The status message to display.
	 */
	public void setStatus(String text) {
		statusBar.setText(" " + text);
	}

	/**
	 * Empty method referenced by CreativeStudioChecker to trigger class loading
	 * and the static initializer block.
	 */
	public static void dummy() {

	}
}
