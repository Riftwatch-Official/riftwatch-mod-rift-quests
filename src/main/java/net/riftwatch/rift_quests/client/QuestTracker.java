package net.riftwatch.rift_quests.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.riftwatch.rift_quests.network.BookView;
import net.riftwatch.rift_quests.network.QuestState;

public final class QuestTracker {
    private static final int WIDTH = 150;
    private static final int PADDING = 5;
    private static final int TITLE_COLOUR = 0xFFFFC070;
    private static final int TEXT_COLOUR = 0xFFE8DCCF;
    private static final int DONE_COLOUR = 0xFF8FD18A;
    private static final int MAX_TASK_LINES = 3;

    private QuestTracker() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.screen != null || minecraft.player == null || !ClientConfig.SHOW_TRACKER.get()
                || !ClientQuests.loaded() || ClientQuests.pinned().length == 0) {
            return;
        }
        Font font = minecraft.font;
        List<Line> lines = new ArrayList<>();
        for (int position : ClientQuests.pinned()) {
            if (position < 0 || position >= ClientQuests.book().quests().size()) {
                continue;
            }
            BookView.QuestView quest = ClientQuests.quest(position);
            QuestState state = ClientQuests.state(position);
            if (!lines.isEmpty()) {
                lines.add(new Line(null, 0, 3));
            }
            for (FormattedCharSequence title : font.split(quest.title(), WIDTH - 2 * PADDING)) {
                lines.add(new Line(title, TITLE_COLOUR, font.lineHeight));
            }
            if (state.done()) {
                Component text = Component.translatable(state == QuestState.COMPLETE ? "tracker.rift_quests.claim" : "tracker.rift_quests.done");
                lines.add(new Line(text.getVisualOrderText(), DONE_COLOUR, font.lineHeight));
                continue;
            }
            int shown = 0;
            for (int task = 0; task < quest.tasks().size() && shown < MAX_TASK_LINES; task++) {
                BookView.TaskView view = quest.tasks().get(task);
                long progress = ClientQuests.progress(position, task);
                if (progress >= view.target()) {
                    continue;
                }
                Component text = Component.literal(progress + "/" + view.target() + " ").append(view.label());
                lines.add(new Line(font.split(text, WIDTH - 2 * PADDING - 4).get(0), TEXT_COLOUR, font.lineHeight));
                shown++;
            }
            if (ClientQuests.waitingForTeam(position)) {
                lines.add(new Line(Component.translatable("tracker.rift_quests.team").getVisualOrderText(), TITLE_COLOUR, font.lineHeight));
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        int height = PADDING * 2;
        for (Line line : lines) {
            height += line.height();
        }
        int x = graphics.guiWidth() - WIDTH - 4;
        int y = Math.max(4, graphics.guiHeight() / 2 - height / 2 - 20);
        graphics.blitSprite(Sprites.HUD, x, y, WIDTH, height);
        int lineY = y + PADDING;
        for (Line line : lines) {
            if (line.text() != null) {
                int indent = line.colour() == TITLE_COLOUR ? 0 : 4;
                graphics.drawString(font, line.text(), x + PADDING + indent, lineY, line.colour(), true);
            }
            lineY += line.height();
        }
    }

    private record Line(FormattedCharSequence text, int colour, int height) {
    }
}
