package net.riftwatch.rift_quests.client;

import net.minecraft.resources.ResourceLocation;
import net.riftwatch.rift_quests.RiftQuestsMod;

public final class Sprites {
    public static final ResourceLocation PANEL_BRASS = book("panel_brass");
    public static final ResourceLocation PANEL_ANDESITE = book("panel_andesite");
    public static final ResourceLocation PANEL_COPPER = book("panel_copper");
    public static final ResourceLocation INSET = book("inset");
    public static final ResourceLocation SLOT = book("slot");
    public static final ResourceLocation CASING_SMALL = book("casing_small");
    public static final ResourceLocation CASING_SMALL_RUST = book("casing_small_rust");
    public static final ResourceLocation CASING_MEDIUM = book("casing_medium");
    public static final ResourceLocation CASING_MEDIUM_RUST = book("casing_medium_rust");
    public static final ResourceLocation COG_RING_WOOD = book("cog_ring_wood");
    public static final ResourceLocation COG_RING_RUST = book("cog_ring_rust");
    public static final ResourceLocation COG_SMALL_WOOD = book("cog_small_wood");
    public static final ResourceLocation COG_SMALL_RUST = book("cog_small_rust");
    public static final ResourceLocation LOCK = book("lock");
    public static final ResourceLocation CHECK = book("check");
    public static final ResourceLocation BANG = book("bang");
    public static final ResourceLocation TEAM = book("team");
    public static final ResourceLocation LEVER_ON = book("lever_on");
    public static final ResourceLocation LEVER_OFF = book("lever_off");
    public static final ResourceLocation LEVER_LOCKED = book("lever_locked");
    public static final ResourceLocation CREST = book("crest");
    public static final ResourceLocation DIAL = book("dial");
    public static final ResourceLocation BUTTON = book("button");
    public static final ResourceLocation BUTTON_HOVER = book("button_hover");
    public static final ResourceLocation BUTTON_DISABLED = book("button_disabled");
    public static final ResourceLocation TOAST = book("toast");
    public static final ResourceLocation HUD = book("hud");

    private static final ResourceLocation[] COG_RING_WARM = {book("cog_ring_warm_0"), book("cog_ring_warm_1"), book("cog_ring_warm_2"), book("cog_ring_warm_3")};
    private static final ResourceLocation[] COG_SMALL_WARM = {book("cog_small_warm_0"), book("cog_small_warm_1"), book("cog_small_warm_2"), book("cog_small_warm_3")};

    private Sprites() {
    }

    private static ResourceLocation book(String name) {
        return ResourceLocation.fromNamespaceAndPath(RiftQuestsMod.MOD_ID, "book/" + name);
    }

    public static ResourceLocation cogRingWarm(int frame) {
        return COG_RING_WARM[Math.floorMod(frame, COG_RING_WARM.length)];
    }

    public static ResourceLocation cogSmallWarm(int frame) {
        return COG_SMALL_WARM[Math.floorMod(frame, COG_SMALL_WARM.length)];
    }
}
