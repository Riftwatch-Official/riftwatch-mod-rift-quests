package net.riftwatch.rift_quests.registry;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.riftwatch.rift_quests.RiftQuestsMod;
import net.riftwatch.rift_quests.item.SealOwner;

public final class ModComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RiftQuestsMod.MOD_ID);

    public static final Supplier<DataComponentType<SealOwner>> SEAL_OWNER = COMPONENTS.register("seal_owner",
            () -> DataComponentType.<SealOwner>builder()
                    .persistent(SealOwner.CODEC)
                    .networkSynchronized(SealOwner.STREAM_CODEC)
                    .cacheEncoding()
                    .build());

    private ModComponents() {
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
