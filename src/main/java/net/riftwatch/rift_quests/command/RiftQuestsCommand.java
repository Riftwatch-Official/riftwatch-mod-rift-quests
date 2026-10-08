package net.riftwatch.rift_quests.command;

import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.riftwatch.rift_quests.book.QuestBook;
import net.riftwatch.rift_quests.load.Problem;
import net.riftwatch.rift_quests.load.QuestBooks;

public final class RiftQuestsCommand {
    private static final int SHOWN_PROBLEMS = 8;

    private RiftQuestsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("riftquests")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("validate").executes(context -> validate(context.getSource()))));
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
