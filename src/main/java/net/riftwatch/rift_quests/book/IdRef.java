package net.riftwatch.rift_quests.book;

import net.minecraft.resources.ResourceLocation;

public record IdRef(ResourceLocation id, boolean tag) {
    @Override
    public String toString() {
        return tag ? "#" + id : id.toString();
    }
}
