package net.riftwatch.rift_quests.client;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class EmiLink {
    private EmiLink() {
    }

    public static boolean available() {
        return ModList.get().isLoaded("emi");
    }

    public static void showRecipes(ItemStack stack) {
        if (available() && !stack.isEmpty()) {
            Bridge.recipes(stack);
        }
    }

    public static void showUses(ItemStack stack) {
        if (available() && !stack.isEmpty()) {
            Bridge.uses(stack);
        }
    }

    private static final class Bridge {
        private Bridge() {
        }

        private static void recipes(ItemStack stack) {
            EmiApi.displayRecipes(EmiStack.of(stack));
        }

        private static void uses(ItemStack stack) {
            EmiApi.displayUses(EmiStack.of(stack));
        }
    }
}
