package ctrmap.creativestudio.ngcs2d.tree;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import xstandard.util.ListenableList;

/**
 * Root node of the 2D sprite resource tree. Creates container children
 * for each resource type (palettes, tile sheets, cells, cell animations,
 * multi-cells, and multi-cell animations) and registers listeners to
 * keep the tree synchronized with the underlying data model.
 *
 * <p>Mirrors {@code CSRootSceneNode} but for 2D sprite resources.</p>
 */
public class CS2DRootNode extends CS2DNode {

	/**
	 * Icon resource ID for the root node.
	 */
	public static final int RESID = 0x420002;

	private final Sprite2DResource resource;

	/**
	 * Constructs the root node and populates it with container children
	 * for the given sprite resource.
	 *
	 * @param resource The sprite resource to display.
	 * @param tree     The tree that owns this node.
	 */
	public CS2DRootNode(Sprite2DResource resource, CS2DTree tree) {
		super(tree);
		this.resource = resource;

		CS2DContainerNode palettes = new CS2DContainerNode(
			"Palettes", resource.palettes, Sprite2DPalette::new, tree
		);
		addChild(palettes);

		CS2DContainerNode tileSheets = new CS2DContainerNode(
			"Tile Sheets", resource.tileSheets, Sprite2DTileSheet::new, tree
		);
		addChild(tileSheets);

		CS2DContainerNode cells = new CS2DContainerNode(
			"Cells", resource.cells, Sprite2DCell::new, tree
		);
		addChild(cells);

		CS2DContainerNode cellAnimations = new CS2DContainerNode(
			"Cell Animations", resource.cellAnimations, Sprite2DCellAnimation::new, tree
		);
		addChild(cellAnimations);

		CS2DContainerNode multiCells = new CS2DContainerNode(
			"Multi-Cells", resource.multiCells, Sprite2DMultiCell::new, tree
		);
		addChild(multiCells);

		CS2DContainerNode multiCellAnimations = new CS2DContainerNode(
			"Multi-Cell Animations", resource.multiCellAnimations, Sprite2DMultiCellAnimation::new, tree
		);
		addChild(multiCellAnimations);

		// Populate existing elements
		for (Sprite2DPalette p : resource.palettes) {
			palettes.addChild(new CS2DPaletteNode(p, tree));
		}

		for (Sprite2DTileSheet t : resource.tileSheets) {
			tileSheets.addChild(new CS2DTileSheetNode(t, tree));
		}

		for (Sprite2DCell c : resource.cells) {
			cells.addChild(new CS2DCellNode(c, tree));
		}

		for (Sprite2DCellAnimation a : resource.cellAnimations) {
			cellAnimations.addChild(new CS2DCellAnimNode(a, tree));
		}

		for (Sprite2DMultiCell mc : resource.multiCells) {
			multiCells.addChild(new CS2DMultiCellNode(mc, tree));
		}

		for (Sprite2DMultiCellAnimation mca : resource.multiCellAnimations) {
			multiCellAnimations.addChild(new CS2DMultiCellAnimNode(mca, tree));
		}

		// Register listeners to keep tree in sync with data model
		resource.palettes.addListener(new CS2DNodeListener<Sprite2DPalette>(palettes) {
			@Override
			protected CS2DNode createNode(Sprite2DPalette elem) {
				return new CS2DPaletteNode(elem, tree);
			}
		});

		resource.tileSheets.addListener(new CS2DNodeListener<Sprite2DTileSheet>(tileSheets) {
			@Override
			protected CS2DNode createNode(Sprite2DTileSheet elem) {
				return new CS2DTileSheetNode(elem, tree);
			}
		});

		resource.cells.addListener(new CS2DNodeListener<Sprite2DCell>(cells) {
			@Override
			protected CS2DNode createNode(Sprite2DCell elem) {
				return new CS2DCellNode(elem, tree);
			}
		});

		resource.cellAnimations.addListener(new CS2DNodeListener<Sprite2DCellAnimation>(cellAnimations) {
			@Override
			protected CS2DNode createNode(Sprite2DCellAnimation elem) {
				return new CS2DCellAnimNode(elem, tree);
			}
		});

		resource.multiCells.addListener(new CS2DNodeListener<Sprite2DMultiCell>(multiCells) {
			@Override
			protected CS2DNode createNode(Sprite2DMultiCell elem) {
				return new CS2DMultiCellNode(elem, tree);
			}
		});

		resource.multiCellAnimations.addListener(new CS2DNodeListener<Sprite2DMultiCellAnimation>(multiCellAnimations) {
			@Override
			protected CS2DNode createNode(Sprite2DMultiCellAnimation elem) {
				return new CS2DMultiCellAnimNode(elem, tree);
			}
		});
	}

	@Override
	protected boolean getAllowRemoveAction() {
		return false;
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return "Sprite Resource";
	}

	@Override
	public Object getContent() {
		return resource;
	}

	@Override
	public ListenableList getParentList() {
		return new ListenableList();
	}

	@Override
	public void setContent(Object cnt) {
		// no-op: root node content is fixed
	}

	@Override
	public void callRemove() {
		// no-op: root node cannot be removed
	}
}
