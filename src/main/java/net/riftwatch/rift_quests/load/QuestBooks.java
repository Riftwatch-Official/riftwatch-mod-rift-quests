package net.riftwatch.rift_quests.load;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.riftwatch.rift_quests.book.QuestBook;
import org.slf4j.Logger;

public final class QuestBooks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile QuestBookLoader.Files staged = QuestBookLoader.Files.EMPTY;
    private static volatile QuestBook current = QuestBook.EMPTY;

    private QuestBooks() {
    }

    public static QuestBook current() {
        return current;
    }

    static void stage(QuestBookLoader.Files files) {
        staged = files;
    }

    public static QuestBook activate(MinecraftServer server) {
        QuestBookLoader.Files files = staged;
        QuestBook book = QuestBookValidator.build(files.json(), files.problems(), new ServerIdLookup(server));
        for (Problem problem : book.problems()) {
            if (problem.severity() == Problem.Severity.ERROR) {
                LOGGER.error("Quest book: {}", problem);
            } else {
                LOGGER.warn("Quest book: {}", problem);
            }
        }
        LOGGER.info("Quest book: {} chapters and {} quests loaded, {} errors, {} warnings",
                book.chapters().size(), book.quests().size(), book.errorCount(), book.warningCount());
        current = book;
        return book;
    }
}
