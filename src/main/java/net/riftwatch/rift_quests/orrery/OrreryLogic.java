package net.riftwatch.rift_quests.orrery;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.riftwatch.rift_quests.book.Quest;
import net.riftwatch.rift_quests.engine.QuestEngine;
import net.riftwatch.rift_quests.engine.ServerBook;
import net.riftwatch.rift_quests.item.SealItem;
import net.riftwatch.rift_quests.item.SealOwner;
import net.riftwatch.rift_quests.network.ClientBridge;
import net.riftwatch.rift_quests.network.Payloads;
import net.riftwatch.rift_quests.progress.PlayerQuestData;
import net.riftwatch.rift_quests.registry.ModComponents;
import net.riftwatch.rift_quests.registry.ModItems;

public final class OrreryLogic {
    public static final int SEAL_COUNT = ModItems.SEAL_DISTRICTS.size();
    public static final int FULL = (1 << SEAL_COUNT) - 1;
    private static final String QUEST_NAMESPACE = "riftwatch";
    private static final String QUEST_FOLDER = "district_seals/";
    private static final String GATE_QUEST = "all_seals";
    private static final String GATE_TASK = "gate";
    private static final double PILLAR_REACH = 2.6;

    private OrreryLogic() {
    }

    public static int mask(ServerPlayer player) {
        return QuestEngine.data(player).orreryMask();
    }

    public static boolean isOpen(Player player) {
        if (player.level().isClientSide) {
            return ClientBridge.handler().orreryOpen();
        }
        return player instanceof ServerPlayer serverPlayer && mask(serverPlayer) == FULL;
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new Payloads.OrreryState(mask(player)));
    }

    public static void reset(ServerPlayer player) {
        QuestEngine.data(player).clearOrrery();
        sync(player);
    }

    public static void showProgress(ServerPlayer player) {
        int inserted = Integer.bitCount(mask(player));
        Component line = inserted == SEAL_COUNT
                ? Component.translatable("message.rift_quests.orrery.open").withStyle(ChatFormatting.GOLD)
                : Component.translatable("message.rift_quests.orrery.progress", inserted, SEAL_COUNT).withStyle(ChatFormatting.AQUA);
        player.displayClientMessage(line, true);
    }

    public static int pillarAt(BlockPos controller, Vec3 hit) {
        BlockPos origin = OrreryStructure.origin(controller);
        List<OrrerySpec.Socket> sockets = OrrerySpec.get().sockets();
        int nearest = -1;
        double best = PILLAR_REACH * PILLAR_REACH;
        for (int index = 0; index < sockets.size(); index++) {
            double[] centre = sockets.get(index).center();
            double dx = hit.x - (origin.getX() + centre[0]);
            double dz = hit.z - (origin.getZ() + centre[2]);
            double distance = dx * dx + dz * dz;
            if (distance <= best) {
                best = distance;
                nearest = index;
            }
        }
        return nearest;
    }

    public static void insert(ServerPlayer player, ItemStack stack, BlockPos controller, Vec3 hit) {
        if (!(stack.getItem() instanceof SealItem seal)) {
            return;
        }
        int bit = ModItems.SEAL_DISTRICTS.indexOf(seal.slug());
        int pillar = pillarAt(controller, hit);
        if (pillar < 0) {
            refuse(player, "message.rift_quests.orrery.no_pillar");
            return;
        }
        if (pillar != bit) {
            player.displayClientMessage(Component.translatable("message.rift_quests.orrery.wrong_pillar", stack.getHoverName()).withStyle(ChatFormatting.RED), true);
            player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6F, 1.0F);
            return;
        }
        SealOwner owner = stack.get(ModComponents.SEAL_OWNER.get());
        if (owner == null) {
            refuse(player, "message.rift_quests.orrery.unbound");
            return;
        }
        if (!owner.id().equals(player.getUUID())) {
            refuse(player, "message.rift_quests.orrery.not_yours");
            return;
        }
        PlayerQuestData data = QuestEngine.data(player);
        if (!allSealQuestsDone(data)) {
            refuse(player, "message.rift_quests.orrery.quests");
            return;
        }
        if (data.hasOrrerySeal(bit)) {
            player.displayClientMessage(Component.translatable("message.rift_quests.orrery.already", stack.getHoverName()).withStyle(ChatFormatting.RED), true);
            return;
        }
        stack.shrink(1);
        data.insertOrrerySeal(bit);
        sync(player);
        int inserted = Integer.bitCount(data.orreryMask());
        celebrate(player, controller, bit, inserted == SEAL_COUNT);
        if (inserted == SEAL_COUNT) {
            player.displayClientMessage(Component.translatable("message.rift_quests.orrery.open").withStyle(ChatFormatting.GOLD), true);
            completeGate(player);
        } else {
            player.displayClientMessage(Component.translatable("message.rift_quests.orrery.inserted", stack.getHoverName(), inserted, SEAL_COUNT).withStyle(ChatFormatting.AQUA), true);
        }
    }

    private static void refuse(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
        player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6F, 1.0F);
    }

    private static boolean allSealQuestsDone(PlayerQuestData data) {
        ServerBook book = ServerBook.current();
        for (String slug : ModItems.SEAL_DISTRICTS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(QUEST_NAMESPACE, QUEST_FOLDER + slug);
            if (book.quest(id) == null || !data.isCompleted(id)) {
                return false;
            }
        }
        return true;
    }

    private static void completeGate(ServerPlayer player) {
        Quest quest = ServerBook.current().quest(ResourceLocation.fromNamespaceAndPath(QUEST_NAMESPACE, QUEST_FOLDER + GATE_QUEST));
        if (quest != null) {
            QuestEngine.invalidate(player);
            QuestEngine.addCustomProgress(player, quest, GATE_TASK, 1);
        }
    }

    private static void celebrate(ServerPlayer player, BlockPos controller, int bit, boolean last) {
        List<OrrerySpec.Socket> sockets = OrrerySpec.get().sockets();
        double[] centre = sockets.get(bit).center();
        BlockPos origin = OrreryStructure.origin(controller);
        double x = origin.getX() + centre[0];
        double y = origin.getY() + centre[1];
        double z = origin.getZ() + centre[2];
        ServerLevel level = player.serverLevel();
        double scale = OrrerySpec.get().scale();
        level.sendParticles(player, ParticleTypes.END_ROD, false, x, y, z, 14, 0.2 * scale, 0.3 * scale, 0.2 * scale, 0.02);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.8F + 0.05F * bit);
        if (last) {
            double[] core = OrrerySpec.get().core();
            level.sendParticles(player, ParticleTypes.FIREWORK, false, origin.getX() + core[0], origin.getY() + core[1], origin.getZ() + core[2], 120, 0.8 * scale, 0.8 * scale, 0.8 * scale, 0.1);
            player.playNotifySound(SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
