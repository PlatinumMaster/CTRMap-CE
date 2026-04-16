package ctrmap.creativestudio.ngcs2d.tree;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DTileSheet;
import xstandard.util.ListenableList;

/**
 * Leaf node representing a {@link Sprite2DTileSheet} in the 2D sprite tree.
 */
public class CS2DTileSheetNode extends CS2DNode {

	/**
	 * Icon resource ID for tile sheet nodes.
	 */
	public static final int RESID = 0x420202;

	private Sprite2DTileSheet tileSheet;

	/**
	 * Constructs a tile sheet node.
	 *
	 * @param tileSheet The tile sheet resource to display.
	 * @param tree      The tree that owns this node.
	 */
	public CS2DTileSheetNode(Sprite2DTileSheet tileSheet, CS2DTree tree) {
		super(tree);
		this.tileSheet = tileSheet;
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return tileSheet.getName();
	}

	@Override
	public Object getContent() {
		return tileSheet;
	}

	@Override
	public void setContent(Object cnt) {
		tileSheet = (Sprite2DTileSheet) cnt;
	}

	@Override
	public ListenableList getParentList() {
		return getCS().getResource().tileSheets;
	}
}
