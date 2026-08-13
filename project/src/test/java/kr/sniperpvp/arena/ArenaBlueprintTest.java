package kr.sniperpvp.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.bukkit.Material;
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
        assertSolidVolume(-142, -105, 65, 72, -140, 140);
        assertSolidVolume(104, 141, 65, 72, -140, 140);
        assertSolidVolume(-142, -105, 73, 80, -48, 48);
        assertSolidVolume(104, 141, 73, 80, -48, 48);
    }

    @Test
    void versionFourUsesSixteenPurposefulThreeByThreeJumpPads() {
        assertEquals(4, ArenaConstants.BUILD_VERSION);
        assertEquals(16, ArenaBlueprint.jumpPads().size());
        assertEquals(16, new HashSet<>(ArenaBlueprint.jumpPads()).size());
        assertEquals(144, ArenaBlueprint.jumpPads().stream().mapToInt(JumpPad::blockCount).sum());
        assertEquals(10, ArenaBlueprint.jumpPads().stream()
            .filter(pad -> pad.surfaceY() == ArenaConstants.FLOOR_Y)
            .count());
        assertEquals(2, ArenaBlueprint.jumpPads().stream()
            .filter(pad -> pad.surfaceY() == 68)
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
        Vector lookDirection = new Vector(0.6, -0.4, 0.8);
        for (JumpPad pad : ArenaBlueprint.jumpPads()) {
            Vector launch = pad.launchVector(existingVelocity, lookDirection);
            assertEquals(existingVelocity.getX() + 0.27, launch.getX(), 0.00001);
            assertEquals(existingVelocity.getZ() + 0.36, launch.getZ(), 0.00001);
            assertEquals(pad.verticalVelocity(), launch.getY(), 0.00001);
            assertTrue(pad.targetSurfaceY() == 68
                || pad.targetSurfaceY() == 72
                || pad.targetSurfaceY() == 73
                || pad.targetSurfaceY() == 80);
            assertEquals(JumpPad.DEFAULT_VERTICAL_VELOCITY, pad.verticalVelocity(), 0.00001);
        }
    }

    @Test
    void everySlimeBlockActivatesTheGenericJumpPadTrigger() {
        assertTrue(ArenaService.activatesJumpPad(Material.SLIME_BLOCK));
        assertFalse(ArenaService.activatesJumpPad(Material.GRAY_CONCRETE));

        Vector launch = JumpPad.launchVector(
            new Vector(-0.12, 0.0, 0.08),
            new Vector(-1.0, 0.5, 0.0),
            JumpPad.DEFAULT_VERTICAL_VELOCITY
        );
        assertEquals(-0.12 - JumpPad.LOOK_DIRECTION_BOOST, launch.getX(), 0.00001);
        assertEquals(0.08, launch.getZ(), 0.00001);
        assertEquals(JumpPad.DEFAULT_VERTICAL_VELOCITY, launch.getY(), 0.00001);
    }

    private static void assertSolidVolume(
        int minX,
        int maxX,
        int minY,
        int maxY,
        int minZ,
        int maxZ
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    assertTrue(hasBlockAt(x, y, z),
                        "Missing solid block at " + x + "," + y + "," + z);
                }
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
