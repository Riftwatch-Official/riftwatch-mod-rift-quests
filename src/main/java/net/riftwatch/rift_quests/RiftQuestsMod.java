package net.riftwatch.rift_quests;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.riftwatch.rift_quests.command.RiftQuestsCommand;
import net.riftwatch.rift_quests.load.QuestBookLoader;
import net.riftwatch.rift_quests.load.QuestBooks;
import org.slf4j.Logger;

@Mod(RiftQuestsMod.MOD_ID)
public final class RiftQuestsMod {
    public static final String MOD_ID = "rift_quests";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RiftQuestsMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("{} {} loaded", modContainer.getModInfo().getDisplayName(), modContainer.getModInfo().getVersion());
        NeoForge.EVENT_BUS.addListener(RiftQuestsMod::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(RiftQuestsMod::serverStarted);
        NeoForge.EVENT_BUS.addListener(RiftQuestsMod::datapackSync);
        NeoForge.EVENT_BUS.addListener(RiftQuestsMod::registerCommands);
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new QuestBookLoader());
    }

    private static void serverStarted(ServerStartedEvent event) {
        QuestBooks.activate(event.getServer());
    }

    private static void datapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            QuestBooks.activate(event.getPlayerList().getServer());
        }
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        RiftQuestsCommand.register(event.getDispatcher());
    }
}
