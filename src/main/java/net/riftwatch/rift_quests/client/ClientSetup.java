package net.riftwatch.rift_quests.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import java.util.zip.DataFormatException;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.riftwatch.rift_quests.RiftQuestsMod;
import net.riftwatch.rift_quests.network.BookView;
import net.riftwatch.rift_quests.network.ClientBridge;
import net.riftwatch.rift_quests.network.Payloads;
import org.slf4j.Logger;

public final class ClientSetup {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String KEY_CATEGORY = "key.categories.rift_quests";
    public static final KeyMapping OPEN_BOOK = new KeyMapping("key.rift_quests.open_book", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), KEY_CATEGORY);

    private ClientSetup() {
    }

    public static void init(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(ModConfigEvent.Loading.class, ClientConfig::onLoad);
        modEventBus.addListener(ModConfigEvent.Reloading.class, ClientConfig::onLoad);
        modEventBus.addListener(RegisterKeyMappingsEvent.class, event -> event.register(OPEN_BOOK));
        modEventBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAbove(VanillaGuiLayers.SCOREBOARD_SIDEBAR,
                ResourceLocation.fromNamespaceAndPath(RiftQuestsMod.MOD_ID, "tracker"), QuestTracker::render));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
            while (OPEN_BOOK.consumeClick()) {
                openBook();
            }
        });
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> ClientQuests.clear());
        SealAnimation.init();
        ClientBridge.install(new ClientBridge.Handler() {
            @Override
            public void openBook() {
                ClientSetup.openBook();
            }

            @Override
            public void book(Payloads.Book payload) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.getConnection() == null) {
                    return;
                }
                try {
                    BookView view = BookView.fromBytes(payload.data(), minecraft.getConnection().registryAccess());
                    ClientQuests.setBook(view);
                    LOGGER.info("Quest book received: {} chapters, {} quests", view.chapters().size(), view.quests().size());
                } catch (DataFormatException | RuntimeException exception) {
                    LOGGER.error("Could not read the quest book from the server", exception);
                }
            }

            @Override
            public void progress(Payloads.Progress payload) {
                ClientQuests.setProgress(payload);
            }

            @Override
            public void questCompleted(Payloads.QuestCompleted payload) {
                if (payload.book() != ClientQuests.book().id() || payload.quest() < 0 || payload.quest() >= ClientQuests.book().quests().size()) {
                    return;
                }
                if (ClientConfig.SHOW_TOASTS.get()) {
                    Minecraft.getInstance().getToasts().addToast(new QuestToast(ClientQuests.quest(payload.quest())));
                }
            }
        });
    }

    public static void openBook() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && !(minecraft.screen instanceof QuestBookScreen)) {
            minecraft.setScreen(new QuestBookScreen());
        }
    }
}
