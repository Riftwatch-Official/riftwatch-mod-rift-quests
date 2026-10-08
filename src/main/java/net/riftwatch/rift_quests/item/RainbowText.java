package net.riftwatch.rift_quests.item;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;

public final class RainbowText {
    private static final float CYCLE_MILLIS = 3000.0F;
    private static final float LETTER_SPREAD = 0.055F;
    private static volatile boolean animated = true;

    private RainbowText() {
    }

    public static void setAnimated(boolean value) {
        animated = value;
    }

    public static MutableComponent of(String text) {
        float start = animated ? (Util.getMillis() % (long) CYCLE_MILLIS) / CYCLE_MILLIS : 0.0F;
        MutableComponent result = Component.empty();
        int letter = 0;
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            float hue = start - letter * LETTER_SPREAD;
            hue -= (float) Math.floor(hue);
            int colour = Mth.hsvToRgb(hue, 0.62F, 1.0F);
            result.append(Component.literal(character).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour))));
            offset += Character.charCount(codePoint);
            if (!Character.isWhitespace(codePoint)) {
                letter++;
            }
        }
        return result;
    }
}
