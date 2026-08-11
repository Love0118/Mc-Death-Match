package kr.sniperpvp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HealthHudLayoutTest {
    @Test
    void usesTheSameElevenRoundedHealthStepsAsTheReferenceHud() {
        assertEquals(0, HealthHudLayout.healthStep(0, 100));
        assertEquals(1, HealthHudLayout.healthStep(5, 100));
        assertEquals(5, HealthHudLayout.healthStep(50, 100));
        assertEquals(10, HealthHudLayout.healthStep(95, 100));
        assertEquals(10, HealthHudLayout.healthStep(100, 100));
    }

    @Test
    void clampsGlyphsToTheAtlasRange() {
        assertEquals('\uE200', HealthHudLayout.glyphForStep(-1));
        assertEquals('\uE205', HealthHudLayout.glyphForStep(5));
        assertEquals('\uE20A', HealthHudLayout.glyphForStep(99));
        assertEquals(11, HealthHudLayout.barCharacters().length());
    }
}
