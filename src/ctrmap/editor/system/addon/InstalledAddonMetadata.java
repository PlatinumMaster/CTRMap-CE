package ctrmap.editor.system.addon;

import ctrmap.util.CMPrefs;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Stores and retrieves metadata about installed addons via Java Preferences.
 */
public class InstalledAddonMetadata {

	private static final Preferences PREFS = CMPrefs.node("AddonStore").node("Installed");

	public final String id;
	public final String version;
	public final String jarPath;
	public final String sourceRepoUrl;

	public InstalledAddonMetadata(String id, String version, String jarPath, String sourceRepoUrl) {
		this.id = id;
		this.version = version;
		this.jarPath = jarPath;
		this.sourceRepoUrl = sourceRepoUrl;
	}

	/**
	 * Saves this addon's metadata to Preferences.
	 */
	public void save() {
		Preferences node = PREFS.node(id);
		node.put("version", version);
		node.put("jarPath", jarPath);
		if (sourceRepoUrl != null) {
			node.put("sourceRepoUrl", sourceRepoUrl);
		}
	}

	/**
	 * Loads metadata for a specific addon ID.
	 *
	 * @return The metadata, or null if not installed.
	 */
	public static InstalledAddonMetadata load(String id) {
		try {
			if (!PREFS.nodeExists(id)) {
				return null;
			}
		} catch (BackingStoreException ex) {
			return null;
		}
		Preferences node = PREFS.node(id);
		String version = node.get("version", null);
		String jarPath = node.get("jarPath", null);
		String sourceRepoUrl = node.get("sourceRepoUrl", null);
		if (version == null || jarPath == null) {
			return null;
		}
		return new InstalledAddonMetadata(id, version, jarPath, sourceRepoUrl);
	}

	/**
	 * Removes metadata for a specific addon ID.
	 */
	public static void remove(String id) {
		try {
			if (PREFS.nodeExists(id)) {
				PREFS.node(id).removeNode();
			}
		} catch (BackingStoreException ex) {
			// ignore
		}
	}

	/**
	 * Returns a list of all installed addon IDs.
	 */
	public static List<String> getInstalledIds() {
		List<String> ids = new ArrayList<>();
		try {
			for (String name : PREFS.childrenNames()) {
				ids.add(name);
			}
		} catch (BackingStoreException ex) {
			// ignore
		}
		return ids;
	}

	/**
	 * Checks whether an addon with the given ID is installed.
	 */
	public static boolean isInstalled(String id) {
		return load(id) != null;
	}

	/**
	 * Returns the installed version of an addon, or null if not installed.
	 */
	public static String getInstalledVersion(String id) {
		InstalledAddonMetadata meta = load(id);
		return meta != null ? meta.version : null;
	}
}
