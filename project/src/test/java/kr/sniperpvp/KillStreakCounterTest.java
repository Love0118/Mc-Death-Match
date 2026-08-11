package kr.sniperpvp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class KillStreakCounterTest {
    @Test
    void fifthSoundRepeatsAfterFiveKills() {
        KillStreakCounter counter = new KillStreakCounter();
        UUID player = UUID.randomUUID();
        KillStreakCounter.Streak latest = null;
        for (int kill = 1; kill <= 8; kill++) {
            latest = counter.recordKill(player);
            assertEquals(kill, latest.count());
            assertEquals(Math.min(kill, 5), latest.soundTier());
        }
        assertEquals(5, latest.soundTier());
    }

    @Test
    void deathResetsTheStreak() {
        KillStreakCounter counter = new KillStreakCounter();
        UUID player = UUID.randomUUID();
        counter.recordKill(player);
        counter.recordKill(player);
        counter.recordDeath(player);
        KillStreakCounter.Streak firstAfterDeath = counter.recordKill(player);
        assertEquals(1, firstAfterDeath.count());
        assertEquals(1, firstAfterDeath.soundTier());
    }

    @Test
    void deathAfterTierFiveRestartsAtTheOneKillSound() {
        KillStreakCounter counter = new KillStreakCounter();
        UUID player = UUID.randomUUID();
        for (int kill = 0; kill < 7; kill++) {
            counter.recordKill(player);
        }
        counter.recordDeath(player);
        assertEquals(1, counter.recordKill(player).soundTier());
    }
}
