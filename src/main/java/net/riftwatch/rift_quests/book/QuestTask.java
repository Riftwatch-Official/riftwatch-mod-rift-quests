package net.riftwatch.rift_quests.book;

import java.util.List;
import java.util.Optional;

public record QuestTask(String key, Optional<Text> title, List<String> replaces, Task task) {
}
