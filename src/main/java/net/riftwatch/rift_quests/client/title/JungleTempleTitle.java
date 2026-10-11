package net.riftwatch.rift_quests.client.title;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.sounds.SoundEvents;

final class JungleTempleTitle extends TitleStyle {
    private static final int[] MOSS = {0x3F7A5A, 0x5F9A5E, 0x4F8A4C, 0x2F6A46};

    private record Moss(float x, float y, float release, float vx, int rgb) {
    }

    private record Tile(float x, float y, float start, float vx, float rot) {
    }

    private final List<Moss> moss = new ArrayList<>();
    private final List<Tile> tiles = new ArrayList<>();
    private final float bx0;
    private final float by0;
    private final float bx1;
    private final float by1;

    JungleTempleTitle(TitleLayout layout) {
        super(layout);
        Random r = new Random(53);
        float[] box = l.tabletBox();
        bx0 = box[0];
        by0 = box[1];
        bx1 = box[2];
        by1 = box[3];
        float[] points = l.points4();
        for (int i = 0; i < points.length; i += 3) {
            float y = points[i + 1];
            moss.add(new Moss(points[i], y, 0.55f + (1 - (y - l.top()) / l.h()) * 0.45f + r.nextFloat() * 0.3f, (r.nextFloat() - 0.5f) * 60, MOSS[r.nextInt(MOSS.length)]));
        }
        int rows = (int) Math.ceil((by1 - by0) / 10);
        int row = 0;
        for (float y = by0; y < by1; y += 10, row++) {
            for (float x = bx0; x < bx1; x += 10) {
                tiles.add(new Tile(x, y, 4.05f + r.nextFloat() * 0.45f + (1 - (float) row / rows) * 0.3f, (r.nextFloat() - 0.5f) * 40, (r.nextFloat() - 0.5f) * 2));
            }
        }
    }

    @Override
    float duration() {
        return 5.6f;
    }

    @Override
    List<Cue> cues() {
        return List.of(new Cue(0.6f, SoundEvents.MOSS_BREAK, 0.9f, 0.7f), new Cue(1.35f, SoundEvents.BEACON_ACTIVATE, 1.2f, 0.5f),
                new Cue(4.05f, SoundEvents.STONE_BREAK, 0.7f, 0.7f));
    }

    @Override
    void draw(TitleCanvas c, float t) {
        scrim(c, seg(t, 0, 0.4f) * (1 - seg(t, 4.8f, 5.6f)), 0x06100A, 0.55f);
        TitleLayout.Layer tablet = l.layer("tablet");
        float rise = 24 * (1 - outCubic(seg(t, 0, 0.5f)));
        float ta = seg(t, 0, 0.35f);

        if (t < 4.05f) {
            slice(c, tablet, bx0, bx1, by0, by1, 0, rise, ta);
        } else {
            for (Tile tile : tiles) {
                float dt = Math.max(0, t - tile.start());
                float a = 1 - seg(dt, 0.3f, 0.7f);
                if (a <= 0) {
                    continue;
                }
                c.push();
                c.translate(tile.x() + 5 + tile.vx() * dt, tile.y() + 5 + 500 * dt * dt);
                c.rotate(tile.rot() * dt);
                c.image(tablet, tile.x(), tile.y(), tile.x() + 10, tile.y() + 10, -5, -5, 5, 5, WHITE, a);
                c.pop();
            }
        }

        if (t < 1.9f) {
            for (Moss m : moss) {
                float dt = t - m.release();
                if (dt > 0.5f) {
                    continue;
                }
                if (dt <= 0) {
                    c.px(m.x(), m.y() + rise, 4, m.rgb(), ta);
                } else {
                    c.px(m.x() + m.vx() * dt, m.y() + 450 * dt * dt, 4, m.rgb(), 1 - dt / 0.5f);
                }
            }
        }

        float front = lerp(l.x0() - 10, l.x1() + 10, seg(t, 1.35f, 1.95f));
        float ga = (t < 1.95f ? 1 : 0.8f + 0.2f * sin((t - 1.95f) * 4)) * (1 - seg(t, 3.8f, 4.1f));
        if (t > 1.35f && ga > 0) {
            slice(c, l.layer("glow"), l.x0() - 20, front, l.top() - 20, l.bottom() + 20, 0, 0, ga);
        }

        float ea = seg(t, 1.6f, 1.9f) * (1 - seg(t, 3.85f, 4.1f));
        emblem(c, "step", CX, by0 - 26, 6, 0xF2C14E, ea, 0x1B2420, 1);
        sub(c, seg(t, 1.7f, 2.15f) * (1 - seg(t, 3.8f, 4.1f)));
    }
}
