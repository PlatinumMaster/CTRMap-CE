
package ctrmap.creativestudio.ngcs2d.rtldr;

import java.awt.Frame;
import rtldr.RExtensionBase;

/**
 * Extension point interface for NGCS2D plugins. Plugins implement this interface
 * to register 2D sprite format handlers and UI contributions with CreativeStudio 2D.
 */
public interface INGCS2DPlugin extends RExtensionBase<NGCS2DJulietIface> {

	/**
	 * Called to register 2D format handlers with the Juliet extension system.
	 *
	 * @param j The NGCS2D Juliet interface singleton.
	 */
	public default void registerFormats(NGCS2DJulietIface j) {

	}

	/**
	 * Called to register UI contributions (menu items, etc.) with a CreativeStudio 2D window.
	 *
	 * @param j               The NGCS2D Juliet interface singleton.
	 * @param uiParent        The parent frame of the CreativeStudio 2D window.
	 * @param contentAccessor The content accessor for the active 2D resource.
	 */
	public default void registerUI(NGCS2DJulietIface j, Frame uiParent, NGCS2DContentAccessor contentAccessor) {

	}
}
