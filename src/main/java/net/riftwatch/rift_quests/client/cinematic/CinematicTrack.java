package net.riftwatch.rift_quests.client.cinematic;

import java.util.List;
import net.minecraft.world.phys.Vec3;

record CinematicTrack(List<Key> keys) {
    record Key(float time, Vec3 value) {
    }

    Vec3 at(float t) {
        int n = keys.size();
        if (t <= keys.get(0).time()) {
            return keys.get(0).value();
        }
        if (t >= keys.get(n - 1).time()) {
            return keys.get(n - 1).value();
        }
        int i = 0;
        while (keys.get(i + 1).time() < t) {
            i++;
        }
        Key p0 = keys.get(Math.max(0, i - 1));
        Key p1 = keys.get(i);
        Key p2 = keys.get(i + 1);
        Key p3 = keys.get(Math.min(n - 1, i + 2));
        double u = (t - p1.time()) / (p2.time() - p1.time());
        return new Vec3(spline(p0.value().x, p1.value().x, p2.value().x, p3.value().x, u),
                spline(p0.value().y, p1.value().y, p2.value().y, p3.value().y, u),
                spline(p0.value().z, p1.value().z, p2.value().z, p3.value().z, u));
    }

    static float scalar(float[][] keys, float t) {
        if (t <= keys[0][0]) {
            return keys[0][1];
        }
        if (t >= keys[keys.length - 1][0]) {
            return keys[keys.length - 1][1];
        }
        int i = 0;
        while (keys[i + 1][0] < t) {
            i++;
        }
        float[] p0 = keys[Math.max(0, i - 1)];
        float[] p1 = keys[i];
        float[] p2 = keys[i + 1];
        float[] p3 = keys[Math.min(keys.length - 1, i + 2)];
        double u = (t - p1[0]) / (p2[0] - p1[0]);
        return (float) spline(p0[1], p1[1], p2[1], p3[1], u);
    }

    private static double spline(double a, double b, double c, double d, double u) {
        double u2 = u * u;
        double u3 = u2 * u;
        return 0.5 * (2 * b + (-a + c) * u + (2 * a - 5 * b + 4 * c - d) * u2 + (-a + 3 * b - 3 * c + d) * u3);
    }
}
