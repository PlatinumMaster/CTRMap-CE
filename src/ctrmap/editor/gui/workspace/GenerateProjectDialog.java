package ctrmap.editor.gui.workspace;

import ctrmap.editor.system.workspace.CTRMapProject;
import ctrmap.editor.system.workspace.GameDetector;
import ctrmap.editor.system.workspace.GameRegistryData;
import ctrmap.formats.common.GameInfo;
import ctrmap.formats.ntr.rom.srl.NDSROM;
import xstandard.fs.FSFile;
import xstandard.fs.FSUtil;
import xstandard.fs.accessors.DiskFile;
import xstandard.gui.DialogUtils;
import xstandard.gui.file.XFileDialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

public class GenerateProjectDialog extends javax.swing.JDialog {

	private JTextField stockRomField;
	private JTextField modRomField;
	private JTextField stockExtractField;
	private JTextField projectPathField;

	private boolean stockExtractUserSelected = false;
	private boolean projectPathUserSelected = false;

	private final List<OnProjectGenerateListener> listeners = new ArrayList<>();
	private final List<CanAddGameVerifier> gameVerifiers = new ArrayList<>();

	public GenerateProjectDialog(Frame parent, boolean modal) {
		super(parent, modal);
		setTitle("Generate Project from ROM");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setResizable(false);
		initLayout();
		setLocationRelativeTo(parent);
	}

	public void addOnProjectGenerateListener(OnProjectGenerateListener l) {
		listeners.add(l);
	}

	public void addCanAddGameVerifier(CanAddGameVerifier v) {
		gameVerifiers.add(v);
	}

	private void initLayout() {
		JPanel content = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(2, 6, 2, 6);
		int row = 0;

		// Stock ROM
		addLabel(content, gbc, row++, "Stock (Unmodified) ROM");
		stockRomField = new JTextField();
		JButton btnBrowseStock = new JButton("Browse");
		addFieldRow(content, gbc, row++, stockRomField, btnBrowseStock);

		// Modified ROM
		addLabel(content, gbc, row++, "Modified ROM");
		modRomField = new JTextField();
		JButton btnBrowseMod = new JButton("Browse");
		addFieldRow(content, gbc, row++, modRomField, btnBrowseMod);

		// Stock extraction path
		addLabel(content, gbc, row++, "Stock ROM Extraction Path");
		stockExtractField = new JTextField();
		JButton btnBrowseExtract = new JButton("Browse");
		addFieldRow(content, gbc, row++, stockExtractField, btnBrowseExtract);

		// Project path
		addLabel(content, gbc, row++, "Project Directory");
		projectPathField = new JTextField();
		JButton btnBrowseProject = new JButton("Browse");
		addFieldRow(content, gbc, row++, projectPathField, btnBrowseProject);

		// Buttons
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		JButton btnCancel = new JButton("Cancel");
		JButton btnGenerate = new JButton("Generate Project");
		buttonPanel.add(btnCancel);
		buttonPanel.add(btnGenerate);

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(12, 6, 6, 6);
		content.add(buttonPanel, gbc);

		setContentPane(content);

		// Events
		btnBrowseStock.addActionListener(e -> browseStockRom());
		btnBrowseMod.addActionListener(e -> browseModRom());
		btnBrowseExtract.addActionListener(e -> browseStockExtract());
		btnBrowseProject.addActionListener(e -> browseProject());
		btnCancel.addActionListener(e -> dispose());
		btnGenerate.addActionListener(e -> doGenerate());

		pack();
		setMinimumSize(new Dimension(500, getHeight()));
	}

	private void addLabel(JPanel panel, GridBagConstraints gbc, int row, String text) {
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		panel.add(new JLabel(text), gbc);
	}

	private void addFieldRow(JPanel panel, GridBagConstraints gbc, int row, JTextField field, JButton btn) {
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		panel.add(field, gbc);

		gbc.gridx = 1;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		panel.add(btn, gbc);
	}

	private void browseStockRom() {
		String path = stockRomField.getText();
		FSFile select;
		if (path != null && !path.trim().isEmpty()) {
			String parent = FSUtil.getParentFilePath(path);
			String name = FSUtil.getFileName(path);
			select = XFileDialog.openFileDialog("Select the stock (unmodified) ROM", false, new DiskFile(parent), name, NDSROM.EXTENSION_FILTER);
		} else {
			select = XFileDialog.openFileDialog("Select the stock (unmodified) ROM", NDSROM.EXTENSION_FILTER);
		}
		if (select != null) {
			stockRomField.setText(select.getPath());
			if (!stockExtractUserSelected) {
				String parent = FSUtil.getParentFilePath(select.getPath());
				String name = FSUtil.getFileNameWithoutExtension(select.getName());
				stockExtractField.setText(parent + "/" + name + "_Extracted");
			}
		}
	}

	private void browseModRom() {
		String path = modRomField.getText();
		FSFile select;
		if (path != null && !path.trim().isEmpty()) {
			String parent = FSUtil.getParentFilePath(path);
			String name = FSUtil.getFileName(path);
			select = XFileDialog.openFileDialog("Select the modified ROM", false, new DiskFile(parent), name, NDSROM.EXTENSION_FILTER);
		} else {
			select = XFileDialog.openFileDialog("Select the modified ROM", NDSROM.EXTENSION_FILTER);
		}
		if (select != null) {
			modRomField.setText(select.getPath());
			if (!projectPathUserSelected) {
				String parent = FSUtil.getParentFilePath(select.getPath());
				String name = FSUtil.getFileNameWithoutExtension(select.getName());
				projectPathField.setText(parent + "/" + name + "_Project");
			}
		}
	}

	private void browseStockExtract() {
		String path = stockExtractField.getText();
		FSFile select = XFileDialog.openDirectoryDialog("Select stock ROM extraction directory", (path == null || path.trim().isEmpty()) ? null : new DiskFile(path));
		if (select != null) {
			stockExtractField.setText(select.getPath());
			stockExtractUserSelected = true;
		}
	}

	private void browseProject() {
		String path = projectPathField.getText();
		FSFile select = XFileDialog.openDirectoryDialog("Select project directory", (path == null || path.trim().isEmpty()) ? null : new DiskFile(path));
		if (select != null) {
			projectPathField.setText(select.getPath());
			projectPathUserSelected = true;
		}
	}

	private void doGenerate() {
		String stockRomPath = stockRomField.getText().trim();
		String modRomPath = modRomField.getText().trim();
		String stockExtractPath = stockExtractField.getText().trim();
		String projectPath = projectPathField.getText().trim();

		// Validate fields
		if (stockRomPath.isEmpty() || modRomPath.isEmpty() || stockExtractPath.isEmpty() || projectPath.isEmpty()) {
			DialogUtils.showErrorMessage(this, "Missing fields", "All fields must be filled in.");
			return;
		}

		DiskFile stockRomFile = new DiskFile(stockRomPath);
		DiskFile modRomFile = new DiskFile(modRomPath);

		if (!stockRomFile.isFile()) {
			DialogUtils.showErrorMessage(this, "File not found", "The stock ROM file does not exist or is a directory.");
			return;
		}
		if (!modRomFile.isFile()) {
			DialogUtils.showErrorMessage(this, "File not found", "The modified ROM file does not exist or is a directory.");
			return;
		}
		if (stockRomPath.equals(modRomPath)) {
			DialogUtils.showErrorMessage(this, "Same ROM", "The stock and modified ROM cannot be the same file.");
			return;
		}

		FSFile stockExtractDir = new DiskFile(stockExtractPath);
		FSFile projectDir = new DiskFile(projectPath);

		if (stockExtractPath.equals(projectPath)) {
			DialogUtils.showErrorMessage(this, "Path conflict", "The stock extraction path and project directory cannot be the same.");
			return;
		}

		if (stockExtractDir.isFile()) {
			DialogUtils.showErrorMessage(this, "Invalid path", "The stock extraction path cannot be a file.");
			return;
		}

		for (CanAddGameVerifier v : gameVerifiers) {
			if (!v.verifyGamePath(GameRegistryData.normalizePath(stockExtractPath))) {
				return;
			}
		}

		if (projectDir.isFile()) {
			DialogUtils.showErrorMessage(this, "Invalid path", "The project directory cannot be a file.");
			return;
		}
		if (projectDir.exists() && projectDir.getChildCount() > 0) {
			if (!DialogUtils.showYesNoWarningDialog(this, "Directory not empty", "The project directory is not empty. Continue?")) {
				return;
			}
		}

		// Show progress dialog
		CTRMapInitDialog progressDlg = new CTRMapInitDialog((Frame) getOwner(), true);
		progressDlg.setTitle("Generating Project...");
		progressDlg.setLocationRelativeTo(this);
		final JTextArea log = progressDlg.getTextArea();

		SwingWorker<FSFile, Void> worker = new SwingWorker<FSFile, Void>() {
			@Override
			protected FSFile doInBackground() throws Exception {
				FSFile tempModDir = null;

				try {
					// Step 1: Extract stock ROM if needed
					boolean needStockExtract = true;
					if (stockExtractDir.exists()) {
						FSFile headerBin = stockExtractDir.getChild("header.bin");
						if (headerBin != null && headerBin.exists()) {
							needStockExtract = false;
						}
					}

					if (needStockExtract) {
						log.append("Extracting stock ROM...\n");
						stockExtractDir.mkdirs();
						NDSROM.extractROM(stockRomFile, stockExtractDir);
					} else {
						log.append("Stock ROM already extracted, skipping.\n");
					}

					// Step 2: Verify stock game is recognized
					log.append("Detecting game type...\n");
					GameInfo.Game game = GameDetector.detectGameType(stockExtractDir);
					if (game == null) {
						String fourCC = GameDetector.getFourCC(stockExtractDir);
						throw new IOException("Stock ROM is not a recognized game (FourCC: " + fourCC + ")");
					}
					GameInfo.SubGame subGame = GameDetector.detectSubGame(stockExtractDir);
					log.append("Detected game: " + (subGame != null ? subGame.friendlyName : game.toString()) + "\n");

					// Step 3: Extract modified ROM to temp directory
					log.append("Extracting modified ROM...\n");
					tempModDir = new DiskFile(projectDir.getPath() + "_modtemp");
					tempModDir.mkdirs();
					NDSROM.extractROM(modRomFile, tempModDir);

					// Step 4: Verify game codes match
					log.append("Verifying game compatibility...\n");
					String stockFourCC = GameDetector.getFourCC(stockExtractDir);
					String modFourCC = GameDetector.getFourCC(tempModDir);
					if (stockFourCC != null && modFourCC != null && !stockFourCC.equals(modFourCC)) {
						throw new IOException("Game codes don't match! Stock: " + stockFourCC + ", Modified: " + modFourCC);
					}
					log.append("Game codes match: " + stockFourCC + "\n");

					// Step 5: Create project structure
					log.append("Creating project structure...\n");
					projectDir.mkdirs();
					FSFile vfsDir = projectDir.getChild("vfs");
					vfsDir.mkdirs();

					// Step 6: Diff and copy changed files to VFS overlay
					log.append("Comparing files and generating VFS overlay...\n");
					int changedCount = diffAndCopyToVfs(stockExtractDir, tempModDir, vfsDir, log);
					log.append("Found " + changedCount + " changed file(s).\n");

					// Step 7: Create .cmproj
					log.append("Creating project configuration...\n");
					FSFile prjFile = CTRMapProject.initProject(projectDir, stockExtractDir.getPath());

					log.append("\nProject generated successfully!\n");
					progressDlg.dispose();
					return prjFile;
				} finally {
					if (tempModDir != null) {
						log.append("Cleaning up temporary files...\n");
						try {
							deleteRecursive(tempModDir);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}

			@Override
			protected void done() {
				try {
					get();
				} catch (Exception ex) {
					Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
					DialogUtils.showExceptionTraceDialog(cause);
					progressDlg.dispose();
				}
			}
		};

		worker.execute();
		progressDlg.setVisible(true);

		try {
			FSFile projectFile = worker.get();
			if (projectFile != null) {
				for (OnProjectGenerateListener l : listeners) {
					l.onProjectGenerated(projectFile, stockExtractPath);
				}
				dispose();
			}
		} catch (Exception ex) {
			// Error already displayed by done()
		}
	}

	private int diffAndCopyToVfs(FSFile stockRoot, FSFile modRoot, FSFile vfsDir, JTextArea log) {
		return diffRecursive(stockRoot, modRoot, vfsDir, stockRoot, modRoot, log);
	}

	private int diffRecursive(FSFile stockNode, FSFile modNode, FSFile vfsDir, FSFile stockRoot, FSFile modRoot, JTextArea log) {
		if (!stockNode.exists() || !modNode.exists()) {
			return 0;
		}

		int count = 0;

		if (stockNode.isDirectory() && modNode.isDirectory()) {
			List<? extends FSFile> stockChildren = stockNode.listFiles();
			Set<String> stockNames = new HashSet<>();

			if (stockChildren != null) {
				for (FSFile sc : stockChildren) {
					stockNames.add(sc.getName());
					FSFile modChild = modNode.getChild(sc.getName());
					if (modChild != null && modChild.exists()) {
						count += diffRecursive(sc, modChild, vfsDir, stockRoot, modRoot, log);
					}
				}
			}

			// Check for new files only in modified ROM
			List<? extends FSFile> modChildren = modNode.listFiles();
			if (modChildren != null) {
				for (FSFile mc : modChildren) {
					if (!stockNames.contains(mc.getName())) {
						count += copyNewToVfs(mc, vfsDir, modRoot, log);
					}
				}
			}
		} else if (!stockNode.isDirectory() && !modNode.isDirectory()) {
			// Both are files, compare contents
			byte[] stockBytes = stockNode.getBytes();
			byte[] modBytes = modNode.getBytes();
			if (!Arrays.equals(stockBytes, modBytes)) {
				String relativePath = stockNode.getPathRelativeTo(stockRoot);
				FSFile vfsTarget = vfsDir.getChild(relativePath);
				vfsTarget.getParent().mkdirs();
				vfsTarget.setBytes(modBytes);
				log.append("  Changed: " + relativePath + "\n");
				count++;
			}
		}

		return count;
	}

	private int copyNewToVfs(FSFile modNode, FSFile vfsDir, FSFile modRoot, JTextArea log) {
		int count = 0;

		if (modNode.isDirectory()) {
			List<? extends FSFile> children = modNode.listFiles();
			if (children != null) {
				for (FSFile child : children) {
					count += copyNewToVfs(child, vfsDir, modRoot, log);
				}
			}
		} else {
			String relativePath = modNode.getPathRelativeTo(modRoot);
			FSFile vfsTarget = vfsDir.getChild(relativePath);
			vfsTarget.getParent().mkdirs();
			vfsTarget.setBytes(modNode.getBytes());
			log.append("  New: " + relativePath + "\n");
			count++;
		}

		return count;
	}

	private static void deleteRecursive(FSFile dir) {
		if (dir.isDirectory()) {
			List<? extends FSFile> children = dir.listFiles();
			if (children != null) {
				for (FSFile child : children) {
					deleteRecursive(child);
				}
			}
		}
		dir.delete();
	}

	public static interface OnProjectGenerateListener {
		public void onProjectGenerated(FSFile projectFile, String gamePath);
	}
}
