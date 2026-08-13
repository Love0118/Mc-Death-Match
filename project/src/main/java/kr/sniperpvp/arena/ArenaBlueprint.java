package kr.sniperpvp.arena;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ArenaBlueprint {
    private static final int[] SIGNS = {1, -1};
    private static final int WEST_DECK_MIN_X = -142;
    private static final int WEST_DECK_MAX_X = -105;
    private static final int EAST_DECK_MIN_X = 104;
    private static final int EAST_DECK_MAX_X = 141;
    private static final int SECOND_FLOOR_Y = 72;
    private static final int THIRD_FLOOR_Y = 80;
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
        addSideDeck(blocks, WEST_DECK_MIN_X, WEST_DECK_MAX_X, true);
        addSideDeck(blocks, EAST_DECK_MIN_X, EAST_DECK_MAX_X, false);
        addCentralDeck(blocks);
        addGroundCover(blocks);
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

    private static void addSideDeck(
        Set<BlockBox> blocks,
        int minX,
        int maxX,
        boolean outerEdgeAtMinimumX
    ) {
        add(blocks, minX, 71, -140, maxX, SECOND_FLOOR_Y, 140);
        add(blocks, minX, 65, -140, maxX, 70, -136);
        add(blocks, minX, 65, 136, maxX, 70, 140);

        int outerMinX = outerEdgeAtMinimumX ? minX : maxX - 3;
        int outerMaxX = outerEdgeAtMinimumX ? minX + 3 : maxX;
        int innerMinX = outerEdgeAtMinimumX ? maxX - 3 : minX;
        int innerMaxX = outerEdgeAtMinimumX ? maxX : minX + 3;
        add(blocks, outerMinX, 65, -135, outerMaxX, 70, 135);
        for (int supportZ : new int[]{-100, -34, 34, 100}) {
            add(blocks, innerMinX, 65, supportZ - 6, innerMaxX, 70, supportZ + 6);
        }

        add(blocks, minX, 79, -48, maxX, THIRD_FLOOR_Y, 48);
        add(blocks, minX, 73, -48, maxX, 78, -45);
        add(blocks, minX, 73, 45, maxX, 78, 48);
        add(blocks, outerMinX, 73, -44, outerMaxX, 78, 44);
        add(blocks, innerMinX, 73, -44, innerMaxX, 78, -28);
        add(blocks, innerMinX, 73, -10, innerMaxX, 78, 10);
        add(blocks, innerMinX, 73, 28, innerMaxX, 78, 44);

        addSideDeckCover(
            blocks,
            minX,
            maxX,
            innerMinX,
            innerMaxX,
            outerEdgeAtMinimumX
        );
    }

    private static void addSideDeckCover(
        Set<BlockBox> blocks,
        int minX,
        int maxX,
        int innerMinX,
        int innerMaxX,
        boolean outerEdgeAtMinimumX
    ) {
        add(blocks, minX + 6, 73, -124, maxX - 6, 75, -120);
        add(blocks, minX + 6, 73, 120, maxX - 6, 75, 124);
        int innerCoverMinX = Math.max(minX, innerMinX - 5);
        int innerCoverMaxX = Math.min(maxX, innerMaxX + 5);
        add(blocks, innerCoverMinX, 73, -92, innerCoverMaxX, 75, -76);
        add(blocks, innerCoverMinX, 73, 76, innerCoverMaxX, 75, 92);

        int middleX = (minX + maxX) / 2;
        add(blocks, minX + 6, 81, -34, middleX - 2, 83, -30);
        add(blocks, middleX + 2, 81, 30, maxX - 6, 83, 34);
        int outerCoverMinX = outerEdgeAtMinimumX ? minX + 5 : maxX - 10;
        int outerCoverMaxX = outerEdgeAtMinimumX ? minX + 10 : maxX - 5;
        add(blocks, outerCoverMinX, 81, -10, outerCoverMaxX, 83, 10);
    }

    private static void addCentralDeck(Set<BlockBox> blocks) {
        add(blocks, -34, 65, -24, 34, 68, 24);
        addCentralStairs(blocks);

        addMirrored(blocks, new BlockBox(10, 69, 15, 28, 71, 18));
        add(blocks, -10, 69, -7, -3, 72, 7);
        add(blocks, 3, 69, -7, 10, 72, 7);
    }

    private static void addCentralStairs(Set<BlockBox> blocks) {
        add(blocks, -9, 65, -33, 9, 65, -31);
        add(blocks, -9, 65, -30, 9, 66, -28);
        add(blocks, -9, 65, -27, 9, 67, -25);
        add(blocks, -9, 65, 31, 9, 65, 33);
        add(blocks, -9, 65, 28, 9, 66, 30);
        add(blocks, -9, 65, 25, 9, 67, 27);

        add(blocks, -33, 65, -9, -31, 65, 9);
        add(blocks, -30, 65, -9, -28, 66, 9);
        add(blocks, -27, 65, -9, -25, 67, 9);
        add(blocks, 31, 65, -9, 33, 65, 9);
        add(blocks, 28, 65, -9, 30, 66, 9);
        add(blocks, 25, 65, -9, 27, 67, 9);
    }

    private static void addGroundCover(Set<BlockBox> blocks) {
        List<BlockBox> firstQuadrant = new ArrayList<>();
        firstQuadrant.add(new BlockBox(18, 65, 43, 42, 68, 47));
        firstQuadrant.add(new BlockBox(48, 65, 18, 52, 68, 39));
        firstQuadrant.add(new BlockBox(58, 65, 68, 72, 67, 77));
        firstQuadrant.add(new BlockBox(22, 65, 92, 40, 68, 96));
        firstQuadrant.add(new BlockBox(72, 65, 120, 90, 67, 124));
        firstQuadrant.add(new BlockBox(82, 65, 50, 96, 68, 57));
        firstQuadrant.forEach(box -> addMirrored(blocks, box));
    }

    private static void addSpawnBunkers(Set<BlockBox> blocks) {
        addNorthBunker(blocks, -62);
        addNorthBunker(blocks, 62);
        addSouthBunker(blocks, -62);
        addSouthBunker(blocks, 62);
    }

    private static void addNorthBunker(Set<BlockBox> blocks, int centerX) {
        add(blocks, centerX - 9, 65, -142, centerX + 9, 68, -139);
        add(blocks, centerX - 9, 65, -138, centerX - 6, 68, -126);
        add(blocks, centerX + 6, 65, -138, centerX + 9, 68, -126);
    }

    private static void addSouthBunker(Set<BlockBox> blocks, int centerX) {
        add(blocks, centerX - 9, 65, 139, centerX + 9, 68, 142);
        add(blocks, centerX - 9, 65, 126, centerX - 6, 68, 138);
        add(blocks, centerX + 6, 65, 126, centerX + 9, 68, 138);
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
        List<JumpPad> pads = new ArrayList<>(8);
        addSideJumpPads(pads, -101, -123);
        addSideJumpPads(pads, 100, 122);
        return List.copyOf(pads);
    }

    private static void addSideJumpPads(List<JumpPad> pads, int groundX, int upperX) {
        pads.add(new JumpPad(groundX, ArenaConstants.FLOOR_Y, -108, 1,
            SECOND_FLOOR_Y, 1.35));
        pads.add(new JumpPad(groundX, ArenaConstants.FLOOR_Y, 108, 1,
            SECOND_FLOOR_Y, 1.35));
        pads.add(new JumpPad(upperX, SECOND_FLOOR_Y, -53, 1,
            THIRD_FLOOR_Y, 1.35));
        pads.add(new JumpPad(upperX, SECOND_FLOOR_Y, 53, 1,
            THIRD_FLOOR_Y, 1.35));
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
