package net.riftwatch.rift_quests.load;

import com.google.gson.JsonElement;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.riftwatch.rift_quests.book.IdRef;

final class FakeIdLookup implements IdLookup {
    private final Map<IdKind, Set<String>> known = new EnumMap<>(IdKind.class);

    FakeIdLookup know(IdKind kind, String... ids) {
        known.computeIfAbsent(kind, key -> new HashSet<>()).addAll(Set.of(ids));
        return this;
    }

    static FakeIdLookup standard() {
        return new FakeIdLookup()
                .know(IdKind.ITEM, "minecraft:stone", "minecraft:iron_ingot", "create:cogwheel", "create:andesite_alloy", "#c:ingots", "#c:empty_tag_marker")
                .know(IdKind.BLOCK, "minecraft:stone")
                .know(IdKind.ENTITY, "minecraft:cow")
                .know(IdKind.ADVANCEMENT, "minecraft:story/mine_stone")
                .know(IdKind.DIMENSION, "minecraft:overworld")
                .know(IdKind.STAT_TYPE, "minecraft:custom", "minecraft:mined")
                .know(IdKind.CUSTOM_STAT, "minecraft:jump")
                .know(IdKind.RECIPE, "sophisticatedbackpacks:backpack_dye");
    }

    @Override
    public Status status(IdKind kind, IdRef ref) {
        Set<String> ids = known.getOrDefault(kind, Set.of());
        if (ref.tag() && ref.id().getPath().equals("empty_tag_marker")) {
            return Status.EMPTY_TAG;
        }
        return ids.contains(ref.toString()) ? Status.OK : Status.MISSING;
    }

    @Override
    public Optional<String> textError(JsonElement text) {
        return text.isJsonObject() && text.getAsJsonObject().has("broken") ? Optional.of("broken component") : Optional.empty();
    }

    @Override
    public Optional<String> componentsError(JsonElement components) {
        return components.getAsJsonObject().has("minecraft:unknown") ? Optional.of("unknown component") : Optional.empty();
    }

    @Override
    public Optional<String> nbtError(String nbt) {
        return nbt.startsWith("{") ? Optional.empty() : Optional.of("not a compound");
    }
}
