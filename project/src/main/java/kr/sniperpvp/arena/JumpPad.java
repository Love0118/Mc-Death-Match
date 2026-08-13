package kr.sniperpvp.arena;

import org.bukkit.util.Vector;

record JumpPad(
    int centerX,
    int surfaceY,
    int centerZ,
    int radius,
    int targetSurfaceY,
    double verticalVelocity
) {
    JumpPad {
        if (radius < 0) {
            throw new IllegalArgumentException("Jump-pad radius must not be negative");
        }
        if (targetSurfaceY <= surfaceY) {
            throw new IllegalArgumentException("Jump-pad target must be above its source surface");
        }
        if (verticalVelocity <= 0.0) {
            throw new IllegalArgumentException("Jump-pad vertical velocity must be positive");
        }
    }

    boolean contains(int blockX, int blockY, int blockZ) {
        return blockY == surfaceY
            && Math.abs(blockX - centerX) <= radius
            && Math.abs(blockZ - centerZ) <= radius;
    }

    int blockCount() {
        int width = radius * 2 + 1;
        return width * width;
    }

    Vector launchVector(Vector currentVelocity) {
        return new Vector(currentVelocity.getX(), verticalVelocity, currentVelocity.getZ());
    }
}
