package kr.sniperpvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class ScopedMovementTest {
    private static final double MOVEMENT_SPEED = 0.25;
    private static final double EXPECTED_WALK_SPEED =
        MOVEMENT_SPEED * ScopedMovement.ATTRIBUTE_SPEED_TO_BLOCKS_PER_TICK;

    @Test
    void forwardMovementUsesYawAndPreservesVerticalVelocity() {
        Vector velocity = ScopedMovement.velocity(
            0.0f,
            input(true, false, false, false, false, false),
            MOVEMENT_SPEED,
            -0.37
        );

        assertEquals(0.0, velocity.getX(), 0.00001);
        assertEquals(-0.37, velocity.getY(), 0.00001);
        assertEquals(EXPECTED_WALK_SPEED, velocity.getZ(), 0.00001);
    }

    @Test
    void diagonalMovementIsNormalizedInsteadOfMovingFaster() {
        Vector velocity = ScopedMovement.velocity(
            0.0f,
            input(true, false, false, true, false, false),
            MOVEMENT_SPEED,
            0.0
        );

        assertEquals(EXPECTED_WALK_SPEED, Math.hypot(velocity.getX(), velocity.getZ()), 0.00001);
        assertEquals(velocity.getX(), -velocity.getZ(), 0.00001);
    }

    @Test
    void sprintAndSneakUseTheirNormalMovementRatios() {
        Vector sprint = ScopedMovement.velocity(
            90.0f,
            input(true, false, false, false, true, false),
            MOVEMENT_SPEED,
            0.2
        );
        Vector sneak = ScopedMovement.velocity(
            90.0f,
            input(true, false, false, false, false, true),
            MOVEMENT_SPEED,
            0.2
        );

        assertEquals(
            EXPECTED_WALK_SPEED * ScopedMovement.SPRINT_MULTIPLIER,
            Math.hypot(sprint.getX(), sprint.getZ()),
            0.00001
        );
        assertEquals(
            EXPECTED_WALK_SPEED * ScopedMovement.SNEAK_MULTIPLIER,
            Math.hypot(sneak.getX(), sneak.getZ()),
            0.00001
        );
    }

    @Test
    void noInputOrOpposingInputLeavesPhysicsUntouched() {
        assertNull(ScopedMovement.velocity(
            0.0f,
            input(false, false, false, false, false, false),
            MOVEMENT_SPEED,
            0.0
        ));
        assertNull(ScopedMovement.velocity(
            0.0f,
            input(true, true, true, true, false, false),
            MOVEMENT_SPEED,
            0.0
        ));
    }

    private static ScopedMovement.InputState input(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean sprint,
        boolean sneak
    ) {
        return new ScopedMovement.InputState(forward, backward, left, right, sprint, sneak);
    }
}
