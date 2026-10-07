package net.riftwatch.template;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TemplateMod.MOD_ID)
public final class TemplateMod {
    public static final String MOD_ID = "riftwatch_template";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TemplateMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("{} {} loaded", modContainer.getModInfo().getDisplayName(), modContainer.getModInfo().getVersion());
    }
}
