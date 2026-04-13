package ctrmap.creativestudio.ngcs2d.tree;

import xstandard.INamed;
import xstandard.util.ListenableList;
import java.util.function.Supplier;

/**
 * Container node for a list of 2D sprite resources. Simplified mirror of
 * {@code ContainerNode} that provides "Add" and "Clear" actions but cannot
 * itself be removed from the tree.
 *
 * <p>Each container wraps a {@link ListenableList} and uses a
 * {@link Supplier} to create new default instances when the user
 * triggers the "Add" action.</p>
 */
public class CS2DContainerNode extends CS2DNode {

	/**
	 * Default icon resource ID for container nodes.
	 */
	public static final int RESID = 1;

	private final String name;
	private final ListenableList list;
	private final Supplier<? extends INamed> factory;

	/**
	 * Constructs a container node.
	 *
	 * @param name    The display name of this container (e.g. "Palettes").
	 * @param list    The backing list of resources.
	 * @param factory A supplier that creates new default instances for the "Add" action.
	 * @param tree    The tree that owns this node.
	 */
	public CS2DContainerNode(String name, ListenableList list, Supplier<? extends INamed> factory, CS2DTree tree) {
		super(tree);
		this.name = name;
		this.list = list;
		this.factory = factory;

		registerAction("Add", this::callAdd);
		registerAction("Clear", this::callClear);
	}

	@Override
	protected boolean getAllowRemoveAction() {
		return false;
	}

	private void callAdd() {
		INamed newElem = factory.get();
		if (newElem != null) {
			list.add(newElem);
			setExpansionState(true);
		}
	}

	private void callClear() {
		list.clear();
	}

	@Override
	public int getIconResourceID() {
		return RESID;
	}

	@Override
	public String getNodeName() {
		return name;
	}

	@Override
	public Object getContent() {
		return null;
	}

	@Override
	public ListenableList getParentList() {
		return new ListenableList();
	}

	@Override
	public void setContent(Object cnt) {
		// no-op: container nodes have no settable content
	}

	@Override
	public void callRemove() {
		// no-op: container nodes cannot be removed
	}
}
