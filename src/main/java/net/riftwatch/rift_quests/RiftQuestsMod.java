package net.riftwatch.rift_quests;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(RiftQuestsMod.MOD_ID)
public final class RiftQuestsMod {
    public static final String MOD_ID = "rift_quests";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RiftQuestsMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("{} {} loaded", modContainer.getModInfo().getDisplayName(), modContainer.getModInfo().getVersion());
    }
}
