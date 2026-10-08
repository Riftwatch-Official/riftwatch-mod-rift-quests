package net.riftwatch.rift_quests.client.art;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class Layer {
    private final PixelTexture texture;
    private Canvas canvas;
    private Object key;
    private int hash;
    private boolean uploaded;

    public Layer(String name) {
        texture = new PixelTexture(name);
    }

    public Canvas canvas() {
        return canvas;
    }

    public ResourceLocation id() {
        return texture.id();
    }

    public boolean stale(Object next) {
        if (canvas != null && Objects.equals(key, next)) {
            return false;
        }
        key = next;
        return true;
    }

    public Canvas begin(int w, int h) {
        if (canvas == null || canvas.width != Math.max(1, w) || canvas.height != Math.max(1, h)) {
            canvas = new Canvas(w, h);
            uploaded = false;
        } else {
            canvas.resetClip();
            canvas.clear();
        }
        return canvas;
    }

    public void commit() {
        int next = Arrays.hashCode(canvas.pixels);
        if (!uploaded || next != hash) {
            texture.upload(canvas);
            hash = next;
            uploaded = true;
        }
    }

    public void commitRegion(int x, int y, int w, int h, boolean full) {
        if (!uploaded || full) {
            texture.upload(canvas);
            uploaded = true;
        } else {
            texture.uploadRegion(canvas, x, y, w, h);
        }
    }

    public void draw(GuiGraphics graphics, int x, int y) {
        if (canvas != null) {
            blit(graphics, texture.id(), x, y, canvas.width, canvas.height);
        }
    }

    public static void blit(GuiGraphics graphics, ResourceLocation id, int x, int y, int w, int h) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(id, x, y, 0.0F, 0.0F, w, h, w, h);
        RenderSystem.disableBlend();
    }

    public void close() {
        texture.close();
        canvas = null;
        key = null;
        uploaded = false;
    }
}
