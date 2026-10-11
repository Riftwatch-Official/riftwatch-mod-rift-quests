package net.riftwatch.rift_quests.client.title;

import java.util.List;
import java.util.Map;
import net.minecraft.sounds.SoundEvents;

final class RiverPalaceTitle extends TitleStyle {
    private static final String[] LANTERN = {".aaa.", "abbba", "abbba", "abbba", ".aaa.", "..a.."};
    private static final Map<Character, Integer> LANTERN_COLORS = Map.of('a', 0x5A4220, 'b', 0x3A2810);
    private static final String[] FLAME = {".....", ".bbb.", ".bcb.", ".bbb.", ".....", "....."};
    private static final Map<Character, Integer> FLAME_COLORS = Map.of('b', 0xFFB347, 'c', 0xFFF4D0);

    private final float gx0;
    private final float gx1;
    private final float gy;
    private final int count;

    RiverPalaceTitle(TitleLayout layout) {
        super(layout);
        gx0 = l.x0() - 40;
        gx1 = l.x1() + 40;
        gy = l.top() - 46;
        count = l.glyphs().size();
    }

    private float ropeY(float x) {
        float mid = (gx0 + gx1) / 2;
        float half = (gx1 - gx0) / 2;
        float k = (x - mid) / half;
        return gy + 14 * (1 - k * k);
    }

    private float on(TitleLayout.Letter g) {
        return 0.6f + g.k() * 0.12f;
    }

    private float off(TitleLayout.Letter g) {
        return 4.45f + (count - 1 - g.k()) * 0.1f;
    }

    @Override
    float duration() {
        return 6.2f;
    }

    @Override
    List<Cue> cues() {
        return List.of(new Cue(0.6f, SoundEvents.FLINTANDSTEEL_USE, 1.0f, 0.6f), new Cue(4.45f, SoundEvents.CANDLE_EXTINGUISH, 0.9f, 0.7f));
    }

    @Override
    void draw(TitleCanvas c, float t) {
        float dim = seg(t, 0, 0.6f) * (1 - seg(t, 5.6f, 6.2f));
        c.rect(c.left(), 0, c.right() - c.left(), c.bottom(), 0x060610, 0.35f * dim);
        scrim(c, dim, 0x080402, 0.5f);

        float ga = 1 - seg(t, 5.6f, 6.2f);
        float xe = lerp(gx0, gx1, sine(seg(t, 0.05f, 0.6f)));
        for (float x = gx0; x < xe; x += 2) {
            c.rect(x, Math.round(ropeY(x)), 2, 1, 0x6A5030, ga);
        }

        TitleLayout.Layer glow = l.layer("glow");
        TitleLayout.Layer full = l.layer("full");
        for (TitleLayout.Letter g : l.glyphs()) {
            if (g.cx() > xe) {
                continue;
            }
            float ti = on(g);
            float to = off(g);
            float lit = seg(t, ti, ti + 0.15f) * (1 - seg(t, to, to + 0.12f));
            float flick = 0.85f + 0.15f * sin(t * 13 + g.k() * 1.7f) * sin(t * 7.3f + g.k());
            float lx = g.cx() + sin(t * 1.4f + g.k()) * 1.2f;
            float ly = ropeY(g.cx()) + 14;
            c.rect(Math.round(g.cx()), Math.round(ropeY(g.cx())), 1, 8, 0x6A5030, ga);
            sprite(c, LANTERN, LANTERN_COLORS, lx, ly, 2, ga, false);
            if (lit > 0) {
                sprite(c, FLAME, FLAME_COLORS, lx, ly, 2, lit, false);
                float flash = bump(t, ti, ti + 0.5f);
                float rad = 26 + 10 * flash;
                c.additive(true);
                c.ellipse(lx, ly, rad, rad, 0xFFAA46, 0.45f * lit * flick, 0xFFAA46, 0);
                c.additive(false);
            }
            float la = seg(t, ti + 0.05f, ti + 0.5f) * (1 - seg(t, to, to + 0.4f));
            if (la > 0) {
                letter(c, glow, g, 0, 0, la * 0.55f * flick);
                letter(c, full, g, 0, 0, la);
            }
            float hold = seg(t, ti + 0.3f, ti + 0.6f) * (1 - seg(t, to - 0.1f, to));
            if (hold > 0) {
                for (int j = 0; j < 3; j++) {
                    float ph = (t * 0.6f + g.k() * 0.37f + j / 3f) % 1;
                    c.px(lx + sin(ph * 6 + g.k()) * 4, ly - 6 - ph * 44, 2, 0xFFCF7A, (1 - ph) * 0.8f * hold);
                }
            }
            float dt = t - to;
            if (dt > 0 && dt < 0.9f) {
                for (int j = 0; j < 3; j++) {
                    float d = dt - j * 0.08f;
                    if (d > 0) {
                        c.px(lx + sin(d * 7 + j) * 3, ly - 4 - d * 46, 2 + Math.round(d * 4), 0xB8B0A8, 0.45f * (1 - d / 0.9f));
                    }
                }
            }
        }

        float ea = seg(t, 1.9f, 2.3f) * (1 - seg(t, 5.2f, 5.7f));
        emblem(c, "dome", CX, gy - 28, 5, 0xF2B860, ea, 0x1A0E06, 1);
        sub(c, seg(t, 2.0f, 2.5f) * (1 - seg(t, 4.4f, 4.9f)));
    }
}
