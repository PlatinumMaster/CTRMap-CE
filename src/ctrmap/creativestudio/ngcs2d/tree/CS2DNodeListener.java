package ctrmap.creativestudio.ngcs2d.tree;

import xstandard.util.ListenableList;
import javax.swing.tree.TreeNode;

/**
 * Listener that synchronizes a container node's children with changes
 * to the underlying {@link ListenableList}. When elements are added,
 * removed, or modified in the list, the corresponding tree nodes are
 * created, removed, or updated.
 *
 * <p>Mirrors {@code CSNodeListener} for 2D sprite resource nodes.</p>
 *
 * @param <T> The element type in the list.
 */
public abstract class CS2DNodeListener<T> implements ListenableList.ElementChangeListener {

	private final CS2DNode node;

	/**
	 * Constructs a listener bound to the given parent node.
	 *
	 * @param node The container node whose children will be managed.
	 */
	public CS2DNodeListener(CS2DNode node) {
		this.node = node;
	}

	/**
	 * Creates a new tree node for the given element.
	 *
	 * @param elem The element to create a node for.
	 * @return The new tree node.
	 */
	protected abstract CS2DNode createNode(T elem);

	@Override
	public void onEntityChange(ListenableList.ElementChangeEvent evt) {
		@SuppressWarnings("unchecked")
		T obj = (T) evt.element;

		int index = -1;
		for (int i = 0; i < node.getChildCount(); i++) {
			CS2DNode ch = (CS2DNode) node.getChildAt(i);
			if (ch.getContent() == obj) {
				index = i;
				break;
			}
		}
		if (index == -1 && evt.type == ListenableList.ElementChangeType.ADD) {
			index = Math.min(node.getChildCount(), evt.index);
		}
		if (index != -1) {
			switch (evt.type) {
				case ADD:
					node.addChild(index, createNode(obj));
					break;
				case REMOVE:
					node.removeChild((CS2DNode) node.getChildAt(index));
					break;
				case MODIFY:
					TreeNode child = node.getChildAt(index);
					if (child instanceof CS2DNode) {
						((CS2DNode) child).updateThis();
					}
					break;
			}
		}
	}
}
