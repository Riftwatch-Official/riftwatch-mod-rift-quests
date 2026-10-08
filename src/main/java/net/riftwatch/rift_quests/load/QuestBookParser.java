package net.riftwatch.rift_quests.load;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import net.riftwatch.rift_quests.book.Chapter;
import net.riftwatch.rift_quests.book.DependencyMode;
import net.riftwatch.rift_quests.book.Icon;
import net.riftwatch.rift_quests.book.IdRef;
import net.riftwatch.rift_quests.book.ItemSpec;
import net.riftwatch.rift_quests.book.Quest;
import net.riftwatch.rift_quests.book.QuestReward;
import net.riftwatch.rift_quests.book.QuestSize;
import net.riftwatch.rift_quests.book.QuestTask;
import net.riftwatch.rift_quests.book.Reward;
import net.riftwatch.rift_quests.book.Task;
import net.riftwatch.rift_quests.book.TeamRule;
import net.riftwatch.rift_quests.book.Text;
import net.riftwatch.rift_quests.book.Visibility;

public final class QuestBookParser {
    public static final String DIRECTORY = "rift_quests";
    private static final Pattern KEY = Pattern.compile("[a-z0-9_]{1,32}");
    private static final int MAX_COUNT = 100_000;
    private static final int MAX_POSITION = 10_000;

    public record Parsed(Map<ResourceLocation, Chapter> chapters, Map<ResourceLocation, Quest> quests, Set<ResourceLocation> questFiles) {
    }

    private QuestBookParser() {
    }

    public static String displayPath(ResourceLocation fileId) {
        return "data/" + fileId.getNamespace() + "/" + DIRECTORY + "/" + fileId.getPath() + ".json";
    }

    public static Parsed parse(Map<ResourceLocation, JsonElement> files, Problems problems) {
        Map<ResourceLocation, Chapter> chapters = new LinkedHashMap<>();
        Map<ResourceLocation, Quest> quests = new LinkedHashMap<>();
        Set<ResourceLocation> questFiles = new HashSet<>();
        files.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    ResourceLocation fileId = entry.getKey();
                    String file = displayPath(fileId);
                    String[] segments = fileId.getPath().split("/");
                    if (segments.length == 2 && segments[0].equals("chapters")) {
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), segments[1]);
                        parseChapter(id, file, entry.getValue(), problems).ifPresent(chapter -> chapters.put(id, chapter));
                    } else if (segments.length == 3 && segments[0].equals("quests")) {
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), segments[1] + "/" + segments[2]);
                        ResourceLocation chapter = ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), segments[1]);
                        questFiles.add(id);
                        parseQuest(id, chapter, file, entry.getValue(), problems).ifPresent(quest -> quests.put(id, quest));
                    } else {
                        problems.error(file, "", "a quest book file must be chapters/<chapter>.json or quests/<chapter>/<quest>.json");
                    }
                });
        return new Parsed(chapters, quests, Set.copyOf(questFiles));
    }

    private static Optional<Chapter> parseChapter(ResourceLocation id, String file, JsonElement json, Problems problems) {
        int errorsBefore = problems.errorCount();
        Optional<Fields> maybeFields = Fields.of(json, file, "", problems);
        if (maybeFields.isEmpty()) {
            return Optional.empty();
        }
        Fields fields = maybeFields.get();
        Optional<Text> title = fields.text("title", true);
        Optional<Text> description = fields.text("description", false);
        Optional<Icon> icon = icon(fields, "icon", true);
        Optional<Integer> order = fields.integer("order", true, -10_000, 10_000);
        List<ResourceLocation> unlock = questRefs(fields, "unlock", id.getNamespace(), null);
        Optional<ResourceLocation> background = fields.id("background", false);
        fields.finish();
        if (problems.errorCount() > errorsBefore) {
            return Optional.empty();
        }
        return Optional.of(new Chapter(id, file, title.orElseThrow(), description, icon.orElseThrow(), order.orElseThrow(), unlock, background));
    }

    private static Optional<Quest> parseQuest(ResourceLocation id, ResourceLocation chapter, String file, JsonElement json, Problems problems) {
        int errorsBefore = problems.errorCount();
        Optional<Fields> maybeFields = Fields.of(json, file, "", problems);
        if (maybeFields.isEmpty()) {
            return Optional.empty();
        }
        Fields fields = maybeFields.get();
        String chapterPath = chapter.getPath();
        Optional<Text> title = fields.text("title", true);
        Optional<Text> subtitle = fields.text("subtitle", false);
        Optional<Text> description = fields.text("description", false);
        Optional<Icon> icon = icon(fields, "icon", true);
        Optional<QuestSize> size = fields.enumValue("size", QuestSize.class, true);
        Optional<int[]> position = position(fields);
        List<ResourceLocation> dependencies = questRefs(fields, "dependencies", id.getNamespace(), chapterPath);
        DependencyMode dependencyMode = fields.enumValue("dependency_mode", DependencyMode.class, false).orElse(DependencyMode.ALL);
        Visibility visibility = fields.enumValue("visibility", Visibility.class, false).orElse(Visibility.VISIBLE);
        List<QuestTask> tasks = tasks(fields);
        List<QuestReward> rewards = rewards(fields, "rewards", false);
        boolean defaultRewards = fields.bool("default_rewards").orElse(true);
        Optional<TeamRule> team = fields.object("team", false).flatMap(QuestBookParser::team);
        Optional<List<QuestReward>> teamRepeatRewards = Optional.empty();
        if (fields.has("team_repeat_rewards")) {
            if (!fields.has("team")) {
                fields.raw("team_repeat_rewards", false);
                fields.error("team_repeat_rewards", "is only allowed on a team quest");
            } else {
                teamRepeatRewards = Optional.of(rewards(fields, "team_repeat_rewards", true));
            }
        }
        OptionalInt cooldown = OptionalInt.empty();
        Optional<Fields> repeatable = fields.object("repeatable", false);
        if (repeatable.isPresent()) {
            Optional<Integer> minutes = repeatable.get().integer("cooldown_minutes", true, 1, 525_600);
            repeatable.get().finish();
            if (minutes.isPresent()) {
                cooldown = OptionalInt.of(minutes.get());
            }
        }
        List<ResourceLocation> replaces = questRefs(fields, "replaces", id.getNamespace(), chapterPath);
        if (dependencies.contains(id)) {
            fields.error("dependencies", "a quest cannot depend on itself");
        }
        fields.finish();
        if (problems.errorCount() > errorsBefore) {
            return Optional.empty();
        }
        return Optional.of(new Quest(id, chapter, file, title.orElseThrow(), subtitle, description, icon.orElseThrow(), size.orElseThrow(),
                position.orElseThrow()[0], position.orElseThrow()[1], dependencies, dependencyMode, visibility, tasks, rewards, defaultRewards,
                team, teamRepeatRewards, cooldown, replaces));
    }

    private static Optional<int[]> position(Fields fields) {
        Optional<JsonArray> array = fields.array("position", true);
        if (array.isEmpty()) {
            return Optional.empty();
        }
        if (array.get().size() != 2) {
            fields.error("position", "must be [x, y]");
            return Optional.empty();
        }
        Optional<Integer> x = fields.asInteger(array.get().get(0), fields.at("position") + "[0]", -MAX_POSITION, MAX_POSITION);
        Optional<Integer> y = fields.asInteger(array.get().get(1), fields.at("position") + "[1]", -MAX_POSITION, MAX_POSITION);
        if (x.isEmpty() || y.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new int[] {x.get(), y.get()});
    }

    private static Optional<TeamRule> team(Fields fields) {
        Optional<Integer> minPlayers = fields.integer("min_players", true, 2, 16);
        int radius = fields.integer("radius", false, 1, 256).orElse(32);
        fields.finish();
        return minPlayers.map(players -> new TeamRule(players, radius));
    }

    static List<ResourceLocation> questRefs(Fields fields, String key, String namespace, String chapterPath) {
        Optional<JsonArray> array = fields.array(key, false);
        if (array.isEmpty()) {
            return List.of();
        }
        List<ResourceLocation> result = new ArrayList<>();
        for (int index = 0; index < array.get().size(); index++) {
            String field = fields.at(key) + "[" + index + "]";
            JsonElement element = array.get().get(index);
            if (!(element instanceof JsonPrimitive primitive) || !primitive.isString()) {
                fields.problems().error(fields.file(), field, "must be a quest id");
                continue;
            }
            Optional<ResourceLocation> id = resolveQuestRef(primitive.getAsString(), namespace, chapterPath);
            if (id.isEmpty()) {
                fields.problems().error(fields.file(), field, "\"" + primitive.getAsString() + "\" is not a quest id (<chapter>/<quest>, optionally with a namespace)");
            } else if (result.contains(id.get())) {
                fields.problems().error(fields.file(), field, id.get() + " is listed twice");
            } else {
                result.add(id.get());
            }
        }
        return List.copyOf(result);
    }

    public static Optional<ResourceLocation> resolveQuestRef(String raw, String namespace, String chapterPath) {
        String full;
        if (raw.contains(":")) {
            full = raw;
        } else if (raw.contains("/")) {
            full = namespace + ":" + raw;
        } else if (chapterPath != null) {
            full = namespace + ":" + chapterPath + "/" + raw;
        } else {
            return Optional.empty();
        }
        ResourceLocation id = ResourceLocation.tryParse(full);
        if (id == null || id.getPath().split("/", -1).length != 2 || id.getPath().startsWith("/") || id.getPath().endsWith("/")) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    private static Optional<Icon> icon(Fields fields, String key, boolean required) {
        Optional<JsonElement> element = fields.raw(key, required);
        if (element.isEmpty()) {
            return Optional.empty();
        }
        if (element.get().isJsonObject()) {
            Fields icon = fields.object(key, true).orElseThrow();
            Optional<IdRef> item = icon.ref("item", true, true);
            Optional<JsonObject> components = icon.objectValue("components", false);
            icon.finish();
            if (item.isPresent() && item.get().tag() && components.isPresent()) {
                icon.error("components", "is not allowed with a tag");
                return Optional.empty();
            }
            return item.map(ref -> new Icon(ref, components.map(value -> (JsonElement) value)));
        }
        return fields.parseRef(element.get(), fields.at(key), true).map(ref -> new Icon(ref, Optional.empty()));
    }

    private static List<QuestTask> tasks(Fields fields) {
        Optional<JsonObject> object = fields.objectValue("tasks", true);
        if (object.isEmpty()) {
            return List.of();
        }
        if (object.get().isEmpty()) {
            fields.error("tasks", "a quest needs at least one task");
            return List.of();
        }
        List<QuestTask> result = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.get().entrySet()) {
            String field = fields.at("tasks") + "." + entry.getKey();
            if (!KEY.matcher(entry.getKey()).matches()) {
                fields.problems().error(fields.file(), field, "task keys must match [a-z0-9_]{1,32}");
                continue;
            }
            Fields.of(entry.getValue(), fields.file(), field, fields.problems())
                    .flatMap(task -> task(entry.getKey(), task))
                    .ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    private static Optional<QuestTask> task(String key, Fields fields) {
        int errorsBefore = fields.problems().errorCount();
        Optional<String> type = fields.string("type", true);
        Optional<Text> title = fields.text("title", false);
        List<String> replaces = fields.strings("replaces").orElse(List.of());
        Optional<Task> task = type.flatMap(value -> taskBody(value, fields));
        fields.finish();
        if (fields.problems().errorCount() > errorsBefore || task.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new QuestTask(key, title, replaces, task.get()));
    }

    private static Optional<Task> taskBody(String type, Fields fields) {
        return switch (type) {
            case "item" -> itemTask(fields);
            case "advancement" -> {
                Optional<List<ResourceLocation>> advancements = fields.ids("advancement", "advancements", true);
                boolean all = fields.enumValue("mode", AllOrAny.class, false).orElse(AllOrAny.ALL) == AllOrAny.ALL;
                yield advancements.map(ids -> new Task.Advancement(ids, all));
            }
            case "place_block" -> blockTask(fields, Task.BlockAction.PLACE);
            case "break_block" -> blockTask(fields, Task.BlockAction.BREAK);
            case "interact_block" -> blockTask(fields, Task.BlockAction.INTERACT);
            case "interact_entity" -> entityTask(fields, Task.EntityAction.INTERACT);
            case "kill_entity" -> entityTask(fields, Task.EntityAction.KILL);
            case "tame_entity" -> entityTask(fields, Task.EntityAction.TAME);
            case "breed_entity" -> entityTask(fields, Task.EntityAction.BREED);
            case "craft" -> {
                Optional<List<IdRef>> items = fields.refs("item", "items", true, true);
                int count = count(fields);
                yield items.map(list -> new Task.Craft(list, count));
            }
            case "use_item" -> {
                Optional<List<IdRef>> items = fields.refs("item", "items", true, true);
                int count = count(fields);
                yield items.map(list -> new Task.UseItem(list, count));
            }
            case "recipe" -> {
                Optional<List<ResourceLocation>> recipes = fields.ids("recipe", "recipes", true);
                int count = count(fields);
                yield recipes.map(list -> new Task.Recipe(list, count));
            }
            case "biome" -> {
                Optional<List<IdRef>> biomes = fields.refs("biome", "biomes", true, true);
                int count = count(fields);
                yield biomes.map(list -> new Task.Biome(list, count));
            }
            case "structure" -> {
                Optional<List<IdRef>> structures = fields.refs("structure", "structures", true, true);
                int count = count(fields);
                yield structures.map(list -> new Task.Structure(list, count));
            }
            case "dimension" -> fields.ids("dimension", "dimensions", true).map(Task.Dimension::new);
            case "location" -> {
                Optional<ResourceLocation> dimension = fields.id("dimension", true);
                Optional<Integer> x = fields.integer("x", true, -30_000_000, 30_000_000);
                Optional<Integer> y = fields.integer("y", true, -2_048, 2_048);
                Optional<Integer> z = fields.integer("z", true, -30_000_000, 30_000_000);
                Optional<Integer> radius = fields.integer("radius", true, 1, 1_000);
                if (dimension.isEmpty() || x.isEmpty() || y.isEmpty() || z.isEmpty() || radius.isEmpty()) {
                    yield Optional.empty();
                }
                yield Optional.of(new Task.Location(dimension.get(), x.get(), y.get(), z.get(), radius.get()));
            }
            case "stat" -> {
                Optional<ResourceLocation> statType = fields.id("stat_type", true);
                Optional<ResourceLocation> stat = fields.id("stat", true);
                Optional<Integer> value = fields.integer("value", true, 1, Integer.MAX_VALUE);
                boolean retroactive = fields.bool("retroactive").orElse(false);
                if (statType.isEmpty() || stat.isEmpty() || value.isEmpty()) {
                    yield Optional.empty();
                }
                yield Optional.of(new Task.Stat(statType.get(), stat.get(), value.get(), retroactive));
            }
            case "xp" -> {
                Optional<Integer> level = fields.integer("level", true, 1, 10_000);
                boolean consume = fields.bool("consume").orElse(false);
                yield level.map(value -> new Task.Xp(value, consume));
            }
            case "checkmark" -> Optional.of(new Task.Checkmark());
            case "observe" -> {
                boolean blocks = fields.has("block") || fields.has("blocks");
                boolean entities = fields.has("entity") || fields.has("entities");
                if (blocks == entities) {
                    fields.error(null, "an observe task needs either blocks or entities");
                    yield Optional.empty();
                }
                if (blocks) {
                    yield fields.refs("block", "blocks", true, true).map(list -> new Task.Observe(list, List.of()));
                }
                yield fields.refs("entity", "entities", true, true).map(list -> new Task.Observe(List.of(), list));
            }
            case "custom" -> Optional.of(new Task.Custom(count(fields)));
            default -> {
                fields.error("type", "unknown task type \"" + type + "\"");
                yield Optional.empty();
            }
        };
    }

    private static Optional<Task> itemTask(Fields fields) {
        Optional<List<IdRef>> items = fields.refs("item", "items", true, true);
        int count = count(fields);
        Task.ItemMode mode = fields.enumValue("mode", Task.ItemMode.class, false).orElse(Task.ItemMode.DETECT);
        Optional<JsonObject> components = fields.objectValue("components", false);
        Optional<Boolean> distinct = fields.bool("distinct");
        if (items.isEmpty()) {
            return Optional.empty();
        }
        if (components.isPresent() && (items.get().size() != 1 || items.get().getFirst().tag())) {
            fields.error("components", "is only allowed with a single item, not with a tag or a list");
            return Optional.empty();
        }
        if (distinct.orElse(false) && items.get().size() < 2) {
            fields.error("distinct", "needs a list of at least two items");
            return Optional.empty();
        }
        if (distinct.orElse(false) && count > items.get().size()) {
            fields.error("count", "cannot be larger than the number of listed items when distinct is true");
            return Optional.empty();
        }
        return Optional.of(new Task.Item(items.get(), count, mode, components.map(value -> (JsonElement) value), distinct.orElse(false)));
    }

    private static Optional<Task> blockTask(Fields fields, Task.BlockAction action) {
        Optional<List<IdRef>> blocks = fields.refs("block", "blocks", true, true);
        int count = count(fields);
        return blocks.map(list -> new Task.Block(action, list, count));
    }

    private static Optional<Task> entityTask(Fields fields, Task.EntityAction action) {
        Optional<List<IdRef>> entities = fields.refs("entity", "entities", true, true);
        int count = count(fields);
        Optional<String> nbt = fields.string("nbt", false);
        return entities.map(list -> new Task.Entity(action, list, count, nbt));
    }

    private static int count(Fields fields) {
        return fields.integer("count", false, 1, MAX_COUNT).orElse(1);
    }

    private static List<QuestReward> rewards(Fields fields, String key, boolean required) {
        Optional<JsonObject> object = fields.objectValue(key, required);
        if (object.isEmpty()) {
            return List.of();
        }
        List<QuestReward> result = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.get().entrySet()) {
            String field = fields.at(key) + "." + entry.getKey();
            if (!KEY.matcher(entry.getKey()).matches() || entry.getKey().startsWith("default_")) {
                fields.problems().error(fields.file(), field, "reward keys must match [a-z0-9_]{1,32} and must not start with default_");
                continue;
            }
            Fields.of(entry.getValue(), fields.file(), field, fields.problems()).ifPresent(reward -> {
                int errorsBefore = fields.problems().errorCount();
                List<String> replaces = reward.strings("replaces").orElse(List.of());
                Optional<Reward> body = reward(reward, true);
                reward.finish();
                if (fields.problems().errorCount() == errorsBefore && body.isPresent()) {
                    result.add(new QuestReward(entry.getKey(), replaces, body.get()));
                }
            });
        }
        return List.copyOf(result);
    }

    private static Optional<Reward> reward(Fields fields, boolean choiceAllowed) {
        Optional<String> type = fields.string("type", true);
        if (type.isEmpty()) {
            return Optional.empty();
        }
        return switch (type.get()) {
            case "item" -> {
                Optional<IdRef> item = fields.ref("item", true, false);
                int count = count(fields);
                Optional<JsonObject> components = fields.objectValue("components", false);
                yield item.map(ref -> new Reward.Item(new ItemSpec(ref.id(), components.map(value -> (JsonElement) value)), count));
            }
            case "ticket" -> Optional.of(new Reward.Ticket(fields.integer("count", false, 1, 64).orElse(1)));
            case "xp" -> fields.integer("levels", true, 1, 100).map(Reward.Xp::new);
            case "loot_table" -> {
                Optional<ResourceLocation> table = fields.id("table", true);
                int rolls = fields.integer("rolls", false, 1, 8).orElse(1);
                yield table.map(id -> new Reward.LootTable(id, rolls));
            }
            case "choice" -> {
                if (!choiceAllowed) {
                    fields.error("type", "a choice cannot contain another choice");
                    yield Optional.empty();
                }
                yield choice(fields);
            }
            case "command" -> {
                if (!choiceAllowed) {
                    fields.error("type", "a choice cannot contain a command");
                    yield Optional.empty();
                }
                yield fields.string("command", true).map(command -> new Reward.Command(command.startsWith("/") ? command.substring(1) : command));
            }
            default -> {
                fields.error("type", "unknown reward type \"" + type.get() + "\"");
                yield Optional.empty();
            }
        };
    }

    private static Optional<Reward> choice(Fields fields) {
        Optional<JsonArray> options = fields.array("options", true);
        if (options.isEmpty()) {
            return Optional.empty();
        }
        if (options.get().size() < 2) {
            fields.error("options", "a choice needs at least two options");
            return Optional.empty();
        }
        List<Reward> result = new ArrayList<>();
        boolean valid = true;
        for (int index = 0; index < options.get().size(); index++) {
            Optional<Fields> option = Fields.of(options.get().get(index), fields.file(), fields.at("options") + "[" + index + "]", fields.problems());
            if (option.isEmpty()) {
                valid = false;
                continue;
            }
            Optional<Reward> reward = reward(option.get(), false);
            option.get().finish();
            if (reward.isEmpty()) {
                valid = false;
            } else {
                result.add(reward.get());
            }
        }
        return valid ? Optional.of(new Reward.Choice(List.copyOf(result))) : Optional.empty();
    }

    private enum AllOrAny {
        ALL,
        ANY
    }
}
