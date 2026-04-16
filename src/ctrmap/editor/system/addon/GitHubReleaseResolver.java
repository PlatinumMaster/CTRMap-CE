package ctrmap.editor.system.addon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves release information from a GitHub repository's releases API.
 */
public class GitHubReleaseResolver {

	private static final String API_BASE = "https://api.github.com/repos/";

	/**
	 * Fetches all releases for a GitHub repository that contain .jar assets.
	 *
	 * @param githubRepo "owner/repo" format.
	 * @return List of releases with .jar assets, newest first.
	 */
	public static List<GitHubReleaseInfo> fetchAllReleases(String githubRepo) throws IOException {
		String apiUrl = API_BASE + githubRepo + "/releases";
		String json = fetchString(apiUrl);
		if (json == null) {
			throw new IOException("Failed to fetch releases from GitHub.");
		}
		return parseReleasesArray(json);
	}

	/**
	 * Resolves the latest (or tagged) release for an addon entry.
	 * Populates entry.version, entry.downloadUrl, and entry.availableReleases.
	 *
	 * @return null on success, or an error message on failure.
	 */
	public static String resolve(AddonManifestEntry entry) {
		if (entry.githubRepo == null) {
			return "No GitHub repository specified.";
		}

		try {
			List<GitHubReleaseInfo> releases = fetchAllReleases(entry.githubRepo);
			entry.availableReleases = releases;

			if (releases.isEmpty()) {
				return "No releases with .jar assets found.";
			}

			// Find the matching release
			GitHubReleaseInfo selected = null;
			if (entry.releaseTag != null && !entry.releaseTag.isEmpty()) {
				for (GitHubReleaseInfo rel : releases) {
					if (entry.releaseTag.equals(rel.tag)) {
						selected = rel;
						break;
					}
				}
			}
			if (selected == null) {
				selected = releases.get(0); // newest
			}

			entry.version = selected.sortableVersion;
			entry.downloadUrl = selected.downloadUrl;

			return null;
		} catch (Exception ex) {
			return "GitHub API error: " + ex.getMessage();
		}
	}

	/**
	 * Parses the JSON array of releases. Each release object is split out and
	 * parsed individually.
	 */
	private static List<GitHubReleaseInfo> parseReleasesArray(String json) {
		List<GitHubReleaseInfo> releases = new ArrayList<>();

		// The response is a JSON array of release objects.
		// Split on each release object by finding top-level { } pairs.
		int depth = 0;
		int objStart = -1;

		for (int i = 0; i < json.length(); i++) {
			char c = json.charAt(i);
			if (c == '"') {
				// Skip strings to avoid counting braces inside them
				i = skipJsonString(json, i);
				continue;
			}
			if (c == '{') {
				if (depth == 0) {
					objStart = i;
				}
				depth++;
			} else if (c == '}') {
				depth--;
				if (depth == 0 && objStart >= 0) {
					String releaseJson = json.substring(objStart, i + 1);
					GitHubReleaseInfo info = parseOneRelease(releaseJson);
					if (info != null) {
						releases.add(info);
					}
					objStart = -1;
				}
			}
		}
		return releases;
	}

	private static GitHubReleaseInfo parseOneRelease(String json) {
		String tag = extractJsonString(json, "tag_name");
		String publishedAt = extractJsonString(json, "published_at");
		boolean prerelease = "true".equals(extractJsonRaw(json, "prerelease"));
		String jarUrl = findJarAssetUrl(json);

		if (tag == null || jarUrl == null) {
			return null;
		}
		return new GitHubReleaseInfo(tag, publishedAt, jarUrl, prerelease);
	}

	private static String fetchString(String urlStr) throws IOException {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
		conn.setRequestProperty("User-Agent", "CTRMap-AddonStore");
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(15000);

		int status = conn.getResponseCode();
		if (status != HttpURLConnection.HTTP_OK) {
			conn.disconnect();
			throw new IOException("HTTP " + status + " from " + urlStr);
		}

		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		}
		conn.disconnect();
		return sb.toString();
	}

	/**
	 * Extracts a JSON string value by key.
	 */
	static String extractJsonString(String json, String key) {
		String search = "\"" + key + "\"";
		int idx = json.indexOf(search);
		if (idx == -1) {
			return null;
		}
		idx = json.indexOf(":", idx + search.length());
		if (idx == -1) {
			return null;
		}
		int qStart = json.indexOf("\"", idx + 1);
		if (qStart == -1) {
			return null;
		}
		int qEnd = json.indexOf("\"", qStart + 1);
		if (qEnd == -1) {
			return null;
		}
		return json.substring(qStart + 1, qEnd);
	}

	/**
	 * Extracts a raw JSON value by key (for booleans/numbers).
	 */
	private static String extractJsonRaw(String json, String key) {
		String search = "\"" + key + "\"";
		int idx = json.indexOf(search);
		if (idx == -1) {
			return null;
		}
		idx = json.indexOf(":", idx + search.length());
		if (idx == -1) {
			return null;
		}
		idx++;
		while (idx < json.length() && json.charAt(idx) == ' ') {
			idx++;
		}
		int end = idx;
		while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && json.charAt(end) != ' ') {
			end++;
		}
		return json.substring(idx, end);
	}

	/**
	 * Finds the first browser_download_url ending in .jar.
	 */
	private static String findJarAssetUrl(String json) {
		int searchFrom = 0;
		while (true) {
			String key = "\"browser_download_url\"";
			int idx = json.indexOf(key, searchFrom);
			if (idx == -1) {
				break;
			}
			idx = json.indexOf(":", idx + key.length());
			if (idx == -1) {
				break;
			}
			int qStart = json.indexOf("\"", idx + 1);
			if (qStart == -1) {
				break;
			}
			int qEnd = json.indexOf("\"", qStart + 1);
			if (qEnd == -1) {
				break;
			}
			String assetUrl = json.substring(qStart + 1, qEnd);
			if (assetUrl.endsWith(".jar")) {
				return assetUrl;
			}
			searchFrom = qEnd + 1;
		}
		return null;
	}

	/**
	 * Skips past a JSON string starting at the opening quote.
	 * Returns the index of the closing quote.
	 */
	private static int skipJsonString(String json, int openQuote) {
		for (int i = openQuote + 1; i < json.length(); i++) {
			char c = json.charAt(i);
			if (c == '\\') {
				i++; // skip escaped char
			} else if (c == '"') {
				return i;
			}
		}
		return json.length() - 1;
	}
}
