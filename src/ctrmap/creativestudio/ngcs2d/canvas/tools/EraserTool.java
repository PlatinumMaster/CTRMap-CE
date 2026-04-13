package ctrmap.creativestudio.ngcs2d.canvas.tools;

/**
 * Eraser tool that paints with palette index 0 (transparent).
 */
public class EraserTool extends BrushTool {

	@Override
	protected int getPaintColorIndex() {
		return 0;
	}

	@Override
	public String getToolName() {
		return "Eraser";
	}
}
