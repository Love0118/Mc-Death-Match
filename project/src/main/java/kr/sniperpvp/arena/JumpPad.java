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
    static final double DEFAULT_VERTICAL_VELOCITY = 1.55;
    static final double LOOK_DIRECTION_BOOST = 0.45;
    private static final double MIN_HORIZONTAL_LOOK_LENGTH = 0.0001;

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

    Vector launchVector(Vector currentVelocity, Vector lookDirection) {
        return launchVector(currentVelocity, lookDirection, verticalVelocity);
    }

    static Vector launchVector(
        Vector currentVelocity,
        Vector lookDirection,
        double requestedVerticalVelocity
    ) {
        Vector launch = currentVelocity.clone();
        Vector horizontalLook = lookDirection.clone().setY(0.0);
        if (horizontalLook.lengthSquared() >= MIN_HORIZONTAL_LOOK_LENGTH) {
            horizontalLook.normalize().multiply(LOOK_DIRECTION_BOOST);
            launch.add(horizontalLook);
        }
        launch.setY(requestedVerticalVelocity);
        return launch;
    }
}
