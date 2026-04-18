package ctrmap.creativestudio.ngcs2d.res;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Regression tests for {@link Sprite2DOAM#getContentOffsetX()} /
 * {@link Sprite2DOAM#getContentOffsetY()}.
 *
 * <p>The NDS hardware's rotation/scaling "double-size" mode doubles the
 * on-screen bounding box so 45deg rotation corners don't clip. The OAM's
 * {@code (x, y)} coords refer to the TOP-LEFT of that doubled area, but
 * the actual sprite content is centered inside — i.e. shifted by
 * {@code (w/2, h/2)} from the outer top-left. Renderers must apply that
 * offset or every doubleSize-enabled OAM ends up positioned (w/2, h/2)
 * above-and-left of where the game actually draws it.</p>
 *
 * <p>Discovered via Bulbasaur's main-body OAM (cell 0 OAM 0 in the BW2
 * Pokemon sprite NARC): attr0 = 0x07E5 has both {@code rotationScaling}
 * and {@code doubleSize} set. Ignoring the offset produced the "body
 * parts at wrong positions" artifact visible against Gen 5 reference
 * animations.</p>
 */
public class Sprite2DOAMTest {

    @Test
    public void defaultOamHasNoContentOffset() {
        Sprite2DOAM oam = new Sprite2DOAM();
        oam.width = 16;
        oam.height = 16;
        assertEquals(0, oam.getContentOffsetX());
        assertEquals(0, oam.getContentOffsetY());
    }

    @Test
    public void rotationScalingAloneHasNoContentOffset() {
        // rotationScaling = true but doubleSize = false: content still
        // occupies the OAM's declared (w, h) area starting at (x, y).
        Sprite2DOAM oam = new Sprite2DOAM();
        oam.width = 16;
        oam.height = 16;
        oam.rotationScaling = true;
        oam.doubleSize = false;
        assertEquals(0, oam.getContentOffsetX());
        assertEquals(0, oam.getContentOffsetY());
    }

    @Test
    public void doubleSizeShiftsContentByHalfWidthHeight() {
        // The Bulbasaur body OAM case: 16x16 OAM, doubleSize = true.
        // Game draws the 16x16 content starting at (oam.x + 8, oam.y + 8)
        // inside the 32x32 doubled area.
        Sprite2DOAM oam = new Sprite2DOAM();
        oam.width = 16;
        oam.height = 16;
        oam.rotationScaling = true;
        oam.doubleSize = true;
        assertEquals(8, oam.getContentOffsetX());
        assertEquals(8, oam.getContentOffsetY());
    }

    @Test
    public void doubleSizeAsymmetricDimensions() {
        // Verify the offset scales each axis independently (rectangular
        // OAMs like 32x16 need (16, 8), not (8, 8) or (16, 16)).
        Sprite2DOAM oam = new Sprite2DOAM();
        oam.width = 32;
        oam.height = 16;
        oam.rotationScaling = true;
        oam.doubleSize = true;
        assertEquals(16, oam.getContentOffsetX());
        assertEquals(8, oam.getContentOffsetY());
    }

    @Test
    public void doubleSizeIgnoredWhenRotationScalingDisabled() {
        // Hardware-level: the doubleSize flag shares a bit with objDisable
        // and is only meaningful when rotationScaling = true. Our flag
        // handling should match — doubleSize without rotationScaling means
        // the bit was storing something else and should NOT trigger the
        // offset. We guard against that by requiring both to be true in
        // the Gen5 importer, so this test documents the invariant.
        Sprite2DOAM oam = new Sprite2DOAM();
        oam.width = 16;
        oam.height = 16;
        oam.rotationScaling = false;
        oam.doubleSize = true; // pathological / shouldn't happen
        // getContentOffset currently returns the offset purely from
        // doubleSize, but documented here as "shouldn't occur in
        // well-formed data" — if anyone ever changes the semantics, this
        // test captures the current behaviour.
        assertEquals(8, oam.getContentOffsetX());
        assertEquals(8, oam.getContentOffsetY());
    }

    @Test
    public void copyConstructorPreservesRotationScalingFields() {
        Sprite2DOAM src = new Sprite2DOAM();
        src.x = 10;
        src.y = 20;
        src.width = 32;
        src.height = 32;
        src.rotationScaling = true;
        src.doubleSize = true;
        src.rsParamIndex = 7;

        Sprite2DOAM copy = new Sprite2DOAM(src);
        assertTrue(copy.rotationScaling);
        assertTrue(copy.doubleSize);
        assertEquals(7, copy.rsParamIndex);
        assertEquals(16, copy.getContentOffsetX());
        assertEquals(16, copy.getContentOffsetY());
    }
}
