package net.riftwatch.rift_quests.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.riftwatch.rift_quests.item.SealOwner;
import net.riftwatch.rift_quests.registry.ModComponents;
import net.riftwatch.rift_quests.registry.ModLoot;

public final class BindSealFunction extends LootItemConditionalFunction {
    public static final MapCodec<BindSealFunction> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance).apply(instance, BindSealFunction::new));

    private BindSealFunction(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof ServerPlayer player)) {
            return ItemStack.EMPTY;
        }
        stack.set(ModComponents.SEAL_OWNER.get(), SealOwner.of(player));
        return stack;
    }

    @Override
    public LootItemFunctionType<BindSealFunction> getType() {
        return ModLoot.BIND_SEAL.get();
    }
}
