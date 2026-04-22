package ctrmap.creativestudio.ngcs2d.layers;

/**
 * Common interface implemented by any object the Layers panel can show
 * as a row. In practice this is either a {@code MultiCellEntry}
 * (body-part granularity — the primary use case: each entry = one
 * animated part of a multi-cell composition) or a {@code Sprite2DOAM}
 * (per-hardware-object granularity — secondary, rarely the right
 * abstraction for end-users but handy when editing a single cell in
 * isolation).
 *
 * <p>Four fields only, matching Photoshop's layer minimum: name,
 * visibility, opacity, lock. NITRO can't encode opacity on disk; the
 * field is editor-preview only. Both the canvas renderer and the
 * format exporters already honour {@code visible} (Phase 1 plumbing).</p>
 */
public interface LayerItem {

	String getLayerName();

	void setLayerName(String name);

	boolean isVisible();

	void setVisible(boolean visible);

	float getOpacity();

	void setOpacity(float opacity);

	boolean isLocked();

	void setLocked(boolean locked);
}
