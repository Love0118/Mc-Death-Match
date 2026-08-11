package kr.sniperpvp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class AimSpreadTest {
    @Test
    void scopedZeroSpreadKeepsTheExactNormalizedDirection() {
        Vector result = AimSpread.apply(new Vector(0.0, 0.0, 4.0), 0.0, 1.0, 0.0);

        assertEquals(0.0, result.getX(), 1.0E-12);
        assertEquals(0.0, result.getY(), 1.0E-12);
        assertEquals(1.0, result.getZ(), 1.0E-12);
    }

    @Test
    void onePointFivePercentSpreadHasTheRequestedMaximumTangentError() {
        Vector forward = new Vector(0.0, 0.0, 1.0);
        Vector result = AimSpread.apply(forward, 0.015, 1.0, 0.0);

        assertEquals(1.0, result.length(), 1.0E-12);
        assertEquals(Math.atan(0.015), result.angle(forward), 1.0E-9);
    }

    @Test
    void verticalAimStillBuildsAStableSpreadBasis() {
        Vector result = AimSpread.apply(new Vector(0.0, 1.0, 0.0), 0.05, 0.5, Math.PI / 3.0);

        assertEquals(1.0, result.length(), 1.0E-12);
    }
}
