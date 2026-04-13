package ctrmap.creativestudio;

import ctrmap.creativestudio.ngcs.NGCS;
import ctrmap.creativestudio.ngcs2d.NGCS2D;

public class CreativeStudioChecker {

	public static boolean isCreativeStudioPresent() {
		try {
			NGCS.dummy();
			return true;
		} catch (NoClassDefFoundError ex) {
			return false;
		}
	}

	public static boolean isCreativeStudio2DPresent() {
		try {
			NGCS2D.dummy();
			return true;
		} catch (NoClassDefFoundError ex) {
			return false;
		}
	}
}
