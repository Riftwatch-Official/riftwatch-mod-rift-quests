package net.riftwatch.rift_quests.orrery;

public final class OrreryPillars {
    public static final int EMPTY = -1;
    private static final int BITS = 4;
    private static final long NIBBLE = (1L << BITS) - 1;

    private OrreryPillars() {
    }

    public static int seal(long packed, int pillar) {
        return (int) ((packed >>> (pillar * BITS)) & NIBBLE) - 1;
    }

    public static long with(long packed, int pillar, int seal) {
        long cleared = packed & ~(NIBBLE << (pillar * BITS));
        return cleared | ((long) (seal + 1) << (pillar * BITS));
    }

    public static int mask(long packed) {
        int mask = 0;
        for (int pillar = 0; pillar < OrreryLogic.SEAL_COUNT; pillar++) {
            int seal = seal(packed, pillar);
            if (seal != EMPTY) {
                mask |= 1 << seal;
            }
        }
        return mask;
    }

    public static long fromMask(int mask) {
        long packed = 0;
        for (int seal = 0; seal < OrreryLogic.SEAL_COUNT; seal++) {
            if ((mask & (1 << seal)) != 0) {
                packed = with(packed, seal, seal);
            }
        }
        return packed;
    }
}
