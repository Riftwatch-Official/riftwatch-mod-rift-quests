package net.riftwatch.rift_quests.registry;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.riftwatch.rift_quests.RiftQuestsMod;
import net.riftwatch.rift_quests.item.QuestBookItem;
import net.riftwatch.rift_quests.item.TicketItem;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RiftQuestsMod.MOD_ID);

    public static final DeferredItem<TicketItem> RIFTWATCH_TICKET = ITEMS.registerItem("riftwatch_ticket", TicketItem::new,
            new Item.Properties().stacksTo(64).rarity(Rarity.EPIC));
    public static final DeferredItem<QuestBookItem> QUEST_BOOK = ITEMS.registerItem("quest_book", QuestBookItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModItems::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(QUEST_BOOK.get());
        }
    }
}
