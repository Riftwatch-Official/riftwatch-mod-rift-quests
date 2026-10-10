package net.riftwatch.rift_quests.registry;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.riftwatch.rift_quests.RiftQuestsMod;
import net.riftwatch.rift_quests.item.QuestBookItem;
import net.riftwatch.rift_quests.item.SealItem;
import net.riftwatch.rift_quests.item.SealOrreryItem;
import net.riftwatch.rift_quests.item.TicketItem;

public final class ModItems {
    public static final List<String> SEAL_DISTRICTS = List.of(
            "castle_city",
            "desert_citadel",
            "ice_settlement",
            "jungle_city",
            "jungle_temple",
            "monastery",
            "pyramid_row",
            "river_oasis",
            "river_palace");

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RiftQuestsMod.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RiftQuestsMod.MOD_ID);

    public static final DeferredItem<TicketItem> RIFTWATCH_TICKET = ITEMS.registerItem("riftwatch_ticket", TicketItem::new,
            new Item.Properties().stacksTo(64).rarity(Rarity.EPIC));
    public static final DeferredItem<QuestBookItem> QUEST_BOOK = ITEMS.registerItem("quest_book", QuestBookItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final List<DeferredItem<SealItem>> SEALS = SEAL_DISTRICTS.stream()
            .map(slug -> ITEMS.registerItem("seal_" + slug, properties -> new SealItem(slug, properties),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)))
            .toList();

    public static final DeferredItem<SealOrreryItem> SEAL_ORRERY = ITEMS.registerItem("seal_orrery",
            properties -> new SealOrreryItem(ModBlocks.SEAL_ORRERY.get(), properties),
            new Item.Properties().rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<BlockItem> SEAL_VEIL = ITEMS.registerSimpleBlockItem(ModBlocks.SEAL_VEIL,
            new Item.Properties().rarity(Rarity.EPIC).fireResistant());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SEAL_TAB = TABS.register("seals",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.rift_quests.seals"))
                    .icon(() -> new ItemStack(RIFTWATCH_TICKET.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(RIFTWATCH_TICKET.get());
                        output.accept(QUEST_BOOK.get());
                        output.accept(SEAL_ORRERY.get());
                        output.accept(SEAL_VEIL.get());
                        SEALS.forEach(seal -> output.accept(seal.get()));
                    })
                    .build());

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        modEventBus.addListener(ModItems::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(QUEST_BOOK.get());
        }
    }
}
