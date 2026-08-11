package kr.sniperpvp.arena;

import java.util.List;
import java.util.random.RandomGenerator;

final class WeightedSpawnSelector {
    private static final double EMPTY_BASELINE = 0.25;
    private static final double DENSITY_DISTANCE = 45.0;
    private static final double CLOSE_PLAYER_DISTANCE = 20.0;
    private static final double CLOSE_PLAYER_PRESSURE = 5.0;
    private static final double RECENT_USE_PRESSURE = 2.0;
    private static final long RECENT_USE_NANOS = 5_000_000_000L;

    int select(
        List<ArenaSpawn> spawns,
        List<PlayerPoint> players,
        long[] lastUsedNanos,
        long nowNanos,
        RandomGenerator random
    ) {
        if (spawns.isEmpty()) {
            throw new IllegalArgumentException("At least one spawn is required");
        }
        if (lastUsedNanos.length != spawns.size()) {
            throw new IllegalArgumentException("lastUsedNanos must match the spawn count");
        }

        double[] weights = new double[spawns.size()];
        double totalWeight = 0.0;
        for (int index = 0; index < spawns.size(); index++) {
            ArenaSpawn spawn = spawns.get(index);
            double pressure = crowdPressure(spawn, players);
            long age = nowNanos - lastUsedNanos[index];
            if (lastUsedNanos[index] != 0L && age >= 0L && age < RECENT_USE_NANOS) {
                pressure += RECENT_USE_PRESSURE * (1.0 - (double) age / RECENT_USE_NANOS);
            }
            double denominator = EMPTY_BASELINE + pressure;
            double weight = 1.0 / (denominator * denominator);
            weights[index] = weight;
            totalWeight += weight;
        }

        double roll = random.nextDouble(totalWeight);
        for (int index = 0; index < weights.length; index++) {
            roll -= weights[index];
            if (roll <= 0.0) {
                return index;
            }
        }
        return weights.length - 1;
    }

    double crowdPressure(ArenaSpawn spawn, List<PlayerPoint> players) {
        double pressure = 0.0;
        for (PlayerPoint player : players) {
            double distance = Math.hypot(spawn.x() - player.x(), spawn.z() - player.z());
            pressure += Math.exp(-distance / DENSITY_DISTANCE);
            if (distance < CLOSE_PLAYER_DISTANCE) {
                pressure += CLOSE_PLAYER_PRESSURE * (1.0 - distance / CLOSE_PLAYER_DISTANCE);
            }
        }
        return pressure;
    }

    record PlayerPoint(double x, double z) {
    }
}
