package ctrmap.creativestudio.ngcs2d.tree;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCell;
import xstandard.util.ListenableList;

/**
 * Leaf node representing a {@link Sprite2DMultiCell} in the 2D sprite tree.
 */
public class CS2DMultiCellNode extends CS2DNode {

	/**
	 * Icon resource ID for multi-cell nodes.
	 */
	public static final int RESID = 0x420206;

	private Sprite2DMultiCell multiCell;

	/**
	 * Constructs a multi-cell node.
	 *
	 * @param multiCell The multi-cell resource to display.
	 * @param tree      The tree that owns this node.
	 */
	public CS2DMultiCellNode(Sprite2DMultiCell multiCell, CS2DTree tree) {
		super(tree);
		this.multiCell = multiCell;
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return multiCell.getName();
	}

	@Override
	public Object getContent() {
		return multiCell;
	}

	@Override
	public void setContent(Object cnt) {
		multiCell = (Sprite2DMultiCell) cnt;
	}

	@Override
	public ListenableList getParentList() {
		return getCS().getResource().multiCells;
	}
}
