package net.riftwatch.rift_quests.engine;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.riftwatch.rift_quests.book.ItemSpec;
import net.riftwatch.rift_quests.book.Reward;
import net.riftwatch.rift_quests.registry.ModItems;
import org.slf4j.Logger;

public final class Rewards {
    private static final Logger LOGGER = LogUtils.getLogger();

    private Rewards() {
    }

    public static void give(ServerPlayer player, Reward reward, int choice) {
        switch (reward) {
            case Reward.Item item -> giveCount(player, stack(player.server, item.item()), item.count());
            case Reward.Ticket ticket -> giveCount(player, new ItemStack(ModItems.RIFTWATCH_TICKET.get()), ticket.count());
            case Reward.Xp xp -> player.giveExperienceLevels(xp.levels());
            case Reward.LootTable table -> {
                LootTable loot = player.server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, table.table()));
                LootParams params = new LootParams.Builder(player.serverLevel())
                        .withParameter(LootContextParams.ORIGIN, player.position())
                        .withParameter(LootContextParams.THIS_ENTITY, player)
                        .withLuck(player.getLuck())
                        .create(LootContextParamSets.CHEST);
                for (int roll = 0; roll < table.rolls(); roll++) {
                    loot.getRandomItems(params).forEach(stack -> QuestEngine.give(player, stack));
                }
            }
            case Reward.Choice options -> {
                if (choice >= 0 && choice < options.options().size()) {
                    give(player, options.options().get(choice), -1);
                }
            }
            case Reward.Command command -> {
                MinecraftServer server = player.server;
                LOGGER.info("Quest reward command for {}: {}", player.getGameProfile().getName(), command.command());
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack()
                        .withSuppressedOutput()
                        .withLevel(player.serverLevel())
                        .withPosition(player.position()), command.command().replace("{player}", player.getGameProfile().getName()));
            }
        }
    }

    private static ItemStack stack(MinecraftServer server, ItemSpec spec) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(spec.item()));
        spec.components().ifPresent(json -> DataComponentPatch.CODEC.parse(server.registryAccess().createSerializationContext(JsonOps.INSTANCE), json)
                .result().ifPresent(stack::applyComponents));
        return stack;
    }

    private static void giveCount(ServerPlayer player, ItemStack template, int count) {
        int remaining = count;
        int max = Math.max(1, template.getMaxStackSize());
        while (remaining > 0) {
            int amount = Math.min(max, remaining);
            QuestEngine.give(player, template.copyWithCount(amount));
            remaining -= amount;
        }
    }
}
