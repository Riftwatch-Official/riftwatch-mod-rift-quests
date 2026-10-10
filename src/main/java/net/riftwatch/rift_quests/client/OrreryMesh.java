package net.riftwatch.rift_quests.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

final class OrreryMesh {
    private static final float[][] CORNERS = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};

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

    private static float[] point(float x, float y, float z, float u, float v) {
        return new float[] {x, y, z, u, v};
    }

    static void box(VertexConsumer consumer, PoseStack.Pose pose, float hx, float hy, float hz, int rgb, int light) {
        float[] half = {hx, hy, hz};
        for (int axis = 0; axis < 3; axis++) {
            int j = (axis + 1) % 3;
            int k = (axis + 2) % 3;
            for (int sign = -1; sign <= 1; sign += 2) {
                float[] normal = new float[3];
                normal[axis] = sign;
                for (int index = 0; index < 4; index++) {
                    float[] position = new float[3];
                    position[axis] = sign * half[axis];
                    position[j] = CORNERS[index][0] * half[j];
                    position[k] = CORNERS[index][1] * half[k];
                    float[] corner = point(position[0], position[1], position[2], (CORNERS[index][0] + 1) * half[j], (CORNERS[index][1] + 1) * half[k]);
                    vertex(consumer, pose, corner[0], corner[1], corner[2], corner[3], corner[4], rgb, light, normal[0], normal[1], normal[2]);
                }
            }
        }
    }

    static void glowBox(VertexConsumer consumer, PoseStack.Pose pose, float hx, float hy, float hz, int rgb, int alpha) {
        float[] half = {hx, hy, hz};
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        for (int axis = 0; axis < 3; axis++) {
            int j = (axis + 1) % 3;
            int k = (axis + 2) % 3;
            for (int sign = -1; sign <= 1; sign += 2) {
                for (int pass = 0; pass < 2; pass++) {
                    for (int step = 0; step < 4; step++) {
                        int index = pass == 0 ? step : 3 - step;
                        float[] position = new float[3];
                        position[axis] = sign * half[axis];
                        position[j] = CORNERS[index][0] * half[j];
                        position[k] = CORNERS[index][1] * half[k];
                        consumer.addVertex(pose, position[0], position[1], position[2]).setColor(r, g, b, alpha);
                    }
                }
            }
        }
    }
}
