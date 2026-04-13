package ctrmap.creativestudio.ngcs2d.io;

import ctrmap.creativestudio.ngcs.io.FormatDetectorInput;
import ctrmap.creativestudio.ngcs2d.NGCS2D;
import ctrmap.creativestudio.ngcs2d.res.Sprite2DResource;
import xstandard.fs.FSFile;
import xstandard.io.base.impl.ext.data.DataIOStream;

/**
 * Static utility for importing 2D sprite files into a CreativeStudio 2D session.
 * Mirrors NGCSImporter for the 3D system, simplified without container extraction
 * or directory recursion.
 */
public class NGCS2DImporter {

	/**
	 * Imports one or more files using all format handlers registered in the
	 * NGCS2D instance's I/O manager. Each file is matched against handlers
	 * first by extension, then by internal data inspection.
	 *
	 * @param cs    The NGCS2D instance providing the I/O manager.
	 * @param files The files to import.
	 * @return A merged Sprite2DResource containing all imported data.
	 */
	public static Sprite2DResource importFiles(NGCS2D cs, FSFile... files) {
		return importFiles(cs, cs.getIOManager().getAllFormatHandlers(), files);
	}

	/**
	 * Imports one or more files using a specific set of format handlers.
	 * Each file is matched against the provided handlers first by extension,
	 * then by internal data inspection.
	 *
	 * @param cs      The NGCS2D instance.
	 * @param formats The format handlers to try.
	 * @param files   The files to import.
	 * @return A merged Sprite2DResource containing all imported data.
	 */
	public static Sprite2DResource importFiles(NGCS2D cs, I2DFormatHandler[] formats, FSFile... files) {
		Sprite2DResource result = new Sprite2DResource();

		for (FSFile file : files) {
			if (file != null && file.exists()) {
				Sprite2DResource readResult = readFile(file, formats);
				if (readResult != null) {
					result.mergeFull(readResult);
				}
			}
		}
		return result;
	}

	/**
	 * Attempts to read a single file by trying all provided format handlers.
	 * First attempts detection by file extension, then falls back to internal
	 * data inspection if no extension match is found.
	 *
	 * @param file    The file to read.
	 * @param formats The format handlers to try.
	 * @return The imported Sprite2DResource, or null if no handler matched.
	 */
	private static Sprite2DResource readFile(FSFile file, I2DFormatHandler[] formats) {
		try {
			I2DFormatHandler handler = null;

			FormatDetectorInput fid = new FormatDetectorInput();
			fid.fileName = file.getName();

			for (I2DFormatHandler hnd : formats) {
				if (hnd.canImport() && hnd.detectByExtension(fid)) {
					handler = hnd;
					break;
				}
			}

			if (handler == null) {
				DataIOStream stream = file.getDataIOStream();

				fid.magic4int = stream.readInt();
				stream.seek(0);
				fid.magic4str = stream.readPaddedString(4);
				stream.seek(0);
				fid.first16Bytes = stream.readBytes(16);
				fid.stream = stream;

				for (I2DFormatHandler hnd : formats) {
					if (hnd.canImport() && hnd.detectInternals(fid)) {
						handler = hnd;
						break;
					}
				}

				stream.close();
			}

			if (handler != null) {
				return handler.importFile(file, null);
			}
		} catch (Exception ex) {
			System.err.println("Error while reading 2D file: " + file);
			ex.printStackTrace();
		}
		return null;
	}
}
