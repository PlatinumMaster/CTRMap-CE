package ctrmap.editor.system.addon;

import java.util.ArrayList;
import java.util.List;
import xstandard.formats.yaml.YamlNode;

/**
 * Represents a single plugin entry from a repository manifest.
 *
 * Supports two modes:
 *   1. Manual: downloadUrl and version specified directly.
 *   2. GitHub: githubRepo specified (e.g. "PlatinumMaster/CTRMapV"),
 *      version and downloadUrl resolved automatically from GitHub releases API.
 */
public class AddonManifestEntry {

	public final String id;
	public final String name;
	public final String description;
	public final String author;

	/** GitHub repo in "owner/repo" format, or null for manual mode. */
	public final String githubRepo;

	/** Which release tag to track (e.g. "nightly"). Null means latest. */
	public final String releaseTag;

	/** Resolved at fetch time for GitHub mode, or specified directly. */
	public String version;
	public String downloadUrl;
	public String sha256;

	/** All available releases (populated for GitHub-backed entries). */
	public List<GitHubReleaseInfo> availableReleases = new ArrayList<>();

	public AddonManifestEntry(String id, String name, String description, String author,
			String version, String downloadUrl, String sha256,
			String githubRepo, String releaseTag) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.author = author;
		this.version = version;
		this.downloadUrl = downloadUrl;
		this.sha256 = sha256;
		this.githubRepo = githubRepo;
		this.releaseTag = releaseTag;
	}

	/**
	 * Returns true if this entry uses GitHub releases for resolution.
	 */
	public boolean isGitHubBacked() {
		return githubRepo != null && !githubRepo.isEmpty();
	}

	/**
	 * Parses an AddonManifestEntry from a YAML list element node.
	 *
	 * Minimal GitHub-backed entry:
	 *   id: ctrmapv
	 *   name: CTRMapV
	 *   description: Pokemon Gen V editing support
	 *   author: HelloOO7
	 *   githubRepo: PlatinumMaster/CTRMapV
	 *   releaseTag: nightly
	 *
	 * Manual entry:
	 *   id: some-plugin
	 *   name: Some Plugin
	 *   version: 1.0.0
	 *   downloadUrl: https://example.com/plugin.jar
	 */
	public static AddonManifestEntry fromYaml(YamlNode node) {
		String id = node.getChildValue("id");
		String name = node.getChildValue("name");
		if (id == null || name == null) {
			return null;
		}
		String description = node.getChildValue("description");
		String author = node.getChildValue("author");
		String version = node.getChildValue("version");
		String downloadUrl = node.getChildValue("downloadUrl");
		String sha256 = node.getChildValue("sha256");
		String githubRepo = node.getChildValue("githubRepo");
		String releaseTag = node.getChildValue("releaseTag");

		if (downloadUrl == null && githubRepo == null) {
			return null;
		}
		return new AddonManifestEntry(id, name, description, author, version, downloadUrl, sha256, githubRepo, releaseTag);
	}
}
