package net.riftwatch.rift_quests.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.riftwatch.rift_quests.book.Quest;
import net.riftwatch.rift_quests.book.QuestBook;
import net.riftwatch.rift_quests.engine.QuestEngine;
import net.riftwatch.rift_quests.engine.ServerBook;
import net.riftwatch.rift_quests.load.Problem;
import net.riftwatch.rift_quests.load.QuestBooks;
import net.riftwatch.rift_quests.network.Payloads;
import net.riftwatch.rift_quests.network.QuestState;
import net.riftwatch.rift_quests.progress.PlayerQuestData;

public final class RiftQuestsCommand {
    private static final int SHOWN_PROBLEMS = 8;
    private static final DynamicCommandExceptionType UNKNOWN_QUEST = new DynamicCommandExceptionType(id -> Component.literal("Unknown quest " + id));
    private static final SuggestionProvider<CommandSourceStack> QUESTS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(ServerBook.current().quests().stream().map(Quest::id), builder);
    private static final SuggestionProvider<CommandSourceStack> TASKS = (context, builder) -> {
        Quest quest = ServerBook.current().quest(ResourceLocationArgument.getId(context, "quest"));
        return quest == null ? builder.buildFuture() : SharedSuggestionProvider.suggest(quest.tasks().stream().map(task -> task.key()), builder);
    };

    private RiftQuestsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("riftquests")
                .then(Commands.literal("open").executes(context -> open(context.getSource())))
                .then(Commands.literal("claimall").executes(context -> {
                    QuestEngine.claimAll(context.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("pin")
                        .then(Commands.argument("quest", ResourceLocationArgument.id()).suggests(QUESTS)
                                .executes(context -> pin(context))))
                .then(Commands.literal("validate").requires(RiftQuestsCommand::admin).executes(context -> validate(context.getSource())))
                .then(Commands.literal("reload").requires(RiftQuestsCommand::admin)
                        .executes(context -> {
                            context.getSource().getServer().getCommands().performPrefixedCommand(context.getSource(), "reload");
                            return 1;
                        }))
                .then(Commands.literal("complete").requires(RiftQuestsCommand::admin)
                        .then(Commands.argument("quest", ResourceLocationArgument.id()).suggests(QUESTS)
                                .then(Commands.argument("targets", EntityArgument.players()).executes(RiftQuestsCommand::complete))))
                .then(Commands.literal("reset").requires(RiftQuestsCommand::admin)
                        .then(Commands.argument("quest", ResourceLocationArgument.id()).suggests(QUESTS)
                                .then(Commands.argument("targets", EntityArgument.players()).executes(RiftQuestsCommand::reset))))
                .then(Commands.literal("resetall").requires(RiftQuestsCommand::admin)
                        .then(Commands.argument("targets", EntityArgument.players()).executes(RiftQuestsCommand::resetAll)))
                .then(Commands.literal("status").requires(RiftQuestsCommand::admin)
                        .then(Commands.argument("player", EntityArgument.player()).executes(RiftQuestsCommand::status)))
                .then(Commands.literal("progress").requires(RiftQuestsCommand::admin)
                        .then(Commands.argument("quest", ResourceLocationArgument.id()).suggests(QUESTS)
                                .then(Commands.argument("task", StringArgumentType.word()).suggests(TASKS)
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(context -> progress(context, 1))
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100_000))
                                                        .executes(context -> progress(context, IntegerArgumentType.getInteger(context, "amount")))))))));
    }

    private static boolean admin(CommandSourceStack source) {
        return source.hasPermission(Commands.LEVEL_GAMEMASTERS);
    }

    private static Quest quest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(context, "quest");
        Quest quest = ServerBook.current().quest(id);
        if (quest == null) {
            throw UNKNOWN_QUEST.create(id);
        }
        return quest;
    }

    private static int open(CommandSourceStack source) throws CommandSyntaxException {
        PacketDistributor.sendToPlayer(source.getPlayerOrException(), new Payloads.OpenBook());
        return 1;
    }

    private static int pin(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Quest quest = quest(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        QuestEngine.pin(player, quest.id());
        boolean pinned = QuestEngine.data(player).pinned().contains(quest.id());
        context.getSource().sendSuccess(() -> Component.literal((pinned ? "Pinned " : "Unpinned ") + quest.id()), false);
        return 1;
    }

    private static int complete(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Quest quest = quest(context);
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        targets.forEach(player -> QuestEngine.forceComplete(player, quest));
        context.getSource().sendSuccess(() -> Component.literal("Completed " + quest.id() + " for " + targets.size() + " players"), true);
        return targets.size();
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Quest quest = quest(context);
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        targets.forEach(player -> QuestEngine.reset(player, quest.id()));
        context.getSource().sendSuccess(() -> Component.literal("Reset " + quest.id() + " for " + targets.size() + " players"), true);
        return targets.size();
    }

    private static int resetAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        targets.forEach(QuestEngine::resetAll);
        context.getSource().sendSuccess(() -> Component.literal("Reset every quest for " + targets.size() + " players"), true);
        return targets.size();
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ServerBook book = ServerBook.current();
        PlayerQuestData data = QuestEngine.data(player);
        int[] counts = new int[QuestState.values().length];
        for (Quest quest : book.quests()) {
            counts[QuestEngine.state(book, data, quest).ordinal()]++;
        }
        context.getSource().sendSuccess(() -> Component.literal(player.getGameProfile().getName() + ": "
                + counts[QuestState.CLAIMED.ordinal()] + " claimed, " + counts[QuestState.COMPLETE.ordinal()] + " complete, "
                + counts[QuestState.IN_PROGRESS.ordinal()] + " in progress, " + counts[QuestState.AVAILABLE.ordinal()] + " available, "
                + counts[QuestState.LOCKED.ordinal()] + " locked, " + data.pinned().size() + " pinned"), false);
        return counts[QuestState.CLAIMED.ordinal()] + counts[QuestState.COMPLETE.ordinal()];
    }

    private static int progress(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Quest quest = quest(context);
        String task = StringArgumentType.getString(context, "task");
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int changed = 0;
        for (ServerPlayer player : targets) {
            if (QuestEngine.addCustomProgress(player, quest, task, amount)) {
                changed++;
            }
        }
        int result = changed;
        context.getSource().sendSuccess(() -> Component.literal("Progress on " + quest.id() + " " + task + " for " + result + " players"), true);
        return result;
    }

    private static int validate(CommandSourceStack source) {
        QuestBook book = QuestBooks.activate(source.getServer());
        ChatFormatting colour = book.errorCount() > 0 ? ChatFormatting.RED : book.warningCount() > 0 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        source.sendSuccess(() -> Component.literal("Quest book: " + book.chapters().size() + " chapters, " + book.quests().size() + " quests, "
                + book.errorCount() + " errors, " + book.warningCount() + " warnings").withStyle(colour), false);
        List<Problem> problems = book.problems();
        for (Problem problem : problems.subList(0, Math.min(SHOWN_PROBLEMS, problems.size()))) {
            ChatFormatting problemColour = problem.severity() == Problem.Severity.ERROR ? ChatFormatting.RED : ChatFormatting.GOLD;
            source.sendSuccess(() -> Component.literal(problem.toString()).withStyle(problemColour), false);
        }
        if (problems.size() > SHOWN_PROBLEMS) {
            source.sendSuccess(() -> Component.literal((problems.size() - SHOWN_PROBLEMS) + " more in the server log").withStyle(ChatFormatting.GRAY), false);
        }
        return book.errorCount() == 0 ? 1 : 0;
    }
}
