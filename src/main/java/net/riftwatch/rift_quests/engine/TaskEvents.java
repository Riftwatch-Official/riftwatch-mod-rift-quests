package net.riftwatch.rift_quests.engine;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.riftwatch.rift_quests.book.Task;

public final class TaskEvents {
    private TaskEvents() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOWEST, ServerTickEvent.Post.class, event -> QuestEngine.serverTick(event.getServer()));
        bus.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                QuestEngine.login(player);
            }
        });
        bus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                QuestEngine.logout(player);
            }
        });
        bus.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                QuestEngine.invalidate(player);
            }
        });
        bus.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                QuestEngine.pollSoon(player);
            }
        });
        bus.addListener(AdvancementEvent.AdvancementEarnEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                QuestEngine.pollSoon(player);
            }
        });
        bus.addListener(EventPriority.LOWEST, BlockEvent.EntityPlaceEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                BlockState state = event.getPlacedBlock();
                QuestEngine.count(player, task -> task instanceof Task.Block block && block.action() == Task.BlockAction.PLACE
                        && Matchers.block(block.blocks(), state) ? 1 : 0);
            }
        });
        bus.addListener(EventPriority.LOWEST, BlockEvent.BreakEvent.class, event -> {
            if (event.getPlayer() instanceof ServerPlayer player) {
                BlockState state = event.getState();
                QuestEngine.count(player, task -> task instanceof Task.Block block && block.action() == Task.BlockAction.BREAK
                        && Matchers.block(block.blocks(), state) ? 1 : 0);
            }
        });
        bus.addListener(EventPriority.LOWEST, PlayerInteractEvent.RightClickBlock.class, event -> {
            if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND) {
                return;
            }
            BlockState state = event.getLevel().getBlockState(event.getPos());
            QuestEngine.count(player, task -> task instanceof Task.Block block && block.action() == Task.BlockAction.INTERACT
                    && Matchers.block(block.blocks(), state) ? 1 : 0);
            used(player, event.getItemStack());
        });
        bus.addListener(EventPriority.LOWEST, PlayerInteractEvent.RightClickItem.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                used(player, event.getItemStack());
            }
        });
        bus.addListener(EventPriority.LOWEST, PlayerInteractEvent.EntityInteract.class, event -> {
            if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND) {
                return;
            }
            QuestEngine.count(player, task -> task instanceof Task.Entity entity && entity.action() == Task.EntityAction.INTERACT
                    && Matchers.entity(entity.entities(), event.getTarget()) && Matchers.entityNbt(entity.nbt(), event.getTarget()) ? 1 : 0);
        });
        bus.addListener(EventPriority.LOWEST, LivingDeathEvent.class, event -> {
            if (event.getSource().getEntity() instanceof ServerPlayer player) {
                QuestEngine.count(player, task -> task instanceof Task.Entity entity && entity.action() == Task.EntityAction.KILL
                        && Matchers.entity(entity.entities(), event.getEntity()) && Matchers.entityNbt(entity.nbt(), event.getEntity()) ? 1 : 0);
            }
        });
        bus.addListener(EventPriority.LOWEST, AnimalTameEvent.class, event -> {
            if (event.getTamer() instanceof ServerPlayer player) {
                QuestEngine.count(player, task -> task instanceof Task.Entity entity && entity.action() == Task.EntityAction.TAME
                        && Matchers.entity(entity.entities(), event.getAnimal()) ? 1 : 0);
            }
        });
        bus.addListener(EventPriority.LOWEST, BabyEntitySpawnEvent.class, event -> {
            if (event.getCausedByPlayer() instanceof ServerPlayer player) {
                QuestEngine.count(player, task -> task instanceof Task.Entity entity && entity.action() == Task.EntityAction.BREED
                        && Matchers.entity(entity.entities(), event.getParentA()) ? 1 : 0);
            }
        });
        bus.addListener(PlayerEvent.ItemCraftedEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                crafted(player, event.getCrafting(), event.getInventory());
            }
        });
    }

    private static void used(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || QuestEngine.usedThisTick(player)) {
            return;
        }
        QuestEngine.count(player, task -> task instanceof Task.UseItem use && use.items().stream().anyMatch(ref -> Matchers.item(ref, stack)) ? 1 : 0);
    }

    private static void crafted(ServerPlayer player, ItemStack result, net.minecraft.world.Container container) {
        if (result.isEmpty()) {
            return;
        }
        int count = result.getCount();
        Optional<CraftingInput> input = container instanceof CraftingContainer grid ? Optional.of(grid.asCraftInput()) : Optional.empty();
        QuestEngine.count(player, task -> {
            if (task instanceof Task.Craft craft) {
                return craft.items().stream().anyMatch(ref -> Matchers.item(ref, result)) ? count : 0;
            }
            if (task instanceof Task.Recipe recipe) {
                for (ResourceLocation id : recipe.recipes()) {
                    if (recipeMatches(player, id, result, input)) {
                        return 1;
                    }
                }
            }
            return 0;
        });
    }

    private static boolean recipeMatches(ServerPlayer player, ResourceLocation id, ItemStack result, Optional<CraftingInput> input) {
        Optional<RecipeHolder<?>> holder = player.server.getRecipeManager().byKey(id);
        if (holder.isEmpty()) {
            return false;
        }
        if (input.isPresent() && holder.get().value() instanceof CraftingRecipe crafting) {
            return crafting.matches(input.get(), player.level());
        }
        ItemStack expected = holder.get().value().getResultItem(player.registryAccess());
        return !expected.isEmpty() && ItemStack.isSameItem(expected, result);
    }
}
