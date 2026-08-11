package kr.sniperpvp;

enum HitZone {
    LEGS,
    BODY,
    HEAD;

    private static final double BOUNDARY_EPSILON = 1.0E-9;

    static HitZone atHeight(
        double hitY,
        double minimumY,
        double height,
        double legHeightRatio,
        double headHeightRatio
    ) {
        if (height <= 0.0) {
            return BODY;
        }
        double ratio = (hitY - minimumY) / height;
        if (ratio < legHeightRatio - BOUNDARY_EPSILON) {
            return LEGS;
        }
        if (ratio >= headHeightRatio - BOUNDARY_EPSILON) {
            return HEAD;
        }
        return BODY;
    }
}
