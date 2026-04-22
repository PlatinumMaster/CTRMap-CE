package ctrmap.creativestudio.ngcs2d.tree;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCellAnimation;
import xstandard.util.ListenableList;

/**
 * Leaf node representing a {@link Sprite2DCellAnimation} in the 2D sprite tree.
 */
public class CS2DCellAnimNode extends CS2DNode {

	/**
	 * Icon resource ID for cell animation nodes.
	 */
	public static final int RESID = 0x420205;

	private Sprite2DCellAnimation cellAnim;

	/**
	 * Constructs a cell animation node.
	 *
	 * @param cellAnim The cell animation resource to display.
	 * @param tree     The tree that owns this node.
	 */
	public CS2DCellAnimNode(Sprite2DCellAnimation cellAnim, CS2DTree tree) {
		super(tree);
		this.cellAnim = cellAnim;
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return cellAnim.getName();
	}

	@Override
	public Object getContent() {
		return cellAnim;
	}

	@Override
	public void setContent(Object cnt) {
		cellAnim = (Sprite2DCellAnimation) cnt;
	}

	@Override
	public ListenableList getParentList() {
		return getCS().getResource().cellAnimations;
	}

	@Override
	public IEditor getEditor() {
		return getCS().getEditorController().cellAnimEditor;
	}
}
