package net.riftwatch.rift_quests.book;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.riftwatch.rift_quests.load.Problem;

public record QuestBook(Map<ResourceLocation, Chapter> chapters, Map<ResourceLocation, Quest> quests, List<Problem> problems) {
    public static final QuestBook EMPTY = new QuestBook(Map.of(), Map.of(), List.of());

    public List<Chapter> orderedChapters() {
        return chapters.values().stream()
                .sorted(Comparator.comparingInt(Chapter::order).thenComparing(chapter -> chapter.id().toString()))
                .toList();
    }

    public long errorCount() {
        return problems.stream().filter(problem -> problem.severity() == Problem.Severity.ERROR).count();
    }

    public long warningCount() {
        return problems.stream().filter(problem -> problem.severity() == Problem.Severity.WARNING).count();
    }
}
