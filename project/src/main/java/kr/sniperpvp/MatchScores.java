package kr.sniperpvp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class MatchScores {
    private static final Comparator<Entry> RANKING = Comparator
        .comparingInt(Entry::kills)
        .reversed()
        .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(Entry::playerId);

    private final Map<UUID, MutableEntry> entries = new HashMap<>();

    void register(UUID playerId, String name) {
        entries.compute(playerId, (ignored, current) -> {
            if (current == null) {
                return new MutableEntry(name, 0);
            }
            current.name = name;
            return current;
        });
    }

    int recordKill(UUID playerId, String name) {
        register(playerId, name);
        MutableEntry entry = entries.get(playerId);
        entry.kills++;
        return entry.kills;
    }

    int kills(UUID playerId) {
        MutableEntry entry = entries.get(playerId);
        return entry == null ? 0 : entry.kills;
    }

    List<Entry> ranking() {
        return ranking(entries.keySet());
    }

    List<Entry> ranking(Collection<UUID> includedPlayerIds) {
        Set<UUID> included = new HashSet<>(includedPlayerIds);
        List<Entry> ranking = new ArrayList<>();
        for (Map.Entry<UUID, MutableEntry> mapEntry : entries.entrySet()) {
            if (!included.contains(mapEntry.getKey())) {
                continue;
            }
            MutableEntry score = mapEntry.getValue();
            ranking.add(new Entry(mapEntry.getKey(), score.name, score.kills));
        }
        ranking.sort(RANKING);
        return List.copyOf(ranking);
    }

    void reset() {
        entries.clear();
    }

    record Entry(UUID playerId, String name, int kills) {
    }

    private static final class MutableEntry {
        private String name;
        private int kills;

        private MutableEntry(String name, int kills) {
            this.name = name;
            this.kills = kills;
        }
    }
}
