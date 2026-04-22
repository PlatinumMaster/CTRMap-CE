package ctrmap.creativestudio.ngcs2d.res;

import ctrmap.creativestudio.ngcs2d.layers.LayerItem;
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
	 * Returns only the entries whose {@link MultiCellEntry#visible} flag
	 * is set, preserving stack order. Parallels
	 * {@link Sprite2DCell#getVisibleOAMs()}: hidden entries are skipped
	 * by both the canvas preview and the NITRO export. Returns a fresh
	 * list — safe to mutate.
	 */
	public List<MultiCellEntry> getVisibleEntries() {
		List<MultiCellEntry> out = new ArrayList<>(entries.size());
		for (MultiCellEntry e : entries) {
			if (e != null && e.visible) {
				out.add(e);
			}
		}
		return out;
	}

	/**
	 * An entry in a multi-cell, referencing a cell animation
	 * and providing a position offset. Each entry is treated as a
	 * "layer" in the NGCS2D Layers panel — this is the granularity at
	 * which trainer / Pokemon sprites are authored (one entry per body
	 * part: head, torso, arm, etc.).
	 */
	public static class MultiCellEntry implements LayerItem {

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

		// ---------------------------------------------------------------------
		// Editor-only layer metadata. Parallel to Sprite2DOAM's fields — each
		// multi-cell entry is treated as a "layer" in the CS 2D layer panel
		// when a multi-cell is selected. Not serialised to NITRO; the NMCR
		// export drops hidden entries and ignores partial opacity.
		// ---------------------------------------------------------------------

		/** User-facing layer name; null = panel synthesises "Layer N". */
		public String layerName;

		/** Whether this entry renders and is exported. Default: {@code true}. */
		public boolean visible = true;

		/** Layer opacity (canvas-only; NITRO can't encode it). Default: 1.0. */
		public float opacity = 1.0f;

		/** UI-level edit guard. Default: {@code false}. */
		public boolean locked = false;

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

		/**
		 * Copy constructor. Deep-copies all fields including editor-only
		 * layer metadata.
		 */
		public MultiCellEntry(MultiCellEntry src) {
			this.animIndex = src.animIndex;
			this.x = src.x;
			this.y = src.y;
			this.layerName = src.layerName;
			this.visible = src.visible;
			this.opacity = src.opacity;
			this.locked = src.locked;
		}

		// --- LayerItem interface (field-backed, trivial) ---

		@Override public String getLayerName() { return layerName; }
		@Override public void setLayerName(String name) { this.layerName = name; }
		@Override public boolean isVisible() { return visible; }
		@Override public void setVisible(boolean visible) { this.visible = visible; }
		@Override public float getOpacity() { return opacity; }
		@Override public void setOpacity(float opacity) { this.opacity = opacity; }
		@Override public boolean isLocked() { return locked; }
		@Override public void setLocked(boolean locked) { this.locked = locked; }
	}
}
