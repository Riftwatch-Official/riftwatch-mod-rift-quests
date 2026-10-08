package net.riftwatch.rift_quests.engine;

import net.minecraft.network.chat.Component;
import net.riftwatch.rift_quests.book.Quest;

public final class BookTexts {
    private BookTexts() {
    }

    public static Component title(Quest quest) {
        ServerBook book = ServerBook.current();
        int index = book.indexOf(quest.id());
        if (index < 0) {
            return Component.literal(quest.id().toString());
        }
        return book.view().quests().get(index).title();
    }
}
