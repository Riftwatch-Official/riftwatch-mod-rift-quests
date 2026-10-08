package net.riftwatch.rift_quests.client.art;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.riftwatch.rift_quests.RiftQuestsMod;

public final class PixelTexture {
    private final ResourceLocation id;
    private DynamicTexture texture;
    private int width;
    private int height;

    public PixelTexture(String name) {
        id = ResourceLocation.fromNamespaceAndPath(RiftQuestsMod.MOD_ID, "dynamic/" + name);
    }

    public ResourceLocation id() {
        return id;
    }

    public void upload(Canvas canvas) {
        uploadRegion(canvas, 0, 0, canvas.width, canvas.height);
    }

    public void uploadRegion(Canvas canvas, int x, int y, int w, int h) {
        boolean fresh = texture == null || width != canvas.width || height != canvas.height;
        if (fresh) {
            close();
            width = canvas.width;
            height = canvas.height;
            texture = new DynamicTexture(new NativeImage(width, height, false));
            Minecraft.getInstance().getTextureManager().register(id, texture);
        }
        NativeImage image = texture.getPixels();
        if (image == null) {
            return;
        }
        int left = fresh ? 0 : Math.max(0, x);
        int top = fresh ? 0 : Math.max(0, y);
        int right = fresh ? width : Math.min(width, x + w);
        int bottom = fresh ? height : Math.min(height, y + h);
        if (left >= right || top >= bottom) {
            return;
        }
        int[] pixels = canvas.pixels;
        for (int row = top; row < bottom; row++) {
            int base = row * width;
            for (int column = left; column < right; column++) {
                int colour = pixels[base + column];
                image.setPixelRGBA(column, row, (colour & 0xFF00FF00) | ((colour >> 16) & 0xFF) | ((colour & 0xFF) << 16));
            }
        }
        texture.bind();
        image.upload(0, left, top, left, top, right - left, bottom - top, false, false);
    }

    public void close() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(id);
            texture = null;
        }
    }
}
