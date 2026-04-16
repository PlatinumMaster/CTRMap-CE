package ctrmap.util.gui.addon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import rtldr.JAbstractPluginDatabase;
import rtldr.JGlobalExtensionDB;

/**
 * A panel representing a locally-installed plugin (added via JAR file, not from a repository).
 */
public class LocalPluginEntryPanel extends JPanel {

	private final JAbstractPluginDatabase.PluginEntry plugin;
	private final AddonStoreFrame storeFrame;

	public LocalPluginEntryPanel(JAbstractPluginDatabase.PluginEntry plugin, AddonStoreFrame storeFrame) {
		this.plugin = plugin;
		this.storeFrame = storeFrame;

		setLayout(new BorderLayout(10, 0));
		setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		setOpaque(false);

		// Left: info
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
		infoPanel.setOpaque(false);

		JLabel nameLabel = new JLabel(plugin.name);
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
		nameLabel.setAlignmentX(LEFT_ALIGNMENT);
		infoPanel.add(nameLabel);

		JLabel pathLabel = new JLabel(plugin.path);
		pathLabel.setForeground(Color.GRAY);
		pathLabel.setAlignmentX(LEFT_ALIGNMENT);
		infoPanel.add(pathLabel);

		JLabel localLabel = new JLabel("Local plug-in");
		localLabel.setForeground(new Color(0x88, 0x88, 0x88));
		localLabel.setFont(localLabel.getFont().deriveFont(Font.ITALIC));
		localLabel.setAlignmentX(LEFT_ALIGNMENT);
		infoPanel.add(localLabel);

		add(infoPanel, BorderLayout.CENTER);

		// Right: uninstall
		JButton btnUninstall = new JButton("Uninstall");
		btnUninstall.setPreferredSize(new Dimension(100, 25));
		btnUninstall.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JGlobalExtensionDB.getInstance().removePlugin(plugin.name);
				storeFrame.refreshAddons();
			}
		});
		add(btnUninstall, BorderLayout.LINE_END);
	}

	/**
	 * Returns true if this entry matches the given search query.
	 */
	public boolean matchesFilter(String query) {
		if (query == null || query.isEmpty()) {
			return true;
		}
		String lower = query.toLowerCase();
		return plugin.name.toLowerCase().contains(lower)
			|| plugin.path.toLowerCase().contains(lower);
	}
}
