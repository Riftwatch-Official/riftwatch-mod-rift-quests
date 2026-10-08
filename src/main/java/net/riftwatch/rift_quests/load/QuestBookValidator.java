package net.riftwatch.rift_quests.load;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.riftwatch.rift_quests.book.Chapter;
import net.riftwatch.rift_quests.book.Icon;
import net.riftwatch.rift_quests.book.IdRef;
import net.riftwatch.rift_quests.book.Quest;
import net.riftwatch.rift_quests.book.QuestBook;
import net.riftwatch.rift_quests.book.QuestReward;
import net.riftwatch.rift_quests.book.QuestTask;
import net.riftwatch.rift_quests.book.Reward;
import net.riftwatch.rift_quests.book.Task;
import net.riftwatch.rift_quests.book.Text;

public final class QuestBookValidator {
    private static final Map<String, IdKind> STAT_TYPES = Map.of(
            "minecraft:custom", IdKind.CUSTOM_STAT,
            "minecraft:mined", IdKind.BLOCK,
            "minecraft:crafted", IdKind.ITEM,
            "minecraft:used", IdKind.ITEM,
            "minecraft:broken", IdKind.ITEM,
            "minecraft:picked_up", IdKind.ITEM,
            "minecraft:dropped", IdKind.ITEM,
            "minecraft:killed", IdKind.ENTITY,
            "minecraft:killed_by", IdKind.ENTITY);

    private final IdLookup lookup;
    private final Problems problems;

    private QuestBookValidator(IdLookup lookup, Problems problems) {
        this.lookup = lookup;
        this.problems = problems;
    }

    public static QuestBook build(Map<ResourceLocation, JsonElement> files, List<Problem> loadProblems, IdLookup lookup) {
        Problems problems = new Problems();
        problems.addAll(loadProblems);
        QuestBookParser.Parsed parsed = QuestBookParser.parse(files, problems);
        return new QuestBookValidator(lookup, problems).validate(parsed);
    }

    private QuestBook validate(QuestBookParser.Parsed parsed) {
        Map<ResourceLocation, Chapter> chapters = new LinkedHashMap<>();
        for (Chapter chapter : parsed.chapters().values()) {
            if (checkChapter(chapter)) {
                chapters.put(chapter.id(), chapter);
            }
        }
        Map<ResourceLocation, Quest> quests = new LinkedHashMap<>();
        for (Quest quest : parsed.quests().values()) {
            if (checkQuest(quest)) {
                quests.put(quest.id(), quest);
            }
        }
        checkReplacedQuests(parsed, quests);
        resolveReferences(parsed, chapters, quests);
        warnLayout(chapters, quests);
        return new QuestBook(Map.copyOf(chapters), Map.copyOf(quests), problems.list());
    }

    private boolean checkChapter(Chapter chapter) {
        int before = problems.errorCount();
        String file = chapter.file();
        checkText(file, "title", chapter.title());
        chapter.description().ifPresent(text -> checkText(file, "description", text));
        checkIcon(file, chapter.icon());
        return problems.errorCount() == before;
    }

    private boolean checkQuest(Quest quest) {
        int before = problems.errorCount();
        String file = quest.file();
        checkText(file, "title", quest.title());
        quest.subtitle().ifPresent(text -> checkText(file, "subtitle", text));
        quest.description().ifPresent(text -> checkText(file, "description", text));
        checkIcon(file, quest.icon());
        for (QuestTask task : quest.tasks()) {
            String field = "tasks." + task.key();
            task.title().ifPresent(text -> checkText(file, field + ".title", text));
            checkTask(file, field, task.task());
        }
        checkReplacedKeys(file, "tasks", quest.tasks().stream().map(QuestTask::key).toList(),
                quest.tasks().stream().map(QuestTask::replaces).toList());
        for (QuestReward reward : quest.rewards()) {
            checkReward(file, "rewards." + reward.key(), reward.reward());
        }
        checkReplacedKeys(file, "rewards", quest.rewards().stream().map(QuestReward::key).toList(),
                quest.rewards().stream().map(QuestReward::replaces).toList());
        quest.teamRepeatRewards().ifPresent(rewards -> {
            for (QuestReward reward : rewards) {
                String field = "team_repeat_rewards." + reward.key();
                checkReward(file, field, reward.reward());
                if (reward.reward().givesTickets()) {
                    problems.error(file, field, "team repeat rewards never give Riftwatch Tickets");
                }
            }
        });
        if (quest.checkmarkOnly() && quest.givesTickets()) {
            problems.error(file, "rewards", "a quest with only checkmark tasks must not give Riftwatch Tickets"
                    + (quest.defaultRewards() && quest.size().defaultTickets() > 0 ? " (the " + quest.size().name().toLowerCase(Locale.ROOT)
                    + " size adds tickets, use size small or default_rewards false)" : ""));
        }
        return problems.errorCount() == before;
    }

    private void checkReplacedKeys(String file, String field, List<String> keys, List<List<String>> replaced) {
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < keys.size(); index++) {
            for (String old : replaced.get(index)) {
                String at = field + "." + keys.get(index) + ".replaces";
                if (keys.contains(old)) {
                    problems.error(file, at, "\"" + old + "\" still exists in this quest");
                } else if (!seen.add(old)) {
                    problems.error(file, at, "\"" + old + "\" is replaced twice");
                }
            }
        }
    }

    private void checkTask(String file, String field, Task task) {
        switch (task) {
            case Task.Item item -> {
                checkRefs(file, field + ".items", IdKind.ITEM, item.items());
                item.components().ifPresent(components -> checkComponents(file, field + ".components", components));
            }
            case Task.Advancement advancement -> checkIds(file, field + ".advancements", IdKind.ADVANCEMENT, advancement.advancements());
            case Task.Block block -> checkRefs(file, field + ".blocks", IdKind.BLOCK, block.blocks());
            case Task.Entity entity -> {
                checkRefs(file, field + ".entities", IdKind.ENTITY, entity.entities());
                entity.nbt().ifPresent(nbt -> lookup.nbtError(nbt).ifPresent(error -> problems.error(file, field + ".nbt", error)));
            }
            case Task.Craft craft -> checkRefs(file, field + ".items", IdKind.ITEM, craft.items());
            case Task.UseItem use -> checkRefs(file, field + ".items", IdKind.ITEM, use.items());
            case Task.Recipe recipe -> checkIds(file, field + ".recipes", IdKind.RECIPE, recipe.recipes());
            case Task.Biome biome -> checkRefs(file, field + ".biomes", IdKind.BIOME, biome.biomes());
            case Task.Structure structure -> checkRefs(file, field + ".structures", IdKind.STRUCTURE, structure.structures());
            case Task.Dimension dimension -> checkIds(file, field + ".dimensions", IdKind.DIMENSION, dimension.dimensions());
            case Task.Location location -> checkIds(file, field + ".dimension", IdKind.DIMENSION, List.of(location.dimension()));
            case Task.Stat stat -> checkStat(file, field, stat);
            case Task.Observe observe -> {
                checkRefs(file, field + ".blocks", IdKind.BLOCK, observe.blocks());
                checkRefs(file, field + ".entities", IdKind.ENTITY, observe.entities());
            }
            case Task.Xp xp -> {
            }
            case Task.Checkmark checkmark -> {
            }
            case Task.Custom custom -> {
            }
        }
    }

    private void checkStat(String file, String field, Task.Stat stat) {
        if (!checkIds(file, field + ".stat_type", IdKind.STAT_TYPE, List.of(stat.statType()))) {
            return;
        }
        IdKind kind = STAT_TYPES.get(stat.statType().toString());
        if (kind == null) {
            problems.warning(file, field + ".stat", "the statistic type " + stat.statType() + " is not known to the validator, the statistic is not checked");
            return;
        }
        checkIds(file, field + ".stat", kind, List.of(stat.stat()));
    }

    private void checkReward(String file, String field, Reward reward) {
        switch (reward) {
            case Reward.Item item -> {
                checkIds(file, field + ".item", IdKind.ITEM, List.of(item.item().item()));
                item.item().components().ifPresent(components -> checkComponents(file, field + ".components", components));
            }
            case Reward.LootTable table -> checkIds(file, field + ".table", IdKind.LOOT_TABLE, List.of(table.table()));
            case Reward.Choice choice -> {
                for (int index = 0; index < choice.options().size(); index++) {
                    checkReward(file, field + ".options[" + index + "]", choice.options().get(index));
                }
            }
            case Reward.Ticket ticket -> {
            }
            case Reward.Xp xp -> {
            }
            case Reward.Command command -> {
            }
        }
    }

    private void checkText(String file, String field, Text text) {
        lookup.textError(text.json()).ifPresent(error -> problems.error(file, field, "broken text component: " + error));
    }

    private void checkComponents(String file, String field, JsonElement components) {
        lookup.componentsError(components).ifPresent(error -> problems.error(file, field, "broken item components: " + error));
    }

    private void checkIcon(String file, Icon icon) {
        checkRefs(file, "icon", IdKind.ITEM, List.of(icon.item()));
        icon.components().ifPresent(components -> checkComponents(file, "icon.components", components));
    }

    private boolean checkIds(String file, String field, IdKind kind, List<ResourceLocation> ids) {
        return checkRefs(file, field, kind, ids.stream().map(id -> new IdRef(id, false)).toList());
    }

    private boolean checkRefs(String file, String field, IdKind kind, List<IdRef> refs) {
        boolean valid = true;
        for (IdRef ref : refs) {
            if (ref.tag() && !kind.taggable()) {
                problems.error(file, field, "a " + kind.label() + " cannot be a tag");
                valid = false;
                continue;
            }
            switch (lookup.status(kind, ref)) {
                case MISSING -> {
                    problems.error(file, field, "unknown " + kind.label() + (ref.tag() ? " tag " : " ") + ref);
                    valid = false;
                }
                case EMPTY_TAG -> {
                    problems.error(file, field, "the " + kind.label() + " tag " + ref + " is empty");
                    valid = false;
                }
                case OK -> {
                }
            }
        }
        return valid;
    }

    private void checkReplacedQuests(QuestBookParser.Parsed parsed, Map<ResourceLocation, Quest> quests) {
        Map<ResourceLocation, List<Quest>> claims = new HashMap<>();
        for (Quest quest : parsed.quests().values()) {
            for (ResourceLocation old : quest.replaces()) {
                claims.computeIfAbsent(old, key -> new ArrayList<>()).add(quest);
            }
        }
        for (Quest quest : parsed.quests().values()) {
            for (ResourceLocation old : quest.replaces()) {
                if (parsed.questFiles().contains(old)) {
                    problems.error(quest.file(), "replaces", old + " still exists, a replaced quest must be deleted");
                    quests.remove(quest.id());
                } else if (claims.get(old).size() > 1) {
                    problems.error(quest.file(), "replaces", old + " is also replaced by "
                            + claims.get(old).stream().filter(other -> other != quest).map(other -> other.id().toString()).toList());
                    quests.remove(quest.id());
                }
            }
        }
    }

    private void resolveReferences(QuestBookParser.Parsed parsed, Map<ResourceLocation, Chapter> chapters, Map<ResourceLocation, Quest> quests) {
        boolean changed = true;
        while (changed) {
            changed = dropBrokenReferences(parsed, chapters, quests);
            if (!changed) {
                changed = dropCycles(chapters, quests);
            }
        }
    }

    private boolean dropBrokenReferences(QuestBookParser.Parsed parsed, Map<ResourceLocation, Chapter> chapters, Map<ResourceLocation, Quest> quests) {
        boolean changed = false;
        for (Chapter chapter : List.copyOf(chapters.values())) {
            for (ResourceLocation quest : chapter.unlock()) {
                if (!quests.containsKey(quest)) {
                    problems.error(chapter.file(), "unlock", describeMissing(parsed, quest));
                    chapters.remove(chapter.id());
                    changed = true;
                    break;
                }
            }
        }
        for (Quest quest : List.copyOf(quests.values())) {
            if (!chapters.containsKey(quest.chapter())) {
                problems.error(quest.file(), "", parsed.chapters().containsKey(quest.chapter())
                        ? "the chapter " + quest.chapter() + " was skipped because of its own errors"
                        : "the chapter " + quest.chapter() + " does not exist (chapters/" + quest.chapter().getPath() + ".json)");
                quests.remove(quest.id());
                changed = true;
                continue;
            }
            for (ResourceLocation dependency : quest.dependencies()) {
                if (!quests.containsKey(dependency)) {
                    problems.error(quest.file(), "dependencies", describeMissing(parsed, dependency));
                    quests.remove(quest.id());
                    changed = true;
                    break;
                }
            }
        }
        return changed;
    }

    private static String describeMissing(QuestBookParser.Parsed parsed, ResourceLocation quest) {
        return parsed.questFiles().contains(quest)
                ? "the quest " + quest + " was skipped because of its own errors"
                : "the quest " + quest + " does not exist";
    }

    private boolean dropCycles(Map<ResourceLocation, Chapter> chapters, Map<ResourceLocation, Quest> quests) {
        Map<ResourceLocation, List<ResourceLocation>> edges = new HashMap<>();
        for (Quest quest : quests.values()) {
            List<ResourceLocation> targets = new ArrayList<>(quest.dependencies());
            for (ResourceLocation unlock : chapters.get(quest.chapter()).unlock()) {
                if (!targets.contains(unlock)) {
                    targets.add(unlock);
                }
            }
            edges.put(quest.id(), targets);
        }
        Map<ResourceLocation, Integer> state = new HashMap<>();
        for (ResourceLocation start : quests.keySet().stream().sorted().toList()) {
            Optional<List<ResourceLocation>> cycle = findCycle(start, edges, state, new ArrayList<>());
            if (cycle.isPresent()) {
                String path = String.join(" -> ", cycle.get().stream().map(ResourceLocation::toString).toList());
                for (ResourceLocation member : new HashSet<>(cycle.get())) {
                    problems.error(quests.get(member).file(), "dependencies", "dependency cycle (dependencies and chapter unlocks): " + path);
                    quests.remove(member);
                }
                return true;
            }
        }
        return false;
    }

    private static Optional<List<ResourceLocation>> findCycle(ResourceLocation node, Map<ResourceLocation, List<ResourceLocation>> edges,
            Map<ResourceLocation, Integer> state, List<ResourceLocation> stack) {
        int current = state.getOrDefault(node, 0);
        if (current == 2) {
            return Optional.empty();
        }
        if (current == 1) {
            List<ResourceLocation> cycle = new ArrayList<>(stack.subList(stack.indexOf(node), stack.size()));
            cycle.add(node);
            return Optional.of(cycle);
        }
        state.put(node, 1);
        stack.add(node);
        for (ResourceLocation next : edges.getOrDefault(node, List.of())) {
            Optional<List<ResourceLocation>> cycle = findCycle(next, edges, state, stack);
            if (cycle.isPresent()) {
                return cycle;
            }
        }
        stack.removeLast();
        state.put(node, 2);
        return Optional.empty();
    }

    private void warnLayout(Map<ResourceLocation, Chapter> chapters, Map<ResourceLocation, Quest> quests) {
        Map<ResourceLocation, Map<Long, Quest>> positions = new HashMap<>();
        for (Quest quest : quests.values().stream().sorted((a, b) -> a.id().compareTo(b.id())).toList()) {
            long key = ((long) quest.x() << 32) ^ (quest.y() & 0xffffffffL);
            Quest other = positions.computeIfAbsent(quest.chapter(), chapter -> new HashMap<>()).putIfAbsent(key, quest);
            if (other != null) {
                problems.warning(quest.file(), "position", "same position as " + other.id());
            }
        }
        for (Chapter chapter : chapters.values()) {
            if (!positions.containsKey(chapter.id())) {
                problems.warning(chapter.file(), "", "the chapter has no quests");
            }
        }
    }
}
