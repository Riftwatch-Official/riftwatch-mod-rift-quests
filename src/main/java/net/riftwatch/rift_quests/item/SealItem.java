package net.riftwatch.rift_quests.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.riftwatch.rift_quests.registry.ModComponents;

public final class SealItem extends Item {
    private final String slug;

    public SealItem(String slug, Properties properties) {
        super(properties);
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public ItemStack boundTo(Player player) {
        ItemStack stack = new ItemStack(this);
        stack.set(ModComponents.SEAL_OWNER.get(), SealOwner.of(player));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        String key = "item.rift_quests.seal_" + slug;
        lines.add(Component.translatable(key + ".lore_1").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        lines.add(Component.translatable(key + ".lore_2").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        SealOwner owner = stack.get(ModComponents.SEAL_OWNER.get());
        if (owner == null) {
            lines.add(Component.translatable("item.rift_quests.seal.unbound").withStyle(ChatFormatting.DARK_RED));
        } else {
            lines.add(Component.translatable("item.rift_quests.seal.bound", owner.name()).withStyle(ChatFormatting.AQUA));
        }
    }
}
