package net.riftwatch.rift_quests.client.title;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

final class TitleCanvas {
    static final float W = 960f;
    static final float H = 540f;
    static final int WHITE = 0xFFFFFF;
    private static final float TAU = (float) (Math.PI * 2);
    private static final int MAX_DEPTH = 32;
    private static final Set<ResourceLocation> FILTERED = new HashSet<>();

    private final GuiGraphics graphics;
    private final Matrix4f base;
    private final float scale;
    private final float offsetX;
    private final float guiWidth;
    private final float guiHeight;
    private final float[] stack = new float[MAX_DEPTH * 7];
    private int depth;
    private float a = 1;
    private float b;
    private float c;
    private float d = 1;
    private float e;
    private float f;
    private float alpha = 1;
    private BufferBuilder buffer;
    private boolean textured;
    private ResourceLocation texture;
    private boolean additive;

    TitleCanvas(GuiGraphics graphics) {
        this.graphics = graphics;
        guiWidth = graphics.guiWidth();
        guiHeight = graphics.guiHeight();
        scale = Math.min(guiWidth / W, guiHeight / H);
        offsetX = (guiWidth - W * scale) / 2f;
        base = new Matrix4f(graphics.pose().last().pose()).translate(offsetX, 0, 0).scale(scale, scale, 1);
    }

    float left() {
        return -offsetX / scale;
    }

    float right() {
        return (guiWidth - offsetX) / scale;
    }

    float bottom() {
        return guiHeight / scale;
    }

    void begin() {
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    void end() {
        flush();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    void push() {
        if (depth >= MAX_DEPTH) {
            throw new IllegalStateException("Title canvas stack overflow");
        }
        int at = depth * 7;
        stack[at] = a;
        stack[at + 1] = b;
        stack[at + 2] = c;
        stack[at + 3] = d;
        stack[at + 4] = e;
        stack[at + 5] = f;
        stack[at + 6] = alpha;
        depth++;
    }

    void pop() {
        depth--;
        int at = depth * 7;
        a = stack[at];
        b = stack[at + 1];
        c = stack[at + 2];
        d = stack[at + 3];
        e = stack[at + 4];
        f = stack[at + 5];
        alpha = stack[at + 6];
    }

    void translate(float x, float y) {
        e += a * x + c * y;
        f += b * x + d * y;
    }

    void scale(float sx, float sy) {
        a *= sx;
        b *= sx;
        c *= sy;
        d *= sy;
    }

    void rotate(float radians) {
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float na = a * cos + c * sin;
        float nb = b * cos + d * sin;
        float nc = -a * sin + c * cos;
        float nd = -b * sin + d * cos;
        a = na;
        b = nb;
        c = nc;
        d = nd;
    }

    void fade(float factor) {
        alpha *= factor;
    }

    void additive(boolean on) {
        if (on != additive) {
            flush();
            additive = on;
        }
    }

    void clip(float x0, float y0, float x1, float y1) {
        flush();
        graphics.enableScissor(Math.round(offsetX + x0 * scale), Math.round(y0 * scale), Math.round(offsetX + x1 * scale), Math.round(y1 * scale));
    }

    void unclip() {
        flush();
        graphics.disableScissor();
    }

    int argb(int rgb, float a) {
        float value = Math.max(0, Math.min(1, a * alpha));
        return (Math.round(value * 255) << 24) | (rgb & 0xFFFFFF);
    }

    void rect(float x, float y, float w, float h, int rgb, float a) {
        if (a <= 0 || w <= 0 || h <= 0) {
            return;
        }
        int color = argb(rgb, a);
        quad(x, y, x + w, y, x + w, y + h, x, y + h, color, color, color, color);
    }

    void px(float x, float y, float size, int rgb, float a) {
        rect(Math.round(x - size / 2f), Math.round(y - size / 2f), size, size, rgb, a);
    }

    void gradientH(float x, float y, float w, float h, int leftRgb, float leftA, int rightRgb, float rightA) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int left = argb(leftRgb, leftA);
        int right = argb(rightRgb, rightA);
        quad(x, y, x + w, y, x + w, y + h, x, y + h, left, right, right, left);
    }

    void gradientV(float x, float y, float w, float h, int topRgb, float topA, int bottomRgb, float bottomA) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int top = argb(topRgb, topA);
        int bottom = argb(bottomRgb, bottomA);
        quad(x, y, x + w, y, x + w, y + h, x, y + h, top, top, bottom, bottom);
    }

    void quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int c1, int c2, int c3, int c4) {
        BufferBuilder buf = colors();
        vertex(buf, x1, y1, c1);
        vertex(buf, x2, y2, c2);
        vertex(buf, x3, y3, c3);
        vertex(buf, x1, y1, c1);
        vertex(buf, x3, y3, c3);
        vertex(buf, x4, y4, c4);
    }

    void quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int rgb, float a) {
        if (a <= 0) {
            return;
        }
        int color = argb(rgb, a);
        quad(x1, y1, x2, y2, x3, y3, x4, y4, color, color, color, color);
    }

    void tri(float x1, float y1, float x2, float y2, float x3, float y3, int rgb, float a) {
        if (a <= 0) {
            return;
        }
        int color = argb(rgb, a);
        BufferBuilder buf = colors();
        vertex(buf, x1, y1, color);
        vertex(buf, x2, y2, color);
        vertex(buf, x3, y3, color);
    }

    void ellipse(float cx, float cy, float rx, float ry, int innerRgb, float innerA, int outerRgb, float outerA) {
        if (innerA <= 0 && outerA <= 0) {
            return;
        }
        int inner = argb(innerRgb, innerA);
        int outer = argb(outerRgb, outerA);
        int segments = 48;
        BufferBuilder buf = colors();
        for (int index = 0; index < segments; index++) {
            float a0 = TAU * index / segments;
            float a1 = TAU * (index + 1) / segments;
            vertex(buf, cx, cy, inner);
            vertex(buf, cx + (float) Math.cos(a0) * rx, cy + (float) Math.sin(a0) * ry, outer);
            vertex(buf, cx + (float) Math.cos(a1) * rx, cy + (float) Math.sin(a1) * ry, outer);
        }
    }

    void disc(float cx, float cy, float r, int rgb, float a) {
        ellipse(cx, cy, r, r, rgb, a, rgb, a);
    }

    void arc(float cx, float cy, float rx, float ry, float width, float from, float to, int rgb, float a) {
        if (a <= 0 || to <= from) {
            return;
        }
        int color = argb(rgb, a);
        int segments = Math.max(4, (int) Math.ceil((to - from) / TAU * 96));
        float half = width / 2f;
        for (int index = 0; index < segments; index++) {
            float a0 = from + (to - from) * index / segments;
            float a1 = from + (to - from) * (index + 1) / segments;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            quad(cx + c0 * (rx - half), cy + s0 * (ry - half), cx + c0 * (rx + half), cy + s0 * (ry + half),
                    cx + c1 * (rx + half), cy + s1 * (ry + half), cx + c1 * (rx - half), cy + s1 * (ry - half), color, color, color, color);
        }
    }

    void line(float x1, float y1, float x2, float y2, float width, int rgb, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0 || a <= 0) {
            return;
        }
        float nx = -dy / length * width / 2f;
        float ny = dx / length * width / 2f;
        quad(x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny, rgb, a);
    }

    void image(TitleLayout.Layer layer, float sx0, float sy0, float sx1, float sy1, float dx0, float dy0, float dx1, float dy1, int rgb, float topA, float bottomA) {
        if ((topA <= 0 && bottomA <= 0) || sx1 <= sx0 || sy1 <= sy0) {
            return;
        }
        float cx0 = Math.max(sx0, layer.x0());
        float cx1 = Math.min(sx1, layer.x1());
        float cy0 = Math.max(sy0, layer.y0());
        float cy1 = Math.min(sy1, layer.y1());
        if (cx1 <= cx0 || cy1 <= cy0) {
            return;
        }
        float kx = (dx1 - dx0) / (sx1 - sx0);
        float ky = (dy1 - dy0) / (sy1 - sy0);
        float px0 = dx0 + (cx0 - sx0) * kx;
        float px1 = dx0 + (cx1 - sx0) * kx;
        float py0 = dy0 + (cy0 - sy0) * ky;
        float py1 = dy0 + (cy1 - sy0) * ky;
        float lw = layer.x1() - layer.x0();
        float lh = layer.y1() - layer.y0();
        float u0 = (cx0 - layer.x0()) / lw;
        float u1 = (cx1 - layer.x0()) / lw;
        float v0 = (cy0 - layer.y0()) / lh;
        float v1 = (cy1 - layer.y0()) / lh;
        float span = sy1 - sy0;
        int top = argb(rgb, topA + (bottomA - topA) * (cy0 - sy0) / span);
        int bottom = argb(rgb, topA + (bottomA - topA) * (cy1 - sy0) / span);
        BufferBuilder buf = textured(layer.id());
        vertex(buf, px0, py0, u0, v0, top);
        vertex(buf, px1, py0, u1, v0, top);
        vertex(buf, px1, py1, u1, v1, bottom);
        vertex(buf, px0, py0, u0, v0, top);
        vertex(buf, px1, py1, u1, v1, bottom);
        vertex(buf, px0, py1, u0, v1, bottom);
    }

    void image(TitleLayout.Layer layer, float sx0, float sy0, float sx1, float sy1, float dx0, float dy0, float dx1, float dy1, int rgb, float a) {
        image(layer, sx0, sy0, sx1, sy1, dx0, dy0, dx1, dy1, rgb, a, a);
    }

    private BufferBuilder colors() {
        if (buffer != null && !textured) {
            return buffer;
        }
        flush();
        textured = false;
        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        return buffer;
    }

    private BufferBuilder textured(ResourceLocation id) {
        if (buffer != null && textured && id.equals(texture)) {
            return buffer;
        }
        flush();
        textured = true;
        texture = id;
        if (FILTERED.add(id)) {
            Minecraft.getInstance().getTextureManager().getTexture(id).setFilter(true, false);
        }
        buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        return buffer;
    }

    private void vertex(BufferBuilder buf, float x, float y, int color) {
        buf.addVertex(base, a * x + c * y + e, b * x + d * y + f, 0).setColor(color);
    }

    private void vertex(BufferBuilder buf, float x, float y, float u, float v, int color) {
        buf.addVertex(base, a * x + c * y + e, b * x + d * y + f, 0).setUv(u, v).setColor(color);
    }

    private void flush() {
        if (buffer == null) {
            return;
        }
        MeshData mesh = buffer.build();
        buffer = null;
        if (mesh == null) {
            return;
        }
        if (additive) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        if (textured) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, texture);
        } else {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
        }
        BufferUploader.drawWithShader(mesh);
    }
}
