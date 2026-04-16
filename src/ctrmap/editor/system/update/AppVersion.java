package ctrmap.editor.system.update;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads build metadata embedded in the JAR at build time by Maven filtering.
 */
public class AppVersion {

	private static final Properties PROPS = new Properties();

	static {
		try (InputStream is = AppVersion.class.getResourceAsStream("/ctrmap/build.properties")) {
			if (is != null) {
				PROPS.load(is);
			}
		} catch (IOException e) {
			// ignore
		}
	}

	/** Build date, e.g. "20260405" or "local" for dev builds. */
	public static String getBuildDate() {
		return PROPS.getProperty("build.date", "local");
	}

	/** Short commit hash, e.g. "abc1234" or "unknown" for dev builds. */
	public static String getBuildCommit() {
		return PROPS.getProperty("build.commit", "unknown");
	}

	/** Build type: "nightly" or "stable". */
	public static String getBuildType() {
		return PROPS.getProperty("build.type", "nightly");
	}

	/** GitHub repo, e.g. "PlatinumMaster/CTRMap-CE". */
	public static String getGitHubRepo() {
		return PROPS.getProperty("build.github.repo", "");
	}

	/** Returns a sortable version string for comparison with GitHub releases. */
	public static String getSortableVersion() {
		String date = getBuildDate();
		if ("local".equals(date)) {
			return "0";
		}
		return date;
	}

	/** Returns a human-readable version string. */
	public static String getDisplayVersion() {
		String date = getBuildDate();
		String commit = getBuildCommit();
		String type = getBuildType();
		if ("local".equals(date)) {
			return "Local development build";
		}
		return type + " " + date + " (" + commit + ")";
	}

	/** Returns true if this is a local/dev build without version info. */
	public static boolean isLocalBuild() {
		return "local".equals(getBuildDate());
	}
}
