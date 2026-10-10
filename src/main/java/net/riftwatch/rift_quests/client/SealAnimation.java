package net.riftwatch.rift_quests.client;

import com.mojang.logging.LogUtils;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.riftwatch.rift_quests.RiftQuestsMod;
import net.riftwatch.rift_quests.registry.ModItems;
import org.slf4j.Logger;

public final class SealAnimation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SODIUM_SPRITE_UTIL = "net.caffeinemc.mods.sodium.api.texture.SpriteUtil";
    private static final List<ResourceLocation> SEAL_SPRITES = ModItems.SEAL_DISTRICTS.stream()
            .map(slug -> ResourceLocation.fromNamespaceAndPath(RiftQuestsMod.MOD_ID, "item/seal_" + slug))
            .toList();

    private SealAnimation() {
    }

    public static void init() {
        if (!ModList.get().isLoaded("sodium")) {
            return;
        }
        MethodHandle markActive = sodiumMarkActive();
        if (markActive == null) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, event -> keepSealsActive(markActive));
    }

    private static MethodHandle sodiumMarkActive() {
        try {
            Class<?> spriteUtil = Class.forName(SODIUM_SPRITE_UTIL);
            Object instance = spriteUtil.getField("INSTANCE").get(null);
            return MethodHandles.publicLookup()
                    .findVirtual(spriteUtil, "markSpriteActive", MethodType.methodType(void.class, TextureAtlasSprite.class))
                    .bindTo(instance);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("Sodium sprite API not found, seal animations follow the Sodium setting", exception);
            return null;
        }
    }

    private static void keepSealsActive(MethodHandle markActive) {
        Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        try {
            for (ResourceLocation sprite : SEAL_SPRITES) {
                markActive.invoke(atlas.apply(sprite));
            }
        } catch (Throwable throwable) {
            LOGGER.warn("Could not mark the seal sprites as active", throwable);
        }
    }
}
