package net.riftwatch.rift_quests.client.title;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.sounds.SoundEvents;

final class RiftMonarchTitle extends TitleStyle {
    private static final int VIOLET = 0xB88EFF;
    private static final int VIOLET_DEEP = 0x6C4BB4;
    private static final int PALE = 0xE9DCFF;
    private static final int GOLD = 0xE8B84A;
    private static final int GEM_GOLD = 0xFFD27A;
    private static final int GEM_VIOLET = 0xCDB2FF;
    private static final int[] SHARD_COLOURS = {0xB88EFF, 0xF070DC, 0xE8B84A, 0xE9DCFF};
    private static final float[][] SPIRES = {{-96, 30}, {-64, 44}, {-32, 58}, {0, 74}, {32, 58}, {64, 44}, {96, 30}};

    private record Shard(float ox, float oy, float dx, float dy, float dist, float orbit, float rx, float ry, float speed, float size, float spin,
                         int colour, float fall) {
    }

    private record Flight(float sx, float sy, float rot, float fall, float drift, float spin) {
    }

    private final float[] crackX;
    private final float[] crackY;
    private final List<Shard> shards = new ArrayList<>();
    private final List<Flight> flights = new ArrayList<>();
    private final float crownBase;

    RiftMonarchTitle(TitleLayout layout) {
        super(layout);
        Random r = new Random(7);
        int count = 31;
        crackX = new float[count];
        crackY = new float[count];
        for (int i = 0; i < count; i++) {
            float y = 30 + 16 * i;
            crackY[i] = y;
            crackX[i] = CX + (r.nextFloat() - 0.5f) * (y > 60 && y < 480 ? 30 : 8);
        }
        for (int i = 0; i < 46; i++) {
            float a = r.nextFloat() * TAU;
            shards.add(new Shard(CX + (r.nextFloat() - 0.5f) * 20, 120 + r.nextFloat() * 320, cos(a), sin(a) * 0.7f, 160 + r.nextFloat() * 280,
                    r.nextFloat() * TAU, 250 + r.nextFloat() * 140, 80 + r.nextFloat() * 70, 0.12f + r.nextFloat() * 0.18f, 3 + r.nextFloat() * 6,
                    (r.nextFloat() - 0.5f) * 6, SHARD_COLOURS[i % 4], 0.6f + r.nextFloat() * 0.8f));
        }
        Random letters = new Random(21);
        for (int i = 0; i < layout.glyphs().size(); i++) {
            flights.add(new Flight(CX + (letters.nextFloat() - 0.5f) * 70, 120 + letters.nextFloat() * 320, (letters.nextFloat() - 0.5f) * 5,
                    80 + letters.nextFloat() * 140, (letters.nextFloat() - 0.5f) * 120, (letters.nextFloat() - 0.5f) * 3));
        }
        crownBase = layout.top() - 34;
    }

    @Override
    float duration() {
        return 7.2f;
    }

    @Override
    List<Cue> cues() {
        return List.of(new Cue(0.8f, SoundEvents.END_PORTAL_SPAWN, 0.7f, 0.45f), new Cue(0.82f, SoundEvents.ENDER_DRAGON_GROWL, 0.6f, 0.35f),
                new Cue(2.4f, SoundEvents.BELL_BLOCK, 0.5f, 0.9f), new Cue(5.6f, SoundEvents.AMETHYST_BLOCK_BREAK, 0.6f, 1.0f));
    }

    @Override
    void draw(TitleCanvas c, float t) {
        float scrim = seg(t, 0, 0.6f) * (1 - seg(t, 6.4f, 7.2f));
        c.rect(0, 0, W, H, 0x050309, 0.32f * scrim);
        rift(c, t);
        float flash = t > 0.8f ? (float) Math.exp(-(t - 0.8f) * 7) : 0;
        float strike = t > 2.4f ? (float) Math.exp(-(t - 2.4f) * 6) : 0;
        if (flash + strike > 0.01f) {
            c.rect(0, 0, W, H, PALE, 0.3f * flash + 0.18f * strike);
        }
        shards(c, t);
        crown(c, t);
        name(c, t);
        float ring = seg(t, 2.4f, 3.3f);
        if (ring > 0 && ring < 1) {
            float o = outCubic(ring);
            c.arc(CX, H / 2, 80 + 560 * o, 30 + 200 * o, 3 * (1 - ring) + 1, 0, TAU, GEM_VIOLET, 0.7f * (1 - ring));
        }
        float rule = sine(seg(t, 2.5f, 3.2f)) * (1 - sine(seg(t, 5.3f, 5.9f)));
        if (rule > 0.01f) {
            float ry = Math.round(l.bottom() + 20);
            float half = 330 * rule;
            c.gradientH(CX - half, ry, half, 2, VIOLET, 0, GOLD, 0.95f);
            c.gradientH(CX, ry, half, 2, GOLD, 0.95f, VIOLET, 0);
        }
        float sub = seg(t, 2.6f, 3.2f) * (1 - seg(t, 5.2f, 5.7f));
        if (sub > 0) {
            TitleLayout.Layer layer = l.sub();
            float half = (layer.x1() - layer.x0()) / 2 + 120;
            float y0 = layer.y0() - 10;
            float h = layer.y1() - layer.y0() + 20;
            c.gradientH(CX - half, y0, half, h, 0x06040A, 0, 0x06040A, 0.86f * sub);
            c.gradientH(CX, y0, half, h, 0x06040A, 0.86f * sub, 0x06040A, 0);
            sub(c, sub);
        }
    }

    private float pf(float y) {
        return sin((float) Math.PI * clamp((y - 30) / 480));
    }

    private void rift(TitleCanvas c, float t) {
        float drawn = sine(seg(t, 0.15f, 0.8f));
        if (drawn <= 0 || t >= 7.0f) {
            return;
        }
        float open = outCubic(seg(t, 0.8f, 1.4f)) * (1 - inCubic(seg(t, 5.8f, 6.9f)));
        float breathe = 1 + 0.08f * sin(t * 3) * seg(t, 3.2f, 3.6f);
        if (open > 0.01f) {
            c.additive(true);
            c.ellipse(CX, H / 2, 260, 260, VIOLET, 0.35f * open, VIOLET, 0);
            c.additive(false);
            float width = 48 * open * breathe;
            int edge = c.argb(VIOLET_DEEP, 0.85f);
            int core = c.argb(0xFAF4FF, 0.95f);
            for (int i = 0; i < crackX.length - 1; i++) {
                float w0 = width * pf(crackY[i]) / 2;
                float w1 = width * pf(crackY[i + 1]) / 2;
                float x0 = crackX[i];
                float x1 = crackX[i + 1];
                float y0 = crackY[i];
                float y1 = crackY[i + 1];
                c.quad(x0 - w0, y0, x0, y0, x1, y1, x1 - w1, y1, edge, core, core, edge);
                c.quad(x0, y0, x0 + w0, y0, x1 + w1, y1, x1, y1, core, edge, edge, core);
            }
        }
        int last = Math.max(1, (int) Math.floor(drawn * (crackX.length - 1)));
        c.additive(true);
        for (int i = 0; i < last; i++) {
            c.line(crackX[i], crackY[i], crackX[i + 1], crackY[i + 1], 7, VIOLET, 0.3f);
        }
        c.additive(false);
        for (int i = 0; i < last; i++) {
            c.line(crackX[i], crackY[i], crackX[i + 1], crackY[i + 1], 2, GEM_VIOLET, 1);
        }
    }

    private void diamond(TitleCanvas c, float x, float y, float s, float angle, int rgb, float a) {
        float ca = cos(angle);
        float sa = sin(angle);
        float tx = -sa * s * 1.6f;
        float ty = ca * s * 1.6f;
        float wx = ca * s * 0.7f;
        float wy = sa * s * 0.7f;
        c.quad(x - tx, y - ty, x + wx, y + wy, x + tx, y + ty, x - wx, y - wy, rgb, a);
    }

    private void shards(TitleCanvas c, float t) {
        if (t <= 0.85f) {
            return;
        }
        float burst = outCubic(seg(t, 0.85f, 1.9f));
        float gather = sine(seg(t, 1.9f, 2.8f));
        float drop = seg(t, 5.6f, 6.9f);
        for (Shard s : shards) {
            float bx = s.ox() + s.dx() * s.dist() * burst;
            float by = s.oy() + s.dy() * s.dist() * burst;
            float a = s.orbit() + t * s.speed();
            float x = lerp(bx, CX + cos(a) * s.rx(), gather);
            float y = lerp(by, 262 + sin(a) * s.ry(), gather) + 420 * inQuad(drop) * s.fall();
            float alpha = Math.min(1, burst * 2) * (1 - drop) * 0.9f;
            if (alpha > 0) {
                diamond(c, x, y, s.size(), s.spin() * t, s.colour(), alpha);
            }
        }
    }

    private void crown(TitleCanvas c, float t) {
        float out = 1 - seg(t, 5.5f, 6.3f);
        float band = sine(seg(t, 1.2f, 1.6f)) * out;
        if (band > 0) {
            float half = 110 * band;
            float px = CX - half;
            float py = crownBase;
            for (int i = 1; i <= 16; i++) {
                float u = i / 16f;
                float x = CX - half + 2 * half * u;
                float y = crownBase + 12 * 4 * u * (1 - u) * 0.5f;
                c.line(px, py, x, y, 3, GOLD, band);
                px = x;
                py = y;
            }
        }
        for (int i = 0; i < SPIRES.length; i++) {
            float x0 = seg(t, 1.3f + 0.08f * Math.abs(i - 3), 1.7f + 0.08f * Math.abs(i - 3));
            if (x0 <= 0) {
                continue;
            }
            float k = (1 + 2.4f * (float) Math.pow(x0 - 1, 3) + 1.4f * (float) Math.pow(x0 - 1, 2)) * out;
            if (k <= 0) {
                continue;
            }
            float bx = CX + SPIRES[i][0];
            float by = crownBase + SPIRES[i][0] * SPIRES[i][0] / 12000f * 12;
            float top = by - SPIRES[i][1] * k;
            c.line(bx, by, bx, top, 3, GOLD, Math.min(1, k));
            float gem = seg(t, 1.8f + 0.06f * i, 2.0f + 0.06f * i);
            if (gem > 0) {
                int colour = i % 2 == 1 ? GEM_GOLD : GEM_VIOLET;
                c.additive(true);
                c.disc(bx, top - 6, 9, colour, 0.35f * gem * out);
                c.additive(false);
                diamond(c, bx, top - 6, 4.5f, 0, colour, gem * out);
            }
        }
    }

    private void name(TitleCanvas c, float t) {
        if (t < 1.6f) {
            return;
        }
        TitleLayout.Layer full = l.layer("full");
        TitleLayout.Layer glow = l.layer("glow");
        List<TitleLayout.Letter> glyphs = l.glyphs();
        if (t < 2.4f) {
            for (int i = 0; i < glyphs.size(); i++) {
                TitleLayout.Letter g = glyphs.get(i);
                Flight f = flights.get(i);
                float k = outCubic(seg(t, 1.6f + 0.05f * i, 2.25f + 0.05f * i));
                letter(c, full, g, lerp(f.sx() - g.cx(), 0, k), lerp(f.sy() - l.cy(), 0, k), 1, 1, lerp(f.rot(), 0, k), k, l.cy());
            }
            return;
        }
        if (t < 5.6f) {
            float glowK = (0.5f + 0.5f * sin((t - 2.4f) * 2.2f)) * (1 - seg(t, 5.2f, 5.6f));
            c.additive(true);
            for (TitleLayout.Letter g : glyphs) {
                letter(c, glow, g, 0, 0, 0.3f + 0.4f * glowK);
            }
            c.additive(false);
            for (TitleLayout.Letter g : glyphs) {
                letter(c, full, g, 0, 0, 1);
            }
            sheen(c, seg(t, 2.4f, 3.0f), 0xFFF3D0, 0.9f, 26);
            return;
        }
        for (int i = 0; i < glyphs.size(); i++) {
            TitleLayout.Letter g = glyphs.get(i);
            Flight f = flights.get(i);
            float k = seg(t, 5.6f + 0.04f * i, 6.5f + 0.04f * i);
            letter(c, full, g, f.drift() * k, f.fall() * inQuad(k) * 2, 1, 1, f.spin() * k, 1 - k, l.cy());
        }
    }
}
