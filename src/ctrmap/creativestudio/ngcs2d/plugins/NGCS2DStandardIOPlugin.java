package ctrmap.creativestudio.ngcs2d.plugins;

import ctrmap.creativestudio.ngcs2d.rtldr.INGCS2DPlugin;
import ctrmap.creativestudio.ngcs2d.rtldr.NGCS2DJulietIface;

/**
 * Built-in I/O plugin for the NGCS2D sprite editor. Placeholder for standard
 * PNG import/export that will be registered in a later phase.
 */
public class NGCS2DStandardIOPlugin implements INGCS2DPlugin {

	@Override
	public void registerFormats(NGCS2DJulietIface j) {
		//Standard format handlers (PNG, etc.) will be registered here.
	}
}
