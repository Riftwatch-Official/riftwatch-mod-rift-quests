package net.riftwatch.rift_quests.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

final class OrreryMesh {
    private OrreryMesh() {
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int rgb, int light, float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z)
                .setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int rgb, int light, float nx, float ny, float nz, float[] a, float[] b, float[] c, float[] d) {
        vertex(consumer, pose, a[0], a[1], a[2], a[3], a[4], rgb, light, nx, ny, nz);
        vertex(consumer, pose, b[0], b[1], b[2], b[3], b[4], rgb, light, nx, ny, nz);
        vertex(consumer, pose, c[0], c[1], c[2], c[3], c[4], rgb, light, nx, ny, nz);
        vertex(consumer, pose, d[0], d[1], d[2], d[3], d[4], rgb, light, nx, ny, nz);
    }

    private static float[] point(float x, float y, float z, float u, float v) {
        return new float[] {x, y, z, u, v};
    }

    static void box(VertexConsumer consumer, PoseStack.Pose pose, float hx, float hy, float hz, int rgb, int light) {
        float[] half = {hx, hy, hz};
        float[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        for (int axis = 0; axis < 3; axis++) {
            int j = (axis + 1) % 3;
            int k = (axis + 2) % 3;
            for (int sign = -1; sign <= 1; sign += 2) {
                float[][] points = new float[4][];
                for (int index = 0; index < 4; index++) {
                    float[] position = new float[3];
                    position[axis] = sign * half[axis];
                    position[j] = corners[index][0] * half[j];
                    position[k] = corners[index][1] * half[k];
                    points[index] = point(position[0], position[1], position[2], (corners[index][0] + 1) * half[j], (corners[index][1] + 1) * half[k]);
                }
                float[] normal = new float[3];
                normal[axis] = sign;
                quad(consumer, pose, rgb, light, normal[0], normal[1], normal[2], points[0], points[1], points[2], points[3]);
            }
        }
    }

    static void cylinder(VertexConsumer consumer, PoseStack.Pose pose, float radius, float height, int segments, int rgb, int light) {
        float top = height / 2;
        float arc = (float) (2 * Math.PI * radius / segments);
        for (int segment = 0; segment < segments; segment++) {
            double a0 = segment * 2 * Math.PI / segments;
            double a1 = (segment + 1) * 2 * Math.PI / segments;
            float x0 = (float) (radius * Math.cos(a0));
            float z0 = (float) (radius * Math.sin(a0));
            float x1 = (float) (radius * Math.cos(a1));
            float z1 = (float) (radius * Math.sin(a1));
            double mid = (a0 + a1) / 2;
            quad(consumer, pose, rgb, light, (float) Math.cos(mid), 0, (float) Math.sin(mid),
                    point(x0, -top, z0, segment * arc, 0), point(x1, -top, z1, (segment + 1) * arc, 0),
                    point(x1, top, z1, (segment + 1) * arc, height), point(x0, top, z0, segment * arc, height));
            quad(consumer, pose, rgb, light, 0, 1, 0,
                    point(0, top, 0, 0, 0), point(x0, top, z0, x0, z0), point(x1, top, z1, x1, z1), point(x1, top, z1, x1, z1));
            quad(consumer, pose, rgb, light, 0, -1, 0,
                    point(0, -top, 0, 0, 0), point(x1, -top, z1, x1, z1), point(x0, -top, z0, x0, z0), point(x0, -top, z0, x0, z0));
        }
    }

    static void sphere(VertexConsumer consumer, PoseStack.Pose pose, float radius, int rings, int segments, int rgb, int light) {
        for (int ring = 0; ring < rings; ring++) {
            double t0 = ring * Math.PI / rings;
            double t1 = (ring + 1) * Math.PI / rings;
            for (int segment = 0; segment < segments; segment++) {
                double p0 = segment * 2 * Math.PI / segments;
                double p1 = (segment + 1) * 2 * Math.PI / segments;
                float[] a = spherePoint(radius, t0, p0, segment, ring, segments, rings);
                float[] b = spherePoint(radius, t0, p1, segment + 1, ring, segments, rings);
                float[] c = spherePoint(radius, t1, p1, segment + 1, ring + 1, segments, rings);
                float[] d = spherePoint(radius, t1, p0, segment, ring + 1, segments, rings);
                double tm = (t0 + t1) / 2;
                double pm = (p0 + p1) / 2;
                quad(consumer, pose, rgb, light, (float) (Math.sin(tm) * Math.cos(pm)), (float) Math.cos(tm), (float) (Math.sin(tm) * Math.sin(pm)), a, b, c, d);
            }
        }
    }

    private static float[] spherePoint(float radius, double theta, double phi, int segment, int ring, int segments, int rings) {
        return point((float) (radius * Math.sin(theta) * Math.cos(phi)), (float) (radius * Math.cos(theta)), (float) (radius * Math.sin(theta) * Math.sin(phi)),
                2F * segment / segments, (float) ring / rings);
    }

    static void ring(VertexConsumer consumer, PoseStack.Pose pose, float radius, float width, float thickness, int segments, int rgb, int light) {
        float inner = radius - width / 2;
        float outer = radius + width / 2;
        float th = thickness / 2;
        float arc = (float) (2 * Math.PI * radius / segments);
        for (int segment = 0; segment < segments; segment++) {
            double a0 = segment * 2 * Math.PI / segments;
            double a1 = (segment + 1) * 2 * Math.PI / segments;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float cm = (float) Math.cos((a0 + a1) / 2);
            float sm = (float) Math.sin((a0 + a1) / 2);
            float u0 = segment * arc;
            float u1 = (segment + 1) * arc;
            quad(consumer, pose, rgb, light, cm, 0, sm,
                    point(outer * c0, -th, outer * s0, u0, 0), point(outer * c1, -th, outer * s1, u1, 0),
                    point(outer * c1, th, outer * s1, u1, thickness), point(outer * c0, th, outer * s0, u0, thickness));
            quad(consumer, pose, rgb, light, -cm, 0, -sm,
                    point(inner * c0, -th, inner * s0, u0, 0), point(inner * c1, -th, inner * s1, u1, 0),
                    point(inner * c1, th, inner * s1, u1, thickness), point(inner * c0, th, inner * s0, u0, thickness));
            quad(consumer, pose, rgb, light, 0, 1, 0,
                    point(inner * c0, th, inner * s0, u0, 0), point(outer * c0, th, outer * s0, u0, width),
                    point(outer * c1, th, outer * s1, u1, width), point(inner * c1, th, inner * s1, u1, 0));
            quad(consumer, pose, rgb, light, 0, -1, 0,
                    point(inner * c0, -th, inner * s0, u0, 0), point(outer * c0, -th, outer * s0, u0, width),
                    point(outer * c1, -th, outer * s1, u1, width), point(inner * c1, -th, inner * s1, u1, 0));
        }
    }

    static void gem(VertexConsumer consumer, PoseStack.Pose pose, float x, float z, int rgb, int light) {
        float rx = 0.3F;
        float ry = 0.36F;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    float nx = sx / rx;
                    float ny = sy / ry;
                    float nz = sz / rx;
                    float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    float[] a = point(x + sx * rx, 0, z, 0, 0);
                    float[] b = point(x, sy * ry, z, 1, 0);
                    float[] c = point(x, 0, z + sz * rx, 1, 1);
                    quad(consumer, pose, rgb, light, nx / length, ny / length, nz / length, a, b, c, c);
                }
            }
        }
    }

    static void glowBox(VertexConsumer consumer, PoseStack.Pose pose, float hx, float hy, float hz, int rgb, int alpha) {
        float[] half = {hx, hy, hz};
        float[][] corners = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        for (int axis = 0; axis < 3; axis++) {
            int j = (axis + 1) % 3;
            int k = (axis + 2) % 3;
            for (int sign = -1; sign <= 1; sign += 2) {
                for (int index = 0; index < 4; index++) {
                    float[] position = new float[3];
                    position[axis] = sign * half[axis];
                    position[j] = corners[index][0] * half[j];
                    position[k] = corners[index][1] * half[k];
                    consumer.addVertex(pose, position[0], position[1], position[2]).setColor(r, g, b, alpha);
                }
            }
        }
    }
}
