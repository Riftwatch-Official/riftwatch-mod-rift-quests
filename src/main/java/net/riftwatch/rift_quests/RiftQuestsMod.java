package net.riftwatch.rift_quests;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.riftwatch.rift_quests.client.ClientSetup;
import net.riftwatch.rift_quests.command.RiftQuestsCommand;
import net.riftwatch.rift_quests.engine.QuestEngine;
import net.riftwatch.rift_quests.engine.TaskEvents;
import net.riftwatch.rift_quests.load.QuestBookLoader;
import net.riftwatch.rift_quests.load.QuestBooks;
import net.riftwatch.rift_quests.network.ModNetwork;
import net.riftwatch.rift_quests.registry.ModAttachments;
import net.riftwatch.rift_quests.registry.ModItems;
import org.slf4j.Logger;

@Mod(RiftQuestsMod.MOD_ID)
public final class RiftQuestsMod {
    public static final String MOD_ID = "rift_quests";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RiftQuestsMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("{} {} loaded", modContainer.getModInfo().getDisplayName(), modContainer.getModInfo().getVersion());
        ModItems.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModNetwork.register(modEventBus);
        TaskEvents.register(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener(RiftQuestsMod::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(RiftQuestsMod::serverStarted);
        NeoForge.EVENT_BUS.addListener(RiftQuestsMod::datapackSync);
        NeoForge.EVENT_BUS.addListener(RiftQuestsMod::registerCommands);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modEventBus, modContainer);
        }
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new QuestBookLoader());
    }

    private static void serverStarted(ServerStartedEvent event) {
        QuestBooks.activate(event.getServer());
    }

    private static void datapackSync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            QuestBooks.activate(event.getPlayerList().getServer());
        } else {
            QuestEngine.sendBook(player);
        }
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        RiftQuestsCommand.register(event.getDispatcher());
    }
}
