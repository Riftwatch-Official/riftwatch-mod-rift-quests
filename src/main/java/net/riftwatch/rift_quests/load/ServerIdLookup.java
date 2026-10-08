package net.riftwatch.rift_quests.load;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.riftwatch.rift_quests.book.IdRef;

public final class ServerIdLookup implements IdLookup {
    private final MinecraftServer server;
    private final RegistryOps<JsonElement> ops;
    private final Set<ResourceLocation> lootTables;

    public ServerIdLookup(MinecraftServer server) {
        this.server = server;
        this.ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        this.lootTables = new HashSet<>(server.reloadableRegistries().getKeys(Registries.LOOT_TABLE));
    }

    @Override
    public Status status(IdKind kind, IdRef ref) {
        return switch (kind) {
            case ITEM -> registryStatus(server.registryAccess().registryOrThrow(Registries.ITEM), ref);
            case BLOCK -> registryStatus(server.registryAccess().registryOrThrow(Registries.BLOCK), ref);
            case ENTITY -> registryStatus(server.registryAccess().registryOrThrow(Registries.ENTITY_TYPE), ref);
            case BIOME -> registryStatus(server.registryAccess().registryOrThrow(Registries.BIOME), ref);
            case STRUCTURE -> registryStatus(server.registryAccess().registryOrThrow(Registries.STRUCTURE), ref);
            case DIMENSION -> known(server.levelKeys().contains(ResourceKey.create(Registries.DIMENSION, ref.id())));
            case ADVANCEMENT -> known(server.getAdvancements().get(ref.id()) != null);
            case LOOT_TABLE -> known(lootTables.contains(ref.id()));
            case RECIPE -> known(server.getRecipeManager().byKey(ref.id()).isPresent());
            case STAT_TYPE -> known(BuiltInRegistries.STAT_TYPE.containsKey(ref.id()));
            case CUSTOM_STAT -> known(BuiltInRegistries.CUSTOM_STAT.containsKey(ref.id()));
        };
    }

    @Override
    public Optional<String> textError(JsonElement text) {
        return ComponentSerialization.CODEC.parse(ops, text).error().map(error -> error.message());
    }

    @Override
    public Optional<String> componentsError(JsonElement components) {
        return DataComponentPatch.CODEC.parse(ops, components).error().map(error -> error.message());
    }

    @Override
    public Optional<String> nbtError(String nbt) {
        try {
            TagParser.parseTag(nbt);
            return Optional.empty();
        } catch (CommandSyntaxException exception) {
            return Optional.of(exception.getMessage());
        }
    }

    private static <T> Status registryStatus(Registry<T> registry, IdRef ref) {
        if (!ref.tag()) {
            return known(registry.containsKey(ref.id()));
        }
        return registry.getTag(TagKey.create(registry.key(), ref.id()))
                .map(tag -> tag.size() == 0 ? Status.EMPTY_TAG : Status.OK)
                .orElse(Status.MISSING);
    }

    private static Status known(boolean present) {
        return present ? Status.OK : Status.MISSING;
    }
}
