package ctrmap.creativestudio.ngcs2d.io;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import xstandard.fs.FSFile;
import xstandard.gui.file.ExtensionFilter;

/**
 * A Default2DFormatHandler that only supports importing. Export calls will throw
 * UnsupportedOperationException. Mirrors DefaultG3DImportOnlyFormatHandler for the NGCS2D subsystem.
 */
public abstract class Default2DImportOnlyFormatHandler extends Default2DFormatHandler {

	public Default2DImportOnlyFormatHandler(ExtensionFilter filter, int extraAttributes) {
		super(filter, extraAttributes | S2DFMT_IMPORT);
	}

	public Default2DImportOnlyFormatHandler(ExtensionFilter filter) {
		this(filter, 0);
	}

	@Override
	public void exportResource(Sprite2DResource res, FSFile target, S2DIOProvider exData) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
