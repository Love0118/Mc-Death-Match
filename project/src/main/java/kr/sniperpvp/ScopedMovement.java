package kr.sniperpvp;

import org.bukkit.Input;
import org.bukkit.util.Vector;

final class ScopedMovement {
    static final double ATTRIBUTE_SPEED_TO_BLOCKS_PER_TICK = 2.1585;
    static final double SPRINT_MULTIPLIER = 1.3;
    static final double SNEAK_MULTIPLIER = 0.3;

    private ScopedMovement() {
    }

    static Vector velocity(float yaw, Input input, double movementSpeed, double verticalVelocity) {
        return velocity(yaw, InputState.from(input), movementSpeed, verticalVelocity);
    }

    static Vector velocity(
        float yaw,
        InputState input,
        double movementSpeed,
        double verticalVelocity
    ) {
        int forwardAxis = booleanValue(input.forward()) - booleanValue(input.backward());
        int strafeAxis = booleanValue(input.right()) - booleanValue(input.left());
        if (forwardAxis == 0 && strafeAxis == 0) {
            return null;
        }

        double yawRadians = Math.toRadians(yaw);
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        double rightX = -Math.cos(yawRadians);
        double rightZ = -Math.sin(yawRadians);
        double x = forwardX * forwardAxis + rightX * strafeAxis;
        double z = forwardZ * forwardAxis + rightZ * strafeAxis;
        double length = Math.hypot(x, z);

        double multiplier = 1.0;
        if (input.sneak()) {
            multiplier = SNEAK_MULTIPLIER;
        } else if (input.sprint() && forwardAxis > 0) {
            multiplier = SPRINT_MULTIPLIER;
        }
        double blocksPerTick = movementSpeed * ATTRIBUTE_SPEED_TO_BLOCKS_PER_TICK * multiplier;
        return new Vector(
            x / length * blocksPerTick,
            verticalVelocity,
            z / length * blocksPerTick
        );
    }

    private static int booleanValue(boolean value) {
        return value ? 1 : 0;
    }

    record InputState(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean sprint,
        boolean sneak
    ) {
        static InputState from(Input input) {
            return new InputState(
                input.isForward(),
                input.isBackward(),
                input.isLeft(),
                input.isRight(),
                input.isSprint(),
                input.isSneak()
            );
        }
    }
}
