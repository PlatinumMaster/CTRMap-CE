package ctrmap.creativestudio.ngcs2d.res;

import java.util.ArrayList;
import java.util.List;
import xstandard.INamed;

/**
 * A game-agnostic 2D sprite cell. A cell is a composition of one or more
 * OAM entries that together form a single displayable sprite image.
 */
public class Sprite2DCell implements INamed {

	/**
	 * Display name of this cell.
	 */
	public String name;

	/**
	 * The list of OAM entries that compose this cell.
	 */
	public List<Sprite2DOAM> oams;

	/**
	 * Optional CHAR VRAM transfer source address (in bytes within the NCGR
	 * pixel data). When {@link #hasVramTransfer} is true, each OAM's
	 * {@code tileIndex} is a byte-offset past this base rather than an
	 * absolute tile index. The renderer adds {@code vramTransferSrcAddr /
	 * bytesPerTile} to each OAM tile index to get the real NCGR tile.
	 * Pokemon B/W battle sprites rely on this heavily.
	 */
	public int vramTransferSrcAddr;

	/**
	 * Optional CHAR VRAM transfer window size (in bytes). Tiles whose
	 * addresses fall outside {@code [0, size)} bypass the remap and read
	 * directly from the NCGR, matching NitroPaint's ChrGetChar fallback.
	 */
	public int vramTransferSize;

	/** True when this cell has a populated VRAM transfer entry. */
	public boolean hasVramTransfer;

	/**
	 * Constructs a default cell named "Cell" with an empty OAM list.
	 */
	public Sprite2DCell() {
		name = "Cell";
		oams = new ArrayList<>();
	}

	/**
	 * Constructs a cell with the given name and an empty OAM list.
	 *
	 * @param name Display name of the cell.
	 */
	public Sprite2DCell(String name) {
		this.name = name;
		oams = new ArrayList<>();
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
	 * Returns the number of OAM entries in this cell.
	 *
	 * @return The OAM count.
	 */
	public int getOAMCount() {
		return oams.size();
	}

	/**
	 * Adds an OAM entry to this cell.
	 *
	 * @param oam The OAM entry to add. Ignored if null.
	 */
	public void addOAM(Sprite2DOAM oam) {
		if (oam != null) {
			oams.add(oam);
		}
	}

	/**
	 * Removes the OAM entry at the given index.
	 *
	 * @param index The index of the OAM entry to remove.
	 */
	public void removeOAM(int index) {
		if (index >= 0 && index < oams.size()) {
			oams.remove(index);
		}
	}

	/**
	 * Returns only the OAMs whose {@link Sprite2DOAM#visible} flag is set,
	 * preserving stack order. Used by both the canvas preview (to skip
	 * hidden layers) and the NITRO export (to omit them from the NCER).
	 * Returns a fresh list — callers may mutate it without affecting the
	 * cell's full OAM list.
	 */
	public List<Sprite2DOAM> getVisibleOAMs() {
		List<Sprite2DOAM> out = new ArrayList<>(oams.size());
		for (Sprite2DOAM o : oams) {
			if (o != null && o.visible) {
				out.add(o);
			}
		}
		return out;
	}
}
