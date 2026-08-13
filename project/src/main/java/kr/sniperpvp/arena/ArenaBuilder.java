package kr.sniperpvp.arena;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

final class ArenaBuilder {
    BuildResult build(World world, boolean clearExistingLayout) {
        long startedAt = System.nanoTime();
        long clearedBlocks = clearExistingLayout ? clearExistingLayout(world) : 0L;
        long plannedBlocks = 0L;
        for (BlockBox box : ArenaBlueprint.blocks()) {
            plannedBlocks += box.volume();
            fill(world, box);
        }
        int jumpPadBlocks = placeJumpPads(world);
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        return new BuildResult(
            ArenaBlueprint.blocks().size(),
            ArenaBlueprint.jumpPads().size(),
            plannedBlocks,
            jumpPadBlocks,
            clearedBlocks,
            elapsedMillis
        );
    }

    private long clearExistingLayout(World world) {
        long clearedBlocks = 0L;
        for (int x = ArenaConstants.MIN_X; x <= ArenaConstants.MAX_X; x++) {
            for (int z = ArenaConstants.MIN_Z; z <= ArenaConstants.MAX_Z; z++) {
                Block floorBlock = world.getBlockAt(x, ArenaConstants.FLOOR_Y, z);
                if (floorBlock.getType() != Material.GRAY_CONCRETE) {
                    floorBlock.setType(Material.GRAY_CONCRETE, false);
                    clearedBlocks++;
                }
                for (int y = ArenaConstants.FLOOR_Y + 1;
                    y <= ArenaConstants.REBUILD_CLEAR_MAX_Y;
                    y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.isEmpty()) {
                        block.setType(Material.AIR, false);
                        clearedBlocks++;
                    }
                }
            }
        }
        return clearedBlocks;
    }

    private void fill(World world, BlockBox box) {
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    world.getBlockAt(x, y, z).setType(Material.GRAY_CONCRETE, false);
                }
            }
        }
    }

    private int placeJumpPads(World world) {
        int placedBlocks = 0;
        for (JumpPad pad : ArenaBlueprint.jumpPads()) {
            for (int x = pad.centerX() - pad.radius(); x <= pad.centerX() + pad.radius(); x++) {
                for (int z = pad.centerZ() - pad.radius(); z <= pad.centerZ() + pad.radius(); z++) {
                    world.getBlockAt(x, pad.surfaceY(), z).setType(Material.SLIME_BLOCK, false);
                    placedBlocks++;
                }
            }
        }
        return placedBlocks;
    }

    record BuildResult(
        int regions,
        int jumpPads,
        long plannedBlocks,
        int jumpPadBlocks,
        long clearedBlocks,
        long elapsedMillis
    ) {
    }
}
