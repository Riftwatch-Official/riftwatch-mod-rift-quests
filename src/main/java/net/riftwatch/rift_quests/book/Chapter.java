package net.riftwatch.rift_quests.book;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record Chapter(
        ResourceLocation id,
        String file,
        Text title,
        Optional<Text> description,
        Icon icon,
        int order,
        List<ResourceLocation> unlock,
        Optional<ResourceLocation> background) {
}
