package net.riftwatch.rift_quests.engine;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.riftwatch.rift_quests.book.IdRef;

public final class Matchers {
    private static final Map<JsonElement, Optional<DataComponentPatch>> PATCHES = new ConcurrentHashMap<>();
    private static final Map<String, Optional<CompoundTag>> NBT = new ConcurrentHashMap<>();
    private static volatile MinecraftServer server;

    private Matchers() {
    }

    public static void reset(MinecraftServer current) {
        server = current;
        PATCHES.clear();
        NBT.clear();
    }

    public static boolean item(IdRef ref, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (ref.tag()) {
            return stack.is(TagKey.create(Registries.ITEM, ref.id()));
        }
        return stack.getItemHolder().is(ref.id());
    }

    public static boolean item(List<IdRef> refs, Optional<JsonElement> components, ItemStack stack) {
        for (IdRef ref : refs) {
            if (item(ref, stack)) {
                return components.isEmpty() || components(components.get(), stack);
            }
        }
        return false;
    }

    public static boolean components(JsonElement json, ItemStack stack) {
        Optional<DataComponentPatch> patch = PATCHES.computeIfAbsent(json, key -> {
            MinecraftServer current = server;
            if (current == null) {
                return Optional.empty();
            }
            return DataComponentPatch.CODEC.parse(current.registryAccess().createSerializationContext(JsonOps.INSTANCE), key).result();
        });
        if (patch.isEmpty()) {
            return false;
        }
        for (Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.get().entrySet()) {
            Object actual = stack.get(entry.getKey());
            if (entry.getValue().isPresent() ? !entry.getValue().get().equals(actual) : actual != null) {
                return false;
            }
        }
        return true;
    }

    public static boolean block(List<IdRef> refs, BlockState state) {
        for (IdRef ref : refs) {
            if (ref.tag() ? state.is(TagKey.create(Registries.BLOCK, ref.id())) : state.getBlockHolder().is(ref.id())) {
                return true;
            }
        }
        return false;
    }

    public static boolean entity(List<IdRef> refs, Entity entity) {
        for (IdRef ref : refs) {
            if (ref.tag() ? entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, ref.id())) : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(ref.id())) {
                return true;
            }
        }
        return false;
    }

    public static boolean entityNbt(Optional<String> nbt, Entity entity) {
        if (nbt.isEmpty()) {
            return true;
        }
        Optional<CompoundTag> expected = NBT.computeIfAbsent(nbt.get(), text -> {
            try {
                return Optional.of(TagParser.parseTag(text));
            } catch (CommandSyntaxException exception) {
                return Optional.empty();
            }
        });
        return expected.isPresent() && NbtUtils.compareNbt(expected.get(), entity.saveWithoutId(new CompoundTag()), true);
    }

    public static boolean biome(IdRef ref, Holder<Biome> biome) {
        if (ref.tag()) {
            return biome.is(TagKey.create(Registries.BIOME, ref.id()));
        }
        return biome.is(ResourceKey.create(Registries.BIOME, ref.id()));
    }

    public static boolean structure(IdRef ref, Holder<Structure> structure) {
        if (ref.tag()) {
            return structure.is(TagKey.create(Registries.STRUCTURE, ref.id()));
        }
        return structure.is(ResourceKey.create(Registries.STRUCTURE, ref.id()));
    }
}
