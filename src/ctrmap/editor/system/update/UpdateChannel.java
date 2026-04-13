package ctrmap.editor.system.update;

import ctrmap.util.CMPrefs;
import java.util.prefs.Preferences;

/**
 * Update channel preference (nightly vs stable).
 */
public class UpdateChannel {

	private static final Preferences PREFS = CMPrefs.node("Updates");
	private static final String KEY = "channel";

	public static final String NIGHTLY = "nightly";
	public static final String STABLE = "stable";

	public static String get() {
		return PREFS.get(KEY, NIGHTLY);
	}

	public static void set(String channel) {
		PREFS.put(KEY, channel);
	}

	public static boolean isNightly() {
		return NIGHTLY.equals(get());
	}

	public static boolean isStable() {
		return STABLE.equals(get());
	}
}
