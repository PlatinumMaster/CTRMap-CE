package ctrmap.creativestudio.ngcs2d.tree;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;
import xstandard.util.ListenableList;

/**
 * Leaf node representing a {@link Sprite2DMultiCellAnimation} in the 2D sprite tree.
 */
public class CS2DMultiCellAnimNode extends CS2DNode {

	/**
	 * Icon resource ID for multi-cell animation nodes.
	 */
	public static final int RESID = 0x420207;

	private Sprite2DMultiCellAnimation multiCellAnim;

	/**
	 * Constructs a multi-cell animation node.
	 *
	 * @param multiCellAnim The multi-cell animation resource to display.
	 * @param tree          The tree that owns this node.
	 */
	public CS2DMultiCellAnimNode(Sprite2DMultiCellAnimation multiCellAnim, CS2DTree tree) {
		super(tree);
		this.multiCellAnim = multiCellAnim;
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return multiCellAnim.getName();
	}

	@Override
	public Object getContent() {
		return multiCellAnim;
	}

	@Override
	public void setContent(Object cnt) {
		multiCellAnim = (Sprite2DMultiCellAnimation) cnt;
	}

	@Override
	public ListenableList getParentList() {
		return getCS().getResource().multiCellAnimations;
	}

	@Override
	public IEditor getEditor() {
		return getCS().getEditorController().multiCellAnimEditor;
	}
}
