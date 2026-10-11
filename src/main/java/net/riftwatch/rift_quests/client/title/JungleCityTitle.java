package net.riftwatch.rift_quests.client.title;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.sounds.SoundEvents;

final class JungleCityTitle extends TitleStyle {
    private static final String[] LEAF = {"..aa", ".aab", "aab.", "ab.."};
    private static final Map<Character, Integer> LEAF_COLORS = Map.of('a', 0x7CC35A, 'b', 0x2F6A2C);

    private record Leaf(boolean upper, float x, int side, boolean flower, float fall, float phase, float drift) {
    }

    private record Drift(float start, float x, float phase) {
    }

    private final List<Leaf> leaves = new ArrayList<>();
    private final List<Drift> drifts = new ArrayList<>();
    private final float xa;
    private final float xb;

    JungleCityTitle(TitleLayout layout) {
        super(layout);
        Random r = new Random(41);
        xa = l.x0() - 70;
        xb = l.x1() + 70;
        int side = 1;
        for (float x = xa + 14; x < xb - 6; x += 26) {
            leaves.add(new Leaf(false, x, side, false, 3.8f + r.nextFloat() * 0.75f, r.nextFloat() * 6, (r.nextFloat() - 0.5f) * 30));
            side = -side;
        }
        side = -1;
        int n = 0;
        for (float x = xb - 20; x > xa + 6; x -= 30) {
            leaves.add(new Leaf(true, x, side, n % 3 == 1, 3.8f + r.nextFloat() * 0.75f, r.nextFloat() * 6, (r.nextFloat() - 0.5f) * 30));
            side = -side;
            n++;
        }
        for (int j = 0; j < 12; j++) {
            drifts.add(new Drift(1.1f + j * 0.22f, 60 + r.nextFloat() * (W - 120), r.nextFloat() * 6));
        }
    }

    private float lowY(float x) {
        return l.bottom() + 14 + sin((x - xa) * 0.035f) * 6;
    }

    private float upY(float x) {
        return l.top() - 18 + sin((x - xa) * 0.03f + 2) * 5;
    }

    private void leaf(TitleCanvas c, float x, float y, float s, int side, float flip, float a) {
        if (s <= 0 || a <= 0) {
            return;
        }
        c.push();
        c.translate(x, y);
        c.scale(s * flip, s);
        sprite(c, LEAF, LEAF_COLORS, 0, side * 7, 3, a, side < 0);
        c.pop();
    }

    @Override
    float duration() {
        return 5.6f;
    }

    @Override
    List<Cue> cues() {
        return List.of(new Cue(0.1f, SoundEvents.CAVE_VINES_PLACE, 0.9f, 0.7f), new Cue(0.6f, SoundEvents.AZALEA_LEAVES_PLACE, 1.0f, 0.7f),
                new Cue(3.9f, SoundEvents.AZALEA_LEAVES_BREAK, 0.9f, 0.4f));
    }

    @Override
    void draw(TitleCanvas c, float t) {
        scrim(c, seg(t, 0, 0.4f) * (1 - seg(t, 4.8f, 5.6f)), 0x061408, 0.55f);
        float tipL = lerp(xa, xb, inOutCubic(seg(t, 0.1f, 1.25f)));
        float tipU = lerp(xb, xa, inOutCubic(seg(t, 0.3f, 1.35f)));
        float va = 1 - seg(t, 4.3f, 5.0f);
        if (va > 0) {
            for (float x = xa; x < tipL; x += 2) {
                c.rect(Math.round(x), Math.round(lowY(x)), 3, 3, 0x3D7A35, va);
            }
            for (float x = xb; x > tipU; x -= 2) {
                c.rect(Math.round(x), Math.round(upY(x)), 2, 2, 0x356C2F, va);
            }
        }

        for (Leaf lf : leaves) {
            float y0 = lf.upper() ? upY(lf.x()) : lowY(lf.x());
            float grown = lf.upper() ? seg(lf.x() - tipU, 0, 30) : seg(tipL - lf.x(), 0, 30);
            float s = outCubic(grown);
            float dt = t - lf.fall();
            if (dt <= 0) {
                leaf(c, lf.x(), y0, s, lf.side(), 1, 1);
                if (lf.flower() && grown > 0.6f) {
                    c.px(lf.x(), y0 - lf.side() * 4, 4, 0xE86B9A, 1);
                    c.px(lf.x(), y0 - lf.side() * 4, 2, 0xF6D24A, 1);
                }
            } else {
                float a = 1 - seg(dt, 0.6f, 1.2f);
                leaf(c, lf.x() + sin(dt * 5 + lf.phase()) * 14 + lf.drift() * dt, y0 + 70 * dt + 60 * dt * dt, 1, lf.side(), cos(dt * 6 + lf.phase()), a);
            }
        }

        TitleLayout.Layer full = l.layer("full");
        for (TitleLayout.Letter g : l.glyphs()) {
            float k = seg(tipL, g.x() - 10, g.x() + g.w());
            if (k <= 0) {
                continue;
            }
            float to = 3.95f + (1 - g.u()) * 0.5f;
            float ko = seg(t, to, to + 0.5f);
            float kk = outCubic(k);
            letter(c, full, g, 0, 10 * (1 - kk) + 14 * ko, 0.9f + 0.1f * kk, 0.9f + 0.1f * kk, 0, k * (1 - ko), l.bottom());
        }

        float env = seg(t, 1.2f, 1.6f) * (1 - seg(t, 3.6f, 4.0f));
        if (env > 0) {
            for (Drift d : drifts) {
                float dt = t - d.start();
                if (dt > 0) {
                    leaf(c, d.x() + sin(dt * 2 + d.phase()) * 30, -16 + dt * 80, 0.9f, 1, cos(dt * 4 + d.phase()), env * 0.85f);
                }
            }
        }

        float ea = seg(t, 1.1f, 1.3f) * (1 - seg(t, 4.0f, 4.4f));
        float es = 0.7f + 0.3f * sine(seg(t, 1.1f, 1.7f));
        c.push();
        c.translate(CX, l.top() - 50);
        c.scale(es, es);
        emblem(c, "leaf", 0, 0, 6, 0x9AD46E, ea, 0x10260E, 1);
        c.pop();
        sub(c, seg(t, 1.35f, 1.85f) * (1 - seg(t, 3.8f, 4.2f)));
    }
}
