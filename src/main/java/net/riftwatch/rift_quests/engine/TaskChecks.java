package net.riftwatch.rift_quests.engine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.riftwatch.rift_quests.book.IdRef;
import net.riftwatch.rift_quests.book.QuestTask;
import net.riftwatch.rift_quests.book.Task;
import net.riftwatch.rift_quests.progress.QuestRecord;

public final class TaskChecks {
    private static final double OBSERVE_RANGE = 16.0;
    private static final int MASK_BITS = 63;

    private TaskChecks() {
    }

    public static long measure(ServerPlayer player, QuestTask questTask, QuestRecord record) {
        return switch (questTask.task()) {
            case Task.Item item -> item.mode() == Task.ItemMode.DETECT ? countItems(player, item) : -1;
            case Task.Advancement advancement -> advancements(player, advancement);
            case Task.Xp xp -> player.experienceLevel;
            case Task.Stat stat -> stat(player, stat, questTask.key(), record);
            case Task.Biome biome -> visit(record, questTask.key(), biome.biomes(), ref -> Matchers.biome(ref, player.serverLevel().getBiome(player.blockPosition())));
            case Task.Structure structure -> structures(player, record, questTask.key(), structure.structures());
            case Task.Dimension dimension -> dimension.dimensions().contains(player.level().dimension().location()) ? 1 : 0;
            case Task.Location location -> location(player, location);
            case Task.Observe observe -> observe(player, observe) ? 1 : 0;
            default -> -1;
        };
    }

    private static long countItems(ServerPlayer player, Task.Item item) {
        Inventory inventory = player.getInventory();
        if (item.distinct()) {
            long found = 0;
            for (IdRef ref : item.items()) {
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                    ItemStack stack = inventory.getItem(slot);
                    if (Matchers.item(ref, stack) && (item.components().isEmpty() || Matchers.components(item.components().get(), stack))) {
                        found++;
                        break;
                    }
                }
            }
            return found;
        }
        long total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (Matchers.item(item.items(), item.components(), stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static long advancements(ServerPlayer player, Task.Advancement task) {
        long done = 0;
        for (ResourceLocation id : task.advancements()) {
            AdvancementHolder holder = player.server.getAdvancements().get(id);
            if (holder != null && player.getAdvancements().getOrStartProgress(holder).isDone()) {
                done++;
            }
        }
        return task.all() ? done : Math.min(done, 1);
    }

    private static long stat(ServerPlayer player, Task.Stat stat, String key, QuestRecord record) {
        Optional<StatType<?>> type = BuiltInRegistries.STAT_TYPE.getOptional(stat.statType());
        if (type.isEmpty()) {
            return -1;
        }
        long value = statValue(player, type.get(), stat.stat());
        if (value < 0) {
            return -1;
        }
        if (stat.retroactive()) {
            return value;
        }
        Map<String, Long> memory = record.memory();
        Long base = memory.get(QuestRecord.BASE + key);
        if (base == null) {
            memory.put(QuestRecord.BASE + key, value);
            return 0;
        }
        return Math.max(0, value - base);
    }

    private static <T> long statValue(ServerPlayer player, StatType<T> type, ResourceLocation id) {
        Optional<T> value = type.getRegistry().getOptional(id);
        if (value.isEmpty()) {
            return -1;
        }
        return player.getStats().getValue(type, value.get());
    }

    private interface RefTest {
        boolean test(IdRef ref);
    }

    private static long visit(QuestRecord record, String key, List<IdRef> refs, RefTest test) {
        long mask = record.memory().getOrDefault(QuestRecord.MASK + key, 0L);
        long before = mask;
        for (int index = 0; index < refs.size(); index++) {
            long bit = 1L << Math.min(index, MASK_BITS - 1);
            if ((mask & bit) == 0 && test.test(refs.get(index))) {
                mask |= bit;
            }
        }
        if (mask != before) {
            record.memory().put(QuestRecord.MASK + key, mask);
        }
        return Long.bitCount(mask);
    }

    private static long structures(ServerPlayer player, QuestRecord record, String key, List<IdRef> refs) {
        ServerLevel level = player.serverLevel();
        BlockPos pos = player.blockPosition();
        Map<Structure, ?> nearby = level.structureManager().getAllStructuresAt(pos);
        if (nearby.isEmpty()) {
            return Long.bitCount(record.memory().getOrDefault(QuestRecord.MASK + key, 0L));
        }
        var registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        return visit(record, key, refs, ref -> {
            for (Structure structure : nearby.keySet()) {
                Holder<Structure> holder = registry.wrapAsHolder(structure);
                if (Matchers.structure(ref, holder) && level.structureManager().getStructureWithPieceAt(pos, structure).isValid()) {
                    return true;
                }
            }
            return false;
        });
    }

    private static long location(ServerPlayer player, Task.Location location) {
        if (!player.level().dimension().location().equals(location.dimension())) {
            return 0;
        }
        double dx = player.getX() - (location.x() + 0.5);
        double dy = player.getY() - location.y();
        double dz = player.getZ() - (location.z() + 0.5);
        return dx * dx + dy * dy + dz * dz <= (double) location.radius() * location.radius() ? 1 : 0;
    }

    private static boolean observe(ServerPlayer player, Task.Observe observe) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(OBSERVE_RANGE));
        if (!observe.entities().isEmpty()) {
            AABB box = player.getBoundingBox().expandTowards(look.scale(OBSERVE_RANGE)).inflate(1.0);
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                    (Entity entity) -> !entity.isSpectator() && Matchers.entity(observe.entities(), entity), OBSERVE_RANGE * OBSERVE_RANGE);
            return hit != null && hit.getType() == HitResult.Type.ENTITY;
        }
        HitResult hit = player.pick(OBSERVE_RANGE, 1.0F, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            return Matchers.block(observe.blocks(), player.level().getBlockState(blockHit.getBlockPos()));
        }
        return false;
    }
}
