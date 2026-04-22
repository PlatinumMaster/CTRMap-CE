package ctrmap.creativestudio.ngcs2d.tree;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCell;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DOAM;
import xstandard.util.ListenableList;

/**
 * Leaf node representing a {@link Sprite2DOAM} entry within a cell.
 * OAM entries do not implement {@code INamed}, so {@link #getNodeName()}
 * returns a descriptive label based on the OAM index and dimensions.
 */
public class CS2DOAMNode extends CS2DNode {

	/**
	 * Icon resource ID for OAM nodes.
	 */
	public static final int RESID = 0x420204;

	private Sprite2DOAM oam;
	private int index;

	/**
	 * Constructs an OAM node.
	 *
	 * @param oam   The OAM entry to display.
	 * @param index The index of this OAM within its parent cell.
	 * @param tree  The tree that owns this node.
	 */
	public CS2DOAMNode(Sprite2DOAM oam, int index, CS2DTree tree) {
		super(tree);
		this.oam = oam;
		this.index = index;
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return "OAM " + index + " (" + oam.width + "x" + oam.height + ")";
	}

	@Override
	public Object getContent() {
		return oam;
	}

	@Override
	public void setContent(Object cnt) {
		oam = (Sprite2DOAM) cnt;
	}

	@Override
	public ListenableList getParentList() {
		// OAMs live in a plain List, not a ListenableList.
		// Removal is handled by the parent CS2DCellNode.
		return new ListenableList();
	}

	@Override
	public void callRemove() {
		CS2DNode parentNode = (CS2DNode) getParent();
		if (parentNode instanceof CS2DCellNode) {
			Sprite2DCell cell = (Sprite2DCell) parentNode.getContent();
			cell.oams.remove(oam);
			((CS2DCellNode) parentNode).rebuildOAMChildren();
		}
	}

	@Override
	public IEditor getEditor() {
		return getCS().getEditorController().oamEditor;
	}
}
