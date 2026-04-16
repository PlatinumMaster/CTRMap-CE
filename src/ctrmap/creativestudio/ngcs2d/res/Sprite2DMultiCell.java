package ctrmap.creativestudio.ngcs2d.res;

import java.util.ArrayList;
import java.util.List;
import xstandard.INamed;

/**
 * A game-agnostic multi-cell. Composes multiple cell animations at specific
 * position offsets, allowing complex sprite compositions built from
 * independently animated parts.
 */
public class Sprite2DMultiCell implements INamed {

	/**
	 * Display name of this multi-cell.
	 */
	public String name;

	/**
	 * The list of entries, each referencing a cell animation at a position.
	 */
	public List<MultiCellEntry> entries;

	/**
	 * Constructs a default multi-cell named "MultiCell" with an empty entry list.
	 */
	public Sprite2DMultiCell() {
		name = "MultiCell";
		entries = new ArrayList<>();
	}

	/**
	 * Constructs a multi-cell with the given name and an empty entry list.
	 *
	 * @param name Display name of the multi-cell.
	 */
	public Sprite2DMultiCell(String name) {
		this.name = name;
		entries = new ArrayList<>();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the number of entries in this multi-cell.
	 *
	 * @return The entry count.
	 */
	public int getEntryCount() {
		return entries.size();
	}

	/**
	 * An entry in a multi-cell, referencing a cell animation
	 * and providing a position offset.
	 */
	public static class MultiCellEntry {

		/**
		 * Index into the cell animation list.
		 */
		public int animIndex;

		/**
		 * Pixel offset X for this entry.
		 */
		public short x;

		/**
		 * Pixel offset Y for this entry.
		 */
		public short y;

		/**
		 * Constructs a default multi-cell entry.
		 */
		public MultiCellEntry() {
		}

		/**
		 * Constructs a multi-cell entry with the given animation index
		 * and position offsets.
		 *
		 * @param animIndex Index into the cell animation list.
		 * @param x         Pixel offset X.
		 * @param y         Pixel offset Y.
		 */
		public MultiCellEntry(int animIndex, short x, short y) {
			this.animIndex = animIndex;
			this.x = x;
			this.y = y;
		}
	}
}
