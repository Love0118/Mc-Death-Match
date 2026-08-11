package kr.sniperpvp.arena;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ArenaBlueprint {
    private static final List<ArenaSpawn> SPAWNS = createSpawns();

    private ArenaBlueprint() {
    }

    static List<BlockBox> blocks() {
        Set<BlockBox> blocks = new LinkedHashSet<>();
        addPerimeter(blocks);
        addCentralCitadel(blocks);
        addQuadrantPlateaus(blocks);
        addCardinalTowers(blocks);
        addFieldCover(blocks);
        addSpawnBunkers(blocks);
        return List.copyOf(blocks);
    }

    public static List<ArenaSpawn> spawns() {
        return SPAWNS;
    }

    private static void addPerimeter(Set<BlockBox> blocks) {
        add(blocks, ArenaConstants.MIN_X, 65, ArenaConstants.MIN_Z,
            ArenaConstants.MIN_X, 70, ArenaConstants.MAX_Z);
        add(blocks, ArenaConstants.MAX_X, 65, ArenaConstants.MIN_Z,
            ArenaConstants.MAX_X, 70, ArenaConstants.MAX_Z);
        add(blocks, ArenaConstants.MIN_X, 65, ArenaConstants.MIN_Z,
            ArenaConstants.MAX_X, 70, ArenaConstants.MIN_Z);
        add(blocks, ArenaConstants.MIN_X, 65, ArenaConstants.MAX_Z,
            ArenaConstants.MAX_X, 70, ArenaConstants.MAX_Z);
    }

    private static void addCentralCitadel(Set<BlockBox> blocks) {
        add(blocks, -22, 65, -22, 22, 68, 22);

        for (int level = 1; level <= 4; level++) {
            int offset = (level - 1) * 3;
            add(blocks, -5, 65, -34 + offset, 5, 64 + level, -32 + offset);
            add(blocks, -5, 65, 32 - offset, 5, 64 + level, 34 - offset);
            add(blocks, -34 + offset, 65, -5, -32 + offset, 64 + level, 5);
            add(blocks, 32 - offset, 65, -5, 34 - offset, 64 + level, 5);
        }

        add(blocks, -18, 69, -2, -6, 71, -1);
        add(blocks, 6, 69, -2, 18, 71, -1);
        add(blocks, -18, 69, 1, -6, 71, 2);
        add(blocks, 6, 69, 1, 18, 71, 2);
        add(blocks, -2, 69, -18, -1, 71, -6);
        add(blocks, -2, 69, 6, -1, 71, 18);
        add(blocks, 1, 69, -18, 2, 71, -6);
        add(blocks, 1, 69, 6, 2, 71, 18);
        addMirrored(blocks, new BlockBox(10, 69, 10, 14, 70, 14));
    }

    private static void addQuadrantPlateaus(Set<BlockBox> blocks) {
        addMirrored(blocks, new BlockBox(63, 65, 63, 91, 67, 91));

        for (int level = 1; level <= 3; level++) {
            int offset = (level - 1) * 3;
            addMirrored(blocks, new BlockBox(54 + offset, 65, 73, 56 + offset, 64 + level, 81));
            addMirrored(blocks, new BlockBox(73, 65, 54 + offset, 81, 64 + level, 56 + offset));
        }

        addMirrored(blocks, new BlockBox(68, 68, 74, 75, 71, 75));
        addMirrored(blocks, new BlockBox(81, 68, 74, 86, 71, 75));
        addMirrored(blocks, new BlockBox(74, 68, 68, 75, 71, 72));
        addMirrored(blocks, new BlockBox(74, 68, 82, 75, 71, 86));
    }

    private static void addCardinalTowers(Set<BlockBox> blocks) {
        add(blocks, -7, 65, -114, 7, 70, -100);
        add(blocks, -7, 65, 100, 7, 70, 114);
        add(blocks, -114, 65, -7, -100, 70, 7);
        add(blocks, 100, 65, -7, 114, 70, 7);

        for (int level = 1; level <= 6; level++) {
            int offset = (level - 1) * 3;
            add(blocks, -4, 65, -82 - offset, 4, 64 + level, -80 - offset);
            add(blocks, -4, 65, 80 + offset, 4, 64 + level, 82 + offset);
            add(blocks, -82 - offset, 65, -4, -80 - offset, 64 + level, 4);
            add(blocks, 80 + offset, 65, -4, 82 + offset, 64 + level, 4);
        }

        add(blocks, -7, 71, -114, -6, 74, -100);
        add(blocks, 6, 71, -114, 7, 74, -100);
        add(blocks, -5, 71, -114, 5, 74, -113);
        add(blocks, -7, 71, 100, -6, 74, 114);
        add(blocks, 6, 71, 100, 7, 74, 114);
        add(blocks, -5, 71, 113, 5, 74, 114);
        add(blocks, -114, 71, -7, -100, 74, -6);
        add(blocks, -114, 71, 6, -100, 74, 7);
        add(blocks, -114, 71, -5, -113, 74, 5);
        add(blocks, 100, 71, -7, 114, 74, -6);
        add(blocks, 100, 71, 6, 114, 74, 7);
        add(blocks, 113, 71, -5, 114, 74, 5);
    }

    private static void addFieldCover(Set<BlockBox> blocks) {
        List<BlockBox> firstQuadrant = new ArrayList<>();
        firstQuadrant.add(new BlockBox(12, 65, 42, 29, 68, 43));
        firstQuadrant.add(new BlockBox(42, 65, 12, 43, 68, 29));
        firstQuadrant.add(new BlockBox(35, 65, 55, 50, 68, 56));
        firstQuadrant.add(new BlockBox(55, 65, 35, 56, 68, 50));
        firstQuadrant.add(new BlockBox(103, 65, 35, 104, 69, 52));
        firstQuadrant.add(new BlockBox(35, 65, 103, 52, 69, 104));
        firstQuadrant.add(new BlockBox(106, 65, 76, 121, 68, 77));
        firstQuadrant.add(new BlockBox(76, 65, 106, 77, 68, 121));
        firstQuadrant.add(new BlockBox(18, 65, 118, 31, 68, 120));
        firstQuadrant.add(new BlockBox(118, 65, 18, 120, 68, 31));
        firstQuadrant.add(new BlockBox(25, 65, 70, 30, 66, 75));
        firstQuadrant.add(new BlockBox(70, 65, 25, 75, 66, 30));
        firstQuadrant.add(new BlockBox(112, 65, 112, 119, 67, 119));
        firstQuadrant.forEach(box -> addMirrored(blocks, box));
    }

    private static void addSpawnBunkers(Set<BlockBox> blocks) {
        addNorthBunker(blocks, -90);
        addNorthBunker(blocks, 90);
        addSouthBunker(blocks, -90);
        addSouthBunker(blocks, 90);
        addWestBunker(blocks, -90);
        addWestBunker(blocks, 90);
        addEastBunker(blocks, -90);
        addEastBunker(blocks, 90);
    }

    private static void addNorthBunker(Set<BlockBox> blocks, int centerX) {
        add(blocks, centerX - 7, 65, -142, centerX + 7, 69, -141);
        add(blocks, centerX - 7, 65, -140, centerX - 6, 69, -128);
        add(blocks, centerX + 6, 65, -140, centerX + 7, 69, -128);
    }

    private static void addSouthBunker(Set<BlockBox> blocks, int centerX) {
        add(blocks, centerX - 7, 65, 141, centerX + 7, 69, 142);
        add(blocks, centerX - 7, 65, 128, centerX - 6, 69, 140);
        add(blocks, centerX + 6, 65, 128, centerX + 7, 69, 140);
    }

    private static void addWestBunker(Set<BlockBox> blocks, int centerZ) {
        add(blocks, -142, 65, centerZ - 7, -141, 69, centerZ + 7);
        add(blocks, -140, 65, centerZ - 7, -128, 69, centerZ - 6);
        add(blocks, -140, 65, centerZ + 6, -128, 69, centerZ + 7);
    }

    private static void addEastBunker(Set<BlockBox> blocks, int centerZ) {
        add(blocks, 141, 65, centerZ - 7, 142, 69, centerZ + 7);
        add(blocks, 128, 65, centerZ - 7, 140, 69, centerZ - 6);
        add(blocks, 128, 65, centerZ + 6, 140, 69, centerZ + 7);
    }

    private static void addMirrored(Set<BlockBox> blocks, BlockBox source) {
        for (int xSign : new int[]{1, -1}) {
            for (int zSign : new int[]{1, -1}) {
                int minX = xSign > 0 ? source.minX() : -source.maxX();
                int maxX = xSign > 0 ? source.maxX() : -source.minX();
                int minZ = zSign > 0 ? source.minZ() : -source.maxZ();
                int maxZ = zSign > 0 ? source.maxZ() : -source.minZ();
                blocks.add(new BlockBox(minX, source.minY(), minZ, maxX, source.maxY(), maxZ));
            }
        }
    }

    private static void add(
        Set<BlockBox> blocks,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ
    ) {
        blocks.add(new BlockBox(minX, minY, minZ, maxX, maxY, maxZ));
    }

    private static List<ArenaSpawn> createSpawns() {
        List<ArenaSpawn> spawns = new ArrayList<>(40);
        addSpawnRing(spawns, 134.0, 16, 0.0);
        addSpawnRing(spawns, 92.0, 16, Math.PI / 16.0);
        addSpawnRing(spawns, 48.0, 8, 0.0);
        return List.copyOf(spawns);
    }

    private static void addSpawnRing(List<ArenaSpawn> spawns, double radius, int count, double offset) {
        for (int index = 0; index < count; index++) {
            double angle = offset + (Math.PI * 2.0 * index / count);
            int x = (int) Math.round(Math.sin(angle) * radius);
            int z = (int) Math.round(Math.cos(angle) * radius);
            spawns.add(new ArenaSpawn(x, z));
        }
    }
}
