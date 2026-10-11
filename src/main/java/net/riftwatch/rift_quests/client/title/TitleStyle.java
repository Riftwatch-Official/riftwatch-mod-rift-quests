package net.riftwatch.rift_quests.client.title;

import java.util.List;
import java.util.Map;
import net.minecraft.sounds.SoundEvent;

abstract class TitleStyle {
    static final float W = TitleCanvas.W;
    static final float H = TitleCanvas.H;
    static final float CX = W / 2f;
    static final int WHITE = TitleCanvas.WHITE;
    static final float TAU = (float) (Math.PI * 2);
    private static final Map<String, String[]> EMBLEMS = Map.of(
            "tower", new String[] {"#.#.#", "#####", ".###.", ".###.", ".###."},
            "gate", new String[] {".###.", "#####", "##.##", "##.##", "##.##"},
            "flake", new String[] {"..#..", "#.#.#", ".###.", "#.#.#", "..#.."},
            "leaf", new String[] {"..##.", ".###.", "####.", ".##..", "#...."},
            "step", new String[] {"..#..", ".###.", ".###.", "#####", "#####"},
            "pagoda", new String[] {"..#..", ".###.", "#####", ".#.#.", ".#.#."},
            "dome", new String[] {".###.", "#####", "#.#.#", "#.#.#", "#.#.#"});

    record Cue(float time, SoundEvent sound, float pitch, float volume) {
    }

    protected final TitleLayout l;

    TitleStyle(TitleLayout layout) {
        l = layout;
    }

    abstract float duration();

    abstract void draw(TitleCanvas c, float t);

    List<Cue> cues() {
        return List.of();
    }

    static float clamp(float x) {
        return x < 0 ? 0 : x > 1 ? 1 : x;
    }

    static float lerp(float from, float to, float k) {
        return from + (to - from) * k;
    }

    static float seg(float t, float from, float to) {
        return clamp((t - from) / (to - from));
    }

    static float bump(float t, float from, float to) {
        return (float) Math.sin(Math.PI * seg(t, from, to));
    }

    static float inQuad(float x) {
        return x * x;
    }

    static float inCubic(float x) {
        return x * x * x;
    }

    static float outCubic(float x) {
        float y = 1 - x;
        return 1 - y * y * y;
    }

    static float inOutCubic(float x) {
        return x < 0.5f ? 4 * x * x * x : 1 - (float) Math.pow(-2 * x + 2, 3) / 2;
    }

    static float sine(float x) {
        return 0.5f - (float) Math.cos(Math.PI * x) / 2;
    }

    static float sin(float x) {
        return (float) Math.sin(x);
    }

    static float cos(float x) {
        return (float) Math.cos(x);
    }

    protected void letter(TitleCanvas c, TitleLayout.Layer layer, TitleLayout.Letter g, float dx, float dy, float sx, float sy, float rot, float alpha, float anchorY) {
        if (alpha <= 0) {
            return;
        }
        float pad = l.size() * 0.35f;
        float x0 = g.s0();
        float x1 = g.s1();
        float y0 = l.top() - pad;
        float y1 = l.bottom() + pad;
        c.push();
        c.translate(g.cx() + dx, anchorY + dy);
        if (rot != 0) {
            c.rotate(rot);
        }
        c.scale(sx, sy);
        c.image(layer, x0, y0, x1, y1, x0 - g.cx(), y0 - anchorY, x1 - g.cx(), y1 - anchorY, WHITE, alpha);
        c.pop();
    }

    protected void letter(TitleCanvas c, TitleLayout.Layer layer, TitleLayout.Letter g, float dx, float dy, float alpha) {
        letter(c, layer, g, dx, dy, 1, 1, 0, alpha, l.cy());
    }

    protected void slice(TitleCanvas c, TitleLayout.Layer layer, float x0, float x1, float y0, float y1, float dx, float dy, float alpha) {
        c.image(layer, x0, y0, x1, y1, x0 + dx, y0 + dy, x1 + dx, y1 + dy, WHITE, alpha);
    }

    protected void name(TitleCanvas c, TitleLayout.Layer layer, float alpha) {
        slice(c, layer, l.x0() - 30, l.x1() + 30, l.top() - 30, l.bottom() + 30, 0, 0, alpha);
    }

    protected void sheen(TitleCanvas c, float k, int rgb, float peak, float width) {
        if (k <= 0 || k >= 1) {
            return;
        }
        TitleLayout.Layer mask = l.layer("mask");
        float centre = lerp(l.x0() - width * 3, l.x1() + width * 3, k);
        c.additive(true);
        for (float x = centre - width; x < centre + width; x += 2) {
            float distance = Math.abs(x + 1 - centre) / width;
            float a = peak * (1 - distance) * (1 - distance);
            c.image(mask, x, l.top() - 12, x + 2, l.bottom() + 12, x, l.top() - 12, x + 2, l.bottom() + 12, rgb, a);
        }
        c.additive(false);
    }

    protected void scrim(TitleCanvas c, float a, int rgb, float strength) {
        if (a <= 0) {
            return;
        }
        float ry = l.h() * 1.45f + 40;
        float rx = Math.max(l.w() * 0.68f, 300);
        c.ellipse(CX, l.cy() + 12, rx, ry, rgb, strength * a, rgb, 0);
    }

    protected void sub(TitleCanvas c, float a) {
        TitleLayout.Layer sub = l.sub();
        c.image(sub, sub.x0(), sub.y0(), sub.x1(), sub.y1(), sub.x0(), sub.y0(), sub.x1(), sub.y1(), WHITE, a);
    }

    protected void emblem(TitleCanvas c, String name, float cx, float cy, float cell, int rgb, float a, int shadowRgb, float shadowA) {
        if (a <= 0) {
            return;
        }
        String[] rows = EMBLEMS.get(name);
        if (shadowA > 0) {
            grid(c, rows, cx + cell * 0.4f, cy + cell * 0.4f, cell, shadowRgb, a * shadowA);
        }
        grid(c, rows, cx, cy, cell, rgb, a);
    }

    private void grid(TitleCanvas c, String[] rows, float cx, float cy, float cell, int rgb, float a) {
        for (int r = 0; r < 5; r++) {
            for (int col = 0; col < 5; col++) {
                if (rows[r].charAt(col) == '#') {
                    c.rect(Math.round(cx + (col - 2.5f) * cell), Math.round(cy + (r - 2.5f) * cell), cell, cell, rgb, a);
                }
            }
        }
    }

    protected void sprite(TitleCanvas c, String[] rows, Map<Character, Integer> colors, float x, float y, float cell, float a, boolean flip) {
        if (a <= 0) {
            return;
        }
        int height = rows.length;
        int width = rows[0].length();
        for (int r = 0; r < height; r++) {
            for (int col = 0; col < width; col++) {
                char key = rows[r].charAt(flip ? width - 1 - col : col);
                Integer rgb = colors.get(key);
                if (rgb != null) {
                    c.rect(x + (col - width / 2f) * cell, y + (r - height / 2f) * cell, cell, cell, rgb, a);
                }
            }
        }
    }

    protected float steps(TitleCanvas c, float cx, float baseY, float width, float stepW, float stepH, int leftRgb, int rightRgb, float a) {
        float w = width;
        float y = baseY;
        while (w > 0) {
            c.rect(cx - w / 2, y - stepH, w / 2, stepH, leftRgb, a);
            c.rect(cx, y - stepH, w / 2, stepH, rightRgb, a);
            y -= stepH;
            w -= stepW * 2;
        }
        return y;
    }
}
