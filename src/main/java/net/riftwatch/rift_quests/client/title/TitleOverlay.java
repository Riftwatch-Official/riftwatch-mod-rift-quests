package net.riftwatch.rift_quests.client.title;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.riftwatch.rift_quests.client.ClientConfig;
import org.slf4j.Logger;

public final class TitleOverlay {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Function<TitleLayout, TitleStyle>> FACTORIES = Map.of(
            "castle_city", CastleCityTitle::new,
            "desert_citadel", DesertCitadelTitle::new,
            "ice_settlement", IceSettlementTitle::new,
            "jungle_city", JungleCityTitle::new,
            "jungle_temple", JungleTempleTitle::new,
            "monastery", MonasteryTitle::new,
            "pyramid_row", PyramidRowTitle::new,
            "river_oasis", RiverOasisTitle::new,
            "river_palace", RiverPalaceTitle::new,
            "sky_fortress", SkyFortressTitle::new);
    private static final Map<String, TitleStyle> STYLES = new HashMap<>();

    private static TitleStyle active;
    private static long startedAt;
    private static int nextCue;

    private TitleOverlay() {
    }

    public static void show(String district) {
        if (!ClientConfig.SHOW_DISTRICT_TITLES.get()) {
            return;
        }
        TitleStyle style = style(district);
        if (style == null) {
            return;
        }
        active = style;
        startedAt = Util.getMillis();
        nextCue = 0;
    }

    public static void clear() {
        active = null;
        STYLES.clear();
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        TitleStyle style = active;
        if (style == null) {
            return;
        }
        float t = (Util.getMillis() - startedAt) / 1000f;
        if (t >= style.duration()) {
            active = null;
            return;
        }
        playCues(style, t);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) {
            return;
        }
        TitleCanvas canvas = new TitleCanvas(graphics);
        canvas.begin();
        try {
            style.draw(canvas, t);
        } catch (RuntimeException exception) {
            LOGGER.error("District title failed to draw", exception);
            active = null;
        } finally {
            canvas.end();
        }
    }

    private static void playCues(TitleStyle style, float t) {
        List<TitleStyle.Cue> cues = style.cues();
        while (nextCue < cues.size() && cues.get(nextCue).time() <= t) {
            TitleStyle.Cue cue = cues.get(nextCue++);
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(cue.sound(), cue.pitch(), cue.volume()));
        }
    }

    private static TitleStyle style(String district) {
        TitleStyle cached = STYLES.get(district);
        if (cached != null) {
            return cached;
        }
        Function<TitleLayout, TitleStyle> factory = FACTORIES.get(district);
        if (factory == null) {
            return null;
        }
        try {
            TitleStyle style = factory.apply(TitleLayout.load(district));
            STYLES.put(district, style);
            return style;
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Could not load the district title {}", district, exception);
            return null;
        }
    }
}
