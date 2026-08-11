package kr.sniperpvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RifleMagazineTest {
    @Test
    void consumesExactlyFiveRoundsThenRefills() {
        RifleMagazine magazine = new RifleMagazine(5);
        assertTrue(magazine.isFull());

        for (int remaining = 4; remaining >= 0; remaining--) {
            assertTrue(magazine.consume());
            assertEquals(remaining, magazine.rounds());
        }
        assertTrue(magazine.isEmpty());
        assertFalse(magazine.isFull());
        assertFalse(magazine.consume());

        magazine.refill();
        assertEquals(5, magazine.rounds());
        assertFalse(magazine.isEmpty());
        assertTrue(magazine.isFull());
    }
}
