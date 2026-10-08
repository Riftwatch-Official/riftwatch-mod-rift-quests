package net.riftwatch.rift_quests.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.riftwatch.rift_quests.network.BookView;
import net.riftwatch.rift_quests.network.Payloads;
import net.riftwatch.rift_quests.network.QuestState;

public final class ClientQuests {
    private static BookView book = BookView.EMPTY;
    private static Map<ResourceLocation, Integer> index = Map.of();
    private static Map<ResourceLocation, List<Integer>> byChapter = Map.of();
    private static byte[] states = new byte[0];
    private static Map<Integer, Payloads.QuestEntry> entries = Map.of();
    private static int[] pinned = new int[0];
    private static Payloads.Progress pendingProgress;
    private static int revision;

    private ClientQuests() {
    }

    public static void clear() {
        book = BookView.EMPTY;
        index = Map.of();
        byChapter = Map.of();
        states = new byte[0];
        entries = Map.of();
        pinned = new int[0];
        pendingProgress = null;
        revision++;
    }

    public static void setBook(BookView view) {
        book = view;
        index = view.indexById();
        Map<ResourceLocation, List<Integer>> chapters = new LinkedHashMap<>();
        view.chapters().forEach(chapter -> chapters.put(chapter.id(), new ArrayList<>()));
        for (int position = 0; position < view.quests().size(); position++) {
            chapters.computeIfAbsent(view.quests().get(position).chapter(), id -> new ArrayList<>()).add(position);
        }
        byChapter = chapters;
        states = new byte[0];
        entries = Map.of();
        pinned = new int[0];
        revision++;
        if (pendingProgress != null && pendingProgress.book() == view.id()) {
            Payloads.Progress progress = pendingProgress;
            pendingProgress = null;
            setProgress(progress);
        }
    }

    public static void setProgress(Payloads.Progress progress) {
        if (progress.book() != book.id()) {
            pendingProgress = progress;
            return;
        }
        states = progress.states();
        Map<Integer, Payloads.QuestEntry> map = new HashMap<>();
        progress.entries().forEach(entry -> map.put(entry.quest(), entry));
        entries = map;
        pinned = progress.pinned();
        revision++;
    }

    public static int revision() {
        return revision;
    }

    public static boolean loaded() {
        return book.id() != 0 && !book.quests().isEmpty();
    }

    public static BookView book() {
        return book;
    }

    public static int indexOf(ResourceLocation id) {
        return index.getOrDefault(id, -1);
    }

    public static BookView.QuestView quest(int position) {
        return book.quests().get(position);
    }

    public static List<Integer> questsOf(ResourceLocation chapter) {
        return byChapter.getOrDefault(chapter, List.of());
    }

    public static QuestState state(int position) {
        if (position < 0 || position >= states.length) {
            return QuestState.LOCKED;
        }
        return QuestState.values()[states[position]];
    }

    public static long progress(int position, int task) {
        QuestState state = state(position);
        BookView.QuestView quest = quest(position);
        if (state == QuestState.CLAIMED || state == QuestState.COMPLETE && !entries.containsKey(position)) {
            return quest.tasks().get(task).target();
        }
        Payloads.QuestEntry entry = entries.get(position);
        if (entry == null || task >= entry.progress().length) {
            return 0;
        }
        return entry.progress()[task];
    }

    public static boolean claimed(int position, int reward) {
        if (state(position) == QuestState.CLAIMED) {
            return true;
        }
        Payloads.QuestEntry entry = entries.get(position);
        return entry != null && reward < 31 && (entry.claimedMask() & (1 << reward)) != 0;
    }

    public static boolean waitingForTeam(int position) {
        Payloads.QuestEntry entry = entries.get(position);
        return entry != null && entry.waitingForTeam();
    }

    public static int[] pinned() {
        return pinned;
    }

    public static boolean isPinned(int position) {
        for (int value : pinned) {
            if (value == position) {
                return true;
            }
        }
        return false;
    }

    public static boolean chapterUnlocked(BookView.ChapterView chapter) {
        for (ResourceLocation required : chapter.unlock()) {
            int position = indexOf(required);
            if (position >= 0 && !state(position).done()) {
                return false;
            }
        }
        return true;
    }

    public static int done(ResourceLocation chapter) {
        int count = 0;
        for (int position : questsOf(chapter)) {
            if (state(position).done()) {
                count++;
            }
        }
        return count;
    }

    public static int unclaimed(ResourceLocation chapter) {
        int count = 0;
        for (int position : questsOf(chapter)) {
            if (state(position) == QuestState.COMPLETE) {
                count++;
            }
        }
        return count;
    }

    public static int totalDone() {
        int count = 0;
        for (int position = 0; position < states.length; position++) {
            if (state(position).done()) {
                count++;
            }
        }
        return count;
    }
}
