package ctrmap.creativestudio.ngcs2d.tree;

import ctrmap.creativestudio.editors.IEditor;
import ctrmap.creativestudio.ngcs2d.NGCS2D;
import xstandard.gui.components.tree.CustomJTreeNode;
import xstandard.util.ListenableList;
import java.util.ArrayList;
import java.util.List;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * Abstract base class for all nodes in the 2D sprite resource tree.
 * Simplified mirror of {@code CSNode} that operates on 2D sprite
 * resources rather than 3D scene graph resources.
 *
 * <p>Unlike {@code CSNode}, this class uses {@link Object} for content
 * rather than {@code NamedResource}, since not all 2D resource types
 * (e.g. {@code Sprite2DOAM}) implement {@code INamed}.</p>
 */
public abstract class CS2DNode extends CustomJTreeNode {

	/**
	 * The tree that owns this node.
	 */
	protected final CS2DTree tree;

	private List<CS2DNodeAction> actions = new ArrayList<>();

	/**
	 * Constructs a new 2D tree node. Non-root nodes automatically
	 * register a "Remove" action.
	 *
	 * @param tree The tree that owns this node.
	 */
	public CS2DNode(CS2DTree tree) {
		super();
		this.tree = tree;
		if (getAllowRemoveAction()) {
			registerAction("Remove", this::callRemove);
		}
	}

	/**
	 * Returns whether this node should have a default Remove action.
	 * Subclasses like container and root nodes override to return false.
	 *
	 * @return True if the Remove action should be registered.
	 */
	protected boolean getAllowRemoveAction() {
		return true;
	}

	/**
	 * Returns the editor for this node's content. Returns null by default;
	 * subclasses override to provide a specific editor.
	 *
	 * @return The editor, or null if no editor is available.
	 */
	public IEditor getEditor() {
		return null;
	}

	/**
	 * Returns the NGCS2D controller that owns this tree.
	 *
	 * @return The NGCS2D instance.
	 */
	public NGCS2D getCS() {
		return tree.getCS();
	}

	/**
	 * Returns the content object represented by this node.
	 *
	 * @return The content object, or null for root/container nodes.
	 */
	public abstract Object getContent();

	/**
	 * Sets the content object of this node.
	 *
	 * @param cnt The new content object.
	 */
	public abstract void setContent(Object cnt);

	/**
	 * Returns the parent list that contains this node's content.
	 * Used by the Remove action to remove the content from its owner.
	 *
	 * @return The parent list.
	 */
	public abstract ListenableList getParentList();

	/**
	 * Removes this node's content from its parent list.
	 */
	public void callRemove() {
		ListenableList parentList = getParentList();
		Object content = getContent();
		if (parentList != null && content != null) {
			parentList.remove(content);
		}
	}

	/**
	 * Registers an action button for this node.
	 *
	 * @param name   The display name of the action.
	 * @param action The callback to execute.
	 */
	public final void registerAction(String name, Runnable action) {
		actions.add(new CS2DNodeAction(name, action));
	}

	/**
	 * Returns the list of actions registered on this node.
	 *
	 * @return The action list.
	 */
	public List<CS2DNodeAction> getActions() {
		return actions;
	}

	/**
	 * Adds a child node to this node.
	 *
	 * @param ch The child node to add.
	 */
	public void addChild(CS2DNode ch) {
		addChild(getChildCount(), ch);
	}

	/**
	 * Adds a child node at the specified index.
	 *
	 * @param index The insertion index.
	 * @param ch    The child node to add.
	 */
	public void addChild(int index, CS2DNode ch) {
		tree.getModel().insertNodeInto(ch, this, index);
	}

	/**
	 * Removes a child node from this node.
	 *
	 * @param ch The child node to remove.
	 */
	public void removeChild(CS2DNode ch) {
		tree.getModel().removeNodeFromParent(ch);
	}

	/**
	 * Removes all child nodes from this node.
	 */
	@Override
	public void removeAllChildren() {
		while (getChildCount() > 0) {
			removeChild((CS2DNode) getChildAt(0));
		}
	}

	/**
	 * Notifies the tree model that this node has changed, causing
	 * a visual update.
	 */
	public void updateThis() {
		tree.getModel().nodeChanged(this);
		updateCellUI();
	}

	/**
	 * Sets the expansion state of this node in the tree.
	 *
	 * @param state True to expand, false to collapse.
	 */
	public void setExpansionState(boolean state) {
		TreeNode[] path = getPath();
		TreePath tp = new TreePath(path);
		if (state) {
			tree.expandPath(tp);
		} else {
			tree.collapsePath(tp);
		}
	}

	/**
	 * An action that can be performed on a tree node, displayed
	 * as a button in the node's context area.
	 */
	public static class CS2DNodeAction {

		/**
		 * Display name of the action.
		 */
		public String name;

		/**
		 * Callback to execute when the action is triggered.
		 */
		public Runnable callback;

		private CS2DNodeAction(String actionButtonName, Runnable action) {
			name = actionButtonName;
			callback = action;
		}
	}
}
