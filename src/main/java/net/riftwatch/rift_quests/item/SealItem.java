package net.riftwatch.rift_quests.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class SealItem extends Item {
    private final String slug;

    public SealItem(String slug, Properties properties) {
        super(properties);
        this.slug = slug;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        String key = "item.rift_quests.seal_" + slug;
        lines.add(Component.translatable(key + ".lore_1").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        lines.add(Component.translatable(key + ".lore_2").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
