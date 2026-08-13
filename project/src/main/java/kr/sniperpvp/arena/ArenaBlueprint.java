package kr.sniperpvp.arena;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ArenaBlueprint {
    private static final int[] SIGNS = {1, -1};
    private static final List<BlockBox> BLOCKS = createBlocks();
    private static final List<JumpPad> JUMP_PADS = createJumpPads();
    private static final List<ArenaSpawn> SPAWNS = createSpawns();

    private ArenaBlueprint() {
    }

    static List<BlockBox> blocks() {
        return BLOCKS;
    }

    static List<JumpPad> jumpPads() {
        return JUMP_PADS;
    }

    static JumpPad jumpPadAt(int blockX, int blockY, int blockZ) {
        for (JumpPad pad : JUMP_PADS) {
            if (pad.contains(blockX, blockY, blockZ)) {
                return pad;
            }
        }
        return null;
    }

    public static List<ArenaSpawn> spawns() {
        return SPAWNS;
    }

    private static List<BlockBox> createBlocks() {
        Set<BlockBox> blocks = new LinkedHashSet<>();
        addPerimeter(blocks);
        addCentralCitadel(blocks);
        addQuadrantTowers(blocks);
        addCardinalTowers(blocks);
        addMidfieldOutposts(blocks);
        addFieldCover(blocks);
        addSpawnBunkers(blocks);
        return List.copyOf(blocks);
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
        add(blocks, -26, 65, -26, 26, 68, 26);
        add(blocks, -16, 69, -16, 16, 74, 16);
        add(blocks, -8, 75, -8, 8, 83, 8);

        addMirrored(blocks, new BlockBox(18, 69, 23, 24, 71, 24));
        addMirrored(blocks, new BlockBox(23, 69, 18, 24, 71, 22));
        addMirrored(blocks, new BlockBox(10, 75, 14, 15, 77, 15));
        addMirrored(blocks, new BlockBox(14, 75, 10, 15, 77, 13));
        addMirrored(blocks, new BlockBox(5, 84, 5, 7, 86, 7));
    }

    private static void addQuadrantTowers(Set<BlockBox> blocks) {
        for (int xSign : SIGNS) {
            for (int zSign : SIGNS) {
                int centerX = 72 * xSign;
                int centerZ = 72 * zSign;
                add(blocks, centerX - 12, 65, centerZ - 12,
                    centerX + 12, 68, centerZ + 12);
                add(blocks, centerX - 8, 69, centerZ - 8,
                    centerX + 8, 75, centerZ + 8);
                add(blocks, centerX - 4, 76, centerZ - 4,
                    centerX + 4, 84, centerZ + 4);
                addCornerPosts(blocks, centerX, 69, centerZ, 11, 3);
                addCornerPosts(blocks, centerX, 76, centerZ, 7, 3);
                addCornerPosts(blocks, centerX, 85, centerZ, 3, 3);
            }
        }
    }

    private static void addCardinalTowers(Set<BlockBox> blocks) {
        addCardinalTower(blocks, 0, -112);
        addCardinalTower(blocks, 112, 0);
        addCardinalTower(blocks, 0, 112);
        addCardinalTower(blocks, -112, 0);
    }

    private static void addCardinalTower(Set<BlockBox> blocks, int centerX, int centerZ) {
        add(blocks, centerX - 10, 65, centerZ - 10,
            centerX + 10, 69, centerZ + 10);
        add(blocks, centerX - 6, 70, centerZ - 6,
            centerX + 6, 77, centerZ + 6);
        add(blocks, centerX - 3, 78, centerZ - 3,
            centerX + 3, 88, centerZ + 3);
        addCornerPosts(blocks, centerX, 70, centerZ, 9, 3);
        addCornerPosts(blocks, centerX, 78, centerZ, 5, 3);
        addCornerPosts(blocks, centerX, 89, centerZ, 2, 2);
    }

    private static void addMidfieldOutposts(Set<BlockBox> blocks) {
        addOutpost(blocks, 0, -52);
        addOutpost(blocks, 52, 0);
        addOutpost(blocks, 0, 52);
        addOutpost(blocks, -52, 0);
    }

    private static void addOutpost(Set<BlockBox> blocks, int centerX, int centerZ) {
        add(blocks, centerX - 8, 65, centerZ - 8,
            centerX + 8, 70, centerZ + 8);
        addCornerPosts(blocks, centerX, 71, centerZ, 7, 3);
    }

    private static void addFieldCover(Set<BlockBox> blocks) {
        List<BlockBox> firstQuadrant = new ArrayList<>();
        firstQuadrant.add(new BlockBox(14, 65, 36, 35, 69, 38));
        firstQuadrant.add(new BlockBox(36, 65, 14, 38, 69, 35));
        firstQuadrant.add(new BlockBox(33, 65, 45, 47, 67, 49));
        firstQuadrant.add(new BlockBox(45, 65, 33, 49, 67, 47));
        firstQuadrant.add(new BlockBox(91, 65, 36, 108, 70, 38));
        firstQuadrant.add(new BlockBox(36, 65, 91, 38, 70, 108));
        firstQuadrant.add(new BlockBox(101, 65, 74, 119, 68, 78));
        firstQuadrant.add(new BlockBox(74, 65, 101, 78, 68, 119));
        firstQuadrant.add(new BlockBox(18, 65, 116, 32, 69, 120));
        firstQuadrant.add(new BlockBox(116, 65, 18, 120, 69, 32));
        firstQuadrant.add(new BlockBox(24, 65, 72, 31, 67, 79));
        firstQuadrant.add(new BlockBox(72, 65, 24, 79, 67, 31));
        firstQuadrant.add(new BlockBox(109, 65, 109, 120, 67, 120));
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

    private static void addCornerPosts(
        Set<BlockBox> blocks,
        int centerX,
        int minY,
        int centerZ,
        int offset,
        int height
    ) {
        for (int xSign : SIGNS) {
            for (int zSign : SIGNS) {
                int firstX = centerX + xSign * offset;
                int secondX = centerX + xSign * (offset - 1);
                int firstZ = centerZ + zSign * offset;
                int secondZ = centerZ + zSign * (offset - 1);
                add(blocks, Math.min(firstX, secondX), minY, Math.min(firstZ, secondZ),
                    Math.max(firstX, secondX), minY + height - 1, Math.max(firstZ, secondZ));
            }
        }
    }

    private static void addMirrored(Set<BlockBox> blocks, BlockBox source) {
        for (int xSign : SIGNS) {
            for (int zSign : SIGNS) {
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

    private static List<JumpPad> createJumpPads() {
        List<JumpPad> pads = new ArrayList<>(40);
        addCentralPads(pads);
        addQuadrantPads(pads);
        addCardinalPads(pads);
        addOutpostPads(pads);
        return List.copyOf(pads);
    }

    private static void addCentralPads(List<JumpPad> pads) {
        for (int sign : SIGNS) {
            addPad(pads, 0, 65, sign * 31, 0, 68, sign * 24, 0.95, 0.82);
            addPad(pads, sign * 31, 65, 0, sign * 24, 68, 0, 0.95, 0.82);
            addPad(pads, 0, 68, sign * 20, 0, 74, sign * 14, 1.15, 0.80);
            addPad(pads, sign * 20, 68, 0, sign * 14, 74, 0, 1.15, 0.80);
            addPad(pads, 0, 74, sign * 12, 0, 83, sign * 6, 1.38, 0.78);
            addPad(pads, sign * 12, 74, 0, sign * 6, 83, 0, 1.38, 0.78);
        }
    }

    private static void addQuadrantPads(List<JumpPad> pads) {
        for (int xSign : SIGNS) {
            for (int zSign : SIGNS) {
                int centerX = 72 * xSign;
                int centerZ = 72 * zSign;
                addPad(pads, centerX + xSign * 16, 65, centerZ,
                    centerX + xSign * 10, 68, centerZ - zSign * 6, 0.95, 0.95);
                addPad(pads, centerX + xSign * 10, 68, centerZ + zSign * 6,
                    centerX + xSign * 6, 75, centerZ + zSign * 5, 1.22, 0.55);
                addPad(pads, centerX + xSign * 6, 75, centerZ,
                    centerX + xSign * 2, 84, centerZ, 1.38, 0.56);
            }
        }
    }

    private static void addCardinalPads(List<JumpPad> pads) {
        addCardinalPads(pads, 0, -112, 0, -1);
        addCardinalPads(pads, 112, 0, 1, 0);
        addCardinalPads(pads, 0, 112, 0, 1);
        addCardinalPads(pads, -112, 0, -1, 0);
    }

    private static void addCardinalPads(
        List<JumpPad> pads,
        int centerX,
        int centerZ,
        int outwardX,
        int outwardZ
    ) {
        int perpendicularX = -outwardZ;
        int perpendicularZ = outwardX;
        addPad(pads,
            centerX + outwardX * 14,
            65,
            centerZ + outwardZ * 14,
            centerX + outwardX * 8 - perpendicularX * 5,
            69,
            centerZ + outwardZ * 8 - perpendicularZ * 5,
            1.05,
            0.90);
        addPad(pads,
            centerX + outwardX * 8 + perpendicularX * 4,
            69,
            centerZ + outwardZ * 8 + perpendicularZ * 4,
            centerX + outwardX * 5 + perpendicularX * 3,
            77,
            centerZ + outwardZ * 5 + perpendicularZ * 3,
            1.30,
            0.45);
        addPad(pads,
            centerX + outwardX * 5,
            77,
            centerZ + outwardZ * 5,
            centerX + outwardX * 2,
            88,
            centerZ + outwardZ * 2,
            1.50,
            0.44);
    }

    private static void addOutpostPads(List<JumpPad> pads) {
        addOutpostPad(pads, 0, -52, 0, -1);
        addOutpostPad(pads, 52, 0, 1, 0);
        addOutpostPad(pads, 0, 52, 0, 1);
        addOutpostPad(pads, -52, 0, -1, 0);
    }

    private static void addOutpostPad(
        List<JumpPad> pads,
        int centerX,
        int centerZ,
        int outwardX,
        int outwardZ
    ) {
        addPad(pads,
            centerX + outwardX * 12,
            65,
            centerZ + outwardZ * 12,
            centerX + outwardX * 7,
            70,
            centerZ + outwardZ * 7,
            1.10,
            0.60);
    }

    private static void addPad(
        List<JumpPad> pads,
        int centerX,
        int surfaceY,
        int centerZ,
        int targetX,
        int targetSurfaceY,
        int targetZ,
        double verticalVelocity,
        double horizontalSpeed
    ) {
        pads.add(new JumpPad(
            centerX,
            surfaceY,
            centerZ,
            1,
            targetX,
            targetSurfaceY,
            targetZ,
            verticalVelocity,
            horizontalSpeed
        ));
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
            double angle = offset + Math.PI * 2.0 * index / count;
            int x = (int) Math.round(Math.sin(angle) * radius);
            int z = (int) Math.round(Math.cos(angle) * radius);
            spawns.add(new ArenaSpawn(x, z));
        }
    }
}
