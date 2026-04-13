
package ctrmap.creativestudio.ngcs2d.rtldr;

import ctrmap.creativestudio.ngcs2d.NGCS2D;

/**
 * Static helper for notifying the NGCS2D Juliet extension system of
 * CreativeStudio 2D window lifecycle events. Mirrors NGCSJulietHelper
 * for the 3D system.
 */
public class NGCS2DJulietHelper {

	/**
	 * Notifies the plugin system that a CreativeStudio 2D window has been loaded.
	 *
	 * @param cs The NGCS2D instance that was loaded.
	 */
	public static void onCSWindowLoad(NGCS2D cs) {
		NGCS2DJulietIface.getInstance().onCSWindowLoad(cs);
	}

	/**
	 * Notifies the plugin system that a CreativeStudio 2D window has been closed.
	 *
	 * @param cs The NGCS2D instance that was closed.
	 */
	public static void onCSWindowClose(NGCS2D cs) {
		NGCS2DJulietIface.getInstance().onCSWindowClose(cs);
	}
}
