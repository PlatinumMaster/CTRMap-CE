package ctrmap.editor.system.addon;

import ctrmap.util.CMPrefs;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Manages the list of addon repository URLs, persisted via Java Preferences.
 */
public class AddonRepositoryManager {

	private static final Preferences PREFS = CMPrefs.node("AddonStore").node("Repositories");

	/**
	 * Adds a repository URL. The URL is used as both the key and the value.
	 */
	public static void addRepository(String url) {
		if (url != null && !url.isEmpty()) {
			PREFS.put(urlToKey(url), url);
		}
	}

	/**
	 * Removes a repository URL.
	 */
	public static void removeRepository(String url) {
		if (url != null) {
			PREFS.remove(urlToKey(url));
		}
	}

	/**
	 * Returns all configured repository URLs.
	 */
	public static List<String> getRepositoryUrls() {
		List<String> urls = new ArrayList<>();
		try {
			for (String key : PREFS.keys()) {
				String url = PREFS.get(key, null);
				if (url != null) {
					urls.add(url);
				}
			}
		} catch (BackingStoreException ex) {
			// ignore
		}
		return urls;
	}

	/**
	 * Fetches manifests from all configured repositories.
	 *
	 * @return List of AddonRepository objects (some may have fetch errors).
	 */
	public static List<AddonRepository> fetchAll() {
		List<AddonRepository> repos = new ArrayList<>();
		for (String url : getRepositoryUrls()) {
			AddonRepository repo = new AddonRepository(url);
			repo.fetch();
			repos.add(repo);
		}
		return repos;
	}

	/**
	 * Converts a URL to a short, safe Preferences key using SHA-256 hash.
	 * Preferences keys have an 80-character limit.
	 */
	private static String urlToKey(String url) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(url.getBytes());
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 16; i++) {
				sb.append(String.format("%02x", hash[i]));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException ex) {
			// SHA-256 is guaranteed to be available
			return String.valueOf(url.hashCode());
		}
	}
}
