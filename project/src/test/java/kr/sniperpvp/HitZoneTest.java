package kr.sniperpvp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HitZoneTest {
    private static final double MINIMUM_Y = 64.0;
    private static final double HEIGHT = 1.8;
    private static final double LEG_RATIO = 0.375;
    private static final double HEAD_RATIO = 0.75;

    @Test
    void separatesLegBodyAndHeadAtConfiguredBoundaries() {
        assertEquals(HitZone.LEGS, zone(0.0));
        assertEquals(HitZone.LEGS, zone(LEG_RATIO - 0.001));
        assertEquals(HitZone.BODY, zone(LEG_RATIO));
        assertEquals(HitZone.BODY, zone(HEAD_RATIO - 0.001));
        assertEquals(HitZone.HEAD, zone(HEAD_RATIO));
        assertEquals(HitZone.HEAD, zone(1.0));
    }

    @Test
    void invalidZeroHeightFallsBackToBody() {
        assertEquals(HitZone.BODY, HitZone.atHeight(64.0, 64.0, 0.0, LEG_RATIO, HEAD_RATIO));
    }

    private static HitZone zone(double ratio) {
        return HitZone.atHeight(
            MINIMUM_Y + HEIGHT * ratio,
            MINIMUM_Y,
            HEIGHT,
            LEG_RATIO,
            HEAD_RATIO
        );
    }
}
