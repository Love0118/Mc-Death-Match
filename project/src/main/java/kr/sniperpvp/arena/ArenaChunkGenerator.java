package kr.sniperpvp.arena;

import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

public final class ArenaChunkGenerator extends ChunkGenerator {
    @Override
    public void generateNoise(
        @NotNull WorldInfo worldInfo,
        @NotNull Random random,
        int chunkX,
        int chunkZ,
        @NotNull ChunkData chunkData
    ) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int localMinX = Math.max(0, ArenaConstants.MIN_X - baseX);
        int localMaxX = Math.min(16, ArenaConstants.MAX_X + 1 - baseX);
        int localMinZ = Math.max(0, ArenaConstants.MIN_Z - baseZ);
        int localMaxZ = Math.min(16, ArenaConstants.MAX_Z + 1 - baseZ);
        if (localMinX >= localMaxX || localMinZ >= localMaxZ) {
            return;
        }
        chunkData.setRegion(
            localMinX,
            ArenaConstants.FLOOR_BOTTOM_Y,
            localMinZ,
            localMaxX,
            ArenaConstants.FLOOR_Y + 1,
            localMaxZ,
            Material.GRAY_CONCRETE
        );
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public @NotNull Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        return new Location(world, 0.5, ArenaConstants.FLOOR_Y + 1.0, 0.5);
    }
}
