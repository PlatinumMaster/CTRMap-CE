package ctrmap.creativestudio.ngcs2d.canvas;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

/**
 * Vertical tool strip panel with drawing tool selection buttons.
 *
 * <p>Each tool has a full text label so the buttons remain readable even
 * when the canvas is narrow. Buttons are sized large enough to fit the
 * longest label without truncation.</p>
 */
public class SpriteToolStrip extends JPanel {

	/**
	 * Available tool types.
	 */
	public enum ToolType {
		PENCIL, BRUSH, ERASER, FILL, COLOR_PICKER, SELECT, MOVE, PAN
	}

	private final ButtonGroup toolGroup = new ButtonGroup();
	private final JToggleButton[] toolButtons;
	private final ToolType[] toolTypes = ToolType.values();
	private final JPanel fgSwatch;
	private final JPanel bgSwatch;
	private final List<ActionListener> toolChangeListeners = new ArrayList<>();

	private Color foregroundColor = Color.BLACK;

	/** Full text labels (visible on the buttons themselves). */
	private static final String[] LABELS = {
		"Pencil", "Brush", "Eraser", "Fill",
		"Picker", "Select", "Move", "Pan"
	};

	/** Tooltips with the longer descriptive form. */
	private static final String[] TOOLTIPS = {
		"Pencil Tool", "Brush Tool", "Eraser Tool", "Fill Tool",
		"Color Picker", "Select Tool", "Move Tool", "Pan Tool"
	};

	private static final int STRIP_WIDTH = 96;
	private static final int BUTTON_HEIGHT = 28;
	private static final int SWATCH_SIZE = 32;

	public SpriteToolStrip() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setPreferredSize(new Dimension(STRIP_WIDTH, 480));
		setMinimumSize(new Dimension(STRIP_WIDTH, 200));
		setBorder(BorderFactory.createEtchedBorder());

		toolButtons = new JToggleButton[toolTypes.length];
		for (int i = 0; i < toolTypes.length; i++) {
			JToggleButton btn = new JToggleButton(LABELS[i]);
			btn.setToolTipText(TOOLTIPS[i]);
			btn.setHorizontalAlignment(SwingConstants.CENTER);
			btn.setAlignmentX(Component.CENTER_ALIGNMENT);
			Dimension btnSize = new Dimension(STRIP_WIDTH - 12, BUTTON_HEIGHT);
			btn.setMinimumSize(btnSize);
			btn.setPreferredSize(btnSize);
			btn.setMaximumSize(btnSize);
			btn.addActionListener(e -> fireToolChange());
			toolGroup.add(btn);
			toolButtons[i] = btn;
			add(btn);
			add(Box.createVerticalStrut(2));
		}

		toolButtons[0].setSelected(true);

		add(Box.createVerticalStrut(4));
		JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
		sep.setMaximumSize(new Dimension(STRIP_WIDTH - 8, 4));
		add(sep);
		add(Box.createVerticalStrut(4));

		fgSwatch = new JPanel();
		fgSwatch.setBackground(Color.BLACK);
		fgSwatch.setAlignmentX(Component.CENTER_ALIGNMENT);
		Dimension swatchDim = new Dimension(SWATCH_SIZE, SWATCH_SIZE);
		fgSwatch.setPreferredSize(swatchDim);
		fgSwatch.setMaximumSize(swatchDim);
		fgSwatch.setMinimumSize(swatchDim);
		fgSwatch.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
		add(fgSwatch);

		add(Box.createVerticalStrut(4));

		bgSwatch = new JPanel();
		bgSwatch.setBackground(Color.WHITE);
		bgSwatch.setAlignmentX(Component.CENTER_ALIGNMENT);
		bgSwatch.setPreferredSize(swatchDim);
		bgSwatch.setMaximumSize(swatchDim);
		bgSwatch.setMinimumSize(swatchDim);
		bgSwatch.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
		add(bgSwatch);
	}

	/**
	 * Gets the currently selected tool type.
	 */
	public ToolType getSelectedTool() {
		for (int i = 0; i < toolButtons.length; i++) {
			if (toolButtons[i].isSelected()) {
				return toolTypes[i];
			}
		}
		return ToolType.PENCIL;
	}

	/**
	 * Adds a listener that is notified when the selected tool changes.
	 */
	public void addToolChangeListener(ActionListener listener) {
		toolChangeListeners.add(listener);
	}

	private void fireToolChange() {
		for (ActionListener l : toolChangeListeners) {
			l.actionPerformed(null);
		}
	}

	/**
	 * Sets the foreground color swatch display.
	 */
	public void setForegroundColor(Color c) {
		this.foregroundColor = c;
		fgSwatch.setBackground(c);
	}

	/**
	 * Gets the current foreground color.
	 */
	public Color getForegroundColor() {
		return foregroundColor;
	}
}
