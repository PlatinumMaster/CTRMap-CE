package ctrmap.creativestudio.ngcs2d.res;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Regression tests for {@link Sprite2DResource#getActiveTileSheet()}.
 *
 * <p>BW / BW2 Pokemon battle sprite NARCs ship two NCGR files per species:
 * a bitmap raster-layout sheet (the one the NCER's OAM tileIndices are
 * actually designed to address) and a smaller tiled fragment sheet used
 * for specific animations. The renderer used to unconditionally pick
 * {@code tileSheets.get(0)}, which rendered Bulbasaur as a small broken
 * fragment when the fragment sheet was at index 0. {@code getActiveTileSheet}
 * now prefers the raster-layout sheet when the resource's mappingMode is
 * 2D, restoring correct rendering.</p>
 */
public class Sprite2DResourceTileSheetTest {

    private Sprite2DTileSheet makeTileSheet(String name, boolean raster) {
        Sprite2DTileSheet ts = new Sprite2DTileSheet();
        ts.name = name;
        ts.rasterLayout = raster;
        ts.format = 3;
        ts.tileData = new byte[32 * 32];
        ts.tileWidth = 32;
        ts.tileHeight = 1;
        return ts;
    }

    @Test
    public void emptyResourceReturnsNull() {
        Sprite2DResource res = new Sprite2DResource();
        assertNull(res.getActiveTileSheet());
    }

    @Test
    public void singleTileSheetReturnsIt() {
        Sprite2DResource res = new Sprite2DResource();
        Sprite2DTileSheet only = makeTileSheet("only", false);
        res.tileSheets.add(only);
        assertSame(only, res.getActiveTileSheet());
    }

    @Test
    public void twoD_prefersRasterLayout_whenFragmentSheetAtIndexZero() {
        // Exactly the Pokemon battle sprite case: tiled fragment sheet
        // loaded first, bitmap raster sheet loaded second. Without the fix
        // getActiveTileSheet would return the fragment sheet and the
        // renderer would produce broken fragments.
        Sprite2DResource res = new Sprite2DResource();
        res.mappingMode = Sprite2DResource.MAPPING_MODE_2D;
        Sprite2DTileSheet fragment = makeTileSheet("fragment", false);
        Sprite2DTileSheet bitmap = makeTileSheet("bitmap", true);
        res.tileSheets.add(fragment);
        res.tileSheets.add(bitmap);
        assertSame("Must pick the raster-layout sheet for 2D mapping",
            bitmap, res.getActiveTileSheet());
    }

    @Test
    public void twoD_prefersRasterLayout_whenBitmapAtIndexZero() {
        Sprite2DResource res = new Sprite2DResource();
        res.mappingMode = Sprite2DResource.MAPPING_MODE_2D;
        Sprite2DTileSheet bitmap = makeTileSheet("bitmap", true);
        Sprite2DTileSheet fragment = makeTileSheet("fragment", false);
        res.tileSheets.add(bitmap);
        res.tileSheets.add(fragment);
        assertSame(bitmap, res.getActiveTileSheet());
    }

    @Test
    public void twoD_fallsBackToFirst_whenNoRasterLayoutSheet() {
        // Pure-tiled resource (e.g. synthetic test data) — 2D mode is set
        // but there's no raster sheet to prefer; falling back to tileSheets[0]
        // keeps single-sheet 2D resources working.
        Sprite2DResource res = new Sprite2DResource();
        res.mappingMode = Sprite2DResource.MAPPING_MODE_2D;
        Sprite2DTileSheet a = makeTileSheet("a", false);
        Sprite2DTileSheet b = makeTileSheet("b", false);
        res.tileSheets.add(a);
        res.tileSheets.add(b);
        assertSame(a, res.getActiveTileSheet());
    }

    @Test
    public void oneDMode_doesNotDisturbExistingOrder() {
        // Trainer sprite case: NCER advertises 1D mapping and ships a
        // single raster NCGR. Only one sheet, no preference change.
        Sprite2DResource res = new Sprite2DResource();
        res.mappingMode = Sprite2DResource.MAPPING_MODE_1D_64K;
        Sprite2DTileSheet only = makeTileSheet("trainer", true);
        res.tileSheets.add(only);
        assertSame(only, res.getActiveTileSheet());
    }

    @Test
    public void oneDMode_withTwoSheets_picksFirst() {
        // Never-seen-in-the-wild 1D multi-NCGR case: no reason to prefer
        // the raster sheet since we're not in the 2D Pokemon-battle model.
        Sprite2DResource res = new Sprite2DResource();
        res.mappingMode = Sprite2DResource.MAPPING_MODE_1D_32K;
        Sprite2DTileSheet a = makeTileSheet("a", false);
        Sprite2DTileSheet b = makeTileSheet("b", true);
        res.tileSheets.add(a);
        res.tileSheets.add(b);
        assertSame("1D mapping keeps legacy first-sheet behavior",
            a, res.getActiveTileSheet());
    }
}
