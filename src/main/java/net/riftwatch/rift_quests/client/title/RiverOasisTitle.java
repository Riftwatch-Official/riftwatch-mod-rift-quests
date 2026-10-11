package net.riftwatch.rift_quests.client.title;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.sounds.SoundEvents;

final class RiverOasisTitle extends TitleStyle {
    private static final float[] FRONDS = {-2.4f, -1.6f, -0.9f, -0.2f, 0.5f};

    private record Glint(float phase, float speed, int row, float length) {
    }

    private final List<Glint> glints = new ArrayList<>();
    private final float yw;
    private final float rh;

    RiverOasisTitle(TitleLayout layout) {
        super(layout);
        Random r = new Random(71);
        yw = Math.round(l.y() + 10);
        rh = Math.round(l.h() + 18);
        for (int i = 0; i < 18; i++) {
            glints.add(new Glint(r.nextFloat() * 1000, 18 + r.nextFloat() * 30, r.nextInt(6), 6 + r.nextFloat() * 14));
        }
    }

    private float rise(TitleLayout.Letter g) {
        return 0.2f + g.k() * 0.07f;
    }

    private float sink(TitleLayout.Letter g) {
        return 3.85f + g.u() * 0.5f;
    }

    private float dy(TitleLayout.Letter g, float t) {
        float ti = rise(g);
        float k = seg(t, ti, ti + 0.65f);
        if (k <= 0) {
            return Float.NaN;
        }
        float ko = inCubic(seg(t, sink(g), sink(g) + 0.6f));
        if (ko >= 1) {
            return Float.NaN;
        }
        return (1 - outCubic(k) + ko) * (l.h() + 20);
    }

    private void palm(TitleCanvas c, float cx, float base, int dir, float sway, float a) {
        if (a <= 0) {
            return;
        }
        for (int i = 0; i < 16; i++) {
            c.rect(Math.round(cx + dir * i * i * 0.12f), base - i * 8, 8, 8, i % 2 == 1 ? 0x4A3620 : 0x5B4426, a);
        }
        float crownX = cx + dir * 30.7f + 4;
        float crownY = base - 124;
        for (int j = 0; j < FRONDS.length; j++) {
            c.push();
            c.translate(crownX, crownY);
            c.scale(dir, 1);
            c.rotate(FRONDS[j] * 0.9f + sway * (j % 2 == 1 ? 1 : -1));
            for (int s = 0; s < 9; s++) {
                c.rect(s * 6, s * s * 0.35f, 7, 5, s % 2 == 1 ? 0x1F4A3A : 0x2A5E44, a);
            }
            c.pop();
        }
    }

    @Override
    float duration() {
        return 5.6f;
    }

    @Override
    List<Cue> cues() {
        return List.of(new Cue(0.2f, SoundEvents.PLAYER_SPLASH, 1.2f, 0.4f), new Cue(0.6f, SoundEvents.WATER_AMBIENT, 1.0f, 0.5f),
                new Cue(3.9f, SoundEvents.PLAYER_SPLASH, 1.0f, 0.35f));
    }

    @Override
    void draw(TitleCanvas c, float t) {
        float wa = seg(t, 0, 0.4f) * (1 - seg(t, 4.9f, 5.6f));
        scrim(c, wa, 0x04181A, 0.5f);

        if (wa > 0) {
            float x0 = l.x0() - 150;
            float x1 = l.x1() + 150;
            float span = x1 - x0;
            float edge = span * 0.2f;
            c.gradientH(x0, yw, edge, rh + 10, 0x28827E, 0, 0x28827E, 0.55f * wa);
            c.rect(x0 + edge, yw, span - edge * 2, rh + 10, 0x28827E, 0.55f * wa);
            c.gradientH(x1 - edge, yw, edge, rh + 10, 0x28827E, 0.55f * wa, 0x28827E, 0);
            c.gradientH(x0, yw, span / 2, 1, 0xC8FAF0, 0, 0xC8FAF0, 0.8f * wa);
            c.gradientH(x0 + span / 2, yw, span / 2, 1, 0xC8FAF0, 0.8f * wa, 0xC8FAF0, 0);
            for (Glint gl : glints) {
                float gx = x0 + 60 + ((gl.phase() + t * gl.speed()) % (span - 120));
                c.rect(Math.round(gx), yw + 5 + gl.row() * 7, Math.round(gl.length()), 1, 0xDCFFF8, 0.5f * wa);
            }
        }

        TitleLayout.Layer full = l.layer("full");
        c.clip(c.left(), l.top() - 20, c.right(), yw);
        for (TitleLayout.Letter g : l.glyphs()) {
            float dy = dy(g, t);
            if (!Float.isNaN(dy)) {
                letter(c, full, g, 0, dy, 1);
            }
        }
        c.unclip();

        if (wa > 0) {
            float pad = l.size() * 0.35f;
            for (TitleLayout.Letter g : l.glyphs()) {
                float dy = dy(g, t);
                if (Float.isNaN(dy)) {
                    continue;
                }
                for (float d = 0; d < rh; d += 2) {
                    float sy = yw - d - 2;
                    float source = sy - dy;
                    if (source + 2 <= l.top() - pad || source >= l.bottom() + pad || sy < l.top() - 20) {
                        continue;
                    }
                    float ripple = sin(d * 0.35f + t * 6) * (1.2f + d * 0.06f);
                    c.image(full, g.s0(), source, g.s1(), source + 2, g.s0() + ripple, yw + d + 2, g.s1() + ripple, yw + d, WHITE,
                            0.38f * (1 - d / rh) * wa);
                }
            }
        }

        for (TitleLayout.Letter g : l.glyphs()) {
            for (float ti : new float[] {rise(g) + 0.05f, sink(g) + 0.35f}) {
                float k = seg(t, ti, ti + 0.9f);
                if (k <= 0 || k >= 1) {
                    continue;
                }
                float rx = 6 + (t - ti) * 70;
                c.arc(g.cx(), yw + 1, rx, rx * 0.22f, 1.5f, 0, TAU, 0xD8FBF4, 0.6f * (1 - k));
            }
        }

        float pin = outCubic(seg(t, 0.5f, 1.2f));
        float pout = inCubic(seg(t, 4.2f, 4.9f));
        float off = 180 * (1 - pin + pout);
        float sway = sin(t * 1.5f) * 0.06f;
        palm(c, l.x0() - 120 - off, yw + 18, 1, sway, pin * (1 - pout));
        palm(c, l.x1() + 112 + off, yw + 18, -1, -sway, pin * (1 - pout));

        sub(c, seg(t, 1.4f, 1.9f) * (1 - seg(t, 3.8f, 4.2f)));
    }
}
