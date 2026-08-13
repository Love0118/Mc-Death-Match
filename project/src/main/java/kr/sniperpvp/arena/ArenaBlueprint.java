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
        add(blocks, minX, 65, -140, maxX, SECOND_FLOOR_Y, 140);
        add(blocks, minX, SECOND_FLOOR_Y + 1, -48,
            maxX, THIRD_FLOOR_Y, 48);
        addSideDeckCover(blocks, minX, maxX, outerEdgeAtMinimumX);
    }

    private static void addSideDeckCover(
        Set<BlockBox> blocks,
        int minX,
        int maxX,
        boolean outerEdgeAtMinimumX
    ) {
        add(blocks, minX + 5, 73, -126, maxX - 5, 75, -120);
        add(blocks, minX + 5, 73, 120, maxX - 5, 75, 126);
        int innerCoverMinX = outerEdgeAtMinimumX ? maxX - 8 : minX;
        int innerCoverMaxX = outerEdgeAtMinimumX ? maxX : minX + 8;
        add(blocks, innerCoverMinX, 73, -92, innerCoverMaxX, 75, -76);
        add(blocks, innerCoverMinX, 73, 76, innerCoverMaxX, 75, 92);

        add(blocks, minX + 5, 73, -108, minX + 13, 75, -101);
        add(blocks, maxX - 13, 73, -67, maxX - 5, 76, -59);
        add(blocks, minX + 8, 73, 59, minX + 16, 76, 67);
        add(blocks, maxX - 14, 73, 101, maxX - 5, 75, 108);

        int middleX = (minX + maxX) / 2;
        add(blocks, minX + 5, 81, -38, middleX - 2, 83, -32);
        add(blocks, middleX + 2, 81, 32, maxX - 5, 83, 38);
        int innerTopCoverMinX = outerEdgeAtMinimumX ? maxX - 10 : minX + 5;
        int innerTopCoverMaxX = outerEdgeAtMinimumX ? maxX - 5 : minX + 10;
        add(blocks, innerTopCoverMinX, 81, -10, innerTopCoverMaxX, 83, 10);
        add(blocks, minX + 7, 81, -23, minX + 14, 83, -16);
        add(blocks, maxX - 15, 81, 13, maxX - 7, 84, 21);
    }

    private static void addCentralDeck(Set<BlockBox> blocks) {
        add(blocks, -36, 65, -26, 36, 68, 26);
        add(blocks, -26, 69, -20, 26, 73, -6);
        add(blocks, -26, 69, 6, 26, 73, 20);

        add(blocks, -32, 69, -24, -18, 71, -22);
        add(blocks, 18, 69, -24, 32, 71, -22);
        add(blocks, -32, 69, 22, -18, 71, 24);
        add(blocks, 18, 69, 22, 32, 71, 24);

        add(blocks, -22, 74, -18, -8, 76, -16);
        add(blocks, 8, 74, -10, 22, 76, -8);
        add(blocks, -22, 74, 8, -8, 76, 10);
        add(blocks, 8, 74, 16, 22, 76, 18);
    }

    private static void addGroundCover(Set<BlockBox> blocks) {
        List<BlockBox> firstQuadrant = new ArrayList<>();
        firstQuadrant.add(new BlockBox(18, 65, 43, 42, 68, 47));
        firstQuadrant.add(new BlockBox(48, 65, 18, 52, 68, 39));
        firstQuadrant.add(new BlockBox(46, 65, 50, 57, 69, 60));
        firstQuadrant.add(new BlockBox(58, 65, 68, 72, 67, 77));
        firstQuadrant.add(new BlockBox(78, 65, 72, 91, 69, 78));
        firstQuadrant.add(new BlockBox(22, 65, 92, 40, 68, 96));
        firstQuadrant.add(new BlockBox(48, 65, 103, 62, 68, 110));
        firstQuadrant.add(new BlockBox(72, 65, 120, 90, 67, 124));
        firstQuadrant.add(new BlockBox(82, 65, 50, 96, 68, 57));
        firstQuadrant.add(new BlockBox(93, 65, 83, 101, 69, 95));
        firstQuadrant.add(new BlockBox(12, 65, 67, 20, 69, 78));
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
        List<JumpPad> pads = new ArrayList<>(16);
        addSideJumpPads(pads, -101, -108, -53);
        addSideJumpPads(pads, 100, 107, 53);
        addCentralJumpPads(pads);
        return List.copyOf(pads);
    }

    private static void addSideJumpPads(
        List<JumpPad> pads,
        int groundX,
        int upperX,
        int connectedZ
    ) {
        pads.add(new JumpPad(groundX, ArenaConstants.FLOOR_Y, -108, 1,
            SECOND_FLOOR_Y, JumpPad.DEFAULT_VERTICAL_VELOCITY));
        pads.add(new JumpPad(groundX, ArenaConstants.FLOOR_Y, 108, 1,
            SECOND_FLOOR_Y, JumpPad.DEFAULT_VERTICAL_VELOCITY));
        pads.add(new JumpPad(groundX, ArenaConstants.FLOOR_Y, connectedZ, 1,
            SECOND_FLOOR_Y, JumpPad.DEFAULT_VERTICAL_VELOCITY));
        pads.add(new JumpPad(upperX, SECOND_FLOOR_Y, -53, 1,
            THIRD_FLOOR_Y, JumpPad.DEFAULT_VERTICAL_VELOCITY));
        pads.add(new JumpPad(upperX, SECOND_FLOOR_Y, 53, 1,
            THIRD_FLOOR_Y, JumpPad.DEFAULT_VERTICAL_VELOCITY));
    }

    private static void addCentralJumpPads(List<JumpPad> pads) {
        for (int sign : SIGNS) {
            pads.add(new JumpPad(sign * 40, ArenaConstants.FLOOR_Y, 0, 1,
                68, JumpPad.DEFAULT_VERTICAL_VELOCITY));
            pads.add(new JumpPad(0, ArenaConstants.FLOOR_Y, sign * 30, 1,
                68, JumpPad.DEFAULT_VERTICAL_VELOCITY));
            pads.add(new JumpPad(0, 68, sign * 3, 1,
                73, JumpPad.DEFAULT_VERTICAL_VELOCITY));
        }
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
