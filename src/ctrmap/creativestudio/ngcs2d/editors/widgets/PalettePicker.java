package ctrmap.creativestudio.ngcs2d.editors.widgets;

import ctrmap.creativestudio.ngcs2d.res.Sprite2DPalette;
import java.awt.Component;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * JComboBox of {@link Sprite2DPalette} entries used by the NCGR / NCER
 * inspectors to pick which palette colors the live preview renders with.
 *
 * <p>Cosmetic only — switching the active palette never produces an undo
 * entry. The combo rebuilds itself on each {@link #setPalettes(List)}
 * call (typically from {@code handleObject()}) so palette additions in
 * the resource tree are picked up the next time the editor is loaded.</p>
 */
public class PalettePicker extends JComboBox<Sprite2DPalette> {

	private Consumer<Sprite2DPalette> selectionListener;
	private boolean suppressEvents = false;

	public PalettePicker() {
		super(new DefaultComboBoxModel<>());
		setRenderer(new PaletteRenderer());
		addActionListener(e -> {
			if (suppressEvents || selectionListener == null) return;
			selectionListener.accept(getSelectedPalette());
		});
	}

	/**
	 * Replaces the combo's contents with the given palette list. Tries to
	 * preserve the previously-selected palette by reference; falls back to
	 * the first entry if it was removed.
	 */
	public void setPalettes(List<Sprite2DPalette> palettes) {
		Sprite2DPalette previous = getSelectedPalette();
		suppressEvents = true;
		try {
			DefaultComboBoxModel<Sprite2DPalette> model =
				(DefaultComboBoxModel<Sprite2DPalette>) getModel();
			model.removeAllElements();
			if (palettes == null || palettes.isEmpty()) {
				return;
			}
			for (Sprite2DPalette p : palettes) {
				model.addElement(p);
			}
			if (previous != null && palettes.contains(previous)) {
				setSelectedItem(previous);
			} else {
				setSelectedIndex(0);
			}
		} finally {
			suppressEvents = false;
		}
	}

	/** @return the currently selected palette, or {@code null} if empty. */
	public Sprite2DPalette getSelectedPalette() {
		Object o = getSelectedItem();
		return (o instanceof Sprite2DPalette) ? (Sprite2DPalette) o : null;
	}

	/**
	 * Installs a callback fired when the user picks a different palette.
	 * Programmatic {@link #setPalettes(List)} updates do not fire it.
	 */
	public void setSelectionListener(Consumer<Sprite2DPalette> listener) {
		this.selectionListener = listener;
	}

	private static class PaletteRenderer extends JLabel implements ListCellRenderer<Sprite2DPalette> {
		PaletteRenderer() {
			setOpaque(true);
		}
		@Override
		public Component getListCellRendererComponent(JList<? extends Sprite2DPalette> list,
				Sprite2DPalette value, int index, boolean isSelected, boolean cellHasFocus) {
			if (value == null) {
				setText("(no palette)");
			} else {
				String name = value.getName();
				setText((name != null && !name.isEmpty() ? name : "Palette")
					+ "  (" + value.format + " colors)");
			}
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			return this;
		}
	}
}
