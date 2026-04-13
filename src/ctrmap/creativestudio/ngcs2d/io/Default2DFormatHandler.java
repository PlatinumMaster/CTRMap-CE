package ctrmap.creativestudio.ngcs2d.io;

import xstandard.gui.file.ExtensionFilter;

/**
 * Base implementation of I2DFormatHandler that stores an extension filter and attribute flags.
 * Mirrors DefaultG3DFormatHandler for the NGCS2D subsystem.
 */
public abstract class Default2DFormatHandler implements I2DFormatHandler {

	private ExtensionFilter filter;
	private int attributes;

	public Default2DFormatHandler(ExtensionFilter filter, int attributes) {
		this.filter = filter;
		this.attributes = attributes;
	}

	public Default2DFormatHandler(ExtensionFilter filter) {
		this(filter, S2DFMT_IMPORT | S2DFMT_EXPORT);
	}

	@Override
	public int getAttributes() {
		return attributes;
	}

	@Override
	public ExtensionFilter getExtensionFilter() {
		return filter;
	}
}
