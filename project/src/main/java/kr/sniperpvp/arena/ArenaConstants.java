package kr.sniperpvp.arena;

public final class ArenaConstants {
    public static final int MIN_X = -150;
    public static final int MAX_X = 149;
    public static final int MIN_Z = -150;
    public static final int MAX_Z = 149;
    public static final int FLOOR_BOTTOM_Y = 60;
    public static final int FLOOR_Y = 64;
    public static final int MAX_BUILD_Y = 90;
    public static final int REBUILD_CLEAR_MAX_Y = 96;
    public static final int BUILD_VERSION = 2;

    private ArenaConstants() {
    }

    public static boolean contains(int x, int z) {
        return x >= MIN_X && x <= MAX_X && z >= MIN_Z && z <= MAX_Z;
    }
}
