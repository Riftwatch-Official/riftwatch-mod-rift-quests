package net.riftwatch.rift_quests.client;

import java.util.Arrays;
import net.riftwatch.rift_quests.orrery.OrreryLogic;

public final class ClientOrrery {
    private static final float SECONDS_PER_TICK = 0.05F;
    private static final float GLOW_EASE = 0.25F;
    private static final float BASE_SPEED = 0.3F;
    private static final float SPEED_PER_SEAL = 0.3F;
    private static final float OPEN_SPEED = 14F;
    private static final float SPEED_EASE = 0.06F;
    private static final float BURST_EASE = 0.035F;
    private static final float[] GLOW = new float[OrreryLogic.SEAL_COUNT];
    private static int mask;
    private static float phase;
    private static float currentSpeed = BASE_SPEED;
    private static float burst;
    private static long ticks;

    private ClientOrrery() {
    }

    public static void setMask(int value) {
        mask = value & OrreryLogic.FULL;
    }

    public static void clear() {
        mask = 0;
        phase = 0;
        currentSpeed = BASE_SPEED;
        burst = 0;
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

    public static boolean has(int index) {
        return (mask & (1 << index)) != 0;
    }

    public static float glow(int index) {
        return GLOW[index];
    }

    public static float speed() {
        return currentSpeed;
    }

    public static float burst() {
        return burst;
    }

    private static float targetSpeed() {
        return BASE_SPEED + SPEED_PER_SEAL * count() + (open() ? OPEN_SPEED : 0F);
    }

    public static void tick() {
        ticks++;
        currentSpeed += (targetSpeed() - currentSpeed) * SPEED_EASE;
        burst += ((open() ? 1F : 0F) - burst) * BURST_EASE;
        phase += currentSpeed * SECONDS_PER_TICK;
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
