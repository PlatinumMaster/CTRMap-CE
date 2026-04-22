package ctrmap.creativestudio.ngcs2d.tree;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;
import xstandard.util.ListenableList;

/**
 * Leaf node representing a {@link Sprite2DPalette} in the 2D sprite tree.
 */
public class CS2DPaletteNode extends CS2DNode {

	/**
	 * Icon resource ID for palette nodes.
	 */
	public static final int RESID = 0x420201;

	private Sprite2DPalette palette;

	/**
	 * Constructs a palette node.
	 *
	 * @param palette The palette resource to display.
	 * @param tree    The tree that owns this node.
	 */
	public CS2DPaletteNode(Sprite2DPalette palette, CS2DTree tree) {
		super(tree);
		this.palette = palette;
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return palette.getName();
	}

	@Override
	public Object getContent() {
		return palette;
	}

	@Override
	public void setContent(Object cnt) {
		palette = (Sprite2DPalette) cnt;
	}

	@Override
	public ListenableList getParentList() {
		return getCS().getResource().palettes;
	}

	@Override
	public IEditor getEditor() {
		return getCS().getEditorController().paletteEditor;
	}
}
