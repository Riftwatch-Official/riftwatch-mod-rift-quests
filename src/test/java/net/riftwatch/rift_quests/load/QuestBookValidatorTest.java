package net.riftwatch.rift_quests.load;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.riftwatch.rift_quests.book.Quest;
import net.riftwatch.rift_quests.book.QuestBook;
import net.riftwatch.rift_quests.book.QuestReward;
import net.riftwatch.rift_quests.book.Reward;
import net.riftwatch.rift_quests.book.Task;
import org.junit.jupiter.api.Test;

class QuestBookValidatorTest {
    private static final String CHAPTER = "{\"title\": \"Kinetics\", \"icon\": \"create:cogwheel\", \"order\": 1}";

    private final Map<ResourceLocation, JsonElement> files = new LinkedHashMap<>();

    private QuestBookValidatorTest file(String path, String json) {
        files.put(ResourceLocation.fromNamespaceAndPath("riftwatch", path), JsonParser.parseString(json));
        return this;
    }

    private QuestBookValidatorTest chapter() {
        return file("chapters/kinetics", CHAPTER);
    }

    private static String quest(String extra) {
        return "{\"title\": \"Quest\", \"icon\": \"minecraft:stone\", \"size\": \"small\", \"position\": [0, 0],"
                + " \"tasks\": {\"get\": {\"type\": \"item\", \"item\": \"minecraft:stone\"}}" + extra + "}";
    }

    private static String questAt(int x, String extra) {
        return quest(extra).replace("[0, 0]", "[" + x + ", 0]");
    }

    private QuestBook build() {
        return QuestBookValidator.build(files, List.of(), FakeIdLookup.standard());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("riftwatch", path);
    }

    private static boolean hasProblem(QuestBook book, String file, String text) {
        return book.problems().stream().anyMatch(problem -> problem.file().contains(file) && problem.message().contains(text));
    }

    @Test
    void validBookLoadsWithShortReferences() {
        chapter()
                .file("quests/kinetics/first", questAt(0, ""))
                .file("quests/kinetics/second", questAt(1, ", \"dependencies\": [\"first\"]"))
                .file("quests/kinetics/third", questAt(2, ", \"dependencies\": [\"riftwatch:kinetics/second\", \"kinetics/first\"]"));
        QuestBook book = build();
        assertEquals(0, book.errorCount(), book.problems().toString());
        assertEquals(3, book.quests().size());
        assertEquals(List.of(id("kinetics/first")), book.quests().get(id("kinetics/second")).dependencies());
        assertEquals(List.of(id("kinetics/second"), id("kinetics/first")), book.quests().get(id("kinetics/third")).dependencies());
    }

    @Test
    void unknownFieldSkipsTheQuestAndItsDependents() {
        chapter()
                .file("quests/kinetics/first", questAt(0, ", \"colour\": \"red\""))
                .file("quests/kinetics/second", questAt(1, ", \"dependencies\": [\"first\"]"))
                .file("quests/kinetics/third", questAt(2, ""));
        QuestBook book = build();
        assertTrue(hasProblem(book, "quests/kinetics/first.json", "unknown field"));
        assertTrue(hasProblem(book, "quests/kinetics/second.json", "was skipped because of its own errors"));
        assertEquals(List.of(id("kinetics/third")), List.copyOf(book.quests().keySet()));
    }

    @Test
    void missingDependencyIsNamed() {
        chapter().file("quests/kinetics/first", quest(", \"dependencies\": [\"ghost\"]"));
        QuestBook book = build();
        assertTrue(hasProblem(book, "first.json", "the quest riftwatch:kinetics/ghost does not exist"));
        assertTrue(book.quests().isEmpty());
    }

    @Test
    void unknownIdsAndEmptyTagsAreErrors() {
        chapter()
                .file("quests/kinetics/first", questAt(0, "").replace("\"item\": \"minecraft:stone\"", "\"item\": \"minecraft:stonee\""))
                .file("quests/kinetics/second", questAt(1, "").replace("\"item\": \"minecraft:stone\"", "\"item\": \"#c:empty_tag_marker\""))
                .file("quests/kinetics/third", questAt(2, "").replace("\"item\": \"minecraft:stone\"", "\"item\": \"#c:ingots\""));
        QuestBook book = build();
        assertTrue(hasProblem(book, "first.json", "unknown item minecraft:stonee"));
        assertTrue(hasProblem(book, "second.json", "is empty"));
        assertEquals(List.of(id("kinetics/third")), List.copyOf(book.quests().keySet()));
    }

    @Test
    void dependencyCyclesAreDropped() {
        chapter()
                .file("quests/kinetics/a", questAt(0, ", \"dependencies\": [\"b\"]"))
                .file("quests/kinetics/b", questAt(1, ", \"dependencies\": [\"a\"]"))
                .file("quests/kinetics/c", questAt(2, ", \"dependencies\": [\"b\"]"))
                .file("quests/kinetics/d", questAt(3, ""));
        QuestBook book = build();
        assertTrue(hasProblem(book, "a.json", "dependency cycle"));
        assertTrue(hasProblem(book, "b.json", "dependency cycle"));
        assertTrue(hasProblem(book, "c.json", "was skipped because of its own errors"));
        assertEquals(List.of(id("kinetics/d")), List.copyOf(book.quests().keySet()));
    }

    @Test
    void chapterUnlockCountsForCycles() {
        file("chapters/kinetics", CHAPTER.replace("}", ", \"unlock\": [\"kinetics/first\"]}"));
        file("quests/kinetics/first", quest(""));
        QuestBook book = build();
        assertTrue(hasProblem(book, "first.json", "dependency cycle"));
        assertTrue(hasProblem(book, "chapters/kinetics.json", "was skipped because of its own errors"));
        assertTrue(book.chapters().isEmpty());
    }

    @Test
    void checkmarkQuestsNeverGiveTickets() {
        String checkmark = "\"tasks\": {\"read\": {\"type\": \"checkmark\"}}";
        chapter()
                .file("quests/kinetics/medium", questAt(0, ", \"size\": \"medium\"").replace("\"size\": \"small\", ", "")
                        .replaceAll("\"tasks\": \\{.*?}}", checkmark))
                .file("quests/kinetics/small", questAt(1, "").replaceAll("\"tasks\": \\{.*?}}", checkmark));
        QuestBook book = build();
        assertTrue(hasProblem(book, "medium.json", "must not give Riftwatch Tickets"));
        assertEquals(List.of(id("kinetics/small")), List.copyOf(book.quests().keySet()));
    }

    @Test
    void teamRepeatRewardsHalveItemsAndRefuseTickets() {
        chapter()
                .file("quests/kinetics/team", questAt(0, ", \"team\": {\"min_players\": 2}, \"rewards\": {\"ingots\": {\"type\": \"item\", \"item\": \"minecraft:iron_ingot\", \"count\": 9}}"))
                .file("quests/kinetics/greedy", questAt(1, ", \"team\": {\"min_players\": 2}, \"team_repeat_rewards\": {\"cash\": {\"type\": \"ticket\"}}"))
                .file("quests/kinetics/solo", questAt(2, ", \"team_repeat_rewards\": {}"));
        QuestBook book = build();
        assertTrue(hasProblem(book, "greedy.json", "never give Riftwatch Tickets"));
        assertTrue(hasProblem(book, "solo.json", "only allowed on a team quest"));
        Quest team = book.quests().get(id("kinetics/team"));
        List<QuestReward> repeat = team.effectiveTeamRepeatRewards();
        assertEquals(1, repeat.size());
        assertEquals(4, ((Reward.Item) repeat.getFirst().reward()).count());
        assertEquals(32, team.team().orElseThrow().radius());
    }

    @Test
    void sizeDefaultsAreAdded() {
        chapter()
                .file("quests/kinetics/large", questAt(0, "").replace("small", "large"))
                .file("quests/kinetics/plain", questAt(1, ", \"default_rewards\": false").replace("small", "large"));
        QuestBook book = build();
        List<QuestReward> rewards = book.quests().get(id("kinetics/large")).effectiveRewards();
        assertTrue(rewards.contains(new QuestReward(Quest.DEFAULT_XP_KEY, List.of(), new Reward.Xp(10))));
        assertTrue(rewards.contains(new QuestReward(Quest.DEFAULT_TICKETS_KEY, List.of(), new Reward.Ticket(3))));
        assertTrue(book.quests().get(id("kinetics/plain")).effectiveRewards().isEmpty());
    }

    @Test
    void replacingAnExistingQuestIsAnError() {
        chapter()
                .file("quests/kinetics/old", questAt(0, ""))
                .file("quests/kinetics/new", questAt(1, ", \"replaces\": [\"old\"]"))
                .file("quests/kinetics/a", questAt(2, ", \"replaces\": [\"gone\"]"))
                .file("quests/kinetics/b", questAt(3, ", \"replaces\": [\"gone\"]"));
        QuestBook book = build();
        assertTrue(hasProblem(book, "new.json", "still exists"));
        assertTrue(hasProblem(book, "/a.json", "is also replaced by"));
        assertTrue(hasProblem(book, "/b.json", "is also replaced by"));
        assertEquals(List.of(id("kinetics/old")), List.copyOf(book.quests().keySet()));
    }

    @Test
    void itemTaskRules() {
        chapter()
                .file("quests/kinetics/distinct", questAt(0, "").replace("\"item\": \"minecraft:stone\"",
                        "\"items\": [\"minecraft:stone\", \"create:cogwheel\"], \"distinct\": true, \"count\": 3"))
                .file("quests/kinetics/components", questAt(1, "").replace("\"item\": \"minecraft:stone\"",
                        "\"item\": \"#c:ingots\", \"components\": {}"))
                .file("quests/kinetics/both", questAt(2, "").replace("\"item\": \"minecraft:stone\"",
                        "\"item\": \"minecraft:stone\", \"items\": [\"minecraft:stone\"]"))
                .file("quests/kinetics/good", questAt(3, "").replace("\"item\": \"minecraft:stone\"",
                        "\"items\": [\"minecraft:stone\", \"create:cogwheel\"], \"distinct\": true, \"count\": 2, \"mode\": \"consume\""));
        QuestBook book = build();
        assertTrue(hasProblem(book, "distinct.json", "cannot be larger"));
        assertTrue(hasProblem(book, "components.json", "only allowed with a single item"));
        assertTrue(hasProblem(book, "both.json", "not both"));
        Task.Item item = (Task.Item) book.quests().get(id("kinetics/good")).tasks().getFirst().task();
        assertTrue(item.distinct());
        assertEquals(Task.ItemMode.CONSUME, item.mode());
        assertEquals(1, book.quests().size());
    }

    @Test
    void rewardRules() {
        chapter()
                .file("quests/kinetics/nested", questAt(0, ", \"rewards\": {\"pick\": {\"type\": \"choice\", \"options\": ["
                        + "{\"type\": \"ticket\"}, {\"type\": \"command\", \"command\": \"say hi\"}]}}"))
                .file("quests/kinetics/single", questAt(1, ", \"rewards\": {\"pick\": {\"type\": \"choice\", \"options\": [{\"type\": \"ticket\"}]}}"))
                .file("quests/kinetics/reserved", questAt(2, ", \"rewards\": {\"default_xp\": {\"type\": \"xp\", \"levels\": 1}}"))
                .file("quests/kinetics/good", questAt(3, ", \"rewards\": {\"pick\": {\"type\": \"choice\", \"options\": ["
                        + "{\"type\": \"item\", \"item\": \"create:andesite_alloy\", \"count\": 16}, {\"type\": \"xp\", \"levels\": 3}]}}"));
        QuestBook book = build();
        assertTrue(hasProblem(book, "nested.json", "cannot contain a command"));
        assertTrue(hasProblem(book, "single.json", "at least two options"));
        assertTrue(hasProblem(book, "reserved.json", "must not start with default_"));
        assertEquals(List.of(id("kinetics/good")), List.copyOf(book.quests().keySet()));
    }

    @Test
    void layoutWarningsAndChapterProblems() {
        chapter()
                .file("chapters/empty", CHAPTER)
                .file("chapters/broken", "{\"title\": {\"broken\": true}, \"icon\": \"create:cogwheel\", \"order\": 2}")
                .file("quests/kinetics/a", questAt(5, ""))
                .file("quests/kinetics/b", questAt(5, ""))
                .file("quests/broken/c", questAt(0, ""))
                .file("quests/missing/d", questAt(0, ""))
                .file("stray", "{}");
        QuestBook book = build();
        assertTrue(hasProblem(book, "b.json", "same position as riftwatch:kinetics/a"));
        assertTrue(hasProblem(book, "chapters/empty.json", "has no quests"));
        assertTrue(hasProblem(book, "chapters/broken.json", "broken text component"));
        assertTrue(hasProblem(book, "quests/broken/c.json", "was skipped because of its own errors"));
        assertTrue(hasProblem(book, "quests/missing/d.json", "does not exist"));
        assertTrue(hasProblem(book, "stray.json", "must be chapters"));
        assertEquals(2, book.quests().size());
        assertEquals(2, book.warningCount());
        assertFalse(book.chapters().containsKey(id("broken")));
    }

    @Test
    void recipeTasks() {
        chapter()
                .file("quests/kinetics/dye", questAt(0, "").replace("{\"type\": \"item\", \"item\": \"minecraft:stone\"}",
                        "{\"type\": \"recipe\", \"recipe\": \"sophisticatedbackpacks:backpack_dye\"}"))
                .file("quests/kinetics/ghost", questAt(1, "").replace("{\"type\": \"item\", \"item\": \"minecraft:stone\"}",
                        "{\"type\": \"recipe\", \"recipes\": [\"pipeorgans:crafting/nothing\"], \"count\": 2}"));
        QuestBook book = build();
        assertTrue(hasProblem(book, "ghost.json", "unknown recipe pipeorgans:crafting/nothing"));
        Task.Recipe recipe = (Task.Recipe) book.quests().get(id("kinetics/dye")).tasks().getFirst().task();
        assertEquals(1, recipe.count());
        assertEquals(List.of(id("kinetics/dye")), List.copyOf(book.quests().keySet()));
    }

    @Test
    void useItemTasks() {
        chapter()
                .file("quests/kinetics/play", questAt(0, "").replace("\"type\": \"item\"", "\"type\": \"use_item\"").replace("minecraft:stone\"}", "#c:ingots\", \"count\": 3}"))
                .file("quests/kinetics/bad", questAt(1, "").replace("\"type\": \"item\"", "\"type\": \"use_item\"").replace("minecraft:stone\"}", "minecraft:lute\"}"));
        QuestBook book = build();
        assertTrue(hasProblem(book, "bad.json", "unknown item minecraft:lute"));
        Task.UseItem use = (Task.UseItem) book.quests().get(id("kinetics/play")).tasks().getFirst().task();
        assertEquals(3, use.count());
        assertTrue(use.items().getFirst().tag());
        assertEquals(1, book.quests().size());
    }

    @Test
    void statAndEntityTasks() {
        chapter()
                .file("quests/kinetics/jump", questAt(0, "").replace("{\"type\": \"item\", \"item\": \"minecraft:stone\"}",
                        "{\"type\": \"stat\", \"stat_type\": \"minecraft:custom\", \"stat\": \"minecraft:jump\", \"value\": 100}"))
                .file("quests/kinetics/wrongstat", questAt(1, "").replace("{\"type\": \"item\", \"item\": \"minecraft:stone\"}",
                        "{\"type\": \"stat\", \"stat_type\": \"minecraft:mined\", \"stat\": \"minecraft:jump\", \"value\": 1}"))
                .file("quests/kinetics/cow", questAt(2, "").replace("{\"type\": \"item\", \"item\": \"minecraft:stone\"}",
                        "{\"type\": \"breed_entity\", \"entity\": \"minecraft:cow\", \"count\": 2, \"nbt\": \"Age:0\"}"))
                .file("quests/kinetics/unknown", questAt(3, "").replace("\"type\": \"item\"", "\"type\": \"fish\""));
        QuestBook book = build();
        assertTrue(hasProblem(book, "wrongstat.json", "unknown block minecraft:jump"));
        assertTrue(hasProblem(book, "cow.json", "not a compound"));
        assertTrue(hasProblem(book, "unknown.json", "unknown task type \"fish\""));
        assertEquals(List.of(id("kinetics/jump")), List.copyOf(book.quests().keySet()));
    }
}
