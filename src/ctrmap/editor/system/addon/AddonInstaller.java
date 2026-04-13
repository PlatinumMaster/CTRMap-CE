package ctrmap.editor.system.addon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import rtldr.JGlobalExtensionDB;

/**
 * Orchestrates downloading, installing, updating, and removing addons.
 */
public class AddonInstaller {

	private static final String ADDONS_DIR_NAME = ".ctrmap" + File.separator + "addons";

	/**
	 * Returns the managed addons directory under the user's home.
	 */
	public static File getAddonsDir() {
		return new File(System.getProperty("user.home"), ADDONS_DIR_NAME);
	}

	/**
	 * Installs an addon from a manifest entry.
	 *
	 * @param entry The manifest entry describing the addon.
	 * @param sourceRepoUrl The URL of the repository this addon came from.
	 * @return null on success, or an error message on failure.
	 */
	public static String install(AddonManifestEntry entry, String sourceRepoUrl) {
		try {
			File addonDir = new File(getAddonsDir(), entry.id);
			if (!addonDir.exists()) {
				addonDir.mkdirs();
			}

			String jarName = entry.id + "-" + entry.version + ".jar";
			File jarFile = new File(addonDir, jarName);

			String dlError = downloadFile(jarFile, entry.downloadUrl);
			if (dlError != null) {
				return dlError;
			}

			if (entry.sha256 != null && !entry.sha256.isEmpty()) {
				String actualHash = computeSha256(jarFile);
				if (!entry.sha256.equalsIgnoreCase(actualHash)) {
					jarFile.delete();
					return "SHA-256 verification failed.\nExpected: " + entry.sha256 + "\nGot: " + actualHash;
				}
			}

			JGlobalExtensionDB.getInstance().addPluginPath(entry.id, jarFile.getAbsolutePath());

			InstalledAddonMetadata meta = new InstalledAddonMetadata(
				entry.id, entry.version, jarFile.getAbsolutePath(), sourceRepoUrl
			);
			meta.save();

			return null;
		} catch (Exception ex) {
			return "Installation failed: " + ex.getMessage();
		}
	}

	/**
	 * Uninstalls an addon by its ID.
	 *
	 * @return null on success, or an error message on failure.
	 */
	public static String uninstall(String id) {
		try {
			InstalledAddonMetadata meta = InstalledAddonMetadata.load(id);
			if (meta == null) {
				return "Addon " + id + " is not installed.";
			}

			JGlobalExtensionDB.getInstance().removePlugin(id);

			File jarFile = new File(meta.jarPath);
			if (jarFile.exists()) {
				if (!jarFile.delete()) {
					// File may be locked by classloader; rename for cleanup on next start
					jarFile.renameTo(new File(jarFile.getAbsolutePath() + ".old"));
				}
			}

			InstalledAddonMetadata.remove(id);

			return null;
		} catch (Exception ex) {
			return "Uninstallation failed: " + ex.getMessage();
		}
	}

	/**
	 * Updates an addon to the version specified in the manifest entry.
	 *
	 * @return null on success, or an error message on failure.
	 */
	public static String update(AddonManifestEntry entry, String sourceRepoUrl) {
		String err = uninstall(entry.id);
		if (err != null) {
			return err;
		}
		return install(entry, sourceRepoUrl);
	}

	/**
	 * Downloads a file from a URL with redirect support.
	 */
	private static String downloadFile(File dest, String urlStr) {
		try {
			URL url = new URL(urlStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setInstanceFollowRedirects(true);
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(30000);

			int status = conn.getResponseCode();

			// Handle redirects manually (Java doesn't follow HTTPS->HTTPS cross-host redirects automatically)
			int redirectCount = 0;
			while ((status == HttpURLConnection.HTTP_MOVED_TEMP
				|| status == HttpURLConnection.HTTP_MOVED_PERM
				|| status == HttpURLConnection.HTTP_SEE_OTHER
				|| status == 307 || status == 308)
				&& redirectCount < 5) {
				String newUrl = conn.getHeaderField("Location");
				conn.disconnect();
				conn = (HttpURLConnection) new URL(newUrl).openConnection();
				conn.setInstanceFollowRedirects(true);
				conn.setConnectTimeout(15000);
				conn.setReadTimeout(30000);
				status = conn.getResponseCode();
				redirectCount++;
			}

			if (status != HttpURLConnection.HTTP_OK) {
				conn.disconnect();
				return "Download failed: HTTP " + status;
			}

			try (InputStream in = conn.getInputStream();
				 FileOutputStream fos = new FileOutputStream(dest)) {
				fos.getChannel().transferFrom(Channels.newChannel(in), 0, Long.MAX_VALUE);
			}
			conn.disconnect();

			if (!dest.exists() || dest.length() == 0) {
				return "Download failed: file is empty or missing.";
			}
			return null;
		} catch (IOException ex) {
			return "Download failed: " + ex.getMessage();
		}
	}

	private static String computeSha256(File file) throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		try (FileInputStream fis = new FileInputStream(file)) {
			byte[] buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = fis.read(buffer)) != -1) {
				digest.update(buffer, 0, bytesRead);
			}
		}
		byte[] hashBytes = digest.digest();
		StringBuilder sb = new StringBuilder();
		for (byte b : hashBytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
