package net.riftwatch.rift_quests.client.title;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.sounds.SoundEvents;

final class DesertCitadelTitle extends TitleStyle {
    private static final int[] SAND = {0xF2D9A0, 0xD9B36A, 0xB98A44, 0xFFF0C8};

    private record Grain(float x, float y, float sx, float sy, float arrive, float start, float phase, float out, float vx, float vy, int rgb, float size) {
    }

    private record Streak(float y, float length, float speed, float phase) {
    }

    private final List<Grain> grains = new ArrayList<>();
    private final List<Streak> streaks = new ArrayList<>();

    DesertCitadelTitle(TitleLayout layout) {
        super(layout);
        Random r = new Random(23);
        float[] points = l.points3();
        for (int i = 0; i < points.length; i += 3) {
            float x = points[i];
            float y = points[i + 1];
            float arrive = 0.15f + (x - l.x0()) / l.w() * 0.85f + r.nextFloat() * 0.18f;
            float travel = 0.5f + r.nextFloat() * 0.3f;
            grains.add(new Grain(x, y, -60 - r.nextFloat() * 340, y + (r.nextFloat() - 0.5f) * 230, arrive, arrive - travel, r.nextFloat() * 6.28f,
                    3.8f + (x - l.x0() + 20) / (l.w() + 40) * 0.95f + r.nextFloat() * 0.08f, 180 + r.nextFloat() * 240, -20 - r.nextFloat() * 70,
                    SAND[r.nextInt(SAND.length)], r.nextFloat() < 0.6f ? 2 : 3));
        }
        for (int i = 0; i < 22; i++) {
            streaks.add(new Streak(40 + r.nextFloat() * 420, 40 + r.nextFloat() * 140, 600 + r.nextFloat() * 700, r.nextFloat() * 1200));
        }
    }

    @Override
    float duration() {
        return 5.4f;
    }

    @Override
    List<Cue> cues() {
        return List.of(new Cue(0.0f, SoundEvents.BRUSH_SAND, 1.0f, 0.6f), new Cue(1.0f, SoundEvents.SAND_PLACE, 0.8f, 0.6f), new Cue(3.8f, SoundEvents.BRUSH_SAND, 0.9f, 0.5f));
    }

    @Override
    void draw(TitleCanvas c, float t) {
        scrim(c, seg(t, 0, 0.4f) * (1 - seg(t, 4.6f, 5.4f)), 0x241406, 0.5f);

        float wind = bump(t, 0, 1.6f) * 0.55f + bump(t, 3.7f, 5.4f) * 0.55f;
        if (wind > 0.01f) {
            float span = c.right() - c.left() + 300;
            for (Streak s : streaks) {
                float x = c.left() - 150 + ((s.phase() + t * s.speed()) % span);
                c.rect(x, s.y(), s.length(), 1, 0xFFEECD, 0.45f * wind);
            }
        }

        if (t < 1.75f) {
            float fade = 1 - seg(t, 1.25f, 1.75f);
            for (Grain g : grains) {
                if (t < g.start()) {
                    continue;
                }
                float k = outCubic(seg(t, g.start(), g.arrive()));
                float x = lerp(g.sx(), g.x(), k);
                float y = lerp(g.sy(), g.y(), k) + sin(k * 7 + g.phase()) * 16 * (1 - k);
                c.px(x, y, g.size(), g.rgb(), seg(t, g.start(), g.start() + 0.1f) * fade);
            }
        }

        TitleLayout.Layer full = l.layer("full");
        float a = seg(t, 1.0f, 1.55f);
        float front = t > 3.8f ? lerp(l.x0() - 20, l.x1() + 20, seg(t, 3.8f, 4.75f)) : l.x0() - 20;
        float amp = seg(t, 1.6f, 2.0f) * (1 - seg(t, 3.3f, 3.75f));
        if (a > 0 && front < l.x1() + 20) {
            if (amp > 0.01f) {
                for (float y = (float) Math.floor(l.top() - 10); y < l.bottom() + 10; y += 2) {
                    slice(c, full, front, l.x1() + 24, y, y + 2, sin(y * 0.3f + t * 10) * 1.6f * amp, 0, a);
                }
            } else {
                slice(c, full, front, l.x1() + 24, l.top() - 12, l.bottom() + 12, 0, 0, a);
            }
        }

        if (t > 3.8f) {
            for (Grain g : grains) {
                float dt = t - g.out();
                if (dt <= 0 || dt > 0.8f) {
                    continue;
                }
                c.px(g.x() + g.vx() * dt + 260 * dt * dt, g.y() + g.vy() * dt + sin(dt * 8 + g.phase()) * 6, g.size(), g.rgb(), 1 - dt / 0.8f);
            }
        }

        float ea = seg(t, 1.1f, 1.5f) * (1 - seg(t, 3.85f, 4.25f));
        emblem(c, "gate", CX, l.top() - 38 + 14 * (1 - outCubic(seg(t, 1.1f, 1.6f))), 7, 0xF2D9A0, ea, 0x3A2810, 1);
        sub(c, seg(t, 1.55f, 2.0f) * (1 - seg(t, 3.8f, 4.2f)));
    }
}
