package net.riftwatch.rift_quests.engine;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.riftwatch.rift_quests.book.Chapter;
import net.riftwatch.rift_quests.book.Quest;
import net.riftwatch.rift_quests.book.QuestBook;
import net.riftwatch.rift_quests.network.BookView;

public final class ServerBook {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    private static volatile ServerBook current = new ServerBook(QuestBook.EMPTY, BookView.EMPTY, new byte[0], List.of());

    private final QuestBook book;
    private final BookView view;
    private final byte[] bytes;
    private final List<Quest> quests;
    private final Map<ResourceLocation, Integer> index = new HashMap<>();

    private ServerBook(QuestBook book, BookView view, byte[] bytes, List<Quest> quests) {
        this.book = book;
        this.view = view;
        this.bytes = bytes;
        this.quests = quests;
        for (int position = 0; position < quests.size(); position++) {
            Quest quest = quests.get(position);
            index.put(quest.id(), position);
        }
    }

    public static ServerBook current() {
        return current;
    }

    public static ServerBook rebuild(MinecraftServer server, QuestBook book) {
        List<Quest> ordered = book.quests().values().stream().sorted(Comparator.comparing(quest -> quest.id().toString())).toList();
        int id = NEXT_ID.getAndIncrement();
        BookView view = BookViews.build(id, book, ordered, server);
        byte[] bytes = view.toBytes(server.registryAccess());
        ServerBook built = new ServerBook(book, view, bytes, ordered);
        current = built;
        return built;
    }

    public int id() {
        return view.id();
    }

    public QuestBook book() {
        return book;
    }

    public BookView view() {
        return view;
    }

    public byte[] bytes() {
        return bytes;
    }

    public List<Quest> quests() {
        return quests;
    }

    public Quest quest(ResourceLocation id) {
        return book.quests().get(id);
    }

    public int indexOf(ResourceLocation id) {
        return index.getOrDefault(id, -1);
    }

    public Chapter chapter(ResourceLocation id) {
        return book.chapters().get(id);
    }
}
