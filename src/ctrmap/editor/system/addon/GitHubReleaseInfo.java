package ctrmap.editor.system.addon;

/**
 * Holds resolved information about a single GitHub release.
 */
public class GitHubReleaseInfo {

	/** The release tag name (e.g. "nightly", "stable-20260405-abc1234", "v1.0.0"). */
	public final String tag;

	/** ISO timestamp of when the release was published. */
	public final String publishedAt;

	/** Normalized timestamp for comparison (e.g. "20260405230000"). */
	public final String sortableVersion;

	/** Download URL for the .jar asset. */
	public final String downloadUrl;

	/** Whether this is marked as a prerelease. */
	public final boolean prerelease;

	public GitHubReleaseInfo(String tag, String publishedAt, String downloadUrl, boolean prerelease) {
		this.tag = tag;
		this.publishedAt = publishedAt;
		this.downloadUrl = downloadUrl;
		this.prerelease = prerelease;
		this.sortableVersion = publishedAt != null
			? publishedAt.replace("-", "").replace("T", "").replace(":", "").replace("Z", "")
			: "0";
	}

	/**
	 * Display string for the version selector.
	 */
	public String getDisplayName() {
		String label = tag;
		if (publishedAt != null && publishedAt.length() >= 10) {
			label += " (" + publishedAt.substring(0, 10) + ")";
		}
		if (prerelease) {
			label += " [pre]";
		}
		return label;
	}

	@Override
	public String toString() {
		return getDisplayName();
	}
}
