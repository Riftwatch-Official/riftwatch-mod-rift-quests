package net.riftwatch.rift_quests.client;

import java.util.Arrays;
import net.riftwatch.rift_quests.orrery.OrreryLogic;

public final class ClientOrrery {
    private static final float SECONDS_PER_TICK = 0.05F;
    private static final float GLOW_EASE = 0.25F;
    private static final float[] GLOW = new float[OrreryLogic.SEAL_COUNT];
    private static int mask;
    private static float phase;
    private static long ticks;

    private ClientOrrery() {
    }

    public static void setMask(int value) {
        mask = value & OrreryLogic.FULL;
    }

    public static void clear() {
        mask = 0;
        phase = 0;
        Arrays.fill(GLOW, 0);
    }

    public static boolean open() {
        return mask == OrreryLogic.FULL;
    }

    public static int count() {
        return Integer.bitCount(mask);
    }

    public static float level() {
        return count() / (float) OrreryLogic.SEAL_COUNT;
    }

    public static float glow(int index) {
        return GLOW[index];
    }

    public static float speed() {
        return 0.3F + 0.18F * count() + (open() ? 1.6F : 0F);
    }

    public static void tick() {
        ticks++;
        phase += speed() * SECONDS_PER_TICK;
        for (int index = 0; index < GLOW.length; index++) {
            float target = (mask & (1 << index)) != 0 ? 1F : 0F;
            GLOW[index] += (target - GLOW[index]) * GLOW_EASE;
        }
    }

    public static float angleDegrees(double degreesPerSecond, float partialTick) {
        return (float) (degreesPerSecond * (phase + partialTick * speed() * SECONDS_PER_TICK));
    }

    public static float seconds(float partialTick) {
        return (ticks + partialTick) * SECONDS_PER_TICK;
    }
}
