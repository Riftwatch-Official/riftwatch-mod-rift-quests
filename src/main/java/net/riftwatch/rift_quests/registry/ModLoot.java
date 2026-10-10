package net.riftwatch.rift_quests.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.riftwatch.rift_quests.RiftQuestsMod;
import net.riftwatch.rift_quests.loot.BindSealFunction;

public final class ModLoot {
    private static final DeferredRegister<LootItemFunctionType<?>> FUNCTIONS = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, RiftQuestsMod.MOD_ID);

    public static final Supplier<LootItemFunctionType<BindSealFunction>> BIND_SEAL = FUNCTIONS.register("bind_seal",
            () -> new LootItemFunctionType<>(BindSealFunction.CODEC));

    private ModLoot() {
    }

    public static void register(IEventBus modEventBus) {
        FUNCTIONS.register(modEventBus);
    }
}
