// SPDX-License-Identifier: GPL-2.0-only
// Copyright (C) 2026 Curved180 contributors
package dev.curved180;

/** Pure projection geometry, independent of the game and graphics context. */
public record ProjectionPlan(double horizontalRadians, double verticalTangent, double captureTangent,
                             boolean multiView, double pitchRadians, double uprightWeight) {
    public static ProjectionPlan create(double degrees, int width, int height) {
        return create(degrees, width, height, 0);
    }
    public static ProjectionPlan create(double degrees, int width, int height, double pitchDegrees) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Viewport dimensions must be positive");
        double base = Double.isFinite(degrees) ? Math.max(0.01, degrees) : 70;
        double pitch = Double.isFinite(pitchDegrees) ? Math.max(-90, Math.min(90, pitchDegrees)) : 0;
        double steep = smooth(20, 85, Math.abs(pitch));
        double h = Math.toRadians(base + (Math.min(base, 100) - base) * steep);
        double v = Math.min(Math.tan(Math.toRadians(40)), h * height / (2.0 * width));
        boolean multi = h > Math.PI / 3;
        double upright = (1 - smooth(25, 75, Math.abs(pitch))) * smooth(60, 100, base);
        // Cube faces include enough overlap to blend without sampling beyond their edges.
        double capture = multi ? 1.2 : 1.02 * Math.max(Math.tan(h / 2), v / Math.cos(h / 2));
        return new ProjectionPlan(h, v, capture, multi, Math.toRadians(pitch), upright);
    }
    private static double smooth(double a, double b, double x) {
        double t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }
    public float captureDegrees() { return (float)Math.toDegrees(2 * Math.atan(captureTangent)); }
    public int[] views() { return multiView ? new int[]{1, 0, 2, 3, 4, 5} : new int[]{1}; }

    /** Reference for the fragment shader, in the unmodified camera's local coordinates. */
    public double[] ray(double u, double v) {
        double theta = (u - 0.5) * horizontalRadians;
        double y = (v * 2 - 1) * verticalTangent;
        double length = Math.sqrt(1 + y * y);
        double x0 = Math.sin(theta) / length, y0 = y / length, z0 = -Math.cos(theta) / length;
        if (uprightWeight == 0) return new double[]{x0, y0, z0};
        double p = Math.max(Math.toRadians(-85), Math.min(Math.toRadians(85), pitchRadians));
        // This elevation mapping keeps every azimuth upright and never crosses a pole.
        double elevation = Math.atan(y - Math.tan(p));
        double x1 = Math.sin(theta) * Math.cos(elevation);
        double wy = Math.sin(elevation), wz = -Math.cos(theta) * Math.cos(elevation);
        double y1 = Math.cos(p) * wy - Math.sin(p) * wz;
        double z1 = Math.sin(p) * wy + Math.cos(p) * wz;
        double x = x0 + (x1 - x0) * uprightWeight;
        double yy = y0 + (y1 - y0) * uprightWeight;
        double z = z0 + (z1 - z0) * uprightWeight;
        double n = Math.sqrt(x*x + yy*yy + z*z);
        return new double[]{x/n, yy/n, z/n};
    }
}


