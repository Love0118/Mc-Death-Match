package kr.sniperpvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoundedKillFeedTest {
    @Test
    void fourthBannerRemovesTheOldestAndPreservesKillOrder() {
        BoundedKillFeed<String> feed = new BoundedKillFeed<>(3);
        assertNull(feed.addLast("first"));
        assertNull(feed.addLast("second"));
        assertNull(feed.addLast("third"));

        assertEquals("first", feed.addLast("fourth"));
        assertEquals(java.util.List.of("second", "third", "fourth"), feed.entries());
    }

    @Test
    void expiredBannerCanBeRemovedWithoutReorderingTheOthers() {
        BoundedKillFeed<String> feed = new BoundedKillFeed<>(3);
        feed.addLast("first");
        feed.addLast("second");
        feed.addLast("third");

        assertTrue(feed.remove("second"));
        assertEquals(java.util.List.of("first", "third"), feed.entries());
    }
}
