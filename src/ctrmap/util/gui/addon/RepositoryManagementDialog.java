package ctrmap.util.gui.addon;

import ctrmap.editor.system.addon.AddonRepositoryManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Modal dialog for managing addon repository URLs.
 */
public class RepositoryManagementDialog extends JDialog {

	private final DefaultListModel<String> listModel;
	private final JList<String> repoList;

	public RepositoryManagementDialog(Frame owner) {
		super(owner, "Manage Repositories", true);
		setSize(500, 300);
		setLocationRelativeTo(owner);
		setLayout(new BorderLayout(5, 5));
		getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		listModel = new DefaultListModel<>();
		repoList = new JList<>(listModel);

		List<String> urls = AddonRepositoryManager.getRepositoryUrls();
		for (String url : urls) {
			listModel.addElement(url);
		}

		add(new JScrollPane(repoList), BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton btnAdd = new JButton("Add");
		btnAdd.setPreferredSize(new Dimension(90, 25));
		btnAdd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String url = JOptionPane.showInputDialog(
					RepositoryManagementDialog.this,
					"Enter repository manifest URL:",
					"Add Repository",
					JOptionPane.PLAIN_MESSAGE
				);
				if (url != null && !url.trim().isEmpty()) {
					url = url.trim();
					AddonRepositoryManager.addRepository(url);
					listModel.addElement(url);
				}
			}
		});
		buttonPanel.add(btnAdd);

		JButton btnRemove = new JButton("Remove");
		btnRemove.setPreferredSize(new Dimension(90, 25));
		btnRemove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selected = repoList.getSelectedValue();
				if (selected != null) {
					AddonRepositoryManager.removeRepository(selected);
					listModel.removeElement(selected);
				}
			}
		});
		buttonPanel.add(btnRemove);

		JButton btnClose = new JButton("Close");
		btnClose.setPreferredSize(new Dimension(90, 25));
		btnClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		buttonPanel.add(btnClose);

		add(buttonPanel, BorderLayout.SOUTH);
	}
}
