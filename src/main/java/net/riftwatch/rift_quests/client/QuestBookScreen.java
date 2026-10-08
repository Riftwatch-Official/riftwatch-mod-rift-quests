package net.riftwatch.rift_quests.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.riftwatch.rift_quests.book.DependencyMode;
import net.riftwatch.rift_quests.book.QuestSize;
import net.riftwatch.rift_quests.book.Visibility;
import net.riftwatch.rift_quests.network.BookView;
import net.riftwatch.rift_quests.network.Payloads;
import net.riftwatch.rift_quests.network.QuestState;
import net.riftwatch.rift_quests.registry.ModItems;
import org.lwjgl.glfw.GLFW;

public final class QuestBookScreen extends Screen {
    private static final int MARGIN = 6;
    private static final int HEADER = 28;
    private static final int LIST_WIDTH = 124;
    private static final int ROW = 20;
    private static final int UNIT = 20;
    private static final float[] ZOOMS = {0.5F, 0.75F, 1.0F, 1.25F, 1.5F};

    private static final int TEXT = 0xFFF4EDE8;
    private static final int MUTED = 0xFFA89484;
    private static final int BRASS = 0xFFE8B48A;
    private static final int GOLD = 0xFFFFC070;
    private static final int NIXIE = 0xFFFF9A3C;
    private static final int GREEN = 0xFF8FD18A;
    private static final int RED = 0xFFE07A5F;
    private static final int DARK = 0xFF2A1A10;
    private static final int SHAFT_DONE = 0xFFC9A14A;
    private static final int SHAFT_OPEN = 0xFFD98A4F;
    private static final int SHAFT_LOCKED = 0xFF4A3A30;
    private static final int SHAFT_EDGE = 0xFF1A100A;
    private static final int BAR_BACK = 0xFF1A120C;
    private static final int BAR_FILL = 0xFFD98A4F;
    private static final int BAR_DONE = 0xFF8FB85A;

    private static ResourceLocation lastChapter;
    private static final Map<ResourceLocation, float[]> VIEWS = new HashMap<>();

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double px, double py) {
            return px >= x && py >= y && px < x + w && py < y + h;
        }

        int right() {
            return x + w;
        }

        int bottom() {
            return y + h;
        }
    }

    private record Hit(Rect area, Runnable action) {
    }

    private record Tip(Rect area, List<Component> lines, ItemStack stack) {
    }

    private Rect frame;
    private Rect list;
    private Rect map;
    private Rect detail;
    private EditBox search;
    private ResourceLocation chapter;
    private int selected = -1;
    private float panX;
    private float panY;
    private int zoom = 2;
    private int listScroll;
    private int detailScroll;
    private int detailContent;
    private boolean dragging;
    private boolean dragged;
    private int seenRevision = -1;
    private final Map<String, Integer> choices = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private final List<Tip> tips = new ArrayList<>();
    private final Map<Integer, int[]> nodes = new HashMap<>();

    public QuestBookScreen() {
        super(Component.translatable("screen.rift_quests.title"));
    }

    @Override
    protected void init() {
        frame = new Rect(MARGIN, MARGIN, width - 2 * MARGIN, height - 2 * MARGIN);
        int bodyTop = frame.y() + HEADER;
        int bodyHeight = frame.bottom() - 6 - bodyTop;
        list = new Rect(frame.x() + 6, bodyTop, LIST_WIDTH, bodyHeight);
        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, list.x() + 5, list.y() + 5, list.w() - 10, 14, Component.translatable("screen.rift_quests.search"));
        search.setHint(Component.translatable("screen.rift_quests.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setMaxLength(48);
        search.setValue(query);
        search.setResponder(value -> listScroll = 0);
        addWidget(search);
        if (chapter == null) {
            chapter = pickChapter();
        }
        layoutDetail();
        if (!VIEWS.containsKey(chapter)) {
            centerChapter();
        } else {
            float[] view = VIEWS.get(chapter);
            panX = view[0];
            panY = view[1];
            zoom = (int) view[2];
        }
    }

    private void layoutDetail() {
        int bodyTop = list.y();
        int bodyHeight = list.h();
        int detailWidth = Mth.clamp(width / 3, 150, 210);
        detail = selected >= 0 ? new Rect(frame.right() - 6 - detailWidth, bodyTop, detailWidth, bodyHeight) : null;
        int mapRight = detail == null ? frame.right() - 6 : detail.x() - 4;
        map = new Rect(list.right() + 4, bodyTop, mapRight - list.right() - 4, bodyHeight);
    }

    private ResourceLocation pickChapter() {
        BookView book = ClientQuests.book();
        if (lastChapter != null && book.chapters().stream().anyMatch(view -> view.id().equals(lastChapter))) {
            return lastChapter;
        }
        for (BookView.ChapterView view : book.chapters()) {
            if (ClientQuests.chapterUnlocked(view) && ClientQuests.done(view.id()) < ClientQuests.questsOf(view.id()).size()) {
                return view.id();
            }
        }
        return book.chapters().isEmpty() ? null : book.chapters().get(0).id();
    }

    private BookView.ChapterView chapterView() {
        for (BookView.ChapterView view : ClientQuests.book().chapters()) {
            if (view.id().equals(chapter)) {
                return view;
            }
        }
        return null;
    }

    private float scale() {
        return ZOOMS[zoom];
    }

    private void centerChapter() {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int position : ClientQuests.questsOf(chapter)) {
            BookView.QuestView quest = ClientQuests.quest(position);
            minX = Math.min(minX, quest.x() * UNIT);
            maxX = Math.max(maxX, quest.x() * UNIT);
            minY = Math.min(minY, quest.y() * UNIT);
            maxY = Math.max(maxY, quest.y() * UNIT);
        }
        if (minX == Integer.MAX_VALUE) {
            panX = map.w() / 2.0F;
            panY = map.h() / 2.0F;
            return;
        }
        float s = scale();
        float contentWidth = (maxX - minX) * s;
        panX = contentWidth + 60 < map.w() ? map.w() / 2.0F - (minX + maxX) / 2.0F * s : 34 - minX * s;
        float contentHeight = (maxY - minY) * s;
        panY = contentHeight + 60 < map.h() ? map.h() / 2.0F - (minY + maxY) / 2.0F * s : 34 - minY * s;
    }

    private void focusQuest(int position) {
        BookView.QuestView quest = ClientQuests.quest(position);
        if (!quest.chapter().equals(chapter)) {
            switchChapter(quest.chapter());
        }
        select(position);
        float s = scale();
        panX = map.w() / 2.0F - quest.x() * UNIT * s;
        panY = map.h() / 2.0F - quest.y() * UNIT * s;
    }

    private void switchChapter(ResourceLocation id) {
        saveView();
        chapter = id;
        lastChapter = id;
        selected = -1;
        layoutDetail();
        if (VIEWS.containsKey(id)) {
            float[] view = VIEWS.get(id);
            panX = view[0];
            panY = view[1];
            zoom = (int) view[2];
        } else {
            centerChapter();
        }
    }

    private void select(int position) {
        if (selected != position) {
            selected = position;
            detailScroll = 0;
            choices.clear();
            layoutDetail();
        }
    }

    private void saveView() {
        if (chapter != null) {
            VIEWS.put(chapter, new float[] {panX, panY, zoom});
        }
    }

    @Override
    public void removed() {
        saveView();
        lastChapter = chapter;
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (seenRevision != ClientQuests.revision()) {
            seenRevision = ClientQuests.revision();
            if (chapter == null || chapterView() == null) {
                chapter = pickChapter();
                selected = -1;
                layoutDetail();
                centerChapter();
            }
            if (selected >= ClientQuests.book().quests().size()) {
                selected = -1;
                layoutDetail();
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        hits.clear();
        tips.clear();
        graphics.blitSprite(Sprites.PANEL_BRASS, frame.x(), frame.y(), frame.w(), frame.h());
        if (!ClientQuests.loaded()) {
            graphics.drawCenteredString(font, Component.translatable("screen.rift_quests.loading"), width / 2, height / 2 - 4, MUTED);
            return;
        }
        renderHeader(graphics);
        renderList(graphics, mouseX, mouseY);
        search.render(graphics, mouseX, mouseY, partialTick);
        renderMap(graphics, mouseX, mouseY);
        if (detail != null && selected >= 0) {
            renderDetail(graphics, mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics graphics) {
        int x = frame.x() + 8;
        int y = frame.y() + 6;
        graphics.blitSprite(Sprites.CREST, x, y, 16, 16);
        graphics.drawString(font, Component.translatable("screen.rift_quests.title"), x + 20, y + 1, BRASS, true);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 20, y + 11, 0);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(font, Component.translatable("screen.rift_quests.motto"), 0, 0, MUTED, false);
        graphics.pose().popPose();
        BookView.ChapterView view = chapterView();
        if (view != null) {
            Component title = view.title().copy().withStyle(style -> style.withColor(NIXIE));
            int titleWidth = font.width(title);
            int centre = frame.x() + frame.w() / 2;
            graphics.fill(centre - titleWidth / 2 - 6, y, centre + titleWidth / 2 + 6, y + 15, 0xFF120A06);
            graphics.renderOutline(centre - titleWidth / 2 - 6, y, titleWidth + 12, 15, 0xFF5A3A22);
            graphics.drawString(font, title, centre - titleWidth / 2, y + 4, NIXIE, true);
            if (view.description().isPresent()) {
                tips.add(new Tip(new Rect(centre - titleWidth / 2 - 6, y, titleWidth + 12, 15), List.of(view.description().get()), ItemStack.EMPTY));
            }
        }
        int right = frame.right() - 8;
        int tickets = ticketCount();
        String ticketText = "x" + tickets;
        int ticketWidth = font.width(ticketText);
        graphics.drawString(font, ticketText, right - ticketWidth, y + 4, GOLD, true);
        ItemStack ticket = new ItemStack(ModItems.RIFTWATCH_TICKET.get());
        graphics.renderFakeItem(ticket, right - ticketWidth - 18, y);
        tips.add(new Tip(new Rect(right - ticketWidth - 18, y, ticketWidth + 18, 16),
                List.of(ticket.getHoverName(), Component.translatable("screen.rift_quests.tickets", tickets).withStyle(ChatFormatting.GRAY)), ItemStack.EMPTY));
        int total = ClientQuests.book().quests().size();
        int done = ClientQuests.totalDone();
        int dialX = right - ticketWidth - 18 - 8 - 30;
        renderDial(graphics, dialX, y - 1, total == 0 ? 0 : done / (float) total);
        String count = done + "/" + total;
        graphics.drawString(font, count, dialX - 4 - font.width(count), y + 4, TEXT, true);
        tips.add(new Tip(new Rect(dialX - 4 - font.width(count), y - 1, font.width(count) + 34, 16),
                List.of(Component.translatable("screen.rift_quests.progress", done, total)), ItemStack.EMPTY));
    }

    private void renderDial(GuiGraphics graphics, int x, int y, float fraction) {
        graphics.blitSprite(Sprites.DIAL, x, y, 30, 16);
        double litFrom = Math.PI * (1.0 - fraction);
        for (int py = 0; py < 16; py++) {
            for (int px = 0; px < 30; px++) {
                double dx = px + 0.5 - 14.5;
                double dy = 14.5 - (py + 0.5);
                double radius = Math.sqrt(dx * dx + dy * dy);
                if (radius < 8.6 || radius > 10.6 || dy < 0) {
                    continue;
                }
                double angle = Math.atan2(dy, dx);
                if (angle >= litFrom) {
                    graphics.fill(x + px, y + py, x + px + 1, y + py + 1, radius > 9.8 ? 0xFFFFC070 : 0xFFFF9A3C);
                }
            }
        }
        double needle = Math.PI * (1.0 - fraction);
        for (int step = 0; step <= 18; step++) {
            double length = step / 2.0;
            int px = (int) Math.round(14.5 + Math.cos(needle) * length - 0.5);
            int py = (int) Math.round(14.5 - Math.sin(needle) * length - 0.5);
            graphics.fill(x + px, y + py, x + px + 1, y + py + 1, 0xFFF4EDE8);
        }
        graphics.fill(x + 14, y + 13, x + 16, y + 15, 0xFFFFE39A);
    }

    private int ticketCount() {
        if (minecraft == null || minecraft.player == null) {
            return 0;
        }
        int count = 0;
        var inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.RIFTWATCH_TICKET.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.blitSprite(Sprites.PANEL_ANDESITE, list.x(), list.y(), list.w(), list.h());
        Rect rows = new Rect(list.x() + 4, list.y() + 23, list.w() - 8, list.h() - 27);
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        int contentHeight;
        graphics.enableScissor(rows.x(), rows.y(), rows.right(), rows.bottom());
        if (query.isEmpty()) {
            List<BookView.ChapterView> chapters = ClientQuests.book().chapters();
            contentHeight = chapters.size() * ROW;
            listScroll = Mth.clamp(listScroll, 0, Math.max(0, contentHeight - rows.h()));
            for (int index = 0; index < chapters.size(); index++) {
                BookView.ChapterView view = chapters.get(index);
                int rowY = rows.y() + index * ROW - listScroll;
                if (rowY + ROW < rows.y() || rowY > rows.bottom()) {
                    continue;
                }
                renderChapterRow(graphics, view, rows.x(), rowY, rows.w(), rows, mouseX, mouseY);
            }
        } else {
            List<Integer> matches = new ArrayList<>();
            for (int position = 0; position < ClientQuests.book().quests().size(); position++) {
                if (searchable(position) && ClientQuests.quest(position).title().getString().toLowerCase(Locale.ROOT).contains(query)) {
                    matches.add(position);
                }
            }
            contentHeight = Math.max(1, matches.size()) * ROW;
            listScroll = Mth.clamp(listScroll, 0, Math.max(0, contentHeight - rows.h()));
            if (matches.isEmpty()) {
                graphics.drawString(font, Component.translatable("screen.rift_quests.no_results"), rows.x() + 4, rows.y() + 6, MUTED, false);
            }
            for (int index = 0; index < matches.size(); index++) {
                int position = matches.get(index);
                int rowY = rows.y() + index * ROW - listScroll;
                if (rowY + ROW < rows.y() || rowY > rows.bottom()) {
                    continue;
                }
                BookView.QuestView quest = ClientQuests.quest(position);
                Rect row = new Rect(rows.x(), rowY, rows.w(), ROW - 2);
                boolean hover = row.contains(mouseX, mouseY) && rows.contains(mouseX, mouseY);
                graphics.fill(row.x(), row.y(), row.right(), row.bottom(), hover ? 0x40FFC070 : 0x20000000);
                if (!quest.icon().isEmpty()) {
                    graphics.renderFakeItem(quest.icon().get(0), row.x() + 2, row.y() + 1);
                }
                drawClipped(graphics, quest.title(), row.x() + 21, row.y() + 5, row.w() - 24, stateColour(ClientQuests.state(position)));
                if (rows.contains(mouseX, mouseY)) {
                    hits.add(new Hit(row, () -> focusQuest(position)));
                }
            }
        }
        graphics.disableScissor();
        if (contentHeight > rows.h()) {
            int barHeight = Math.max(12, rows.h() * rows.h() / contentHeight);
            int barY = rows.y() + (rows.h() - barHeight) * listScroll / Math.max(1, contentHeight - rows.h());
            graphics.fill(list.right() - 4, barY, list.right() - 2, barY + barHeight, 0xFF8A8A8A);
        }
    }

    private boolean searchable(int position) {
        BookView.QuestView quest = ClientQuests.quest(position);
        QuestState state = ClientQuests.state(position);
        if (quest.visibility() == Visibility.SECRET && !state.done()) {
            return false;
        }
        return !(quest.visibility() == Visibility.HIDDEN_UNTIL_UNLOCKED && state == QuestState.LOCKED);
    }

    private void renderChapterRow(GuiGraphics graphics, BookView.ChapterView view, int x, int y, int w, Rect clip, int mouseX, int mouseY) {
        Rect row = new Rect(x, y, w, ROW - 2);
        boolean current = view.id().equals(chapter);
        boolean unlocked = ClientQuests.chapterUnlocked(view);
        boolean hover = row.contains(mouseX, mouseY) && clip.contains(mouseX, mouseY);
        graphics.fill(row.x(), row.y(), row.right(), row.bottom(), current ? 0x50D98A4F : hover ? 0x30FFC070 : 0x20000000);
        graphics.blitSprite(unlocked ? current ? Sprites.LEVER_ON : Sprites.LEVER_OFF : Sprites.LEVER_LOCKED, x + 2, y + 3, 9, 11);
        if (!view.icon().isEmpty()) {
            graphics.renderFakeItem(view.icon().get(0), x + 13, y + 1);
        }
        int total = ClientQuests.questsOf(view.id()).size();
        int done = ClientQuests.done(view.id());
        String count = done + "/" + total;
        int countWidth = font.width(count);
        int unclaimed = ClientQuests.unclaimed(view.id());
        int titleWidth = w - 34 - countWidth - 4 - (unclaimed > 0 ? 9 : 0);
        drawClipped(graphics, view.title(), x + 32, y + 5, titleWidth, unlocked ? current ? GOLD : TEXT : MUTED);
        graphics.drawString(font, count, x + w - countWidth - 2, y + 5, done == total && total > 0 ? GREEN : MUTED, false);
        if (unclaimed > 0) {
            graphics.blitSprite(Sprites.BANG, x + w - countWidth - 11, y + 3, 7, 11);
        }
        if (!unlocked) {
            graphics.blitSprite(Sprites.LOCK, x + 20, y + 8, 9, 11);
        }
        if (clip.contains(mouseX, mouseY)) {
            hits.add(new Hit(row, () -> switchChapter(view.id())));
            List<Component> lines = new ArrayList<>();
            lines.add(view.title());
            view.description().ifPresent(description -> lines.add(description.copy().withStyle(ChatFormatting.GRAY)));
            if (!unlocked) {
                lines.add(Component.translatable("screen.rift_quests.chapter_locked").withStyle(ChatFormatting.RED));
                for (ResourceLocation required : view.unlock()) {
                    int position = ClientQuests.indexOf(required);
                    if (position >= 0 && !ClientQuests.state(position).done()) {
                        lines.add(Component.literal("- ").append(ClientQuests.quest(position).title()).withStyle(ChatFormatting.GRAY));
                    }
                }
            }
            if (unclaimed > 0) {
                lines.add(Component.translatable("screen.rift_quests.unclaimed", unclaimed).withStyle(ChatFormatting.GOLD));
            }
            tips.add(new Tip(row, lines, ItemStack.EMPTY));
        }
    }

    private void drawClipped(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int colour) {
        if (font.width(text) <= maxWidth) {
            graphics.drawString(font, text, x, y, colour, false);
            return;
        }
        String plain = font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - font.width("..."))) + "...";
        graphics.drawString(font, plain, x, y, colour, false);
    }

    private boolean visible(int position) {
        BookView.QuestView quest = ClientQuests.quest(position);
        return !(quest.visibility() == Visibility.HIDDEN_UNTIL_UNLOCKED && ClientQuests.state(position) == QuestState.LOCKED);
    }

    private boolean secret(int position) {
        return ClientQuests.quest(position).visibility() == Visibility.SECRET && !ClientQuests.state(position).done();
    }

    private void renderMap(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.blitSprite(Sprites.INSET, map.x(), map.y(), map.w(), map.h());
        nodes.clear();
        if (chapter == null) {
            return;
        }
        float s = scale();
        List<Integer> quests = ClientQuests.questsOf(chapter);
        graphics.enableScissor(map.x() + 2, map.y() + 2, map.right() - 2, map.bottom() - 2);
        graphics.pose().pushPose();
        graphics.pose().translate(map.x() + panX, map.y() + panY, 0);
        graphics.pose().scale(s, s, 1.0F);
        int frameTick = (int) (Util.getMillis() / 180L);
        for (int position : quests) {
            if (!visible(position)) {
                continue;
            }
            BookView.QuestView quest = ClientQuests.quest(position);
            for (ResourceLocation dependency : quest.dependencies()) {
                int from = ClientQuests.indexOf(dependency);
                if (from < 0 || !ClientQuests.quest(from).chapter().equals(chapter) || !visible(from)) {
                    continue;
                }
                renderShaft(graphics, ClientQuests.quest(from), quest, shaftColour(from, position), frameTick);
            }
        }
        int hovered = -1;
        double localX = (mouseX - map.x() - panX) / s;
        double localY = (mouseY - map.y() - panY) / s;
        boolean mouseInMap = map.contains(mouseX, mouseY);
        for (int position : quests) {
            if (!visible(position)) {
                continue;
            }
            BookView.QuestView quest = ClientQuests.quest(position);
            int cx = quest.x() * UNIT;
            int cy = quest.y() * UNIT;
            int half = quest.size() == QuestSize.SMALL ? 11 : 13;
            nodes.put(position, new int[] {cx, cy, half});
            if (mouseInMap && Math.abs(localX - cx) <= half && Math.abs(localY - cy) <= half) {
                hovered = position;
            }
            renderNode(graphics, position, quest, cx, cy, half, frameTick);
        }
        if (hovered >= 0) {
            int[] node = nodes.get(hovered);
            graphics.renderOutline(node[0] - node[2] - 1, node[1] - node[2] - 1, node[2] * 2 + 2, node[2] * 2 + 2, 0xFFFFE39A);
        }
        graphics.pose().popPose();
        graphics.disableScissor();
        String zoomText = Math.round(s * 100) + "%";
        int zoomX = map.right() - 6 - font.width(zoomText);
        graphics.fill(zoomX - 3, map.bottom() - 14, map.right() - 3, map.bottom() - 3, 0xC0120A06);
        graphics.drawString(font, zoomText, zoomX, map.bottom() - 12, MUTED, false);
        if (hovered >= 0) {
            int position = hovered;
            tips.add(new Tip(new Rect(mouseX, mouseY, 1, 1), nodeTooltip(position), ItemStack.EMPTY));
        }
        if (quests.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.rift_quests.empty_chapter"), map.x() + map.w() / 2, map.y() + map.h() / 2, MUTED);
        }
    }

    private List<Component> nodeTooltip(int position) {
        List<Component> lines = new ArrayList<>();
        BookView.QuestView quest = ClientQuests.quest(position);
        QuestState state = ClientQuests.state(position);
        lines.add(secret(position) ? Component.literal("???") : quest.title());
        if (!secret(position)) {
            quest.subtitle().ifPresent(subtitle -> lines.add(subtitle.copy().withStyle(ChatFormatting.GRAY)));
        }
        lines.add(stateLabel(state).copy().withStyle(style -> style.withColor(stateColour(state))));
        if (quest.team()) {
            lines.add(Component.translatable("screen.rift_quests.team_short", quest.teamMinPlayers()).withStyle(ChatFormatting.AQUA));
        }
        return lines;
    }

    private int shaftColour(int from, int to) {
        if (!ClientQuests.state(from).done()) {
            return SHAFT_LOCKED;
        }
        return ClientQuests.state(to).done() ? SHAFT_DONE : SHAFT_OPEN;
    }

    private void renderShaft(GuiGraphics graphics, BookView.QuestView from, BookView.QuestView to, int colour, int frameTick) {
        int x1 = from.x() * UNIT;
        int y1 = from.y() * UNIT;
        int x2 = to.x() * UNIT;
        int y2 = to.y() * UNIT;
        if (x1 == x2 || y1 == y2) {
            line(graphics, x1, y1, x2, y2, colour);
            return;
        }
        int bend = x1 < x2 ? x2 - UNIT : x2 + UNIT;
        if (x1 < x2 ? bend < x1 : bend > x1) {
            bend = (x1 + x2) / 2;
        }
        line(graphics, x1, y1, bend, y1, colour);
        line(graphics, bend, y1, bend, y2, colour);
        line(graphics, bend, y2, x2, y2, colour);
        boolean turning = colour == SHAFT_OPEN;
        graphics.blitSprite(colour == SHAFT_LOCKED ? Sprites.COG_SMALL_RUST : turning ? Sprites.cogSmallWarm(frameTick) : Sprites.COG_SMALL_WOOD,
                bend - 7, y1 - 7, 14, 14);
        graphics.blitSprite(colour == SHAFT_LOCKED ? Sprites.COG_SMALL_RUST : turning ? Sprites.cogSmallWarm(frameTick + 2) : Sprites.COG_SMALL_WOOD,
                bend - 7, y2 - 7, 14, 14);
    }

    private void line(GuiGraphics graphics, int x1, int y1, int x2, int y2, int colour) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        graphics.fill(minX - 2, minY - 2, maxX + 2, maxY + 2, SHAFT_EDGE);
        graphics.fill(minX - 1, minY - 1, maxX + 1, maxY + 1, colour);
    }

    private void renderNode(GuiGraphics graphics, int position, BookView.QuestView quest, int cx, int cy, int half, int frameTick) {
        QuestState state = ClientQuests.state(position);
        boolean locked = state == QuestState.LOCKED;
        boolean active = state == QuestState.AVAILABLE || state == QuestState.IN_PROGRESS || state == QuestState.COMPLETE;
        if (quest.size() == QuestSize.LARGE) {
            ResourceLocation ring = locked ? Sprites.COG_RING_RUST : active ? Sprites.cogRingWarm(frameTick) : Sprites.COG_RING_WOOD;
            graphics.blitSprite(ring, cx - 23, cy - 23, 46, 46);
        }
        ResourceLocation casing = quest.size() == QuestSize.SMALL
                ? locked ? Sprites.CASING_SMALL_RUST : Sprites.CASING_SMALL
                : locked ? Sprites.CASING_MEDIUM_RUST : Sprites.CASING_MEDIUM;
        graphics.blitSprite(casing, cx - half, cy - half, half * 2, half * 2);
        if (position == selected) {
            graphics.renderOutline(cx - half - 2, cy - half - 2, half * 2 + 4, half * 2 + 4, GOLD);
        }
        if (secret(position)) {
            graphics.drawCenteredString(font, "?", cx, cy - 4, MUTED);
        } else if (!quest.icon().isEmpty()) {
            ItemStack icon = quest.icon().get((int) (Util.getMillis() / 1000L % quest.icon().size()));
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 10);
            graphics.renderFakeItem(icon, cx - 8, cy - 8);
            graphics.pose().popPose();
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 220);
        if (locked) {
            graphics.blitSprite(Sprites.LOCK, cx + half - 6, cy - half - 4, 9, 11);
        } else if (state == QuestState.CLAIMED) {
            graphics.blitSprite(Sprites.CHECK, cx + half - 6, cy - half - 3, 9, 8);
        } else if (state == QuestState.COMPLETE) {
            graphics.blitSprite(Sprites.BANG, cx + half - 5, cy - half - 4, 7, 11);
        }
        if (quest.team()) {
            graphics.blitSprite(Sprites.TEAM, cx - half - 4, cy + half - 6, 14, 9);
        }
        if (locked) {
            graphics.fill(cx - half, cy - half, cx + half, cy + half, 0x70000000);
        }
        graphics.pose().popPose();
    }

    private void renderDetail(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.blitSprite(Sprites.PANEL_COPPER, detail.x(), detail.y(), detail.w(), detail.h());
        int position = selected;
        BookView.QuestView quest = ClientQuests.quest(position);
        QuestState state = ClientQuests.state(position);
        boolean hidden = secret(position);
        int buttonsHeight = 22;
        Rect body = new Rect(detail.x() + 6, detail.y() + 6, detail.w() - 12, detail.h() - 12 - buttonsHeight);
        int textWidth = body.w() - 6;
        detailScroll = Mth.clamp(detailScroll, 0, Math.max(0, detailContent - body.h()));
        graphics.enableScissor(body.x(), body.y(), body.right(), body.bottom());
        int x = body.x();
        int y = body.y() - detailScroll;
        int top = y;
        for (FormattedCharSequence line : font.split(hidden ? Component.literal("???") : quest.title(), textWidth)) {
            graphics.drawString(font, line, x, y, GOLD, true);
            y += 10;
        }
        if (!hidden && quest.subtitle().isPresent()) {
            for (FormattedCharSequence line : font.split(quest.subtitle().get(), textWidth)) {
                graphics.drawString(font, line, x, y, MUTED, false);
                y += 10;
            }
        }
        y += 2;
        Component status = Component.translatable("screen.rift_quests.size." + quest.size().name().toLowerCase(Locale.ROOT))
                .append(" | ").append(stateLabel(state));
        graphics.drawString(font, status, x, y, stateColour(state), false);
        y += 12;
        if (quest.team()) {
            graphics.blitSprite(Sprites.TEAM, x, y, 14, 9);
            Component team = Component.translatable("screen.rift_quests.team", quest.teamMinPlayers(), quest.teamRadius());
            for (FormattedCharSequence line : font.split(team, textWidth - 18)) {
                graphics.drawString(font, line, x + 18, y, 0xFF7FD4E0, false);
                y += 10;
            }
            if (ClientQuests.waitingForTeam(position)) {
                graphics.drawString(font, Component.translatable("tracker.rift_quests.team"), x + 18, y, GOLD, false);
                y += 10;
            }
            y += 2;
        }
        if (!hidden && quest.description().isPresent()) {
            for (FormattedCharSequence line : font.split(quest.description().get(), textWidth)) {
                graphics.drawString(font, line, x, y, 0xFFE8DCCF, false);
                y += 10;
            }
            y += 4;
        }
        if (!quest.dependencies().isEmpty()) {
            y = heading(graphics, Component.translatable(quest.dependencies().size() > 1 && quest.dependencyMode() == DependencyMode.ANY
                    ? "screen.rift_quests.requires_any" : "screen.rift_quests.requires"), x, y, body.w());
            for (ResourceLocation dependency : quest.dependencies()) {
                int from = ClientQuests.indexOf(dependency);
                if (from < 0) {
                    continue;
                }
                boolean done = ClientQuests.state(from).done();
                graphics.blitSprite(done ? Sprites.CHECK : Sprites.LOCK, x, y + (done ? 1 : -1), 9, done ? 8 : 11);
                Rect row = new Rect(x, y - 1, body.w(), 10);
                boolean hover = row.contains(mouseX, mouseY) && body.contains(mouseX, mouseY);
                drawClipped(graphics, secret(from) ? Component.literal("???") : ClientQuests.quest(from).title(), x + 12, y, textWidth - 12,
                        hover ? GOLD : done ? TEXT : MUTED);
                if (body.contains(mouseX, mouseY) && visible(from)) {
                    hits.add(new Hit(row, () -> focusQuest(from)));
                }
                y += 11;
            }
            y += 3;
        }
        y = heading(graphics, Component.translatable("screen.rift_quests.tasks"), x, y, body.w());
        boolean active = state == QuestState.AVAILABLE || state == QuestState.IN_PROGRESS;
        for (int task = 0; task < quest.tasks().size(); task++) {
            BookView.TaskView view = quest.tasks().get(task);
            long progress = ClientQuests.progress(position, task);
            boolean taskDone = progress >= view.target();
            int rowTop = y;
            ItemStack icon = view.icons().isEmpty() ? ItemStack.EMPTY : view.icons().get((int) (Util.getMillis() / 1000L % view.icons().size()));
            if (!icon.isEmpty()) {
                graphics.blitSprite(Sprites.SLOT, x, y, 18, 18);
                graphics.renderFakeItem(icon, x + 1, y + 1);
                Rect slot = new Rect(x, y, 18, 18);
                if (body.contains(mouseX, mouseY)) {
                    tips.add(new Tip(slot, List.of(), icon));
                    hits.add(new Hit(slot, () -> EmiLink.showRecipes(icon)));
                }
            }
            int textX = icon.isEmpty() ? x : x + 22;
            int labelWidth = body.right() - textX - 4;
            List<FormattedCharSequence> label = font.split(hidden ? Component.literal("???") : view.label(), labelWidth);
            for (FormattedCharSequence line : label) {
                graphics.drawString(font, line, textX, y + 1, taskDone ? GREEN : TEXT, false);
                y += 10;
            }
            y = Math.max(y, rowTop + 10);
            boolean actionable = active && !taskDone && view.action() != BookView.TaskAction.NONE;
            int barRight = actionable ? body.right() - 48 : body.right() - 4;
            int barY = y + 1;
            graphics.fill(textX, barY, barRight, barY + 5, BAR_BACK);
            float fraction = view.target() <= 0 ? 1.0F : Math.min(1.0F, progress / (float) view.target());
            graphics.fill(textX, barY, textX + Math.round((barRight - textX) * fraction), barY + 5, taskDone ? BAR_DONE : BAR_FILL);
            graphics.renderOutline(textX - 1, barY - 1, barRight - textX + 2, 7, 0xFF3A2416);
            String count = progress + "/" + view.target();
            y = barY + 7;
            graphics.pose().pushPose();
            graphics.pose().translate(textX, y, 0);
            graphics.pose().scale(0.75F, 0.75F, 1.0F);
            graphics.drawString(font, count, 0, 0, MUTED, false);
            graphics.pose().popPose();
            if (actionable) {
                String key = view.key();
                Component text = Component.translatable(view.action() == BookView.TaskAction.CHECK ? "screen.rift_quests.check" : "screen.rift_quests.submit");
                Rect button = new Rect(body.right() - 44, barY - 4, 40, 13);
                button(graphics, button, text, true, mouseX, mouseY, body);
                if (body.contains(mouseX, mouseY)) {
                    hits.add(new Hit(button, () -> PacketDistributor.sendToServer(new Payloads.TaskAction(quest.id(), key))));
                }
            }
            y = Math.max(y + 9, rowTop + 20);
            if (!view.details().isEmpty() && body.contains(mouseX, mouseY)) {
                Rect area = new Rect(textX, rowTop, body.right() - textX, y - rowTop);
                List<Component> lines = new ArrayList<>(view.details());
                tips.add(new Tip(area, lines, ItemStack.EMPTY));
            }
            y += 3;
        }
        y += 2;
        y = heading(graphics, Component.translatable("screen.rift_quests.rewards"), x, y, body.w());
        y = renderRewards(graphics, quest.rewards(), position, true, x, y, body, mouseX, mouseY);
        if (quest.team() && !quest.teamRepeatRewards().isEmpty()) {
            y += 2;
            graphics.drawString(font, Component.translatable("screen.rift_quests.team_repeat"), x, y, MUTED, false);
            y += 11;
            y = renderRewards(graphics, quest.teamRepeatRewards(), position, false, x, y, body, mouseX, mouseY);
        }
        detailContent = y + detailScroll - top + 4;
        graphics.disableScissor();
        if (detailContent > body.h()) {
            int barHeight = Math.max(12, body.h() * body.h() / detailContent);
            int barY = body.y() + (body.h() - barHeight) * detailScroll / Math.max(1, detailContent - body.h());
            graphics.fill(detail.right() - 5, barY, detail.right() - 3, barY + barHeight, 0xFFB86A3A);
        }
        int buttonY = detail.bottom() - 6 - 16;
        int buttonWidth = (detail.w() - 12 - 4) * 2 / 3;
        Rect claim = new Rect(detail.x() + 6, buttonY, buttonWidth, 16);
        boolean claimable = state == QuestState.COMPLETE && choicesReady(quest, position);
        button(graphics, claim, Component.translatable(state == QuestState.CLAIMED ? "screen.rift_quests.claimed" : "screen.rift_quests.claim"),
                claimable, mouseX, mouseY, null);
        if (claimable) {
            hits.add(new Hit(claim, () -> PacketDistributor.sendToServer(new Payloads.Claim(quest.id(), Map.copyOf(choices)))));
        } else if (state == QuestState.COMPLETE) {
            tips.add(new Tip(claim, List.of(Component.translatable("screen.rift_quests.choose_first")), ItemStack.EMPTY));
        }
        Rect pin = new Rect(claim.right() + 4, buttonY, detail.right() - 6 - claim.right() - 4, 16);
        boolean pinned = ClientQuests.isPinned(position);
        boolean pinnable = pinned || !state.done();
        button(graphics, pin, Component.translatable(pinned ? "screen.rift_quests.unpin" : "screen.rift_quests.pin"), pinnable, mouseX, mouseY, null);
        if (pinnable) {
            hits.add(new Hit(pin, () -> PacketDistributor.sendToServer(new Payloads.Pin(quest.id()))));
            tips.add(new Tip(pin, List.of(Component.translatable("screen.rift_quests.pin_hint")), ItemStack.EMPTY));
        }
    }

    private boolean choicesReady(BookView.QuestView quest, int position) {
        for (int index = 0; index < quest.rewards().size(); index++) {
            BookView.RewardView reward = quest.rewards().get(index);
            if (reward.kind() == BookView.RewardKind.CHOICE && !ClientQuests.claimed(position, index) && !choices.containsKey(reward.key())) {
                return false;
            }
        }
        return true;
    }

    private int renderRewards(GuiGraphics graphics, List<BookView.RewardView> rewards, int position, boolean claimable, int x, int y,
            Rect body, int mouseX, int mouseY) {
        int slotX = x;
        for (int index = 0; index < rewards.size(); index++) {
            BookView.RewardView reward = rewards.get(index);
            boolean claimed = claimable && ClientQuests.claimed(position, index);
            if (reward.kind() == BookView.RewardKind.CHOICE) {
                if (slotX != x) {
                    slotX = x;
                    y += 20;
                }
                graphics.drawString(font, reward.label(), x, y + 1, claimed ? MUTED : GOLD, false);
                y += 11;
                Integer picked = choices.get(reward.key());
                for (int option = 0; option < reward.options().size(); option++) {
                    if (slotX + 18 > body.right()) {
                        slotX = x;
                        y += 20;
                    }
                    BookView.RewardView choice = reward.options().get(option);
                    Rect slot = new Rect(slotX, y, 18, 18);
                    rewardSlot(graphics, choice, slot, claimed, body, mouseX, mouseY);
                    if (picked != null && picked == option) {
                        graphics.renderOutline(slot.x() - 1, slot.y() - 1, 20, 20, GOLD);
                    }
                    if (!claimed && claimable && ClientQuests.state(position) == QuestState.COMPLETE && body.contains(mouseX, mouseY)) {
                        int chosen = option;
                        String key = reward.key();
                        hits.add(new Hit(slot, () -> choices.put(key, chosen)));
                    }
                    slotX += 20;
                }
                slotX = x;
                y += 21;
                continue;
            }
            if (slotX + 18 > body.right()) {
                slotX = x;
                y += 20;
            }
            rewardSlot(graphics, reward, new Rect(slotX, y, 18, 18), claimed, body, mouseX, mouseY);
            slotX += 20;
        }
        return slotX == x ? y : y + 21;
    }

    private void rewardSlot(GuiGraphics graphics, BookView.RewardView reward, Rect slot, boolean claimed, Rect body, int mouseX, int mouseY) {
        graphics.blitSprite(Sprites.SLOT, slot.x(), slot.y(), 18, 18);
        ItemStack stack = reward.stacks().isEmpty() ? ItemStack.EMPTY : reward.stacks().get((int) (Util.getMillis() / 1000L % reward.stacks().size()));
        graphics.renderFakeItem(stack, slot.x() + 1, slot.y() + 1);
        if (reward.amount() > 1) {
            graphics.renderItemDecorations(font, stack, slot.x() + 1, slot.y() + 1, String.valueOf(reward.amount()));
        }
        if (claimed) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 250);
            graphics.fill(slot.x() + 1, slot.y() + 1, slot.right() - 1, slot.bottom() - 1, 0x90000000);
            graphics.blitSprite(Sprites.CHECK, slot.x() + 5, slot.y() + 5, 9, 8);
            graphics.pose().popPose();
        }
        if (body.contains(mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            lines.add(reward.label());
            if (reward.kind() == BookView.RewardKind.ITEM && !stack.isEmpty()) {
                tips.add(new Tip(slot, List.of(), stack.copyWithCount(Math.max(1, Math.min(reward.amount(), stack.getMaxStackSize())))));
            } else {
                tips.add(new Tip(slot, lines, ItemStack.EMPTY));
            }
            if (reward.kind() == BookView.RewardKind.ITEM) {
                hits.add(new Hit(slot, () -> EmiLink.showRecipes(stack)));
            }
        }
    }

    private int heading(GuiGraphics graphics, Component text, int x, int y, int w) {
        graphics.drawString(font, text, x, y, BRASS, false);
        int textWidth = font.width(text);
        graphics.fill(x + textWidth + 4, y + 4, x + w - 4, y + 5, 0xFF6A4228);
        return y + 12;
    }

    private void button(GuiGraphics graphics, Rect area, Component text, boolean enabled, int mouseX, int mouseY, Rect clip) {
        boolean hover = enabled && area.contains(mouseX, mouseY) && (clip == null || clip.contains(mouseX, mouseY));
        graphics.blitSprite(enabled ? hover ? Sprites.BUTTON_HOVER : Sprites.BUTTON : Sprites.BUTTON_DISABLED, area.x(), area.y(), area.w(), area.h());
        int textWidth = font.width(text);
        int textX = area.x() + (area.w() - textWidth) / 2;
        graphics.drawString(font, text, textX, area.y() + (area.h() - 8) / 2, enabled ? DARK : 0xFF4A4030, false);
    }

    private Component stateLabel(QuestState state) {
        return Component.translatable("screen.rift_quests.state." + state.name().toLowerCase(Locale.ROOT));
    }

    private int stateColour(QuestState state) {
        return switch (state) {
            case LOCKED -> MUTED;
            case AVAILABLE -> TEXT;
            case IN_PROGRESS -> NIXIE;
            case COMPLETE -> GOLD;
            case CLAIMED -> GREEN;
        };
    }

    private void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int index = tips.size() - 1; index >= 0; index--) {
            Tip tip = tips.get(index);
            if (tip.area().contains(mouseX, mouseY) || tip.area().w() == 1 && tip.area().x() == mouseX && tip.area().y() == mouseY) {
                if (!tip.stack().isEmpty()) {
                    graphics.renderTooltip(font, tip.stack(), mouseX, mouseY);
                } else if (!tip.lines().isEmpty()) {
                    graphics.renderComponentTooltip(font, tip.lines(), mouseX, mouseY);
                }
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (search.mouseClicked(mouseX, mouseY, button)) {
            setFocused(search);
            return true;
        }
        search.setFocused(false);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int index = hits.size() - 1; index >= 0; index--) {
                Hit hit = hits.get(index);
                if (hit.area().contains(mouseX, mouseY)) {
                    playClick();
                    hit.action().run();
                    return true;
                }
            }
        }
        if (map != null && map.contains(mouseX, mouseY)) {
            dragging = true;
            dragged = false;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            panX += (float) dragX;
            panY += (float) dragY;
            if (Math.abs(dragX) + Math.abs(dragY) > 0.5) {
                dragged = true;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            if (!dragged && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                int position = nodeAt(mouseX, mouseY);
                if (position >= 0) {
                    playClick();
                    select(position);
                } else if (selected >= 0) {
                    select(-1);
                }
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int nodeAt(double mouseX, double mouseY) {
        float s = scale();
        double localX = (mouseX - map.x() - panX) / s;
        double localY = (mouseY - map.y() - panY) / s;
        for (Map.Entry<Integer, int[]> entry : nodes.entrySet()) {
            int[] node = entry.getValue();
            if (Math.abs(localX - node[0]) <= node[2] && Math.abs(localY - node[1]) <= node[2]) {
                return entry.getKey();
            }
        }
        return -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (list.contains(mouseX, mouseY)) {
            listScroll -= (int) (scrollY * ROW);
            return true;
        }
        if (detail != null && detail.contains(mouseX, mouseY)) {
            detailScroll -= (int) (scrollY * 12);
            return true;
        }
        if (map.contains(mouseX, mouseY)) {
            int next = Mth.clamp(zoom + (scrollY > 0 ? 1 : -1), 0, ZOOMS.length - 1);
            if (next != zoom) {
                float before = scale();
                zoom = next;
                float after = scale();
                double anchorX = mouseX - map.x();
                double anchorY = mouseY - map.y();
                panX = (float) (anchorX - (anchorX - panX) * after / before);
                panY = (float) (anchorY - (anchorY - panY) * after / before);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (search.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                search.setFocused(false);
                return true;
            }
            return search.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (ClientSetup.OPEN_BOOK.matches(keyCode, scanCode) && !ClientSetup.OPEN_BOOK.isUnbound()) {
            onClose();
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (search.isFocused()) {
            return search.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
