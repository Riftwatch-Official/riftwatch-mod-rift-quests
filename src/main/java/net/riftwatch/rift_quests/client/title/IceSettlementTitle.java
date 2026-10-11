package net.riftwatch.rift_quests.client.title;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.sounds.SoundEvents;

final class IceSettlementTitle extends TitleStyle {
    private static final int[] FROST = {0xFFFFFF, 0xDFF4FF, 0xA9DAF2};

    private record Frost(float x, float y, float at, int rgb) {
    }

    private record Shard(float x0, float x1, float start, float rot, float vx) {
    }

    private record Chip(float x, float y, Shard shard, float vx, float vy) {
    }

    private record Sparkle(float x, float y, float phase) {
    }

    private final List<Frost> frost = new ArrayList<>();
    private final List<Shard> shards = new ArrayList<>();
    private final List<Chip> chips = new ArrayList<>();
    private final List<Sparkle> sparkles = new ArrayList<>();

    IceSettlementTitle(TitleLayout layout) {
        super(layout);
        Random r = new Random(31);
        float[] cuts = l.cuts();
        for (int i = 0; i + 1 < cuts.length; i++) {
            shards.add(new Shard(cuts[i], cuts[i + 1], 3.98f + r.nextFloat() * 0.38f, (r.nextFloat() - 0.5f) * 0.9f, (r.nextFloat() - 0.5f) * 50));
        }
        float[] points = l.points3();
        float maxDistance = 1;
        for (int i = 0; i < points.length; i += 3) {
            maxDistance = Math.max(maxDistance, distance(points[i], points[i + 1]));
        }
        for (int i = 0; i < points.length; i += 3) {
            float x = points[i];
            float y = points[i + 1];
            frost.add(new Frost(x, y, 0.25f + distance(x, y) / maxDistance * 0.9f + r.nextFloat() * 0.08f, FROST[r.nextInt(FROST.length)]));
            if ((i / 3) % 7 == 0) {
                chips.add(new Chip(x, y, shardAt(x), (r.nextFloat() - 0.5f) * 160, -40 - r.nextFloat() * 90));
            }
        }
        int count = points.length / 3;
        for (int i = 0; i < 14 && count > 0; i++) {
            int at = r.nextInt(count) * 3;
            sparkles.add(new Sparkle(points[at], points[at + 1], r.nextFloat() * 2.2f));
        }
    }

    private float distance(float x, float y) {
        float dx = x - CX;
        float dy = (y - l.cy()) * 2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private Shard shardAt(float x) {
        for (Shard shard : shards) {
            if (x >= shard.x0() && x < shard.x1()) {
                return shard;
            }
        }
        return shards.get(0);
    }

    @Override
    float duration() {
        return 5.4f;
    }

    @Override
    List<Cue> cues() {
        return List.of(new Cue(0.25f, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.8f), new Cue(0.8f, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.7f),
                new Cue(3.85f, SoundEvents.GLASS_BREAK, 0.6f, 0.5f));
    }

    @Override
    void draw(TitleCanvas c, float t) {
        scrim(c, seg(t, 0, 0.4f) * (1 - seg(t, 4.6f, 5.4f)), 0x061222, 0.55f);

        if (t > 0.2f && t < 1.75f) {
            float fade = 1 - seg(t, 1.25f, 1.7f);
            for (Frost f : frost) {
                if (t > f.at()) {
                    c.px(f.x(), f.y(), 3, f.rgb(), seg(t, f.at(), f.at() + 0.08f) * fade);
                }
            }
        }

        TitleLayout.Layer full = l.layer("full");
        TitleLayout.Layer cracked = l.layer("cracked");
        float top = l.top() - 14;
        float bottom = l.bottom() + 14;
        if (t < 3.95f) {
            slice(c, full, l.x0() - 20, l.x1() + 20, top, bottom, 0, 0, seg(t, 0.95f, 1.5f));
            if (t > 3.8f) {
                slice(c, cracked, l.x0() - 20, l.x1() + 20, top, bottom, 0, 0, seg(t, 3.8f, 3.95f));
            }
        } else {
            for (Shard s : shards) {
                float dt = Math.max(0, t - s.start());
                float a = 1 - seg(dt, 0.2f, 0.7f);
                if (a <= 0) {
                    continue;
                }
                float half = (s.x1() - s.x0()) / 2;
                c.push();
                c.translate((s.x0() + s.x1()) / 2 + s.vx() * dt, l.cy() + 550 * dt * dt);
                c.rotate(s.rot() * dt * 2);
                c.image(cracked, s.x0(), top, s.x1(), bottom, -half, top - l.cy(), half, bottom - l.cy(), WHITE, a);
                c.pop();
            }
            for (Chip chip : chips) {
                float dt = t - chip.shard().start();
                if (dt <= 0 || dt > 0.7f) {
                    continue;
                }
                c.px(chip.x() + chip.vx() * dt, chip.y() + chip.vy() * dt + 450 * dt * dt, 2, 0xE6F6FF, 1 - dt / 0.7f);
            }
        }

        if (t > 1.8f && t < 2.5f) {
            sheen(c, seg(t, 1.8f, 2.5f), 0xFFFFFF, 0.9f, 34);
        }
        float sa = seg(t, 2.2f, 2.5f) * (1 - seg(t, 3.6f, 3.8f));
        if (sa > 0) {
            for (Sparkle s : sparkles) {
                float k = bump((t + s.phase()) % 1.1f, 0, 0.5f) * sa;
                if (k <= 0) {
                    continue;
                }
                c.px(s.x(), s.y(), 2, 0xFFFFFF, k);
                c.px(s.x() - 3, s.y(), 2, 0xFFFFFF, k * 0.6f);
                c.px(s.x() + 3, s.y(), 2, 0xFFFFFF, k * 0.6f);
                c.px(s.x(), s.y() - 3, 2, 0xFFFFFF, k * 0.6f);
                c.px(s.x(), s.y() + 3, 2, 0xFFFFFF, k * 0.6f);
            }
        }

        float ea = seg(t, 1.0f, 1.35f) * (1 - seg(t, 3.8f, 3.95f));
        float cell = Math.max(1, Math.round(7 * (0.6f + 0.4f * sine(seg(t, 1.0f, 1.6f)))));
        emblem(c, "flake", CX, l.top() - 38, cell, 0xE8F7FF, ea, 0x14304A, 1);
        sub(c, seg(t, 1.5f, 2.0f) * (1 - seg(t, 3.8f, 4.1f)));
    }
}
