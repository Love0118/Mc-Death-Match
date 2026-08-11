package kr.sniperpvp.arena;

record BlockBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    BlockBox {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("BlockBox minimums must not exceed maximums");
        }
    }

    long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
}
