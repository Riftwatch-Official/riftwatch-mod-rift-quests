package net.riftwatch.rift_quests.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.riftwatch.rift_quests.RiftQuestsMod;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, RiftQuestsMod.MOD_ID);

    public static final Supplier<SoundEvent> RIFT_MONARCH_MUSIC = SOUNDS.register("music.rift_monarch",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RiftQuestsMod.MOD_ID, "music.rift_monarch")));

    private ModSounds() {
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
