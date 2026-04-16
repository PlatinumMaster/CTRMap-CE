package ctrmap.editor.system.update;

import java.awt.Desktop;
import java.net.URI;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import ctrmap.editor.system.addon.GitHubReleaseInfo;

/**
 * UI for checking and presenting update information.
 */
public class UpdateCheckDialog {

	/**
	 * Runs an update check in the background and shows a dialog with the result.
	 *
	 * @param parent Parent component for the dialog.
	 * @param silent If true, only show dialog when an update is available.
	 */
	public static void checkForUpdates(java.awt.Component parent, boolean silent) {
		new SwingWorker<UpdateChecker.UpdateResult, Void>() {
			@Override
			protected UpdateChecker.UpdateResult doInBackground() {
				return UpdateChecker.check();
			}

			@Override
			protected void done() {
				try {
					UpdateChecker.UpdateResult result = get();

					if (result.error != null) {
						if (!silent) {
							JOptionPane.showMessageDialog(parent, result.error,
								"Update Check", JOptionPane.WARNING_MESSAGE);
						}
						return;
					}

					if (result.updateAvailable) {
						showUpdateAvailable(parent, result.latestRelease);
					} else if (!silent) {
						JOptionPane.showMessageDialog(parent,
							"You are running the latest version.\n" + AppVersion.getDisplayVersion(),
							"Update Check", JOptionPane.INFORMATION_MESSAGE);
					}
				} catch (Exception ex) {
					if (!silent) {
						JOptionPane.showMessageDialog(parent,
							"Update check failed: " + ex.getMessage(),
							"Update Check", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}.execute();
	}

	private static void showUpdateAvailable(java.awt.Component parent, GitHubReleaseInfo release) {
		String message = "A new version is available!\n\n"
			+ "Current: " + AppVersion.getDisplayVersion() + "\n"
			+ "Available: " + release.getDisplayName() + "\n\n"
			+ "Channel: " + UpdateChannel.get() + "\n\n"
			+ "Would you like to open the download page?";

		int choice = JOptionPane.showConfirmDialog(parent, message,
			"Update Available", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

		if (choice == JOptionPane.YES_OPTION) {
			openReleasePage(release);
		}
	}

	private static void openReleasePage(GitHubReleaseInfo release) {
		try {
			String repo = AppVersion.getGitHubRepo();
			String url = "https://github.com/" + repo + "/releases/tag/" + release.tag;
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(new URI(url));
			}
		} catch (Exception ex) {
			// ignore
		}
	}

	/**
	 * Shows a dialog to let the user pick their update channel.
	 */
	public static void showChannelSelector(java.awt.Component parent) {
		String[] options = {UpdateChannel.NIGHTLY, UpdateChannel.STABLE};
		String current = UpdateChannel.get();

		String selected = (String) JOptionPane.showInputDialog(parent,
			"Select update channel:\n\n"
			+ "Nightly - Latest development builds (may be unstable)\n"
			+ "Stable - Release builds only",
			"Update Channel",
			JOptionPane.PLAIN_MESSAGE,
			null,
			options,
			current);

		if (selected != null) {
			UpdateChannel.set(selected);
		}
	}
}
