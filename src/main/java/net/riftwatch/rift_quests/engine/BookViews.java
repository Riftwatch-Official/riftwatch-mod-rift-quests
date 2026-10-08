package net.riftwatch.rift_quests.engine;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.riftwatch.rift_quests.book.Chapter;
import net.riftwatch.rift_quests.book.Icon;
import net.riftwatch.rift_quests.book.IdRef;
import net.riftwatch.rift_quests.book.ItemSpec;
import net.riftwatch.rift_quests.book.Quest;
import net.riftwatch.rift_quests.book.QuestBook;
import net.riftwatch.rift_quests.book.QuestReward;
import net.riftwatch.rift_quests.book.QuestTask;
import net.riftwatch.rift_quests.book.Reward;
import net.riftwatch.rift_quests.book.Task;
import net.riftwatch.rift_quests.book.Text;
import net.riftwatch.rift_quests.network.BookView;
import net.riftwatch.rift_quests.registry.ModItems;

public final class BookViews {
    private static final int MAX_ICONS = 24;
    private static final int MAX_DETAILS = 40;

    private final MinecraftServer server;
    private final RegistryAccess access;
    private final RegistryOps<JsonElement> ops;

    private BookViews(MinecraftServer server) {
        this.server = server;
        this.access = server.registryAccess();
        this.ops = access.createSerializationContext(JsonOps.INSTANCE);
    }

    public static BookView build(int id, QuestBook book, List<Quest> orderedQuests, MinecraftServer server) {
        BookViews views = new BookViews(server);
        List<BookView.ChapterView> chapters = new ArrayList<>();
        for (Chapter chapter : book.orderedChapters()) {
            chapters.add(new BookView.ChapterView(chapter.id(), views.text(chapter.title()), chapter.description().map(views::text),
                    views.icon(chapter.icon()), chapter.unlock()));
        }
        List<BookView.QuestView> quests = new ArrayList<>();
        for (Quest quest : orderedQuests) {
            quests.add(views.quest(quest));
        }
        return new BookView(id, List.copyOf(chapters), List.copyOf(quests));
    }

    private BookView.QuestView quest(Quest quest) {
        List<BookView.TaskView> tasks = new ArrayList<>();
        for (QuestTask task : quest.tasks()) {
            tasks.add(task(task));
        }
        List<BookView.RewardView> rewards = quest.effectiveRewards().stream().map(this::reward).toList();
        List<BookView.RewardView> repeat = quest.team().isPresent()
                ? quest.effectiveTeamRepeatRewards().stream().map(this::reward).toList()
                : List.of();
        return new BookView.QuestView(quest.id(), quest.chapter(), text(quest.title()), quest.subtitle().map(this::text),
                quest.description().map(this::text), icon(quest.icon()), quest.size(), quest.x(), quest.y(), quest.dependencies(),
                quest.dependencyMode(), quest.visibility(), quest.team().map(team -> team.minPlayers()).orElse(0),
                quest.team().map(team -> team.radius()).orElse(0), List.copyOf(tasks), rewards, repeat);
    }

    public static long target(Task task) {
        return switch (task) {
            case Task.Item item -> item.count();
            case Task.Advancement advancement -> advancement.all() ? advancement.advancements().size() : 1;
            case Task.Block block -> block.count();
            case Task.Entity entity -> entity.count();
            case Task.Craft craft -> craft.count();
            case Task.Recipe recipe -> recipe.count();
            case Task.UseItem use -> use.count();
            case Task.Biome biome -> biome.count();
            case Task.Structure structure -> structure.count();
            case Task.Dimension dimension -> 1;
            case Task.Location location -> 1;
            case Task.Stat stat -> stat.value();
            case Task.Xp xp -> xp.level();
            case Task.Checkmark checkmark -> 1;
            case Task.Observe observe -> 1;
            case Task.Custom custom -> custom.count();
        };
    }

    private BookView.TaskView task(QuestTask questTask) {
        Task task = questTask.task();
        List<Component> details = new ArrayList<>();
        List<ItemStack> icons = new ArrayList<>();
        MutableComponent label;
        BookView.TaskAction action = BookView.TaskAction.NONE;
        switch (task) {
            case Task.Item item -> {
                icons.addAll(itemStacks(item.items(), item.components()));
                MutableComponent what = names(item.items(), BookViews::itemName, details);
                if (item.distinct()) {
                    label = Component.literal("Collect " + item.count() + " different: ").append(what);
                } else {
                    label = Component.literal(verbFor(item.mode()) + item.count() + "x ").append(what);
                }
                if (item.mode() != Task.ItemMode.DETECT) {
                    action = BookView.TaskAction.SUBMIT;
                }
            }
            case Task.Advancement advancement -> {
                List<Component> titles = new ArrayList<>();
                for (ResourceLocation id : advancement.advancements()) {
                    AdvancementHolder holder = server.getAdvancements().get(id);
                    Optional<DisplayInfo> display = holder == null ? Optional.empty() : holder.value().display();
                    titles.add(display.map(DisplayInfo::getTitle).orElse(Component.literal(pretty(id))));
                    display.ifPresent(info -> icons.add(info.getIcon().copy()));
                }
                if (titles.size() == 1) {
                    label = Component.literal("Advancement: ").append(titles.getFirst());
                } else {
                    label = Component.literal(advancement.all() ? "Advancements: all " + titles.size() : "Advancement: any of " + titles.size());
                    details.addAll(titles);
                }
                if (icons.isEmpty()) {
                    icons.add(new ItemStack(Items.KNOWLEDGE_BOOK));
                }
            }
            case Task.Block block -> {
                for (IdRef ref : block.blocks()) {
                    icons.addAll(blockStacks(ref));
                }
                String verb = switch (block.action()) {
                    case PLACE -> "Place ";
                    case BREAK -> "Break ";
                    case INTERACT -> "Use ";
                };
                label = Component.literal(verb + (block.count() > 1 ? block.count() + "x " : "a ")).append(names(block.blocks(), this::blockName, details));
            }
            case Task.Entity entity -> {
                for (IdRef ref : entity.entities()) {
                    icons.addAll(entityStacks(ref));
                }
                String verb = switch (entity.action()) {
                    case INTERACT -> "Interact with ";
                    case KILL -> "Defeat ";
                    case TAME -> "Tame ";
                    case BREED -> "Breed ";
                };
                label = Component.literal(verb + (entity.count() > 1 ? entity.count() + "x " : "a ")).append(names(entity.entities(), this::entityName, details));
            }
            case Task.Craft craft -> {
                icons.addAll(itemStacks(craft.items(), Optional.empty()));
                label = Component.literal("Craft " + craft.count() + "x ").append(names(craft.items(), BookViews::itemName, details));
            }
            case Task.Recipe recipe -> {
                List<Component> results = new ArrayList<>();
                for (ResourceLocation id : recipe.recipes()) {
                    Optional<RecipeHolder<?>> holder = server.getRecipeManager().byKey(id);
                    ItemStack result = holder.map(value -> value.value().getResultItem(access)).orElse(ItemStack.EMPTY);
                    if (!result.isEmpty()) {
                        icons.add(result.copyWithCount(1));
                        results.add(result.getHoverName());
                    } else {
                        results.add(Component.literal(pretty(id)));
                    }
                }
                if (icons.isEmpty()) {
                    icons.add(new ItemStack(Items.CRAFTING_TABLE));
                }
                label = Component.literal("Craft " + (recipe.count() > 1 ? recipe.count() + "x " : "")).append(results.size() == 1 ? results.getFirst() : Component.literal("with one of " + results.size() + " recipes"));
                if (results.size() > 1) {
                    details.addAll(results);
                }
            }
            case Task.UseItem use -> {
                icons.addAll(itemStacks(use.items(), Optional.empty()));
                label = Component.literal("Use ").append(names(use.items(), BookViews::itemName, details)).append(use.count() > 1 ? " " + use.count() + " times" : "");
            }
            case Task.Biome biome -> {
                icons.add(new ItemStack(Items.COMPASS));
                label = visitLabel(biome.biomes(), biome.count(), "biome", "biomes", details, this::biomeName);
            }
            case Task.Structure structure -> {
                icons.add(new ItemStack(Items.FILLED_MAP));
                label = visitLabel(structure.structures(), structure.count(), "structure", "structures", details, BookViews::structureName);
            }
            case Task.Dimension dimension -> {
                icons.add(new ItemStack(Items.ENDER_EYE));
                List<Component> names = dimension.dimensions().stream().map(BookViews::dimensionName).toList();
                label = Component.literal("Enter ").append(joinOr(names));
            }
            case Task.Location location -> {
                icons.add(new ItemStack(Items.RECOVERY_COMPASS));
                label = Component.literal("Reach " + location.x() + " " + location.y() + " " + location.z() + " in ").append(dimensionName(location.dimension()));
            }
            case Task.Stat stat -> {
                icons.add(new ItemStack(Items.WRITABLE_BOOK));
                label = statName(stat).append(": " + stat.value());
            }
            case Task.Xp xp -> {
                icons.add(new ItemStack(Items.EXPERIENCE_BOTTLE));
                label = Component.literal((xp.consume() ? "Spend " : "Reach level ") + xp.level() + (xp.consume() ? " levels" : ""));
            }
            case Task.Checkmark checkmark -> {
                icons.add(new ItemStack(Items.PAPER));
                label = Component.literal("Confirm when done");
                action = BookView.TaskAction.CHECK;
            }
            case Task.Observe observe -> {
                icons.add(new ItemStack(Items.SPYGLASS));
                MutableComponent what = observe.blocks().isEmpty()
                        ? names(observe.entities(), this::entityName, details)
                        : names(observe.blocks(), this::blockName, details);
                label = Component.literal("Look at a ").append(what);
            }
            case Task.Custom custom -> {
                icons.add(new ItemStack(Items.NAME_TAG));
                label = Component.literal("Special task");
            }
        }
        Component shown = questTask.title().map(this::text).orElse(label);
        List<ItemStack> limited = icons.stream().filter(stack -> !stack.isEmpty()).limit(MAX_ICONS).toList();
        return new BookView.TaskView(questTask.key(), shown, target(task), limited, action, details.stream().limit(MAX_DETAILS).toList());
    }

    private static String verbFor(Task.ItemMode mode) {
        return mode == Task.ItemMode.DETECT ? "Obtain " : "Hand in ";
    }

    private interface Namer {
        Component name(IdRef ref);
    }

    private MutableComponent visitLabel(List<IdRef> refs, int count, String one, String many, List<Component> details, Namer namer) {
        if (refs.size() == 1 && count == 1) {
            return Component.literal("Visit ").append(namer.name(refs.getFirst()));
        }
        refs.forEach(ref -> details.add(namer.name(ref)));
        if (count == 1) {
            return Component.literal("Visit one of " + refs.size() + " " + many);
        }
        return Component.literal("Visit " + count + " different " + (count == 1 ? one : many) + " of " + refs.size());
    }

    private static MutableComponent names(List<IdRef> refs, Namer namer, List<Component> details) {
        if (refs.size() == 1) {
            return namer.name(refs.getFirst()).copy();
        }
        refs.forEach(ref -> details.add(namer.name(ref)));
        return Component.literal("one of " + refs.size());
    }

    private static MutableComponent joinOr(List<Component> names) {
        MutableComponent result = Component.empty();
        for (int index = 0; index < names.size(); index++) {
            if (index > 0) {
                result.append(index == names.size() - 1 ? " or " : ", ");
            }
            result.append(names.get(index));
        }
        return result;
    }

    private BookView.RewardView reward(QuestReward questReward) {
        return reward(questReward.key(), questReward.reward());
    }

    private BookView.RewardView reward(String key, Reward reward) {
        return switch (reward) {
            case Reward.Item item -> {
                ItemStack stack = stack(item.item());
                yield new BookView.RewardView(key, BookView.RewardKind.ITEM, Component.literal(item.count() + "x ").append(stack.getHoverName()),
                        List.of(stack.copyWithCount(1)), item.count(), List.of());
            }
            case Reward.Ticket ticket -> new BookView.RewardView(key, BookView.RewardKind.TICKET,
                    Component.literal(ticket.count() + "x Riftwatch Ticket"), List.of(new ItemStack(ModItems.RIFTWATCH_TICKET.get())), ticket.count(), List.of());
            case Reward.Xp xp -> new BookView.RewardView(key, BookView.RewardKind.XP,
                    Component.literal(xp.levels() + " levels of experience"), List.of(new ItemStack(Items.EXPERIENCE_BOTTLE)), xp.levels(), List.of());
            case Reward.LootTable table -> new BookView.RewardView(key, BookView.RewardKind.LOOT_TABLE,
                    Component.literal("Loot: " + pretty(table.table()) + (table.rolls() > 1 ? " x" + table.rolls() : "")),
                    List.of(new ItemStack(Items.CHEST)), table.rolls(), List.of());
            case Reward.Choice choice -> {
                List<BookView.RewardView> options = new ArrayList<>();
                for (int index = 0; index < choice.options().size(); index++) {
                    options.add(reward(key + "#" + index, choice.options().get(index)));
                }
                yield new BookView.RewardView(key, BookView.RewardKind.CHOICE, Component.literal("Choose one of " + options.size()),
                        List.of(new ItemStack(Items.BUNDLE)), 1, List.copyOf(options));
            }
            case Reward.Command command -> new BookView.RewardView(key, BookView.RewardKind.COMMAND, Component.literal("A special reward"),
                    List.of(new ItemStack(Items.NETHER_STAR)), 1, List.of());
        };
    }

    Component text(Text text) {
        return ComponentSerialization.CODEC.parse(ops, text.json()).result()
                .orElseGet(() -> Component.literal(text.json().toString()).withStyle(ChatFormatting.RED));
    }

    List<ItemStack> icon(Icon icon) {
        if (icon.item().tag()) {
            List<ItemStack> stacks = tagItems(icon.item().id());
            return stacks.isEmpty() ? List.of(new ItemStack(Items.BARRIER)) : stacks;
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(icon.item().id()));
        icon.components().ifPresent(json -> DataComponentPatch.CODEC.parse(ops, json).result().ifPresent(stack::applyComponents));
        return List.of(stack);
    }

    ItemStack stack(ItemSpec spec) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(spec.item()));
        spec.components().ifPresent(json -> DataComponentPatch.CODEC.parse(ops, json).result().ifPresent(stack::applyComponents));
        return stack;
    }

    private List<ItemStack> itemStacks(List<IdRef> refs, Optional<JsonElement> components) {
        List<ItemStack> stacks = new ArrayList<>();
        for (IdRef ref : refs) {
            if (ref.tag()) {
                stacks.addAll(tagItems(ref.id()));
            } else {
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ref.id()));
                components.ifPresent(json -> DataComponentPatch.CODEC.parse(ops, json).result().ifPresent(stack::applyComponents));
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private List<ItemStack> tagItems(ResourceLocation tag) {
        Registry<Item> items = access.registryOrThrow(Registries.ITEM);
        return items.getTag(TagKey.create(Registries.ITEM, tag))
                .map(set -> set.stream().limit(MAX_ICONS).map(holder -> new ItemStack(holder.value())).toList())
                .orElse(List.of());
    }

    private List<ItemStack> blockStacks(IdRef ref) {
        Registry<Block> blocks = access.registryOrThrow(Registries.BLOCK);
        if (ref.tag()) {
            return blocks.getTag(TagKey.create(Registries.BLOCK, ref.id()))
                    .map(set -> set.stream().map(holder -> new ItemStack(holder.value().asItem())).filter(stack -> !stack.isEmpty()).limit(MAX_ICONS).toList())
                    .orElse(List.of());
        }
        ItemStack stack = new ItemStack(blocks.get(ref.id()).asItem());
        return stack.isEmpty() ? List.of(new ItemStack(Items.GRASS_BLOCK)) : List.of(stack);
    }

    private List<ItemStack> entityStacks(IdRef ref) {
        Registry<EntityType<?>> types = access.registryOrThrow(Registries.ENTITY_TYPE);
        List<EntityType<?>> matched = new ArrayList<>();
        if (ref.tag()) {
            types.getTag(TagKey.create(Registries.ENTITY_TYPE, ref.id())).ifPresent(set -> set.forEach(holder -> matched.add(holder.value())));
        } else {
            matched.add(types.get(ref.id()));
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (EntityType<?> type : matched) {
            SpawnEggItem egg = SpawnEggItem.byId(type);
            stacks.add(egg == null ? new ItemStack(Items.NAME_TAG) : new ItemStack(egg));
        }
        return stacks;
    }

    private static Component itemName(IdRef ref) {
        if (ref.tag()) {
            return Component.literal("any " + pretty(ref.id()));
        }
        return BuiltInRegistries.ITEM.get(ref.id()).getDescription();
    }

    private Component blockName(IdRef ref) {
        if (ref.tag()) {
            return Component.literal("any " + pretty(ref.id()));
        }
        return access.registryOrThrow(Registries.BLOCK).get(ref.id()).getName();
    }

    private Component entityName(IdRef ref) {
        if (ref.tag()) {
            return Component.literal("any " + pretty(ref.id()));
        }
        return access.registryOrThrow(Registries.ENTITY_TYPE).get(ref.id()).getDescription();
    }

    private Component biomeName(IdRef ref) {
        if (ref.tag()) {
            return Component.literal("any " + pretty(ref.id()) + " biome");
        }
        return Component.translatableWithFallback(Util.makeDescriptionId("biome", ref.id()), pretty(ref.id()));
    }

    private static Component structureName(IdRef ref) {
        return Component.literal(ref.tag() ? "any " + pretty(ref.id()) : pretty(ref.id()));
    }

    private static Component dimensionName(ResourceLocation id) {
        return Component.translatableWithFallback(Util.makeDescriptionId("dimension", id), pretty(id));
    }

    private MutableComponent statName(Task.Stat stat) {
        Optional<StatType<?>> type = BuiltInRegistries.STAT_TYPE.getOptional(stat.statType());
        if (type.isPresent() && type.get() == BuiltInRegistries.STAT_TYPE.get(ResourceLocation.withDefaultNamespace("custom"))) {
            return Component.translatableWithFallback("stat." + stat.stat().toString().replace(':', '.'), pretty(stat.stat()));
        }
        Optional<Holder.Reference<Item>> item = BuiltInRegistries.ITEM.getHolder(stat.stat());
        Component target = item.map(holder -> holder.value().getDescription()).orElse(Component.literal(pretty(stat.stat())));
        return Component.literal(pretty(stat.statType()) + " ").append(target);
    }

    static String pretty(ResourceLocation id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        String last = slash >= 0 ? path.substring(slash + 1) : path;
        StringBuilder result = new StringBuilder();
        for (String word : last.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return result.toString();
    }
}
