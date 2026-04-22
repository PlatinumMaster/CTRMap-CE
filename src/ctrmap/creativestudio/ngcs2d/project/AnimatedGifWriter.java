package ctrmap.creativestudio.ngcs2d.project;

import ctrmap.creativestudio.ngcs2d.canvas.SpriteRenderer;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DAnimFrame;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DMultiCellAnimation;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import xstandard.fs.FSFile;

/**
 * Writes a {@link Sprite2DCellAnimation} or
 * {@link Sprite2DMultiCellAnimation} to an animated GIF using only
 * {@code javax.imageio} — no external library.
 *
 * <p>Frame delays come from each frame's {@code duration} field (NDS
 * ticks, 60 Hz) converted to GIF centiseconds (100 Hz). Loops forever
 * via the NETSCAPE2.0 application extension. Disposal method is
 * {@code restoreToBackgroundColor} so transparent regions don't bleed
 * between frames.</p>
 */
public final class AnimatedGifWriter {

	private static final String FORMAT = "gif";

	private AnimatedGifWriter() {
	}

	public static void writeCellAnim(Sprite2DCellAnimation anim, Sprite2DResource res, FSFile gifFile) throws IOException {
		if (anim == null || anim.frames.isEmpty()) {
			throw new IOException("Animation has no frames.");
		}
		int n = anim.frames.size();
		BufferedImage[] frames = new BufferedImage[n];
		int[] delaysCs = new int[n];
		for (int i = 0; i < n; i++) {
			BufferedImage raw = SpriteRenderer.renderAnimFrame(anim, i, res);
			frames[i] = ensureArgb(raw);
			Sprite2DAnimFrame f = anim.frames.get(i);
			delaysCs[i] = ticksToCs(f.duration);
		}
		writeGif(frames, delaysCs, gifFile);
	}

	public static void writeMultiCellAnim(Sprite2DMultiCellAnimation anim, Sprite2DResource res, FSFile gifFile) throws IOException {
		if (anim == null || anim.frames.isEmpty()) {
			throw new IOException("Animation has no frames.");
		}
		int n = anim.frames.size();
		BufferedImage[] frames = new BufferedImage[n];
		int[] delaysCs = new int[n];
		for (int i = 0; i < n; i++) {
			BufferedImage raw = SpriteRenderer.renderMultiCellAnimFrame(anim, i, res);
			frames[i] = ensureArgb(raw);
			delaysCs[i] = ticksToCs(anim.frames.get(i).duration);
		}
		writeGif(frames, delaysCs, gifFile);
	}

	// ------------------------------------------------------------------
	// internals
	// ------------------------------------------------------------------

	private static int ticksToCs(int ticks) {
		// NDS frame ticks (60 Hz) -> GIF centiseconds (100 Hz). Round
		// half-up; clamp to >=1 so frames never get a zero delay (which
		// browsers sometimes round up to 100 ms).
		int cs = Math.round(ticks * 100f / 60f);
		return Math.max(1, cs);
	}

	private static BufferedImage ensureArgb(BufferedImage src) {
		// Normalise to ARGB so frames have consistent dimensions /
		// metadata. SpriteRenderer.EMPTY is a 1x1 placeholder; replace
		// any 0-area frame with a 2x2 transparent block to avoid
		// ImageWriter complaints.
		if (src == null || src.getWidth() < 1 || src.getHeight() < 1) {
			return new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
		}
		if (src.getType() == BufferedImage.TYPE_INT_ARGB) {
			return src;
		}
		BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(),
			BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = copy.createGraphics();
		g.setComposite(java.awt.AlphaComposite.Src);
		g.setColor(new Color(0, 0, 0, 0));
		g.fillRect(0, 0, copy.getWidth(), copy.getHeight());
		g.drawImage(src, 0, 0, null);
		g.dispose();
		return copy;
	}

	private static void writeGif(BufferedImage[] frames, int[] delaysCs, FSFile gifFile) throws IOException {
		// Frames may differ in size (e.g. translateX changing frame canvas).
		// GIF logical screen needs to fit the largest one.
		int maxW = 0, maxH = 0;
		for (BufferedImage f : frames) {
			maxW = Math.max(maxW, f.getWidth());
			maxH = Math.max(maxH, f.getHeight());
		}

		ImageWriter writer = ImageIO.getImageWritersByFormatName(FORMAT).next();
		try (ImageOutputStream out = ImageIO.createImageOutputStream(gifFile.getNativeOutputStream())) {
			writer.setOutput(out);
			writer.prepareWriteSequence(null);

			ImageTypeSpecifier type = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
			javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();

			for (int i = 0; i < frames.length; i++) {
				BufferedImage padded = padToCanvas(frames[i], maxW, maxH);
				IIOMetadata metadata = writer.getDefaultImageMetadata(type, param);
				configureFrameMetadata(metadata, delaysCs[i], i == 0);
				writer.writeToSequence(new IIOImage(padded, null, metadata), param);
			}

			writer.endWriteSequence();
		} finally {
			writer.dispose();
		}
	}

	private static BufferedImage padToCanvas(BufferedImage src, int w, int h) {
		if (src.getWidth() == w && src.getHeight() == h
			&& src.getType() == BufferedImage.TYPE_INT_ARGB) {
			return src;
		}
		BufferedImage padded = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = padded.createGraphics();
		// Center the smaller frame in the canvas so motion stays visually
		// stable across frames of different sizes.
		int dx = (w - src.getWidth()) / 2;
		int dy = (h - src.getHeight()) / 2;
		g.drawImage(src, dx, dy, null);
		g.dispose();
		return padded;
	}

	/**
	 * Sets the GIF GraphicControlExtension (delay + disposal) and, on
	 * the first frame only, the NETSCAPE2.0 ApplicationExtension for
	 * infinite loop. Pattern follows the {@code javax_imageio_gif_image_1.0}
	 * metadata format documented in the JDK Javadoc.
	 */
	private static void configureFrameMetadata(IIOMetadata meta, int delayCs, boolean firstFrame) throws IOException {
		String formatName = meta.getNativeMetadataFormatName();
		IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(formatName);

		IIOMetadataNode gce = getOrCreateChild(root, "GraphicControlExtension");
		gce.setAttribute("disposalMethod", "restoreToBackgroundColor");
		gce.setAttribute("userInputFlag", "FALSE");
		gce.setAttribute("transparentColorFlag", "TRUE");
		gce.setAttribute("delayTime", String.valueOf(delayCs));
		gce.setAttribute("transparentColorIndex", "0");

		if (firstFrame) {
			IIOMetadataNode appExtensions = getOrCreateChild(root, "ApplicationExtensions");
			IIOMetadataNode netscape = new IIOMetadataNode("ApplicationExtension");
			netscape.setAttribute("applicationID", "NETSCAPE");
			netscape.setAttribute("authenticationCode", "2.0");
			// 0x01 = sub-block id, then loopCount as little-endian uint16.
			// 0 = infinite loop.
			netscape.setUserObject(new byte[]{0x1, 0x0, 0x0});
			appExtensions.appendChild(netscape);
		}

		meta.setFromTree(formatName, root);
	}

	private static IIOMetadataNode getOrCreateChild(IIOMetadataNode parent, String name) {
		for (int i = 0; i < parent.getLength(); i++) {
			if (parent.item(i).getNodeName().equalsIgnoreCase(name)) {
				return (IIOMetadataNode) parent.item(i);
			}
		}
		IIOMetadataNode created = new IIOMetadataNode(name);
		parent.appendChild(created);
		return created;
	}
}
