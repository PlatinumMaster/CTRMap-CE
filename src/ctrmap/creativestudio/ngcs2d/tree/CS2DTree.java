package ctrmap.creativestudio.ngcs2d.tree;

import ctrmap.creativestudio.ngcs2d.NGCS2D;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import xstandard.gui.components.tree.CustomJTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

/**
 * JTree subclass for displaying a 2D sprite resource hierarchy.
 * Mirrors {@code CSJTree} but operates on {@link Sprite2DResource}
 * instead of {@code G3DResource}.
 */
public class CS2DTree extends CustomJTree {

	private NGCS2D cs;

	/**
	 * Constructs a new 2D tree.
	 */
	public CS2DTree() {
		super();
	}

	/**
	 * Returns the NGCS2D controller that owns this tree.
	 *
	 * @return The NGCS2D instance.
	 */
	public NGCS2D getCS() {
		return cs;
	}

	/**
	 * Returns the tree model as a {@link DefaultTreeModel}.
	 *
	 * @return The tree model.
	 */
	@Override
	public DefaultTreeModel getModel() {
		return model;
	}

	/**
	 * Initializes this tree with the given 2D controller and sprite resource.
	 * Clears any existing children and creates the root node hierarchy.
	 *
	 * @param cs       The NGCS2D controller.
	 * @param resource The sprite resource to display.
	 */
	public void initTree(NGCS2D cs, Sprite2DResource resource) {
		this.cs = cs;

		registerIconResource(CS2DContainerNode.RESID, "directory_node");
		registerIconResource(CS2DRootNode.RESID, "scene");
		registerIconResource(CS2DPaletteNode.RESID, "image");
		registerIconResource(CS2DTileSheetNode.RESID, "texture");
		registerIconResource(CS2DCellNode.RESID, "model");
		registerIconResource(CS2DOAMNode.RESID, "mesh");
		registerIconResource(CS2DCellAnimNode.RESID, "anime_skl");
		registerIconResource(CS2DMultiCellNode.RESID, "model");
		registerIconResource(CS2DMultiCellAnimNode.RESID, "anime_skl");

		while (root.getChildCount() > 0) {
			model.removeNodeFromParent((MutableTreeNode) root.getChildAt(0));
		}

		CS2DRootNode rootNode = new CS2DRootNode(resource, this);
		model.insertNodeInto(rootNode, root, 0);
		model.reload();

		rootNode.setExpansionState(true);
	}

	/**
	 * Returns the root CS2D node.
	 *
	 * @return The root node, or null if the tree has not been initialized.
	 */
	public CS2DRootNode getRootCSNode() {
		if (root.getChildCount() == 0) {
			return null;
		}
		return (CS2DRootNode) root.getChildAt(0);
	}

	private void registerIconResource(int resID, String name) {
		registerIconResourceImpl(resID, "ctrmap/resources/cs/data/" + name + ".png");
	}
}
