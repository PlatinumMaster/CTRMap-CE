package ctrmap.util.gui.addon;

import ctrmap.editor.system.addon.AddonInstaller;
import ctrmap.editor.system.addon.AddonManifestEntry;
import ctrmap.editor.system.addon.GitHubReleaseInfo;
import ctrmap.editor.system.addon.InstalledAddonMetadata;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import xstandard.gui.DialogUtils;
import xstandard.text.VersionString;

/**
 * A panel representing a single addon entry in the addon store list.
 */
public class AddonEntryPanel extends JPanel {

	private final AddonManifestEntry entry;
	private final String sourceRepoUrl;
	private final AddonStoreFrame storeFrame;

	private final JButton actionButton;
	private final JLabel statusLabel;
	private final JComboBox<GitHubReleaseInfo> versionSelector;

	private static final int STATE_NOT_INSTALLED = 0;
	private static final int STATE_INSTALLED = 1;
	private static final int STATE_UPDATE_AVAILABLE = 2;

	public AddonEntryPanel(AddonManifestEntry entry, String sourceRepoUrl, AddonStoreFrame storeFrame) {
		this.entry = entry;
		this.sourceRepoUrl = sourceRepoUrl;
		this.storeFrame = storeFrame;

		setLayout(new BorderLayout(10, 0));
		setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		setOpaque(false);

		// Left: info panel
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
		infoPanel.setOpaque(false);

		JLabel nameLabel = new JLabel(entry.name);
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
		nameLabel.setAlignmentX(LEFT_ALIGNMENT);
		infoPanel.add(nameLabel);

		String metaText = "";
		if (entry.author != null) {
			metaText = "by " + entry.author;
		}
		if (!metaText.isEmpty()) {
			JLabel metaLabel = new JLabel(metaText);
			metaLabel.setForeground(Color.GRAY);
			metaLabel.setAlignmentX(LEFT_ALIGNMENT);
			infoPanel.add(metaLabel);
		}

		if (entry.description != null) {
			JLabel descLabel = new JLabel(entry.description);
			descLabel.setAlignmentX(LEFT_ALIGNMENT);
			infoPanel.add(descLabel);
		}

		add(infoPanel, BorderLayout.CENTER);

		// Right: version selector + action button + status
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		rightPanel.setOpaque(false);

		// Version dropdown (only for GitHub-backed entries with multiple releases)
		versionSelector = new JComboBox<>();
		versionSelector.setMaximumSize(new Dimension(220, 25));
		versionSelector.setPreferredSize(new Dimension(220, 25));
		versionSelector.setAlignmentX(RIGHT_ALIGNMENT);

		if (entry.availableReleases != null && !entry.availableReleases.isEmpty()) {
			DefaultComboBoxModel<GitHubReleaseInfo> model = new DefaultComboBoxModel<>();
			for (GitHubReleaseInfo rel : entry.availableReleases) {
				model.addElement(rel);
			}
			versionSelector.setModel(model);
			versionSelector.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					onVersionSelected();
				}
			});
			rightPanel.add(versionSelector);
		} else {
			// Show static version label for manual entries
			if (entry.version != null) {
				JLabel verLabel = new JLabel("v" + entry.version);
				verLabel.setForeground(Color.GRAY);
				verLabel.setAlignmentX(RIGHT_ALIGNMENT);
				rightPanel.add(verLabel);
			}
		}

		actionButton = new JButton();
		actionButton.setPreferredSize(new Dimension(220, 25));
		actionButton.setMaximumSize(new Dimension(220, 25));
		actionButton.setAlignmentX(RIGHT_ALIGNMENT);
		rightPanel.add(actionButton);

		statusLabel = new JLabel(" ");
		statusLabel.setForeground(Color.GRAY);
		statusLabel.setAlignmentX(RIGHT_ALIGNMENT);
		rightPanel.add(statusLabel);

		add(rightPanel, BorderLayout.LINE_END);

		refreshState();
	}

	private void onVersionSelected() {
		GitHubReleaseInfo selected = (GitHubReleaseInfo) versionSelector.getSelectedItem();
		if (selected != null) {
			entry.version = selected.sortableVersion;
			entry.downloadUrl = selected.downloadUrl;
			refreshState();
		}
	}

	private void refreshState() {
		int state = getState();
		switch (state) {
			case STATE_NOT_INSTALLED:
				actionButton.setText("Install");
				statusLabel.setText(" ");
				setActionListener(new Runnable() {
					@Override
					public void run() {
						doInstall();
					}
				});
				break;
			case STATE_INSTALLED:
				actionButton.setText("Uninstall");
				GitHubReleaseInfo sel = (GitHubReleaseInfo) versionSelector.getSelectedItem();
				if (sel != null) {
					String installedVer = InstalledAddonMetadata.getInstalledVersion(entry.id);
					if (installedVer != null && !installedVer.equals(entry.version)) {
						// Different version selected than what's installed
						actionButton.setText("Switch Version");
						statusLabel.setText("Installed: " + getInstalledTagDisplay());
						setActionListener(new Runnable() {
							@Override
							public void run() {
								doUpdate();
							}
						});
						break;
					}
				}
				statusLabel.setText("Installed");
				setActionListener(new Runnable() {
					@Override
					public void run() {
						doUninstall();
					}
				});
				break;
			case STATE_UPDATE_AVAILABLE:
				actionButton.setText("Update");
				statusLabel.setText("Update available");
				setActionListener(new Runnable() {
					@Override
					public void run() {
						doUpdate();
					}
				});
				break;
		}
	}

	private String getInstalledTagDisplay() {
		String installedVersion = InstalledAddonMetadata.getInstalledVersion(entry.id);
		if (installedVersion != null && entry.availableReleases != null) {
			for (GitHubReleaseInfo rel : entry.availableReleases) {
				if (rel.sortableVersion.equals(installedVersion)) {
					return rel.tag;
				}
			}
		}
		return installedVersion;
	}

	private int getState() {
		if (!InstalledAddonMetadata.isInstalled(entry.id)) {
			return STATE_NOT_INSTALLED;
		}
		String installedVersion = InstalledAddonMetadata.getInstalledVersion(entry.id);
		if (entry.version != null && installedVersion != null) {
			VersionString remote = new VersionString(entry.version);
			VersionString local = new VersionString(installedVersion);
			if (remote.isNewerThan(local)) {
				return STATE_UPDATE_AVAILABLE;
			}
		}
		return STATE_INSTALLED;
	}

	private void setActionListener(final Runnable action) {
		for (ActionListener al : actionButton.getActionListeners()) {
			actionButton.removeActionListener(al);
		}
		actionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
	}

	private void doInstall() {
		actionButton.setEnabled(false);
		statusLabel.setText("Installing...");
		new SwingWorker<String, Void>() {
			@Override
			protected String doInBackground() {
				return AddonInstaller.install(entry, sourceRepoUrl);
			}

			@Override
			protected void done() {
				try {
					String error = get();
					if (error != null) {
						DialogUtils.showErrorMessage(AddonEntryPanel.this, "Install Failed", error);
					}
				} catch (Exception ex) {
					DialogUtils.showErrorMessage(AddonEntryPanel.this, "Install Failed", ex.getMessage());
				}
				actionButton.setEnabled(true);
				refreshState();
			}
		}.execute();
	}

	private void doUninstall() {
		actionButton.setEnabled(false);
		statusLabel.setText("Removing...");
		new SwingWorker<String, Void>() {
			@Override
			protected String doInBackground() {
				return AddonInstaller.uninstall(entry.id);
			}

			@Override
			protected void done() {
				try {
					String error = get();
					if (error != null) {
						DialogUtils.showErrorMessage(AddonEntryPanel.this, "Uninstall Failed", error);
					}
				} catch (Exception ex) {
					DialogUtils.showErrorMessage(AddonEntryPanel.this, "Uninstall Failed", ex.getMessage());
				}
				actionButton.setEnabled(true);
				refreshState();
			}
		}.execute();
	}

	private void doUpdate() {
		actionButton.setEnabled(false);
		statusLabel.setText("Updating...");
		new SwingWorker<String, Void>() {
			@Override
			protected String doInBackground() {
				return AddonInstaller.update(entry, sourceRepoUrl);
			}

			@Override
			protected void done() {
				try {
					String error = get();
					if (error != null) {
						DialogUtils.showErrorMessage(AddonEntryPanel.this, "Update Failed", error);
					}
				} catch (Exception ex) {
					DialogUtils.showErrorMessage(AddonEntryPanel.this, "Update Failed", ex.getMessage());
				}
				actionButton.setEnabled(true);
				refreshState();
			}
		}.execute();
	}

	/**
	 * Returns true if this entry matches the given search query.
	 */
	public boolean matchesFilter(String query) {
		if (query == null || query.isEmpty()) {
			return true;
		}
		String lower = query.toLowerCase();
		if (entry.name != null && entry.name.toLowerCase().contains(lower)) {
			return true;
		}
		if (entry.description != null && entry.description.toLowerCase().contains(lower)) {
			return true;
		}
		if (entry.author != null && entry.author.toLowerCase().contains(lower)) {
			return true;
		}
		if (entry.id != null && entry.id.toLowerCase().contains(lower)) {
			return true;
		}
		return false;
	}
}
