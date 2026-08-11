package kr.sniperpvp.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class WeightedSpawnSelectorTest {
    private final WeightedSpawnSelector selector = new WeightedSpawnSelector();

    @Test
    void emptySideHasMuchHigherWeightThanCrowdedSide() {
        ArenaSpawn crowded = new ArenaSpawn(-100, 0);
        ArenaSpawn empty = new ArenaSpawn(100, 0);
        List<WeightedSpawnSelector.PlayerPoint> players = List.of(
            new WeightedSpawnSelector.PlayerPoint(-100, 0),
            new WeightedSpawnSelector.PlayerPoint(-90, 4),
            new WeightedSpawnSelector.PlayerPoint(-80, -5)
        );

        double crowdedPressure = selector.crowdPressure(crowded, players);
        double emptyPressure = selector.crowdPressure(empty, players);
        assertTrue(crowdedPressure > emptyPressure * 10.0);
    }

    @Test
    void weightedRandomStronglyPrefersTheEmptySide() {
        List<ArenaSpawn> spawns = List.of(new ArenaSpawn(-100, 0), new ArenaSpawn(100, 0));
        List<WeightedSpawnSelector.PlayerPoint> players = List.of(
            new WeightedSpawnSelector.PlayerPoint(-100, 0),
            new WeightedSpawnSelector.PlayerPoint(-95, 0)
        );
        long[] lastUsed = new long[2];
        Random random = new Random(123456L);
        int emptySelections = 0;
        for (int sample = 0; sample < 1_000; sample++) {
            if (selector.select(spawns, players, lastUsed, 10_000_000_000L, random) == 1) {
                emptySelections++;
            }
        }
        assertTrue(emptySelections > 950, "empty selections=" + emptySelections);
    }

    @Test
    void singleAvailableSpawnIsAlwaysSelected() {
        assertEquals(0, selector.select(
            List.of(new ArenaSpawn(0, 0)),
            List.of(),
            new long[1],
            1L,
            new Random(1L)
        ));
    }
}
