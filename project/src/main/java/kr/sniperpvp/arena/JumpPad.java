package kr.sniperpvp.arena;

import org.bukkit.util.Vector;

record JumpPad(
    int centerX,
    int surfaceY,
    int centerZ,
    int radius,
    int targetX,
    int targetSurfaceY,
    int targetZ,
    double verticalVelocity,
    double horizontalSpeed
) {
    private static final double MIN_DIRECTION_LENGTH = 0.0001;
    private static final double MINECRAFT_GRAVITY = 0.08;
    private static final double VERTICAL_DRAG = 0.98;
    private static final int MAX_GUIDANCE_DELAY_TICKS = 40;

    JumpPad {
        if (radius < 0) {
            throw new IllegalArgumentException("Jump-pad radius must not be negative");
        }
        if (targetSurfaceY <= surfaceY) {
            throw new IllegalArgumentException("Jump-pad target must be above its source surface");
        }
        if (verticalVelocity <= 0.0 || horizontalSpeed < 0.0) {
            throw new IllegalArgumentException("Jump-pad velocity values must be positive");
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

    Vector verticalLaunchVector() {
        return new Vector(0.0, verticalVelocity, 0.0);
    }

    Vector guidedLaunchVector(double playerX, double playerZ, double currentVerticalVelocity) {
        double deltaX = targetX + 0.5 - playerX;
        double deltaZ = targetZ + 0.5 - playerZ;
        double length = Math.hypot(deltaX, deltaZ);
        if (length < MIN_DIRECTION_LENGTH) {
            return new Vector(0.0, currentVerticalVelocity, 0.0);
        }
        return new Vector(
            deltaX / length * horizontalSpeed,
            currentVerticalVelocity,
            deltaZ / length * horizontalSpeed
        );
    }

    int horizontalGuidanceDelayTicks() {
        double height = 0.0;
        double velocity = verticalVelocity;
        int requiredRise = targetSurfaceY - surfaceY;
        for (int tick = 1; tick <= MAX_GUIDANCE_DELAY_TICKS; tick++) {
            height += velocity;
            velocity = (velocity - MINECRAFT_GRAVITY) * VERTICAL_DRAG;
            if (height >= requiredRise + 0.25) {
                return tick;
            }
        }
        throw new IllegalStateException("Jump pad cannot reach its target height");
    }
}
