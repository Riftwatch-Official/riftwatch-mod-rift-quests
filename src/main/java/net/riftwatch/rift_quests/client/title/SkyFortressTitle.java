package net.riftwatch.rift_quests.client.title;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.sounds.SoundEvents;

final class SkyFortressTitle extends TitleStyle {
    private static final float R = 96;
    private static final float UP = (float) (-Math.PI / 2);

    private record Puff(float ox, float oy, float rad) {
    }

    private final List<Puff> bank = new ArrayList<>();

    SkyFortressTitle(TitleLayout layout) {
        super(layout);
        Random r = new Random(83);
        for (int i = 0; i < 26; i++) {
            bank.add(new Puff(r.nextFloat() * 420, (r.nextFloat() - 0.5f) * 150, 34 + r.nextFloat() * 46));
        }
    }

    @Override
    float duration() {
        return 6.4f;
    }

    @Override
    List<Cue> cues() {
        return List.of(new Cue(0.4f, SoundEvents.NOTE_BLOCK_HAT.value(), 0.6f, 0.4f), new Cue(1.7f, SoundEvents.BELL_BLOCK, 0.7f, 0.8f),
                new Cue(1.72f, SoundEvents.BELL_RESONATE, 0.8f, 0.6f));
    }

    @Override
    void draw(TitleCanvas c, float t) {
        float cy = l.cy();
        scrim(c, seg(t, 0, 0.5f) * (1 - seg(t, 5.4f, 6.4f)), 0x0E0804, 0.55f);

        float dialA = seg(t, 0, 0.3f) * (1 - 0.62f * seg(t, 1.7f, 2.2f)) * (1 - seg(t, 4.8f, 5.6f));
        if (dialA > 0) {
            float draw = sine(seg(t, 0.1f, 1.0f)) * (1 - sine(seg(t, 4.7f, 5.5f)));
            float spin = -0.7f * (1 - outCubic(seg(t, 0.1f, 1.2f))) + 0.4f * inCubic(seg(t, 4.7f, 5.6f));
            c.push();
            c.translate(CX, cy);
            c.rotate(spin);
            c.arc(0, 0, R, R, 3, UP, UP + TAU * draw, 0xD98A4F, dialA);
            c.arc(0, 0, R - 12, R - 12, 1.5f, UP, UP + TAU * draw, 0xD98A4F, dialA);
            for (int i = 0; i < 60; i++) {
                if (i / 60f > draw) {
                    break;
                }
                float a = UP + i * TAU / 60;
                boolean big = i % 5 == 0;
                float len = i % 15 == 0 ? 12 : big ? 7 : 3;
                c.line(cos(a) * (R - 2), sin(a) * (R - 2), cos(a) * (R - 2 - len), sin(a) * (R - 2 - len), big ? 2 : 1, 0xE8B48A, dialA);
            }
            for (int i = 0; i < 48; i++) {
                if (i / 48f > draw) {
                    break;
                }
                c.push();
                c.rotate(UP + i * TAU / 48 + t * 0.05f);
                c.rect(R + 3, -2, 5, 4, 0xB8703C, dialA);
                c.pop();
            }
            float sweep = TAU * inOutCubic(seg(t, 0.4f, 1.7f));
            float tick = t > 1.7f ? (float) Math.floor(t - 1.7f) + outCubic(seg((t - 1.7f) % 1, 0, 0.12f)) : 0;
            c.line(0, 0, sin(sweep) * (R - 20), -cos(sweep) * (R - 20), 3, 0xF4D7B8, dialA);
            float sa = tick * TAU / 60;
            c.line(0, 0, sin(sa) * (R - 14), -cos(sa) * (R - 14), 1, 0xFFD2A8, dialA);
            c.disc(0, 0, 5, 0xC9A14A, dialA);
            c.pop();
        }

        float strike = seg(t, 1.7f, 2.6f);
        if (strike > 0 && strike < 1) {
            float radius = R + outCubic(strike) * 260;
            c.arc(CX, cy, radius, radius, 2, 0, TAU, 0xFFD2A8, 0.6f * (1 - strike));
        }

        float k = sine(seg(t, 1.7f, 2.4f));
        float ko = sine(seg(t, 4.8f, 5.7f));
        if (k > 0 && ko < 1) {
            TitleLayout.Layer glow = l.layer("glow");
            TitleLayout.Layer full = l.layer("full");
            for (TitleLayout.Letter g : l.glyphs()) {
                float dx = (g.cx() - CX) * (0.1f * (1 - k) + 0.08f * ko);
                letter(c, glow, g, dx, 0, (0.25f + 0.75f * (1 - k)) * k * (1 - ko));
                letter(c, full, g, dx, 0, k * (1 - ko));
            }
        }

        float ruleW = (l.w() / 2 + 70) * sine(seg(t, 1.8f, 2.6f)) * (1 - sine(seg(t, 4.8f, 5.6f)));
        if (ruleW > 1) {
            float ry = Math.round(l.bottom() + 18);
            c.gradientH(CX - ruleW, ry, ruleW, 2, 0xD98A4F, 0, 0xE8B48A, 0.95f);
            c.gradientH(CX, ry, ruleW, 2, 0xE8B48A, 0.95f, 0xD98A4F, 0);
            c.push();
            c.translate(CX, ry + 1);
            c.rotate((float) (Math.PI / 4));
            c.rect(-4, -4, 8, 8, 0xE8B48A, 1);
            c.pop();
        }
        sub(c, seg(t, 2.1f, 2.7f) * (1 - seg(t, 4.6f, 5.1f)));

        float bankA = seg(t, 4.6f, 4.9f) * (1 - seg(t, 5.9f, 6.4f));
        if (bankA > 0) {
            float base = -260 + (t - 4.6f) * 560;
            for (Puff p : bank) {
                float bx = base + p.ox();
                float by = cy + p.oy();
                c.disc(bx, by + 6, p.rad(), 0xDFE7EE, 0.9f * bankA);
                c.disc(bx, by, p.rad(), 0xF5F8FB, 0.9f * bankA);
            }
        }
    }
}
