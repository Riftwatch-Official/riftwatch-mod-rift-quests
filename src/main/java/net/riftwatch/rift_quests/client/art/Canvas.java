package net.riftwatch.rift_quests.client.art;

import java.util.Arrays;

public final class Canvas {
    public final int width;
    public final int height;
    public final int[] pixels;
    private int clipLeft;
    private int clipTop;
    private int clipRight;
    private int clipBottom;

    public Canvas(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.pixels = new int[this.width * this.height];
        resetClip();
    }

    public void clip(int x, int y, int w, int h) {
        clipLeft = Math.max(0, x);
        clipTop = Math.max(0, y);
        clipRight = Math.min(width, x + w);
        clipBottom = Math.min(height, y + h);
    }

    public void resetClip() {
        clipLeft = 0;
        clipTop = 0;
        clipRight = width;
        clipBottom = height;
    }

    public void clear() {
        Arrays.fill(pixels, 0);
    }

    public void copyFrom(Canvas source) {
        System.arraycopy(source.pixels, 0, pixels, 0, Math.min(pixels.length, source.pixels.length));
    }

    public int get(int x, int y) {
        return x < 0 || y < 0 || x >= width || y >= height ? 0 : pixels[y * width + x];
    }

    public void set(int x, int y, int colour) {
        if (x >= clipLeft && y >= clipTop && x < clipRight && y < clipBottom) {
            pixels[y * width + x] = colour;
        }
    }

    public void px(int x, int y, int colour) {
        if (x < clipLeft || y < clipTop || x >= clipRight || y >= clipBottom) {
            return;
        }
        int index = y * width + x;
        pixels[index] = (colour >>> 24) == 0xFF ? colour : blend(pixels[index], colour);
    }

    public void rect(int x, int y, int w, int h, int colour) {
        int left = Math.max(clipLeft, x);
        int top = Math.max(clipTop, y);
        int right = Math.min(clipRight, x + w);
        int bottom = Math.min(clipBottom, y + h);
        if (left >= right || top >= bottom) {
            return;
        }
        boolean opaque = (colour >>> 24) == 0xFF;
        for (int row = top; row < bottom; row++) {
            int base = row * width;
            if (opaque) {
                Arrays.fill(pixels, base + left, base + right, colour);
            } else {
                for (int column = left; column < right; column++) {
                    pixels[base + column] = blend(pixels[base + column], colour);
                }
            }
        }
    }

    public void erase(int x, int y, int w, int h) {
        int left = Math.max(clipLeft, x);
        int top = Math.max(clipTop, y);
        int right = Math.min(clipRight, x + w);
        int bottom = Math.min(clipBottom, y + h);
        for (int row = top; row < bottom; row++) {
            Arrays.fill(pixels, row * width + left, row * width + Math.max(left, right), 0);
        }
    }

    public void draw(Canvas source, int x, int y) {
        int left = Math.max(clipLeft, x);
        int top = Math.max(clipTop, y);
        int right = Math.min(clipRight, x + source.width);
        int bottom = Math.min(clipBottom, y + source.height);
        for (int row = top; row < bottom; row++) {
            int sourceBase = (row - y) * source.width - x;
            int base = row * width;
            for (int column = left; column < right; column++) {
                int colour = source.pixels[sourceBase + column];
                int alpha = colour >>> 24;
                if (alpha == 0xFF) {
                    pixels[base + column] = colour;
                } else if (alpha != 0) {
                    pixels[base + column] = blend(pixels[base + column], colour);
                }
            }
        }
    }

    public void drawScaled(Canvas source, int x, int y, int scale) {
        for (int row = 0; row < source.height; row++) {
            for (int column = 0; column < source.width; column++) {
                int colour = source.pixels[row * source.width + column];
                if ((colour >>> 24) != 0) {
                    rect(x + column * scale, y + row * scale, scale, scale, colour);
                }
            }
        }
    }

    public void drawTiled(Canvas tile, int offsetX, int offsetY) {
        for (int row = clipTop; row < clipBottom; row++) {
            int tileRow = Math.floorMod(row - offsetY, tile.height) * tile.width;
            int base = row * width;
            for (int column = clipLeft; column < clipRight; column++) {
                pixels[base + column] = tile.pixels[tileRow + Math.floorMod(column - offsetX, tile.width)];
            }
        }
    }

    public static int blend(int destination, int source) {
        int sourceAlpha = source >>> 24;
        int destinationAlpha = destination >>> 24;
        int outAlpha = sourceAlpha + destinationAlpha * (255 - sourceAlpha) / 255;
        if (outAlpha == 0) {
            return 0;
        }
        int destinationWeight = destinationAlpha * (255 - sourceAlpha) / 255;
        int red = (((source >> 16) & 0xFF) * sourceAlpha + ((destination >> 16) & 0xFF) * destinationWeight) / outAlpha;
        int green = (((source >> 8) & 0xFF) * sourceAlpha + ((destination >> 8) & 0xFF) * destinationWeight) / outAlpha;
        int blue = ((source & 0xFF) * sourceAlpha + (destination & 0xFF) * destinationWeight) / outAlpha;
        return outAlpha << 24 | red << 16 | green << 8 | blue;
    }
}
