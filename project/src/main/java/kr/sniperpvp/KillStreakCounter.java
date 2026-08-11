package kr.sniperpvp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class KillStreakCounter {
    private final Map<UUID, Integer> streaks = new HashMap<>();

    Streak recordKill(UUID playerId) {
        int count = streaks.merge(playerId, 1, Integer::sum);
        return new Streak(count, Math.min(count, 5));
    }

    void recordDeath(UUID playerId) {
        streaks.remove(playerId);
    }

    void remove(UUID playerId) {
        streaks.remove(playerId);
    }

    void reset() {
        streaks.clear();
    }

    record Streak(int count, int soundTier) {
    }
}
