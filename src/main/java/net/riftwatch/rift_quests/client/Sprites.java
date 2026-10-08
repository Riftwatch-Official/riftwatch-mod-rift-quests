package net.riftwatch.rift_quests.client;

import net.minecraft.resources.ResourceLocation;
import net.riftwatch.rift_quests.RiftQuestsMod;

public final class Sprites {
    public static final ResourceLocation TOAST = book("toast");
    public static final ResourceLocation HUD = book("hud");

    private Sprites() {
    }

    private static ResourceLocation book(String name) {
        return ResourceLocation.fromNamespaceAndPath(RiftQuestsMod.MOD_ID, "book/" + name);
    }
}
