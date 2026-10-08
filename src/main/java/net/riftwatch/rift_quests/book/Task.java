package net.riftwatch.rift_quests.book;

import com.google.gson.JsonElement;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public sealed interface Task {
    enum ItemMode {
        DETECT,
        CONSUME,
        SUBMIT
    }

    enum BlockAction {
        PLACE,
        BREAK,
        INTERACT
    }

    enum EntityAction {
        INTERACT,
        KILL,
        TAME,
        BREED
    }

    record Item(List<IdRef> items, int count, ItemMode mode, Optional<JsonElement> components, boolean distinct) implements Task {
    }

    record Advancement(List<ResourceLocation> advancements, boolean all) implements Task {
    }

    record Block(BlockAction action, List<IdRef> blocks, int count) implements Task {
    }

    record Entity(EntityAction action, List<IdRef> entities, int count, Optional<String> nbt) implements Task {
    }

    record Craft(List<IdRef> items, int count) implements Task {
    }

    record Recipe(List<ResourceLocation> recipes, int count) implements Task {
    }

    record UseItem(List<IdRef> items, int count) implements Task {
    }

    record Biome(List<IdRef> biomes, int count) implements Task {
    }

    record Structure(List<IdRef> structures, int count) implements Task {
    }

    record Dimension(List<ResourceLocation> dimensions) implements Task {
    }

    record Location(ResourceLocation dimension, int x, int y, int z, int radius) implements Task {
    }

    record Stat(ResourceLocation statType, ResourceLocation stat, int value, boolean retroactive) implements Task {
    }

    record Xp(int level, boolean consume) implements Task {
    }

    record Checkmark() implements Task {
    }

    record Observe(List<IdRef> blocks, List<IdRef> entities) implements Task {
    }

    record Custom(int count) implements Task {
    }
}
