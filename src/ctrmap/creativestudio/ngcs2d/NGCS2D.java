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
import ctrmap.creativestudio.ngcs2d.layers.LayerItem;
import ctrmap.creativestudio.ngcs2d.layers.LayersPanel;
import ctrmap.creativestudio.ngcs2d.project.AnimatedGifWriter;
import ctrmap.creativestudio.ngcs2d.project.Cs2dProject;
import ctrmap.creativestudio.ngcs2d.project.Cs2dProjectIO;
import ctrmap.creativestudio.ngcs2d.timeline.TimelinePanel;
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
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
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

	/** Photoshop-style Layers panel bound to whichever cell the tree
	 *  selection points at. Empty placeholder when a non-cell node (or
	 *  nothing) is selected. */
	private LayersPanel layersPanel;

	/** Vegas-style keyframe timeline bound to the selected cell-
	 *  animation or multi-cell-animation. Sits between the canvas and
	 *  the play/pause controls, replacing the linear frame slider as
	 *  the primary scrubber. */
	private TimelinePanel timelinePanel;

	/** Menu item for the explicit embedded-mode save. Only enabled when an
	 *  embedded callback is wired; greyed out in standalone use. */
	private JMenuItem applyToRomItem;

	/** Snapshot of {@link SpriteUndoManager#getModCount()} at the moment of
	 *  the last successful Apply-to-ROM. If the current modCount differs,
	 *  the session has unsaved edits — used to drive the close prompt and
	 *  the {@code *} title-bar dirty indicator. */
	private int lastAppliedModCount = 0;

	/** Companion to {@link #lastAppliedModCount} — modCount snapshot from
	 *  the last successful project (YAML+PNG) save. The session is "clean"
	 *  when the live modCount matches <em>either</em> snapshot, so a
	 *  Save-Project alone is enough to clear the {@code *} marker even
	 *  without an Apply-to-ROM. */
	private int lastSavedProjectModCount = 0;

	/** Directory the project was last loaded-from / saved-to. Drives the
	 *  Project → Save shortcut (which writes back here) and disables it
	 *  until the user does Save As at least once. {@code null} = no
	 *  project file is associated with the session yet. */
	private FSFile currentProjectDir;

	/** Project menu items that depend on having a {@link #currentProjectDir} —
	 *  re-evaluated by {@link #refreshProjectMenuState()}. */
	private JMenuItem saveProjectItem;

	/** Export menu item enabled only when the tree selection is a cell- or
	 *  multi-cell-animation node (the only nodes whose content is meaningful
	 *  to flatten into a GIF). */
	private JMenuItem exportGifItem;

	/** Title without the dirty marker. {@link #refreshTitle()} toggles a
	 *  leading "{@code *}" based on {@link #isDirty()}. */
	private String baseTitle = "CTRMap Creative Studio 2D";

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

		editors = new NGCS2DEditorController(undoManager);

		// Inject the shared UI singletons into the editors that host
		// them. The multi-cell editor displays the Photoshop-style
		// layer panel; both animation editors display the Vegas-style
		// timeline + the same play/pause/stop/step controls. The
		// NCGR/NCER property inspectors get the resource (for the
		// palette picker + resource-wide mapping mode) and a canvas-
		// refresh hook so their own previews stay in sync with the
		// main canvas after every edit. The cell editor also reuses
		// the shared layers panel to display its OAM stack.
		editors.multiCellEditor.setLayersPanel(layersPanel);
		editors.cellAnimEditor.attachComponents(timelinePanel, animControlPanel);
		editors.multiCellAnimEditor.attachComponents(timelinePanel, animControlPanel);
		editors.tileSheetEditor.attachContext(resource, canvas::refreshRender);
		editors.cellEditor.attachContext(resource, canvas::refreshRender);
		editors.cellEditor.setLayersPanel(layersPanel);

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
				if (layersPanel != null) layersPanel.clear();
				if (timelinePanel != null) timelinePanel.clear();
			}
			lastSelectedNode = node;
			refreshExportMenuState();
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
			public void windowClosing(WindowEvent e) {
				attemptClose();
			}

			@Override
			public void windowClosed(WindowEvent e) {
				NGCS2DJulietHelper.onCSWindowClose(NGCS2D.this);
			}
		});

		// Keep the "*" title-bar dirty indicator in sync with edits as
		// they land. fireModCountChanged() dispatches on the EDT (the
		// edit path is already EDT), so it's safe to touch setTitle().
		undoManager.addModCountListener(this::refreshTitle);

		setSize(1600, 1000);
		setLocationRelativeTo(null);
		// DO_NOTHING_ON_CLOSE so attemptClose() can interpose a save-prompt
		// in embedded mode. attemptClose() calls dispose() on its own once
		// the user has resolved pending edits (or if there aren't any).
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		refreshTitle();
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
		baseTitle = "CTRMap Creative Studio 2D - Embedded Mode";
		// The unified close-attempt handler installed in the standalone
		// constructor already knows how to prompt when there are unsaved
		// edits in embedded mode — no extra windowClosing listener needed.
		// Enable the explicit Apply-to-ROM menu item now that a callback
		// is wired up.
		if (applyToRomItem != null) {
			applyToRomItem.setEnabled(true);
		}
		// Clean slate: a freshly-merged source has no pending edits.
		lastAppliedModCount = undoManager.getModCount();
		refreshTitle();
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
		canvas.setPreferredSize(new Dimension(900, 750));

		//Tool strip (left edge)
		toolStrip = new SpriteToolStrip();
		toolStrip.addToolChangeListener((ActionEvent e) -> activateSelectedTool());

		//Animation playback: shared CS2DAnimControlPanel and TimelinePanel
		//are created here but NOT mounted into the left canvas pane. They
		//live inside the animation editors (CellAnimEditor /
		//MultiCellAnimEditor) — see attachComponents() on those. Only the
		//frame-change listeners wire back here so the canvas keeps
		//re-rendering during playback.
		animControlPanel = new CS2DAnimControlPanel();
		animControlPanel.addFrameChangeListener(() -> {
			if (canvas != null) {
				canvas.setAnimationFrame(
					animControlPanel.getCurrentFrame(),
					animControlPanel.getElapsedTicks());
			}
		});

		timelinePanel = new TimelinePanel(undoManager);
		timelinePanel.setPlayheadSource(animControlPanel::getElapsedTicks);
		timelinePanel.setTickSeekListener(animControlPanel::seekToTick);
		animControlPanel.addFrameChangeListener(timelinePanel::repaint);

		//Compose canvas + tool strip into a single left panel. No south
		//row — the timeline + play controls live inside the right-side
		//inspector editors now, per the "one inspector for everything"
		//redesign.
		JPanel canvasPane = new JPanel(new BorderLayout());
		canvasPane.add(toolStrip, BorderLayout.WEST);
		canvasPane.add(canvas, BorderLayout.CENTER);
		canvasPane.setMinimumSize(new Dimension(320, 200));
		canvasPane.setPreferredSize(new Dimension(1000, 800));

		// Activate the default tool (pencil) so the canvas is editable
		// straight after launch.
		activateSelectedTool();

		//Data tree
		dataTree = new CS2DTree();
		JScrollPane treeScrollPane = new JScrollPane(dataTree);
		treeScrollPane.setMinimumSize(new Dimension(200, 150));
		treeScrollPane.setPreferredSize(new Dimension(300, 350));

		//Editor container — the SINGLE contextual inspector area on the
		//right side. Whatever the tree selects, the matching editor
		//(palette / tile sheet / cell / OAM / cell-anim / multi-cell /
		//multi-cell-anim) is swapped into here by
		//NGCS2DEditorController.switchEditor(). The Layers panel lives
		//inside the multi-cell editor; the Timeline + play controls
		//live inside the cell/multi-cell-animation editors.
		editorContainer = new JPanel(new BorderLayout());
		editorContainer.setMinimumSize(new Dimension(240, 200));
		editorScrollPane = new JScrollPane(editorContainer);
		editorScrollPane.setMinimumSize(new Dimension(240, 200));
		editorScrollPane.setPreferredSize(new Dimension(320, 500));

		//Layers panel — shared singleton, owned by NGCS2D, mounted inside
		//MultiCellEditor when a multi-cell is selected. The canvas
		//refresh hook + single-selection listener are wired here once
		//and reused for every multi-cell.
		layersPanel = new LayersPanel(undoManager);
		layersPanel.setCanvasRefresh(() -> {
			if (canvas != null) canvas.refreshRender();
		});
		layersPanel.setSelectionListener((LayerItem item) -> {
			if (canvas == null) return;
			if (item instanceof Sprite2DMultiCell.MultiCellEntry) {
				canvas.setHighlightedEntry((Sprite2DMultiCell.MultiCellEntry) item);
			} else {
				canvas.setHighlightedEntry(null);
			}
		});

		//Right split: tree on top, single contextual inspector on the
		//bottom. No more Layers/Timeline in a separate middle region —
		//they're routed through the inspector's editor-swap mechanism.
		JSplitPane rightSplit = new JSplitPane(
			JSplitPane.VERTICAL_SPLIT, treeScrollPane, editorScrollPane);
		rightSplit.setDividerLocation(260);
		rightSplit.setResizeWeight(0.25);

		//Main split: canvas+tool strip on left, right panel on right
		JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvasPane, rightSplit);
		mainSplit.setDividerLocation(1000);
		mainSplit.setResizeWeight(0.7);

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
			currentProjectDir = null;
			lastSavedProjectModCount = undoManager.getModCount();
			refreshProjectMenuState();
			refreshTitle();
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

		JMenuItem openProjectItem = new JMenuItem("Open Project Folder...");
		openProjectItem.setToolTipText(
			"Load a CS2D project directory (YAML metadata + indexed PNG tile sheets).");
		openProjectItem.addActionListener((ActionEvent e) -> openProjectFolder());

		saveProjectItem = new JMenuItem("Save Project");
		saveProjectItem.setToolTipText(
			"Write the current resource back into the previously-loaded project folder.");
		saveProjectItem.addActionListener((ActionEvent e) -> saveProject());

		JMenuItem saveProjectAsItem = new JMenuItem("Save Project As...");
		saveProjectAsItem.setToolTipText(
			"Write the current resource as a CS2D project directory.");
		saveProjectAsItem.addActionListener((ActionEvent e) -> saveProjectAs());

		// Embedded-mode explicit write-back. In standalone mode this stays
		// disabled (there's no callback to fire); the embedded constructor
		// enables it once the caller's NGCS2DEmbeddedCallback is wired up.
		// Ctrl+S is bound as an accelerator so the user doesn't have to
		// drop into the menu every time.
		applyToRomItem = new JMenuItem("Apply to ROM");
		applyToRomItem.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_S, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		applyToRomItem.setToolTipText(
			"Write pending edits back to the host project (NARC / ROM). "
			+ "Only available in embedded mode.");
		applyToRomItem.addActionListener((ActionEvent e) -> applyEmbedded());
		applyToRomItem.setEnabled(false);

		projectMenu.add(newItem);
		projectMenu.addSeparator();
		projectMenu.add(openSpriteItem);
		projectMenu.add(importGenericItem);
		projectMenu.add(saveItem);
		projectMenu.addSeparator();
		projectMenu.add(openProjectItem);
		projectMenu.add(saveProjectItem);
		projectMenu.add(saveProjectAsItem);
		projectMenu.addSeparator();
		projectMenu.add(applyToRomItem);

		JMenu importMenu = new JMenu("Import");
		JMenu exportMenu = new JMenu("Export");
		JMenu viewMenu = new JMenu("View");

		exportGifItem = new JMenuItem("Export Animation as GIF...");
		exportGifItem.setToolTipText(
			"Export the selected cell or multi-cell animation as an animated GIF.");
		exportGifItem.addActionListener((ActionEvent e) -> exportSelectedAnimAsGif());
		exportGifItem.setEnabled(false);
		exportMenu.add(exportGifItem);

		menuBar.add(projectMenu);
		menuBar.add(importMenu);
		menuBar.add(exportMenu);
		menuBar.add(viewMenu);

		refreshProjectMenuState();
	}

	/** Toggles project-related menu items based on whether the session has
	 *  a {@link #currentProjectDir} associated with it. */
	private void refreshProjectMenuState() {
		if (saveProjectItem != null) {
			saveProjectItem.setEnabled(currentProjectDir != null);
		}
	}

	/** Updates the {@link #exportGifItem} enabled state based on whether
	 *  the current tree selection is an animation node. */
	private void refreshExportMenuState() {
		if (exportGifItem == null) return;
		CS2DNode node = getSelectedNode();
		Object content = node == null ? null : node.getContent();
		exportGifItem.setEnabled(
			content instanceof Sprite2DCellAnimation
			|| content instanceof Sprite2DMultiCellAnimation);
	}

	// -------------------------------------------------------------------------
	// Embedded-mode save flow
	// -------------------------------------------------------------------------

	/**
	 * Has the user made any edits since the last successful Apply? Compares
	 * the live undo-manager modCount against the snapshot taken on apply
	 * (or 0 if no apply has happened yet). Only meaningful in embedded
	 * mode; standalone has no "applied" concept and this always reflects
	 * whether any edits have happened since startup.
	 */
	private boolean isDirty() {
		int now = undoManager.getModCount();
		// Clean if the live modCount matches either persistence snapshot —
		// the user only has to save to one of them to drop the dirty marker.
		return now != lastAppliedModCount && now != lastSavedProjectModCount;
	}

	/**
	 * Re-renders the title bar, prefixing with {@code "*"} when
	 * {@link #isDirty()} is true. Called after every apply / close-attempt
	 * path and at the end of both constructors.
	 */
	private void refreshTitle() {
		setTitle((isDirty() ? "*" : "") + baseTitle);
	}

	/**
	 * Fires the embedded callback and records a clean modCount snapshot
	 * on success. No-op (with a status-bar warning) if the callback
	 * refuses the save or if we're in standalone mode.
	 */
	private void applyEmbedded() {
		if (embeddedCallback == null) {
			statusBar.setText(" No host to apply to (standalone mode).");
			return;
		}
		boolean ok = embeddedCallback.onSave(resource);
		if (ok) {
			lastAppliedModCount = undoManager.getModCount();
			statusBar.setText(" Applied.");
			refreshTitle();
		} else {
			statusBar.setText(" Apply failed — edits kept in memory.");
		}
	}

	// -------------------------------------------------------------------------
	// CS2D project (YAML + indexed PNG) save/load
	// -------------------------------------------------------------------------

	/**
	 * Prompts for a directory and loads a CS2D project from it. Replaces
	 * the in-memory resource (clear + merge) so the existing tree
	 * listeners and canvas refresh hooks pick up the new data.
	 */
	private void openProjectFolder() {
		DiskFile dir = XFileDialog.openDirectoryDialog("Open Project Folder");
		if (dir == null || !dir.exists() || !dir.isDirectory()) {
			return;
		}
		try {
			Cs2dProject project = Cs2dProjectIO.load(dir);
			resource.clear();
			resource.merge(project.resource);
			resource.linkTileSheetsToCells();
			reinitTree();
			refreshCanvasResource();
			refreshActiveToolResource();
			currentProjectDir = dir;
			lastSavedProjectModCount = undoManager.getModCount();
			refreshProjectMenuState();
			refreshTitle();
			statusBar.setText(" Loaded project from " + dir.getName());
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Failed to load project:\n" + ex.getMessage(),
				"Open project", JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Re-saves into {@link #currentProjectDir}. No-op (with status message)
	 *  if no project dir is associated with the session yet — Save As
	 *  has to happen first. */
	private void saveProject() {
		if (currentProjectDir == null) {
			statusBar.setText(" No project folder set — use Save Project As first.");
			return;
		}
		writeProjectTo(currentProjectDir);
	}

	/** Prompts for a directory and saves the project there. The chosen
	 *  directory becomes the new {@link #currentProjectDir}. */
	private void saveProjectAs() {
		DiskFile dir = XFileDialog.openDirectoryDialog("Save Project Folder");
		if (dir == null) return;
		writeProjectTo(dir);
	}

	private void writeProjectTo(FSFile dir) {
		try {
			Cs2dProject project = new Cs2dProject(resource, dir);
			Cs2dProjectIO.save(project, dir);
			currentProjectDir = dir;
			lastSavedProjectModCount = undoManager.getModCount();
			refreshProjectMenuState();
			refreshTitle();
			statusBar.setText(" Saved project to " + dir.getName());
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Failed to save project:\n" + ex.getMessage(),
				"Save project", JOptionPane.ERROR_MESSAGE);
		}
	}

	// -------------------------------------------------------------------------
	// Animated GIF export (selected animation only)
	// -------------------------------------------------------------------------

	private void exportSelectedAnimAsGif() {
		CS2DNode node = getSelectedNode();
		Object content = node == null ? null : node.getContent();
		if (!(content instanceof Sprite2DCellAnimation)
			&& !(content instanceof Sprite2DMultiCellAnimation)) {
			statusBar.setText(" Select an animation node first.");
			return;
		}
		ExtensionFilter gif = new ExtensionFilter("Animated GIF", "*.gif");
		DiskFile target = XFileDialog.openSaveFileDialog("Export Animation as GIF", gif);
		if (target == null) return;
		try {
			if (content instanceof Sprite2DCellAnimation) {
				AnimatedGifWriter.writeCellAnim((Sprite2DCellAnimation) content, resource, target);
			} else {
				AnimatedGifWriter.writeMultiCellAnim((Sprite2DMultiCellAnimation) content, resource, target);
			}
			statusBar.setText(" Exported GIF: " + target.getName());
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Failed to export GIF:\n" + ex.getMessage(),
				"Export GIF", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Unified close handler. Standalone / clean embedded sessions dispose
	 * immediately; dirty embedded sessions get a Photoshop-style
	 * Apply / Discard / Cancel prompt. Called from the {@code
	 * windowClosing} listener installed in {@link #NGCS2D()} instead of
	 * relying on {@link #DISPOSE_ON_CLOSE} — so the user can cancel out of
	 * the prompt and keep editing.
	 */
	private void attemptClose() {
		if (embeddedCallback == null || !isDirty()) {
			dispose();
			return;
		}
		int choice = JOptionPane.showConfirmDialog(
			NGCS2D.this,
			"You have unsaved changes.\n\nApply them to the ROM before closing?",
			"Unsaved changes",
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE);
		switch (choice) {
			case JOptionPane.YES_OPTION:
				applyEmbedded();
				// Only dispose if the apply actually succeeded — keep the
				// window open so the user can retry on failure.
				if (!isDirty()) {
					dispose();
				}
				break;
			case JOptionPane.NO_OPTION:
				// Explicit discard.
				dispose();
				break;
			case JOptionPane.CANCEL_OPTION:
			case JOptionPane.CLOSED_OPTION:
			default:
				// Stay open — the user backed out of the close.
				break;
		}
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
	 * Accessor for the shared editor controller. Tree nodes reach this
	 * via {@code getCS().getEditorController()} inside their
	 * {@code getEditor()} overrides so each node can wire itself to the
	 * singleton editor that matches its content type.
	 */
	public NGCS2DEditorController getEditorController() {
		return editors;
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
		// Canvas display logic only. The right-side inspector is now
		// configured by NGCS2DEditorController.switchEditor(...) which
		// calls the matching editor's handleObject() — that's where
		// the Layers panel, Timeline, and play controls get their
		// content. We just decide what, if anything, paints on the
		// central canvas.
		if (content instanceof Sprite2DPalette) {
			// Per the inspector redesign: palettes are edited in the
			// right panel only. The main canvas does NOT show the
			// palette grid — it stays empty so the user's mental model
			// "canvas = sprite" holds.
			canvas.showNothing();
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
			canvas.showCellAnimation((Sprite2DCellAnimation) content);
			// Duration / current-frame setup is done by the CellAnimEditor's
			// handleObject() — switchEditor was already called by the tree
			// selection listener before we got here.
		} else if (content instanceof Sprite2DMultiCell) {
			canvas.showMultiCell((Sprite2DMultiCell) content);
			if (animControlPanel != null) {
				// Multi-cell static preview has no outer NMAR but each
				// entry's sub-NANR still needs to tick in real time.
				animControlPanel.setContinuousPlayback(true);
			}
		} else if (content instanceof Sprite2DMultiCellAnimation) {
			canvas.showMultiCellAnimation((Sprite2DMultiCellAnimation) content);
			// Same note as CellAnimation above.
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

	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(() -> {
			ComponentUtils.setSystemNativeLookAndFeel();
			NGCS2D cs = new NGCS2D();
			cs.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			cs.setVisible(true);
		});
	}
}
