package net.riftwatch.rift_quests.registry;

import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.riftwatch.rift_quests.RiftQuestsMod;
import net.riftwatch.rift_quests.progress.PlayerQuestData;

public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, RiftQuestsMod.MOD_ID);

    public static final Supplier<AttachmentType<PlayerQuestData>> PROGRESS = ATTACHMENTS.register("progress",
            () -> AttachmentType.serializable(PlayerQuestData::new).copyOnDeath().build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }

    public static PlayerQuestData progress(Player player) {
        return player.getData(PROGRESS);
    }
}
