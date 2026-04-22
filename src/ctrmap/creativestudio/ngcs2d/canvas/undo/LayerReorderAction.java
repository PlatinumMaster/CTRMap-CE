package ctrmap.creativestudio.ngcs2d.canvas.undo;

import java.util.List;

/**
 * Undoable action for moving an entry up or down within a layer stack.
 *
 * <p>Works against any {@code List<T>} — the Layers panel passes
 * {@code cell.oams} when editing cell-level layers and
 * {@code multiCell.entries} when editing multi-cell entries. The list
 * is mutated in place; no element copying.</p>
 */
public class LayerReorderAction<T> implements SpriteAction {

	private final List<T> list;
	private final int from;
	private final int to;

	public LayerReorderAction(List<T> list, int from, int to) {
		this.list = list;
		this.from = from;
		this.to = to;
	}

	@Override
	public void execute() {
		move(from, to);
	}

	@Override
	public void undo() {
		move(to, from);
	}

	private void move(int src, int dst) {
		if (list == null) return;
		int n = list.size();
		if (src < 0 || src >= n || dst < 0 || dst >= n || src == dst) return;
		T e = list.remove(src);
		list.add(dst, e);
	}

	@Override
	public String getDescription() {
		return "Reorder layer " + from + " -> " + to;
	}
}
