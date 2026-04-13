package ctrmap.creativestudio.ngcs2d.rtldr;

import ctrmap.creativestudio.ngcs2d.io.CS2DIOContentType;
import ctrmap.creativestudio.ngcs2d.io.I2DFormatHandler;
import xstandard.gui.file.ExtensionFilter;
import xstandard.util.ArraysEx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton manager for 2D sprite I/O format handlers. Maintains a mapping from
 * content types to their associated format handlers. Mirrors NGCSIOManager for the 3D system.
 */
public class NGCS2DIOManager {

	private static NGCS2DIOManager INSTANCE;

	private Map<CS2DIOContentType, List<I2DFormatHandler>> formatHandlers = new HashMap<>();

	private NGCS2DIOManager() {
		for (CS2DIOContentType t : CS2DIOContentType.values()) {
			formatHandlers.put(t, new ArrayList<>());
		}
	}

	/**
	 * Gets the singleton instance of the I/O manager.
	 *
	 * @return The singleton instance.
	 */
	public static NGCS2DIOManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new NGCS2DIOManager();
		}
		return INSTANCE;
	}

	/**
	 * Registers a format handler for the given content types.
	 *
	 * @param h     The format handler to register.
	 * @param types The content types that this handler supports.
	 */
	public void registHandler(I2DFormatHandler h, CS2DIOContentType... types) {
		if (h != null && types != null) {
			for (CS2DIOContentType t : types) {
				if (t != null) {
					ArraysEx.addIfNotNullOrContains(formatHandlers.get(t), h);
				}
			}
		}
	}

	/**
	 * Unregisters a format handler from all content types.
	 *
	 * @param h The format handler to remove.
	 */
	public void unregistHandler(I2DFormatHandler h) {
		if (h != null) {
			for (List<I2DFormatHandler> l : formatHandlers.values()) {
				l.remove(h);
			}
		}
	}

	/**
	 * Gets all registered format handlers across all content types, deduplicated
	 * and ordered by content type enumeration order. Handlers for the UNKNOWN type
	 * are excluded.
	 *
	 * @return An array of all unique format handlers.
	 */
	public I2DFormatHandler[] getAllFormatHandlers() {
		List<I2DFormatHandler> out = new ArrayList<>();
		List<ExtensionFilter> efList = new ArrayList<>();

		for (CS2DIOContentType t : CS2DIOContentType.values()) {
			if (t != CS2DIOContentType.UNKNOWN) {
				//in order
				List<I2DFormatHandler> l = formatHandlers.get(t);
				for (I2DFormatHandler h : l) {
					if (!out.contains(h)) {
						ExtensionFilter filter = h.getExtensionFilter();
						if (!efList.contains(filter)) {
							out.add(h);
							efList.add(filter);
						}
					}
				}
			}
		}
		return out.toArray(new I2DFormatHandler[out.size()]);
	}

	/**
	 * Gets format handlers registered for a specific content type. If the type is
	 * null, returns all handlers.
	 *
	 * @param type The content type to query, or null for all handlers.
	 * @return An array of matching format handlers.
	 */
	public I2DFormatHandler[] getFormatHandlers(CS2DIOContentType type) {
		if (type == null) {
			return getAllFormatHandlers();
		} else {
			return formatHandlers.get(type).toArray(new I2DFormatHandler[0]);
		}
	}
}
