package ctrmap.editor.system.addon;

import java.util.ArrayList;
import java.util.List;
import xstandard.formats.yaml.Yaml;
import xstandard.formats.yaml.YamlNode;
import xstandard.fs.accessors.MemoryFile;
import xstandard.net.FileDownloader;

/**
 * Represents a single addon repository fetched from a remote YAML manifest.
 */
public class AddonRepository {

	public final String url;
	public String name;
	public String maintainer;
	public final List<AddonManifestEntry> plugins = new ArrayList<>();

	private String errorMessage;

	public AddonRepository(String url) {
		this.url = url;
		this.name = url;
	}

	/**
	 * Fetches and parses the repository manifest from the remote URL.
	 *
	 * @return true if the fetch succeeded, false otherwise.
	 */
	public boolean fetch() {
		plugins.clear();
		errorMessage = null;
		try {
			MemoryFile memFile = FileDownloader.downloadToMemory(url);
			if (memFile == null) {
				errorMessage = "Failed to download manifest from " + url;
				return false;
			}
			Yaml yaml = new Yaml(memFile.getInputStream(), "manifest.yml");

			YamlNode repoNode = yaml.getRootNodeKeyNode("repository");
			if (repoNode != null) {
				String repoName = repoNode.getChildValue("name");
				if (repoName != null) {
					name = repoName;
				}
				String repoMaintainer = repoNode.getChildValue("maintainer");
				if (repoMaintainer != null) {
					maintainer = repoMaintainer;
				}
			}

			YamlNode pluginsNode = yaml.getRootNodeKeyNode("plugins");
			if (pluginsNode != null) {
				for (YamlNode child : pluginsNode.children) {
					AddonManifestEntry entry = AddonManifestEntry.fromYaml(child);
					if (entry != null) {
						if (entry.isGitHubBacked()) {
							String err = GitHubReleaseResolver.resolve(entry);
							if (err != null) {
								System.err.println("Warning: Could not resolve " + entry.id + ": " + err);
								continue;
							}
						}
						plugins.add(entry);
					}
				}
			}
			return true;
		} catch (Exception ex) {
			errorMessage = "Error fetching repository: " + ex.getMessage();
			return false;
		}
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}
