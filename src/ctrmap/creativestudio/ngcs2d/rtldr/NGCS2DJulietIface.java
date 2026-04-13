package ctrmap.creativestudio.ngcs2d.rtldr;

import ctrmap.creativestudio.ngcs2d.NGCS2D;
import ctrmap.creativestudio.ngcs2d.io.CS2DIOContentType;
import ctrmap.creativestudio.ngcs2d.io.I2DFormatHandler;
import xstandard.util.ArraysEx;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenuItem;
import rtldr.JExtensionManager;
import rtldr.JExtensionStateListener;
import rtldr.JGarbageCollector;
import rtldr.JRTLDRCore;
import rtldr.JExtensionReceiver;
import rtldr.RExtensionBase;

/**
 * Juliet extension receiver for the NGCS2D plugin system. Manages plugin lifecycle,
 * format handler registration, and UI contributions for 2D sprite editing.
 * Mirrors NGCSJulietIface for the 3D system.
 */
public class NGCS2DJulietIface implements JExtensionReceiver<INGCS2DPlugin> {

	private static NGCS2DJulietIface INSTANCE;

	private JExtensionManager<NGCS2DJulietIface, INGCS2DPlugin> extensionManager;

	private JGarbageCollector<I2DFormatHandler> formatGc = new JGarbageCollector<>();
	private JGarbageCollector<JMenuItem> menuGc = new JGarbageCollector<>();

	private NGCS2DIOManager ioMgr = NGCS2DIOManager.getInstance();
	private NGCS2DUIManager currentUiMgr;

	private List<NGCS2D> csInstances = new ArrayList<>();

	private NGCS2DJulietIface() {

	}

	private void registCS(NGCS2D cs) {
		ArraysEx.addIfNotNullOrContains(csInstances, cs);
	}

	private void unregistCS(NGCS2D cs) {
		csInstances.remove(cs);
	}

	/**
	 * Gets the singleton instance of the NGCS2D Juliet interface, creating it
	 * and binding the extension manager on first access.
	 *
	 * @return The singleton instance.
	 */
	public static NGCS2DJulietIface getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new NGCS2DJulietIface();
			INSTANCE.extensionManager = JRTLDRCore.bindExtensionManager("NGCS2DPlugin", INSTANCE, INSTANCE.createExtensionListener());
		}
		return INSTANCE;
	}

	private JExtensionStateListener<INGCS2DPlugin> createExtensionListener() {
		return new JExtensionStateListener<INGCS2DPlugin>() {
			@Override
			public void onExtensionLoaded(INGCS2DPlugin ext) {
				//Format registration is resident
				formatGc.startListening(ext);
				ext.registerFormats(NGCS2DJulietIface.this);
				formatGc.stopListening();
				menuGc.startListening(ext);
				for (NGCS2D cs : csInstances) {
					currentUiMgr = cs.getUIManager();
					ext.registerUI(INSTANCE, cs, cs);
				}
				menuGc.stopListening();
			}

			@Override
			public void onExtensionUnloaded(INGCS2DPlugin ext) {
				unloadPluginCommon(ext);
			}
		};
	}

	/**
	 * Called when a CreativeStudio 2D window is loaded. Registers UI contributions
	 * from all loaded plugins and tracks the instance.
	 *
	 * @param cs The NGCS2D instance that was loaded.
	 */
	void onCSWindowLoad(NGCS2D cs) {
		currentUiMgr = cs.getUIManager();
		for (INGCS2DPlugin plg : extensionManager.getLoadedExtensions()) {
			menuGc.startListening(plg);
			plg.registerUI(this, cs, cs);
		}
		menuGc.stopListening();
		registCS(cs);
	}

	/**
	 * Called when a CreativeStudio 2D window is closed. Removes the instance from tracking.
	 *
	 * @param cs The NGCS2D instance that was closed.
	 */
	void onCSWindowClose(NGCS2D cs) {
		unregistCS(cs);
	}

	private void unloadPluginCommon(RExtensionBase pluginIdentity) {
		formatGc.collect(pluginIdentity, (t) -> {
			ioMgr.unregistHandler(t);
		});
		menuGc.collect(pluginIdentity, (t) -> {
			for (NGCS2D cs : csInstances) {
				cs.getUIManager().removeMenuItem(t);
			}
		});
	}

	/**
	 * Registers an I/O handler for all present and future CreativeStudio 2D instances.
	 *
	 * @param handler The I/O handler singleton.
	 * @param types   Generic resource content types that the class can handle.
	 */
	public void registFormatSupport(I2DFormatHandler handler, CS2DIOContentType... types) {
		I2DFormatHandler result = formatGc.regGc(handler);
		if (result != null) {
			ioMgr.registHandler(handler, types);
		} else if (handler != null) {
			System.out.println("Could not register format " + handler.getExtensionFilter().formatName + ": not allowed here.");
		}
	}

	/**
	 * Registers a batch of I/O handlers for all present and future CreativeStudio 2D instances.
	 *
	 * @param type     The generic resource type that these I/O classes handle.
	 * @param handlers An arbitrary number of handler classes.
	 */
	public void registFormatSupport(CS2DIOContentType type, I2DFormatHandler... handlers) {
		for (I2DFormatHandler h : handlers) {
			registFormatSupport(h, type);
		}
	}

	/**
	 * Adds a UI menu item into all CreativeStudio 2D windows.
	 *
	 * @param menuName Name of the item's parent menu.
	 * @param item     The item to add.
	 */
	public void addMenuItem(String menuName, JMenuItem item) {
		if (menuName != null && item != null) {
			if (currentUiMgr != null) {
				if ((item = menuGc.regGc(item)) != null) {
					currentUiMgr.addMenuItem(menuName, item);
				}
			}
		}
	}
}
