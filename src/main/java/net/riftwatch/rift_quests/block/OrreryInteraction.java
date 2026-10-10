package net.riftwatch.rift_quests.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.riftwatch.rift_quests.item.SealItem;
import net.riftwatch.rift_quests.orrery.OrreryLogic;

final class OrreryInteraction {
    private OrreryInteraction() {
    }

    static ItemInteractionResult useItem(ItemStack stack, Level level, BlockPos controller, Player player) {
        if (!(stack.getItem() instanceof SealItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            OrreryLogic.insert(serverPlayer, stack, controller);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    static InteractionResult use(Level level, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            OrreryLogic.showProgress(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
