package ctrmap.util.gui.addon;

import ctrmap.Launc;
import ctrmap.editor.system.addon.AddonManifestEntry;
import ctrmap.editor.system.addon.AddonRepository;
import ctrmap.editor.system.addon.AddonRepositoryManager;
import ctrmap.editor.system.addon.InstalledAddonMetadata;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import rtldr.JAbstractPluginDatabase;
import rtldr.JGlobalExtensionDB;
import xstandard.formats.zip.ZipArchive;
import xstandard.fs.FSFile;
import xstandard.gui.DialogUtils;
import xstandard.gui.components.ComponentList;
import xstandard.gui.components.ComponentUtils;
import xstandard.gui.file.CommonExtensionFilters;
import xstandard.gui.file.XFileDialog;

/**
 * Unified plug-in manager. Browse and install from repositories, or add local JAR files.
 */
public class AddonStoreFrame extends JFrame {

	public static final Launc.SubprocessStarter STARTER_FROM_LAUNCHER = new Launc.SubprocessStarter() {
		@Override
		public boolean start() {
			AddonStoreFrame frame = new AddonStoreFrame();
			frame.setDisposeCallback(new Consumer<AddonStoreFrame>() {
				@Override
				public void accept(AddonStoreFrame t) {
					Launc.returnToLauncher();
				}
			});
			frame.setVisible(true);
			return true;
		}
	};

	private Consumer<AddonStoreFrame> disposeCallback;

	private final ComponentList addonList;
	private final JLabel statusLabel;
	private final JTextField searchField;
	private final List<LocalPluginEntryPanel> localEntries = new ArrayList<>();
	private final List<AddonEntryPanel> repoEntries = new ArrayList<>();

	public AddonStoreFrame() {
		setTitle("Plug-in Manager");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(650, 500);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());

		// Top toolbar
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

		JButton btnInstallLocal = new JButton("Install from file");
		btnInstallLocal.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				installFromFile();
			}
		});
		toolbar.add(btnInstallLocal);

		JButton btnRepos = new JButton("Repositories");
		btnRepos.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new RepositoryManagementDialog(AddonStoreFrame.this).setVisible(true);
			}
		});
		toolbar.add(btnRepos);

		JButton btnRefresh = new JButton("Refresh");
		btnRefresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshAddons();
			}
		});
		toolbar.add(btnRefresh);

		searchField = new JTextField();
		searchField.setPreferredSize(new Dimension(180, 25));
		searchField.setToolTipText("Search...");
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				applyFilter();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				applyFilter();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				applyFilter();
			}
		});
		toolbar.add(new JLabel("Search:"));
		toolbar.add(searchField);

		add(toolbar, BorderLayout.NORTH);

		// Center: combined list
		addonList = new ComponentList();
		JScrollPane scrollPane = new JScrollPane(addonList);
		scrollPane.getVerticalScrollBar().setUnitIncrement(15);
		add(scrollPane, BorderLayout.CENTER);

		// Bottom: status + back
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

		statusLabel = new JLabel(" ");
		bottomPanel.add(statusLabel, BorderLayout.CENTER);

		JButton btnBack = new JButton("Back");
		btnBack.setPreferredSize(new Dimension(90, 25));
		btnBack.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		bottomPanel.add(btnBack, BorderLayout.LINE_END);

		add(bottomPanel, BorderLayout.SOUTH);

		refreshAddons();
	}

	public void setDisposeCallback(Consumer<AddonStoreFrame> callback) {
		this.disposeCallback = callback;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (disposeCallback != null) {
			disposeCallback.accept(this);
		}
	}

	private void installFromFile() {
		FSFile jar = XFileDialog.openFileDialog("Select a plug-in JAR file", CommonExtensionFilters.JAR);
		if (jar != null && ZipArchive.isZip(jar)) {
			String name = jar.getName();
			JGlobalExtensionDB db = JGlobalExtensionDB.getInstance();
			if (!db.hasPlugin(name)) {
				db.addPluginPath(name, jar.getPath());
				refreshAddons();
			} else {
				DialogUtils.showErrorMessage(this, "Duplicate plugin",
					"A plugin with name " + name + " is already registered.");
			}
		}
	}

	void refreshAddons() {
		statusLabel.setText("Loading...");
		addonList.clear();
		localEntries.clear();
		repoEntries.clear();

		// Collect IDs managed by the addon store (repo-installed)
		final Set<String> repoManagedIds = new HashSet<>(InstalledAddonMetadata.getInstalledIds());

		// Load local plugins (ones NOT managed by the addon store)
		JGlobalExtensionDB db = JGlobalExtensionDB.getInstance();
		for (JAbstractPluginDatabase.PluginEntry plg : db.getPlugins()) {
			if (!repoManagedIds.contains(plg.name)) {
				localEntries.add(new LocalPluginEntryPanel(plg, this));
			}
		}

		// Fetch repos in background
		new SwingWorker<List<AddonRepository>, Void>() {
			@Override
			protected List<AddonRepository> doInBackground() {
				return AddonRepositoryManager.fetchAll();
			}

			@Override
			protected void done() {
				try {
					List<AddonRepository> repos = get();
					int repoPluginCount = 0;
					int errorCount = 0;

					for (AddonRepository repo : repos) {
						if (repo.getErrorMessage() != null) {
							errorCount++;
							continue;
						}
						for (AddonManifestEntry entry : repo.plugins) {
							repoEntries.add(new AddonEntryPanel(entry, repo.url, AddonStoreFrame.this));
							repoPluginCount++;
						}
					}

					applyFilter();

					int localCount = localEntries.size();
					String status = localCount + " local, " + repoPluginCount + " from repositories";
					if (errorCount > 0) {
						status += " (" + errorCount + " repo" + (errorCount != 1 ? "s" : "") + " failed)";
					}
					statusLabel.setText(status);
				} catch (Exception ex) {
					// Still show local plugins even if repo fetch fails
					applyFilter();
					statusLabel.setText(localEntries.size() + " local (repo fetch failed: " + ex.getMessage() + ")");
				}
			}
		}.execute();
	}

	@SuppressWarnings("unchecked")
	private void applyFilter() {
		String query = searchField.getText();
		addonList.clear();

		// Local plugins first
		for (LocalPluginEntryPanel entry : localEntries) {
			if (entry.matchesFilter(query)) {
				addonList.addElement(entry);
			}
		}

		// Then repo addons
		for (AddonEntryPanel entry : repoEntries) {
			if (entry.matchesFilter(query)) {
				addonList.addElement(entry);
			}
		}

		addonList.revalidate();
		addonList.repaint();
	}

	public static void main(String[] args) {
		ComponentUtils.setSystemNativeLookAndFeel();
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				new AddonStoreFrame().setVisible(true);
			}
		});
	}
}
