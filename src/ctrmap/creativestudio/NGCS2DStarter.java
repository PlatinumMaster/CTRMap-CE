package ctrmap.creativestudio;

import ctrmap.Launc;
import ctrmap.creativestudio.ngcs2d.NGCS2D;

public class NGCS2DStarter implements Launc.SubprocessStarter {

	public static final NGCS2DStarter INSTANCE = new NGCS2DStarter();

	private NGCS2DStarter() {

	}

	@Override
	public boolean start() {
		if (CreativeStudioChecker.isCreativeStudio2DPresent()) {
			NGCS2D.main(null);
			return true;
		} else {
			return false;
		}
	}
}
