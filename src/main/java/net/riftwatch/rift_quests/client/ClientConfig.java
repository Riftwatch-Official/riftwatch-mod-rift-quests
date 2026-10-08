package net.riftwatch.rift_quests.client;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.riftwatch.rift_quests.item.RainbowText;

public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ANIMATED_TICKET_NAME = BUILDER
            .comment("Animate the rainbow name of the Riftwatch Ticket.")
            .define("animatedTicketName", true);
    public static final ModConfigSpec.BooleanValue SHOW_TRACKER = BUILDER
            .comment("Show pinned quests on the screen.")
            .define("showTracker", true);
    public static final ModConfigSpec.BooleanValue SHOW_TOASTS = BUILDER
            .comment("Show a toast when a quest is completed.")
            .define("showToasts", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }

    static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            RainbowText.setAnimated(ANIMATED_TICKET_NAME.get());
        }
    }
}
