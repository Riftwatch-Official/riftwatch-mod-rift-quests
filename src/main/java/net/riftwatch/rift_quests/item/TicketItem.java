package net.riftwatch.rift_quests.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class TicketItem extends Item {
    public static final String NAME = "Riftwatch Ticket";

    public TicketItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return RainbowText.of(NAME);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.rift_quests.riftwatch_ticket.lore").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.rift_quests.riftwatch_ticket.source").withStyle(ChatFormatting.DARK_GRAY));
    }
}
