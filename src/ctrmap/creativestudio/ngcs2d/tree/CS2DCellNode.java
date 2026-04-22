package ctrmap.creativestudio.ngcs2d.tree;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;
import xstandard.util.ListenableList;

/**
 * Leaf node representing a {@link Sprite2DCell} in the 2D sprite tree.
 * Each cell node also creates child {@link CS2DOAMNode} entries for
 * the OAM objects that compose the cell.
 */
public class CS2DCellNode extends CS2DNode {

	/**
	 * Icon resource ID for cell nodes.
	 */
	public static final int RESID = 0x420203;

	private Sprite2DCell cell;

	/**
	 * Constructs a cell node and populates child OAM nodes.
	 *
	 * @param cell The cell resource to display.
	 * @param tree The tree that owns this node.
	 */
	public CS2DCellNode(Sprite2DCell cell, CS2DTree tree) {
		super(tree);
		this.cell = cell;

		for (int i = 0; i < cell.oams.size(); i++) {
			addChild(new CS2DOAMNode(cell.oams.get(i), i, tree));
		}
	}

	/**
	 * Rebuilds OAM child nodes to reflect the current state of the cell.
	 */
	public void rebuildOAMChildren() {
		removeAllChildren();
		for (int i = 0; i < cell.oams.size(); i++) {
			addChild(new CS2DOAMNode(cell.oams.get(i), i, tree));
		}
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return cell.getName();
	}

	@Override
	public Object getContent() {
		return cell;
	}

	@Override
	public void setContent(Object cnt) {
		cell = (Sprite2DCell) cnt;
	}

	@Override
	public ListenableList getParentList() {
		return getCS().getResource().cells;
	}

	@Override
	public IEditor getEditor() {
		return getCS().getEditorController().cellEditor;
	}
}
