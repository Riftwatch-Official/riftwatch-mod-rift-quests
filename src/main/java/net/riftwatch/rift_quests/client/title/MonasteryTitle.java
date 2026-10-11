package net.riftwatch.rift_quests.client.title;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.sounds.SoundEvents;

final class MonasteryTitle extends TitleStyle {
    private static final int[] FLAGS = {0x2F6FB5, 0xF2F2EE, 0xC43A2F, 0x3F9A4F, 0xE8C547};

    private record Smoke(float x, float y, float release, float phase) {
    }

    private final List<Smoke> smoke = new ArrayList<>();
    private final List<Float> flags = new ArrayList<>();
    private final float sx0;
    private final float sx1;
    private final float sy0;
    private final float sy1;
    private final float fx0;
    private final float fx1;
    private final float fy;

    MonasteryTitle(TitleLayout layout) {
        super(layout);
        Random r = new Random(61);
        float[] stroke = l.stroke();
        sx0 = stroke[0];
        sx1 = stroke[1];
        sy0 = stroke[2];
        sy1 = stroke[3];
        float[] points = l.points4();
        for (int i = 0; i < points.length; i += 6) {
            float x = points[i];
            smoke.add(new Smoke(x, points[i + 1], 3.8f + (x - l.x0()) / l.w() * 0.25f + r.nextFloat() * 0.6f, r.nextFloat() * 6));
        }
        fx0 = l.x0() - 60;
        fx1 = l.x1() + 60;
        fy = l.top() - 58;
        for (float x = fx0 + 14; x < fx1 - 6; x += 30) {
            flags.add(x);
        }
    }

    private float ropeY(float x) {
        float mid = (fx0 + fx1) / 2;
        float half = (fx1 - fx0) / 2;
        float k = (x - mid) / half;
        return fy + 16 * (1 - k * k);
    }

    @Override
    float duration() {
        return 5.6f;
    }

    @Override
    List<Cue> cues() {
        return List.of(new Cue(0.72f, SoundEvents.BELL_BLOCK, 0.8f, 0.8f), new Cue(0.75f, SoundEvents.BELL_RESONATE, 1.0f, 0.6f),
                new Cue(3.8f, SoundEvents.CAMPFIRE_CRACKLE, 1.0f, 0.4f));
    }

    @Override
    void draw(TitleCanvas c, float t) {
        scrim(c, seg(t, 0, 0.4f) * (1 - seg(t, 4.8f, 5.6f)), 0x140806, 0.45f);

        float front = lerp(sx0, sx1 + 10, outCubic(seg(t, 0.1f, 0.7f)));
        float back = lerp(sx0, sx1 + 10, inOutCubic(seg(t, 4.3f, 5.0f)));
        slice(c, l.layer("stroke"), back, front, sy0, sy1, 0, 0, 1 - seg(t, 4.7f, 5.1f));

        for (float d = 0; d <= 0.24f; d += 0.12f) {
            float ti = 0.72f + d;
            float k = seg(t, ti, ti + 0.9f);
            if (k > 0 && k < 1) {
                float radius = 30 + (t - ti) * 460;
                c.arc(CX, l.cy(), radius, radius, 3, 0, TAU, 0xE2B04A, 0.55f * (1 - k));
            }
        }

        float ra = 1 - seg(t, 4.2f, 4.8f);
        float lineK = seg(t, 0.85f, 1.2f);
        if (lineK > 0 && ra > 0) {
            float end = lerp(fx0, fx1, lineK);
            for (float x = fx0; x < end; x += 2) {
                c.rect(x, Math.round(ropeY(x)), 2, 2, 0x3A2A22, ra);
            }
            for (int i = 0; i < flags.size(); i++) {
                float x = flags.get(i);
                float s = sine(seg(end - x, 0, 40));
                if (s <= 0) {
                    continue;
                }
                float y = ropeY(x) + 1;
                float flutter = sin(t * 6 + i) * 3;
                c.quad(x - 8, y, x + 8, y, x + 8 + flutter, y + 20 * s, x - 8 + flutter, y + 20 * s, FLAGS[i % FLAGS.length], ra);
            }
        }

        float ta = seg(t, 0.7f, 0.95f) * (1 - seg(t, 3.9f, 4.5f));
        if (ta > 0) {
            float s = 1 + 0.22f * (1 - outCubic(seg(t, 0.7f, 1.0f)));
            c.push();
            c.translate(CX, l.cy());
            c.scale(s, s);
            c.translate(-CX, -l.cy());
            name(c, l.layer("full"), ta);
            c.pop();
        }

        for (Smoke m : smoke) {
            float dt = t - m.release();
            if (dt <= 0 || dt > 1.1f) {
                continue;
            }
            float size = Math.round(3 + 9 * seg(dt, 0, 1.1f));
            c.px(m.x() + sin(dt * 3 + m.phase()) * 12 * dt, m.y() - 60 * dt, size, 0xD8D2C8, 0.4f * (1 - seg(dt, 0.2f, 1.1f)));
        }

        float sa = seg(t, 1.15f, 1.3f) * (1 - seg(t, 4.0f, 4.5f));
        if (sa > 0) {
            float ss = 1 + 0.18f * (1 - outCubic(seg(t, 1.15f, 1.4f)));
            c.push();
            c.translate(Math.min(sx1 + 28, W - 30), l.cy() + 22);
            c.rotate(-0.06f);
            c.scale(ss, ss);
            c.rect(-18, -18, 36, 36, 0xC4342A, sa);
            c.rect(-15, -15, 30, 30, 0xA82A22, sa);
            emblem(c, "pagoda", 0, 0, 5, 0xF6E8D0, sa, 0, 0);
            c.pop();
        }
        sub(c, seg(t, 1.3f, 1.8f) * (1 - seg(t, 3.8f, 4.2f)));
    }
}
