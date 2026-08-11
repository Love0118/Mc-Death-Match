package kr.sniperpvp.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class ArenaBlueprintTest {
    @Test
    void exposesFortyUniqueSpawnsInsideTheArena() {
        assertEquals(40, ArenaBlueprint.spawns().size());
        assertEquals(40, new HashSet<>(ArenaBlueprint.spawns()).size());
        assertTrue(ArenaBlueprint.spawns().stream().allMatch(
            spawn -> ArenaConstants.contains(spawn.x(), spawn.z())
        ));
    }

    @Test
    void everyStructureStaysInsideTheThreeHundredBlockArena() {
        assertTrue(ArenaBlueprint.blocks().stream().allMatch(box ->
            box.minX() >= ArenaConstants.MIN_X
                && box.maxX() <= ArenaConstants.MAX_X
                && box.minZ() >= ArenaConstants.MIN_Z
                && box.maxZ() <= ArenaConstants.MAX_Z
                && box.minY() >= ArenaConstants.FLOOR_Y + 1
                && box.maxY() <= 90
        ));
    }
}
