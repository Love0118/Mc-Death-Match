package kr.sniperpvp;

import org.bukkit.util.Vector;

final class AimSpread {
    private AimSpread() {
    }

    static Vector apply(
        Vector direction,
        double maximumSpread,
        double radialSample,
        double angleRadians
    ) {
        Vector forward = direction.clone().normalize();
        if (maximumSpread <= 0.0) {
            return forward;
        }
        double sample = Math.max(0.0, Math.min(1.0, radialSample));
        double radius = Math.sqrt(sample) * maximumSpread;
        Vector reference = Math.abs(forward.getY()) < 0.999
            ? new Vector(0.0, 1.0, 0.0)
            : new Vector(1.0, 0.0, 0.0);
        Vector right = forward.clone().crossProduct(reference).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();
        return forward
            .add(right.multiply(radius * Math.cos(angleRadians)))
            .add(up.multiply(radius * Math.sin(angleRadians)))
            .normalize();
    }
}
