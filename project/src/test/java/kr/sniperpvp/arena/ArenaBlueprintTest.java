package kr.sniperpvp.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.bukkit.util.Vector;

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
                && box.maxY() <= ArenaConstants.MAX_BUILD_Y
        ));
    }

    @Test
    void layoutVersionTwoHasFortyThreeByThreeJumpPads() {
        assertEquals(2, ArenaConstants.BUILD_VERSION);
        assertEquals(40, ArenaBlueprint.jumpPads().size());
        assertEquals(40, new HashSet<>(ArenaBlueprint.jumpPads()).size());
        assertEquals(360, ArenaBlueprint.jumpPads().stream().mapToInt(JumpPad::blockCount).sum());
        assertTrue(ArenaBlueprint.jumpPads().stream().allMatch(pad ->
            ArenaConstants.contains(pad.centerX() - pad.radius(), pad.centerZ() - pad.radius())
                && ArenaConstants.contains(pad.centerX() + pad.radius(), pad.centerZ() + pad.radius())
                && pad.surfaceY() >= ArenaConstants.FLOOR_Y + 1
                && pad.targetSurfaceY() <= ArenaConstants.MAX_BUILD_Y
        ));
    }

    @Test
    void everyRaisedTierHasSupportedLaunchAndLandingSurfaces() {
        Set<Integer> targetLevels = new HashSet<>();
        for (JumpPad pad : ArenaBlueprint.jumpPads()) {
            for (int x = pad.centerX() - pad.radius(); x <= pad.centerX() + pad.radius(); x++) {
                for (int z = pad.centerZ() - pad.radius(); z <= pad.centerZ() + pad.radius(); z++) {
                    assertTrue(pad.surfaceY() == ArenaConstants.FLOOR_Y + 1
                        || hasBlockAt(x, pad.surfaceY() - 1, z));
                    assertFalse(hasBlockAt(x, pad.surfaceY() + 1, z));
                }
            }
            assertTrue(hasSurfaceAt(pad.targetX(), pad.targetSurfaceY(), pad.targetZ()));
            assertFalse(hasBlockAt(pad.targetX(), pad.targetSurfaceY() + 1, pad.targetZ()));
            assertNotNull(ArenaBlueprint.jumpPadAt(pad.centerX(), pad.surfaceY(), pad.centerZ()));

            Vector verticalLaunch = pad.verticalLaunchVector();
            assertEquals(pad.verticalVelocity(), verticalLaunch.getY(), 0.00001);
            assertEquals(0.0, Math.hypot(verticalLaunch.getX(), verticalLaunch.getZ()), 0.00001);
            Vector guidedLaunch = pad.guidedLaunchVector(
                pad.centerX() + 0.5,
                pad.centerZ() + 0.5,
                0.25
            );
            assertEquals(0.25, guidedLaunch.getY(), 0.00001);
            assertEquals(
                pad.horizontalSpeed(),
                Math.hypot(guidedLaunch.getX(), guidedLaunch.getZ()),
                0.00001
            );
            assertTrue(pad.horizontalGuidanceDelayTicks() >= 4);
            assertTrue(pad.horizontalGuidanceDelayTicks() <= 13);
            targetLevels.add(pad.targetSurfaceY());
        }
        assertEquals(
            new HashSet<>(Arrays.asList(68, 69, 70, 74, 75, 77, 83, 84, 88)),
            targetLevels
        );
    }

    private static boolean hasSurfaceAt(int x, int surfaceY, int z) {
        return ArenaBlueprint.blocks().stream().anyMatch(box ->
            x >= box.minX()
                && x <= box.maxX()
                && z >= box.minZ()
                && z <= box.maxZ()
                && box.maxY() == surfaceY
        );
    }

    private static boolean hasBlockAt(int x, int y, int z) {
        return ArenaBlueprint.blocks().stream().anyMatch(box ->
            x >= box.minX()
                && x <= box.maxX()
                && y >= box.minY()
                && y <= box.maxY()
                && z >= box.minZ()
                && z <= box.maxZ()
        );
    }
}
