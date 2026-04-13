package ctrmap.creativestudio.ngcs2d.io;

import ctrmap.creativestudio.ngcs.io.FormatDetectorInput;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import xstandard.fs.FSFile;
import xstandard.gui.file.ExtensionFilter;

/**
 * Format handler interface for 2D sprite resources. Mirrors IG3DFormatHandler
 * for the NGCS2D subsystem.
 */
public interface I2DFormatHandler {

	public static final int S2DFMT_IMPORT = (1 << 0);
	public static final int S2DFMT_IMPORT_HAS_EXCONFIG = (1 << 1);
	public static final int S2DFMT_IMPORT_EXDATA_NEEDS_PALETTE = (1 << 2);
	public static final int S2DFMT_IMPORT_EXDATA_NEEDS_TILESHEET = (1 << 3);

	public static final int S2DFMT_EXPORT = (1 << 16);
	public static final int S2DFMT_EXPORT_HAS_EXCONFIG = (1 << 17);
	public static final int S2DFMT_EXPORT_EXDATA_NEEDS_PALETTE = (1 << 18);
	public static final int S2DFMT_EXPORT_EXDATA_NEEDS_TILESHEET = (1 << 19);

	/**
	 * Finds a handler whose extension filter matches the given filter.
	 *
	 * @param filter           The extension filter to search for.
	 * @param needCheckAttrib  Attribute flags that the handler must satisfy, or 0 for any.
	 * @param handlers         The handlers to search through.
	 * @return The first matching handler, or null if none match.
	 */
	public static I2DFormatHandler findByFilter(ExtensionFilter filter, int needCheckAttrib, I2DFormatHandler... handlers) {
		for (I2DFormatHandler h : handlers) {
			if (needCheckAttrib == 0 || h.checkAttribute(needCheckAttrib)) {
				if (h.getExtensionFilter() == filter) {
					return h;
				}
			}
		}
		return null;
	}

	/**
	 * Gets the capability attribute flags for this handler.
	 *
	 * @return Bitmask of S2DFMT_* attribute flags.
	 */
	public int getAttributes();

	/**
	 * Checks whether the handler has the given attribute flag set.
	 *
	 * @param attr The attribute flag to check.
	 * @return True if the attribute is set.
	 */
	public default boolean checkAttribute(int attr) {
		return (getAttributes() & attr) != 0;
	}

	/**
	 * Checks whether this handler supports importing.
	 *
	 * @return True if importing is supported.
	 */
	public default boolean canImport() {
		return checkAttribute(S2DFMT_IMPORT);
	}

	/**
	 * Checks whether this handler supports exporting.
	 *
	 * @return True if exporting is supported.
	 */
	public default boolean canExport() {
		return checkAttribute(S2DFMT_EXPORT);
	}

	/**
	 * Detects whether the input file matches this format based on its file extension.
	 *
	 * @param input The format detector input containing the file name.
	 * @return True if the extension matches this handler's filter.
	 */
	public default boolean detectByExtension(FormatDetectorInput input) {
		boolean accept = getExtensionFilter().accepts(input.fileName);
		return accept;
	}

	/**
	 * Detects whether the input file matches this format by inspecting its internal data.
	 *
	 * @param input The format detector input containing the file header and stream.
	 * @return True if the file internals match this format.
	 */
	public boolean detectInternals(FormatDetectorInput input);

	/**
	 * Gets the extension filter for files handled by this format handler.
	 *
	 * @return The extension filter.
	 */
	public ExtensionFilter getExtensionFilter();

	/**
	 * Imports a 2D sprite resource from the given file.
	 *
	 * @param fsf    The file to import from.
	 * @param exData Supplementary data provider for the import operation.
	 * @return The imported Sprite2DResource.
	 */
	public Sprite2DResource importFile(FSFile fsf, S2DIOProvider exData);

	/**
	 * Exports a 2D sprite resource to the given file.
	 *
	 * @param res    The resource to export.
	 * @param target The target file to write to.
	 * @param exData Supplementary data provider for the export operation.
	 */
	public void exportResource(Sprite2DResource res, FSFile target, S2DIOProvider exData);
}
