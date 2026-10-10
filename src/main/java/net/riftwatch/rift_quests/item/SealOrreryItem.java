package net.riftwatch.rift_quests.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.riftwatch.rift_quests.orrery.OrreryStructure;

public final class SealOrreryItem extends BlockItem {
    public SealOrreryItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        if (!OrreryStructure.canPlace(context.getLevel(), context.getClickedPos())) {
            Player player = context.getPlayer();
            if (player != null && !context.getLevel().isClientSide) {
                int[] size = OrreryStructure.size();
                player.displayClientMessage(Component.translatable("message.rift_quests.orrery.blocked", size[0], size[1], size[2]).withStyle(ChatFormatting.RED), true);
            }
            return null;
        }
        return super.getPlacementState(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        int[] size = OrreryStructure.size();
        lines.add(Component.translatable("item.rift_quests.seal_orrery.lore_1", size[0], size[1], size[2]).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        lines.add(Component.translatable("item.rift_quests.seal_orrery.lore_2").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
