package ctrmap.editor.system.update;

import ctrmap.editor.system.addon.GitHubReleaseInfo;
import ctrmap.editor.system.addon.GitHubReleaseResolver;
import java.util.List;

/**
 * Checks for CTRMap-CE updates against GitHub releases.
 */
public class UpdateChecker {

	/**
	 * Result of an update check.
	 */
	public static class UpdateResult {
		public final boolean updateAvailable;
		public final GitHubReleaseInfo latestRelease;
		public final String currentVersion;
		public final String error;

		private UpdateResult(boolean updateAvailable, GitHubReleaseInfo latestRelease, String currentVersion, String error) {
			this.updateAvailable = updateAvailable;
			this.latestRelease = latestRelease;
			this.currentVersion = currentVersion;
			this.error = error;
		}

		static UpdateResult available(GitHubReleaseInfo release, String currentVersion) {
			return new UpdateResult(true, release, currentVersion, null);
		}

		static UpdateResult upToDate(String currentVersion) {
			return new UpdateResult(false, null, currentVersion, null);
		}

		static UpdateResult error(String error) {
			return new UpdateResult(false, null, null, error);
		}
	}

	/**
	 * Checks for updates using the configured update channel.
	 */
	public static UpdateResult check() {
		return check(UpdateChannel.get());
	}

	/**
	 * Checks for updates on a specific channel.
	 *
	 * @param channel "nightly" or "stable"
	 */
	public static UpdateResult check(String channel) {
		String repo = AppVersion.getGitHubRepo();
		if (repo == null || repo.isEmpty()) {
			return UpdateResult.error("No GitHub repository configured.");
		}

		if (AppVersion.isLocalBuild()) {
			return UpdateResult.error("Cannot check for updates on a local build.");
		}

		try {
			List<GitHubReleaseInfo> releases = GitHubReleaseResolver.fetchAllReleases(repo);

			GitHubReleaseInfo match = null;
			if (UpdateChannel.NIGHTLY.equals(channel)) {
				// Find the release tagged "nightly"
				for (GitHubReleaseInfo rel : releases) {
					if ("nightly".equals(rel.tag)) {
						match = rel;
						break;
					}
				}
				// Fallback: newest prerelease
				if (match == null) {
					for (GitHubReleaseInfo rel : releases) {
						if (rel.prerelease) {
							match = rel;
							break;
						}
					}
				}
			} else {
				// Stable: find newest non-prerelease
				for (GitHubReleaseInfo rel : releases) {
					if (!rel.prerelease) {
						match = rel;
						break;
					}
				}
			}

			if (match == null) {
				return UpdateResult.error("No " + channel + " release found.");
			}

			String currentVersion = AppVersion.getSortableVersion();
			if (match.sortableVersion.compareTo(currentVersion) > 0) {
				return UpdateResult.available(match, currentVersion);
			}

			return UpdateResult.upToDate(currentVersion);
		} catch (Exception ex) {
			return UpdateResult.error("Update check failed: " + ex.getMessage());
		}
	}
}
