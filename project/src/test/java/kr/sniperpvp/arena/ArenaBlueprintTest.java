package kr.sniperpvp.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.bukkit.util.Vector;
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
                && box.maxY() <= ArenaConstants.MAX_BUILD_Y
        ));
    }

    @Test
    void sideDecksProvideContinuousSecondAndThirdFloors() {
        assertContinuousSurface(-142, -105, -140, 140, 72);
        assertContinuousSurface(104, 141, -140, 140, 72);
        assertContinuousSurface(-142, -105, -48, 48, 80);
        assertContinuousSurface(104, 141, -48, 48, 80);
    }

    @Test
    void versionThreeUsesEightExposedThreeByThreeJumpPads() {
        assertEquals(3, ArenaConstants.BUILD_VERSION);
        assertEquals(8, ArenaBlueprint.jumpPads().size());
        assertEquals(8, new HashSet<>(ArenaBlueprint.jumpPads()).size());
        assertEquals(72, ArenaBlueprint.jumpPads().stream().mapToInt(JumpPad::blockCount).sum());
        assertEquals(4, ArenaBlueprint.jumpPads().stream()
            .filter(pad -> pad.surfaceY() == ArenaConstants.FLOOR_Y)
            .count());
        assertEquals(4, ArenaBlueprint.jumpPads().stream()
            .filter(pad -> pad.surfaceY() == 72)
            .count());

        for (JumpPad pad : ArenaBlueprint.jumpPads()) {
            assertTrue(ArenaConstants.contains(
                pad.centerX() - pad.radius(),
                pad.centerZ() - pad.radius()
            ));
            assertTrue(ArenaConstants.contains(
                pad.centerX() + pad.radius(),
                pad.centerZ() + pad.radius()
            ));
            assertNotNull(ArenaBlueprint.jumpPadAt(pad.centerX(), pad.surfaceY(), pad.centerZ()));
            for (int x = pad.centerX() - pad.radius(); x <= pad.centerX() + pad.radius(); x++) {
                for (int z = pad.centerZ() - pad.radius(); z <= pad.centerZ() + pad.radius(); z++) {
                    if (pad.surfaceY() > ArenaConstants.FLOOR_Y) {
                        assertTrue(hasBlockAt(x, pad.surfaceY() - 1, z));
                    }
                    assertFalse(hasBlockAt(x, pad.surfaceY() + 1, z));
                }
            }
        }
    }

    @Test
    void jumpPadsOnlyReplaceVerticalVelocity() {
        Vector existingVelocity = new Vector(0.21, -0.35, -0.17);
        for (JumpPad pad : ArenaBlueprint.jumpPads()) {
            Vector launch = pad.launchVector(existingVelocity);
            assertEquals(existingVelocity.getX(), launch.getX(), 0.00001);
            assertEquals(existingVelocity.getZ(), launch.getZ(), 0.00001);
            assertEquals(pad.verticalVelocity(), launch.getY(), 0.00001);
            assertTrue(pad.targetSurfaceY() == 72 || pad.targetSurfaceY() == 80);
        }
    }

    private static void assertContinuousSurface(
        int minX,
        int maxX,
        int minZ,
        int maxZ,
        int y
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                assertTrue(hasBlockAt(x, y, z),
                    "Missing deck block at " + x + "," + y + "," + z);
            }
        }
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
