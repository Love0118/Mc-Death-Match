package kr.sniperpvp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchScoresTest {
    @Test
    void ranksByTotalKillsWithoutResettingOnPlayerDeath() {
        MatchScores scores = new MatchScores();
        UUID alpha = UUID.randomUUID();
        UUID beta = UUID.randomUUID();
        scores.register(alpha, "Alpha");
        scores.register(beta, "Beta");

        scores.recordKill(beta, "Beta");
        scores.recordKill(alpha, "Alpha");
        scores.recordKill(alpha, "Alpha");

        List<MatchScores.Entry> ranking = scores.ranking();
        assertEquals("Alpha", ranking.get(0).name());
        assertEquals(2, ranking.get(0).kills());
        assertEquals("Beta", ranking.get(1).name());
        assertEquals(1, ranking.get(1).kills());
    }
}
