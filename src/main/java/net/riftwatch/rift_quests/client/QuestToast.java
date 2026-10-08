package net.riftwatch.rift_quests.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.riftwatch.rift_quests.network.BookView;

public final class QuestToast implements Toast {
    private static final long DISPLAY_TIME = 5000L;
    private static final int TITLE_COLOUR = 0xFFFFC070;
    private static final int TEXT_COLOUR = 0xFFF4EDE8;

    private final Component heading;
    private final Component title;
    private final ItemStack icon;

    public QuestToast(BookView.QuestView quest) {
        this.heading = Component.translatable("toast.rift_quests.complete");
        this.title = quest.title();
        this.icon = quest.icon().isEmpty() ? ItemStack.EMPTY : quest.icon().get(0);
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toasts, long timeSinceLastVisible) {
        Font font = toasts.getMinecraft().font;
        graphics.blitSprite(Sprites.TOAST, 0, 0, width(), height());
        graphics.renderFakeItem(icon, 8, 8);
        graphics.drawString(font, heading, 30, 7, TITLE_COLOUR, false);
        List<FormattedCharSequence> lines = font.split(title, 125);
        if (!lines.isEmpty()) {
            graphics.drawString(font, lines.get(0), 30, 18, TEXT_COLOUR, false);
        }
        return timeSinceLastVisible >= DISPLAY_TIME * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
