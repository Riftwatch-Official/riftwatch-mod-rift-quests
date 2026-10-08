package net.riftwatch.rift_quests.book;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public sealed interface Reward {
    record Item(ItemSpec item, int count) implements Reward {
    }

    record Ticket(int count) implements Reward {
    }

    record Xp(int levels) implements Reward {
    }

    record LootTable(ResourceLocation table, int rolls) implements Reward {
    }

    record Choice(List<Reward> options) implements Reward {
    }

    record Command(String command) implements Reward {
    }

    default boolean givesTickets() {
        return switch (this) {
            case Ticket ticket -> true;
            case Choice choice -> choice.options().stream().anyMatch(Reward::givesTickets);
            default -> false;
        };
    }
}
