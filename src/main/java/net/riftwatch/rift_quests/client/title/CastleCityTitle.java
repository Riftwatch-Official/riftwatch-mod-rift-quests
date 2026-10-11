package net.riftwatch.rift_quests.client.title;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import net.minecraft.sounds.SoundEvents;

final class CastleCityTitle extends TitleStyle {
    private record Mote(float x, float y, float speed, float phase, float size) {
    }

    private final List<Mote> motes;
    private final float ruleY;

    CastleCityTitle(TitleLayout layout) {
        super(layout);
        Random r = new Random(11);
        motes = IntStream.range(0, 46)
                .mapToObj(i -> new Mote(CX - 260 + r.nextFloat() * 520, r.nextFloat() * H, 6 + r.nextFloat() * 12, r.nextFloat() * 6, r.nextFloat() < 0.7f ? 2 : 3))
                .toList();
        ruleY = Math.round(l.bottom() + 22);
    }

    @Override
    float duration() {
        return 6.6f;
    }

    @Override
    List<Cue> cues() {
        return List.of(new Cue(0.3f, SoundEvents.BELL_RESONATE, 0.6f, 0.6f), new Cue(0.35f, SoundEvents.BEACON_ACTIVATE, 0.5f, 0.35f));
    }

    @Override
    void draw(TitleCanvas c, float t) {
        float env = sine(seg(t, 0, 1.2f)) * (1 - sine(seg(t, 5.6f, 6.6f)));
        c.rect(c.left(), 0, c.right() - c.left(), c.bottom(), 0x04060A, 0.28f * env);
        if (env > 0) {
            float peak = 0.35f * 0.55f * env;
            float my = H * 0.45f;
            float topL = CX - 140;
            float topR = CX + 60;
            float midL = lerp(CX - 140, CX - 20, 0.45f);
            float midR = lerp(CX + 60, CX + 300, 0.45f);
            int clear = c.argb(0xFFF4D6, 0);
            int lit = c.argb(0xFFF4D6, peak);
            c.quad(topL, 0, topR, 0, midR, my, midL, my, clear, clear, lit, lit);
            c.quad(midL, my, midR, my, CX + 300, H, CX - 20, H, lit, lit, clear, clear);
        }
        scrim(c, env, 0x080A0E, 0.55f);

        for (Mote m : motes) {
            float y = ((m.y() - m.speed() * t) % H + H) % H;
            c.px(m.x() + sin(t * 0.5f + m.phase()) * 14, y, m.size(), 0xFFF1D0, env * (0.25f + 0.25f * sin(t * 1.3f + m.phase())));
        }

        float open = inOutCubic(seg(t, 0.3f, 1.8f)) * (1 - inOutCubic(seg(t, 5.0f, 6.0f)));
        float half = (l.w() / 2 + 80) * open;
        float gap = 26;
        if (half > gap) {
            for (int side = -1; side <= 1; side += 2) {
                float x0 = CX + side * gap;
                float x1 = CX + side * half;
                if (side < 0) {
                    c.gradientH(x1, ruleY, x0 - x1, 1, 0xD6DBE0, 0.15f, 0xD6DBE0, 0.95f);
                    c.rect(x1, ruleY + 4, x0 - x1, 1, 0xD6DBE0, 0.35f);
                } else {
                    c.gradientH(x0, ruleY, x1 - x0, 1, 0xD6DBE0, 0.95f, 0xD6DBE0, 0.15f);
                    c.rect(x0, ruleY + 4, (x1 - x0) * 0.8f, 1, 0xD6DBE0, 0.35f);
                }
                c.push();
                c.translate(x1, ruleY + 2);
                c.rotate((float) Math.PI / 4);
                c.rect(-3, -3, 6, 6, 0xD6DBE0, seg(open, 0.3f, 1));
                c.pop();
            }
        }
        float ea = sine(seg(t, 0.2f, 1.0f)) * (1 - sine(seg(t, 5.8f, 6.4f)));
        emblem(c, "tower", CX, ruleY + 1, 4, 0xE3E7EA, ea, 0x080A0E, 0.8f);

        TitleLayout.Layer glow = l.layer("glow");
        TitleLayout.Layer full = l.layer("full");
        for (TitleLayout.Letter g : l.glyphs()) {
            float d = Math.abs(g.u() - 0.5f) * 2;
            float ti = 0.6f + d * 0.8f;
            float k = sine(seg(t, ti, ti + 0.8f));
            if (k <= 0) {
                continue;
            }
            float ko = sine(seg(t, 4.8f + (1 - d) * 0.3f, 5.9f));
            float dx = (g.cx() - CX) * (0.12f * (1 - k) + 0.1f * ko);
            float a = k * (1 - ko);
            letter(c, glow, g, dx, -4 * ko, a * (0.15f + 0.65f * (1 - k)));
            letter(c, full, g, dx, -4 * ko, a);
        }
        sub(c, sine(seg(t, 2.0f, 2.6f)) * (1 - sine(seg(t, 4.8f, 5.3f))));
    }
}
