package kr.sniperpvp.arena;

import org.bukkit.Material;
import org.bukkit.World;

final class ArenaBuilder {
    BuildResult build(World world) {
        long startedAt = System.nanoTime();
        long plannedBlocks = 0L;
        for (BlockBox box : ArenaBlueprint.blocks()) {
            plannedBlocks += box.volume();
            fill(world, box);
        }
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        return new BuildResult(ArenaBlueprint.blocks().size(), plannedBlocks, elapsedMillis);
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

    record BuildResult(int regions, long plannedBlocks, long elapsedMillis) {
    }
}
