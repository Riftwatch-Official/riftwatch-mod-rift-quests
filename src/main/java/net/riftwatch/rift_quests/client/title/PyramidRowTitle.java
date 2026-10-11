package net.riftwatch.rift_quests.client.title;

import java.util.List;
import net.minecraft.sounds.SoundEvents;

final class PyramidRowTitle extends TitleStyle {
    private static final float[][] PYRAMIDS = {{CX - 250, 130, 0.1f, 4.3f}, {CX + 10, 200, 0.25f, 4.15f}, {CX + 270, 280, 0.4f, 4.0f}};

    private final float hy;

    PyramidRowTitle(TitleLayout layout) {
        super(layout);
        hy = Math.round(l.bottom() + 30);
    }

    @Override
    float duration() {
        return 5.6f;
    }

    @Override
    List<Cue> cues() {
        return List.of(new Cue(0.2f, SoundEvents.BEACON_ACTIVATE, 0.7f, 0.5f), new Cue(4.0f, SoundEvents.BEACON_DEACTIVATE, 0.7f, 0.45f));
    }

    @Override
    void draw(TitleCanvas c, float t) {
        float lineW = (l.w() + 300) * outCubic(seg(t, 0, 0.5f)) * (1 - inCubic(seg(t, 4.8f, 5.5f)));

        c.clip(c.left(), 0, c.right(), hy);
        float sunUp = outCubic(seg(t, 0.2f, 1.3f));
        float sunDown = inCubic(seg(t, 3.8f, 5.0f));
        float sy = lerp(hy + 60, l.cy() - 6, sunUp) + (hy + 80 - (l.cy() - 6)) * sunDown;
        float sa = seg(t, 0.2f, 0.5f) * (1 - seg(t, 4.6f, 5.1f));
        if (sa > 0) {
            c.ellipse(CX, sy, 190, 190, 0xFFD66E, 0.55f * sa, 0xFFD66E, 0);
            for (int i = 0; i < 12; i++) {
                float a0 = t * 0.15f + i * TAU / 12;
                c.tri(CX, sy, CX + cos(a0 - 0.05f) * 420, sy + sin(a0 - 0.05f) * 420, CX + cos(a0 + 0.05f) * 420, sy + sin(a0 + 0.05f) * 420,
                        0xFFE28C, 0.14f * sa);
            }
            c.ellipse(CX, sy, 58, 58, 0xFFF3B8, sa, 0xFFC94A, sa);
        }

        for (float[] p : PYRAMIDS) {
            float up = outCubic(seg(t, p[2], p[2] + 0.7f));
            float down = inCubic(seg(t, p[3], p[3] + 0.8f));
            float off = (p[1] / 2 + 8) * (1 - up + down);
            if (off >= p[1] / 2 + 8) {
                continue;
            }
            steps(c, p[0], hy + off, p[1], 10, 10, 0x6B4422, 0x83552A, 1);
        }

        scrim(c, seg(t, 0.4f, 0.9f) * (1 - seg(t, 4.8f, 5.6f)), 0x1E1004, 0.55f);

        TitleLayout.Layer full = l.layer("full");
        for (TitleLayout.Letter g : l.glyphs()) {
            float d = Math.abs(g.u() - 0.5f) * 2;
            float ti = 0.8f + d * 0.35f;
            float k = seg(t, ti, ti + 0.3f);
            if (k <= 0) {
                continue;
            }
            float to = 4.0f + (1 - d) * 0.35f;
            float ko = inCubic(seg(t, to, to + 0.6f));
            letter(c, full, g, 0, 12 * (1 - outCubic(k)) + ko * (hy - l.top() + 10), k);
        }
        if (t > 1.7f && t < 2.4f) {
            sheen(c, seg(t, 1.7f, 2.4f), 0xFFFCE6, 0.9f, 40);
        }
        c.unclip();

        if (lineW > 1) {
            c.rect(CX - lineW / 2, hy, lineW, 2, 0xF2CF72, 1);
            c.rect(CX - lineW / 2, hy + 2, lineW, 1, 0x2E1A08, 0.8f);
        }
        sub(c, seg(t, 1.5f, 2.0f) * (1 - seg(t, 3.8f, 4.2f)));
    }
}
