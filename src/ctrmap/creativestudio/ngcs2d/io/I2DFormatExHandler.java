package ctrmap.creativestudio.ngcs2d.io;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import ctrmap.formats.common.FormatIOExConfig;
import xstandard.fs.FSFile;
import java.awt.Frame;

/**
 * Extended format handler interface for 2D sprite resources that supports additional
 * import/export configuration dialogs. Mirrors IG3DFormatExHandler for the NGCS2D subsystem.
 *
 * @param <I> The import configuration type.
 * @param <E> The export configuration type.
 */
public interface I2DFormatExHandler<I extends FormatIOExConfig, E extends FormatIOExConfig> extends I2DFormatHandler {

	/**
	 * Shows a dialog for configuring import options.
	 *
	 * @param parent The parent frame for the dialog.
	 * @return The import configuration, or null if cancelled.
	 */
	public I popupImportExConfigDialog(Frame parent);

	/**
	 * Shows a dialog for configuring export options.
	 *
	 * @param parent The parent frame for the dialog.
	 * @return The export configuration, or null if cancelled.
	 */
	public E popupExportExConfigDialog(Frame parent);

	/**
	 * Imports a 2D sprite resource using extended configuration.
	 *
	 * @param file   The file to import from.
	 * @param exData Supplementary data provider.
	 * @param config The import configuration.
	 * @return The imported Sprite2DResource.
	 */
	public Sprite2DResource importFileEx(FSFile file, S2DIOProvider exData, I config);

	/**
	 * Exports a 2D sprite resource using extended configuration.
	 *
	 * @param rsc    The resource to export.
	 * @param exData Supplementary data provider.
	 * @param target The target file to write to.
	 * @param config The export configuration.
	 */
	public void exportResourceEx(Sprite2DResource rsc, S2DIOProvider exData, FSFile target, E config);

	@Override
	public default Sprite2DResource importFile(FSFile fsf, S2DIOProvider exData) {
		return importFileEx(fsf, null, popupImportExConfigDialog(null));
	}

	@Override
	public default void exportResource(Sprite2DResource rsc, FSFile target, S2DIOProvider exData) {
		exportResourceEx(rsc, null, target, popupExportExConfigDialog(null));
	}
}
