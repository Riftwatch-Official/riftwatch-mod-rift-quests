package net.riftwatch.rift_quests.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.riftwatch.rift_quests.book.DependencyMode;
import net.riftwatch.rift_quests.book.QuestSize;
import net.riftwatch.rift_quests.book.Visibility;
import net.riftwatch.rift_quests.client.art.Art;
import net.riftwatch.rift_quests.client.art.Canvas;
import net.riftwatch.rift_quests.client.art.Layer;
import net.riftwatch.rift_quests.item.RainbowText;
import net.riftwatch.rift_quests.network.BookView;
import net.riftwatch.rift_quests.network.Payloads;
import net.riftwatch.rift_quests.network.QuestState;
import net.riftwatch.rift_quests.registry.ModItems;
import org.lwjgl.glfw.GLFW;

public final class QuestBookScreen extends Screen {
    private static final int ROW = 14;
    private static final int RESULT_ROW = 18;
    private static final int PAD = 44;
    private static final int STEP_X = 28;
    private static final int STEP_Y = 20;
    private static final int EDGE = 8;
    private static final long FRAME_MILLIS = 140L;
    private static final long LEVER_MILLIS = 90L;
    private static final long SLIDE_MILLIS = 200L;
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    private static final int TOP = 4;
    private static final int BOTTOM = 8;

    private static final int PAPER = 0xFFF4EDE8;
    private static final int SAND = 0xFFC9A58A;
    private static final int LCOPPER = 0xFFE8B48A;
    private static final int COPPER = 0xFFD98A4F;
    private static final int MCOPPER = 0xFFB8743F;
    private static final int NIXIE = 0xFFFFA347;
    private static final int AMBER = 0xFFFFB347;
    private static final int OK = 0xFF8FD486;
    private static final int DIM = 0xFF8D7A6C;
    private static final int LOCKED_TEXT = 0xFF7D6A5E;
    private static final int LOCK_CHIP = 0xFFA58C7C;
    private static final int GOLD = 0xFFFFD23C;
    private static final int TICKET_TEXT = 0xFFFFD84A;
    private static final int HALO = 0x55FF6E1E;
    private static final int BUTTON_DARK = 0xFF2A1608;
    private static final int BUTTON_GREY = 0xFF1E1A16;

    private static ResourceLocation lastChapter;
    private static final Map<ResourceLocation, double[]> VIEWS = new HashMap<>();
    private static final Map<ResourceLocation, Selection> SELECTIONS = new HashMap<>();
    private static String lastSearch = "";
    private static int lastListScroll;

    private record Selection(ResourceLocation quest, int detailScroll) {
    }

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

    private record Node(int position, int x, int y, int size, int half, int sides) {
    }

    private record Edge(int from, int to, List<int[]> segments, List<int[]> bends) {
    }

    private record TextOp(FormattedCharSequence text, int x, int y, int colour, boolean shadow, boolean glow, Rect clip) {
    }

    private record ItemOp(ItemStack stack, int x, int y, String count, boolean dim, Rect clip) {
    }

    private enum Sprite {
        LOCK(0, 9, 11), CHECK(10, 9, 8), BANG(20, 7, 11), TEAM(28, 14, 9);

        final int u;
        final int w;
        final int h;

        Sprite(int u, int w, int h) {
            this.u = u;
            this.w = w;
            this.h = h;
        }
    }

    private final Layer chrome = new Layer("book_chrome");
    private final Layer headerLayer = new Layer("book_header");
    private final Layer listLayer = new Layer("book_list");
    private final Layer plateLayer = new Layer("book_plate");
    private final Layer lightLayer = new Layer("book_light");
    private final Layer vignetteLayer = new Layer("book_vignette");
    private final Layer lockedLayer = new Layer("book_locked");
    private final Layer worldLayer = new Layer("book_world");
    private final Layer detailLayer = new Layer("book_detail");
    private final Layer buttonLayer = new Layer("book_buttons");
    private final Layer spriteLayer = new Layer("book_sprites");

    private Rect header;
    private Rect list;
    private Rect mapPanel;
    private Rect view;
    private Rect detail;
    private EditBox search;
    private ResourceLocation chapter;
    private int selected = -1;
    private double panX;
    private double panY;
    private int zoom;
    private int listScroll;
    private int detailScroll;
    private int detailContent;
    private boolean dragging;
    private boolean dragged;
    private int seenRevision = -1;
    private int hovered = -1;
    private long detailOpenedAt;
    private ResourceLocation leverFrom;
    private long leverAt;
    private float dialShown = -1.0F;
    private long dialAt;
    private Canvas detailBase;
    private Object modelKey;
    private Object worldKey;
    private int worldTick = Integer.MIN_VALUE;
    private int worldW;
    private int worldH;
    private final List<Node> nodes = new ArrayList<>();
    private final Map<Integer, Node> nodeByPosition = new HashMap<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<String, Integer> choices = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private final List<Tip> tips = new ArrayList<>();
    private final List<TextOp> texts = new ArrayList<>();
    private final List<ItemOp> items = new ArrayList<>();
    private final List<Rect> claimedSlots = new ArrayList<>();

    public QuestBookScreen() {
        super(Component.translatable("screen.rift_quests.title"));
    }

    @Override
    protected void init() {
        int listW = listWidth();
        header = new Rect(4, 4, width - 8, 26);
        list = new Rect(4, 32, listW, height - 36);
        mapPanel = new Rect(list.right() + 4, 32, width - 4 - list.right() - 4, height - 36);
        view = new Rect(mapPanel.x() + 5, mapPanel.y() + 5, mapPanel.w() - 10, mapPanel.h() - 10);
        int detailW = Math.min(Mth.clamp(width * 34 / 100, 190, 260), Math.max(120, mapPanel.w() - 40));
        detail = new Rect(width - 4 - detailW, 32, detailW, height - 36);
        boolean opening = search == null;
        String query = opening ? lastSearch : search.getValue();
        search = new EditBox(font, list.x() + 10, list.y() + 9, list.w() - 20, 10, Component.translatable("screen.rift_quests.search"));
        search.setBordered(false);
        search.setTextColor(PAPER);
        search.setHint(Component.translatable("screen.rift_quests.search").withStyle(style -> style.withColor(DIM)));
        search.setMaxLength(48);
        search.setValue(query);
        search.setResponder(value -> listScroll = 0);
        addWidget(search);
        if (opening) {
            listScroll = lastListScroll;
        }
        if (chapter == null) {
            chapter = pickChapter();
            restoreView();
        }
        zoom = Mth.clamp(zoom == 0 ? guiScale() : zoom, 1, guiScale() * 2);
        modelKey = null;
        ensureModel();
        clampPan();
    }

    private int guiScale() {
        return Math.max(1, (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale()));
    }

    private int listWidth() {
        int widest = 0;
        for (BookView.ChapterView view : ClientQuests.book().chapters()) {
            widest = Math.max(widest, font.width(view.title()));
        }
        int needed = 8 + 15 + widest + 6 + 9 + font.width("33/33") + 10;
        int limit = Math.max(150, width * 32 / 100);
        return Mth.clamp(needed, 150, limit);
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

    private void restoreView() {
        double[] saved = chapter == null ? null : VIEWS.get(chapter);
        if (saved != null) {
            panX = saved[0];
            panY = saved[1];
            zoom = (int) saved[2];
        } else {
            panX = 0;
            panY = 0;
            zoom = guiScale();
        }
        zoom = Mth.clamp(zoom == 0 ? guiScale() : zoom, 1, guiScale() * 2);
        restoreSelection();
    }

    private void restoreSelection() {
        Selection saved = chapter == null ? null : SELECTIONS.get(chapter);
        int position = saved == null ? -1 : ClientQuests.indexOf(saved.quest());
        boolean valid = position >= 0 && ClientQuests.quest(position).chapter().equals(chapter);
        selected = valid ? position : -1;
        detailScroll = valid ? saved.detailScroll() : 0;
        detailOpenedAt = 0;
        choices.clear();
    }

    private void saveView() {
        if (chapter == null) {
            return;
        }
        VIEWS.put(chapter, new double[] {panX, panY, zoom});
        if (selected >= 0 && selected < ClientQuests.book().quests().size()) {
            SELECTIONS.put(chapter, new Selection(ClientQuests.quest(selected).id(), detailScroll));
        } else {
            SELECTIONS.remove(chapter);
        }
    }

    private void switchChapter(ResourceLocation id) {
        if (Objects.equals(id, chapter)) {
            return;
        }
        saveView();
        leverFrom = chapter;
        leverAt = Util.getMillis();
        chapter = id;
        lastChapter = id;
        selected = -1;
        hovered = -1;
        modelKey = null;
        ensureModel();
        restoreView();
        clampPan();
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LEVER_CLICK, 1.0F, 0.35F));
    }

    private void select(int position) {
        if (position < 0) {
            selected = -1;
            clampPan();
            return;
        }
        if (selected != position) {
            detailScroll = 0;
            choices.clear();
        }
        selected = position;
        detailOpenedAt = Util.getMillis();
        Node node = nodeByPosition.get(position);
        if (node != null) {
            int g = guiScale();
            double effective = effectiveWidth() * g;
            double sx = panX + node.x() * zoom;
            double sy = panY + node.y() * zoom;
            if (sx > effective - 34 * g || sx < 34 * g) {
                panX = effective * 0.55 - node.x() * zoom;
            }
            if (sy > view.h() * g - 34 * g || sy < 34 * g) {
                panY = view.h() * g / 2.0 - node.y() * zoom;
            }
        }
        clampPan();
    }

    private void focusQuest(int position) {
        BookView.QuestView quest = ClientQuests.quest(position);
        if (!quest.chapter().equals(chapter)) {
            switchChapter(quest.chapter());
        }
        ensureModel();
        Node node = nodeByPosition.get(position);
        if (node != null) {
            int g = guiScale();
            panX = effectiveWidth() * g * 0.45 - node.x() * zoom;
            panY = view.h() * g / 2.0 - node.y() * zoom;
        }
        select(position);
    }

    private int effectiveWidth() {
        return selected >= 0 ? Math.max(40, detail.x() - view.x()) : view.w();
    }

    private void clampPan() {
        if (view == null) {
            return;
        }
        int g = guiScale();
        double viewW = effectiveWidth() * (double) g;
        double viewH = view.h() * (double) g;
        double contentW = worldW * (double) zoom;
        double contentH = worldH * (double) zoom;
        double margin = EDGE * (double) zoom;
        panX = contentW + 2 * margin <= viewW ? Math.round((viewW - contentW) / 2) : Mth.clamp(panX, viewW - contentW - margin, margin);
        panY = contentH + 2 * margin <= viewH ? Math.round((viewH - contentH) / 2) : Mth.clamp(panY, viewH - contentH - margin, margin);
    }

    @Override
    public void removed() {
        saveView();
        lastChapter = chapter;
        lastSearch = search == null ? "" : search.getValue();
        lastListScroll = listScroll;
        for (Layer layer : List.of(chrome, headerLayer, listLayer, plateLayer, lightLayer, vignetteLayer, lockedLayer, worldLayer, detailLayer,
                buttonLayer, spriteLayer)) {
            layer.close();
        }
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
                restoreView();
            }
            if (selected >= ClientQuests.book().quests().size()) {
                selected = -1;
            }
            modelKey = null;
            if (list != null && list.w() != listWidth()) {
                rebuildWidgets();
            }
        }
    }

    private void ensureModel() {
        Object key = List.of(Objects.toString(chapter), ClientQuests.revision());
        if (key.equals(modelKey)) {
            return;
        }
        modelKey = key;
        worldKey = null;
        nodes.clear();
        nodeByPosition.clear();
        edges.clear();
        List<Integer> quests = chapter == null ? List.of() : ClientQuests.questsOf(chapter);
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int position : quests) {
            BookView.QuestView quest = ClientQuests.quest(position);
            minX = Math.min(minX, quest.x());
            minY = Math.min(minY, quest.y());
            maxX = Math.max(maxX, quest.x());
            maxY = Math.max(maxY, quest.y());
        }
        if (minX == Integer.MAX_VALUE) {
            worldW = 2 * PAD;
            worldH = 2 * PAD;
            return;
        }
        worldW = (maxX - minX) * STEP_X + 2 * PAD;
        worldH = (maxY - minY) * STEP_Y + 2 * PAD;
        Map<Integer, int[]> centres = new LinkedHashMap<>();
        for (int position : quests) {
            if (!visible(position)) {
                continue;
            }
            BookView.QuestView quest = ClientQuests.quest(position);
            centres.put(position, new int[] {PAD + (quest.x() - minX) * STEP_X, PAD + (quest.y() - minY) * STEP_Y});
        }
        Map<Integer, Integer> sides = new HashMap<>();
        for (Map.Entry<Integer, int[]> entry : centres.entrySet()) {
            int to = entry.getKey();
            int[] target = entry.getValue();
            int targetHalf = half(ClientQuests.quest(to).size());
            for (ResourceLocation dependency : ClientQuests.quest(to).dependencies()) {
                int from = ClientQuests.indexOf(dependency);
                int[] source = from < 0 ? null : centres.get(from);
                if (source == null) {
                    continue;
                }
                List<int[]> segments = new ArrayList<>();
                List<int[]> bends = new ArrayList<>();
                int side;
                if (source[1] == target[1]) {
                    boolean forward = source[0] < target[0];
                    segments.add(new int[] {0, source[0], forward ? target[0] - targetHalf : target[0] + targetHalf, source[1]});
                    side = forward ? LEFT : RIGHT;
                } else if (source[0] == target[0]) {
                    boolean down = source[1] < target[1];
                    segments.add(new int[] {1, source[0], source[1], down ? target[1] - targetHalf : target[1] + targetHalf});
                    side = down ? TOP : BOTTOM;
                } else {
                    boolean forward = source[0] < target[0];
                    int bendX = forward ? target[0] - 26 : target[0] + 26;
                    int targetX = forward ? target[0] - targetHalf : target[0] + targetHalf;
                    segments.add(new int[] {0, source[0], forward ? bendX + 2 : bendX - 2, source[1]});
                    segments.add(new int[] {1, bendX, source[1], target[1]});
                    segments.add(new int[] {0, forward ? bendX - 2 : bendX + 2, targetX, target[1]});
                    bends.add(new int[] {bendX, source[1]});
                    bends.add(new int[] {bendX, target[1]});
                    side = forward ? LEFT : RIGHT;
                }
                edges.add(new Edge(from, to, segments, bends));
                sides.merge(to, side, (a, b) -> a | b);
            }
        }
        for (Map.Entry<Integer, int[]> entry : centres.entrySet()) {
            int position = entry.getKey();
            QuestSize size = ClientQuests.quest(position).size();
            Node node = new Node(position, entry.getValue()[0], entry.getValue()[1], size == QuestSize.SMALL ? 22 : 26, half(size),
                    sides.getOrDefault(position, 0));
            nodes.add(node);
            nodeByPosition.put(position, node);
        }
    }

    private static int half(QuestSize size) {
        return size == QuestSize.LARGE ? 22 : size == QuestSize.MEDIUM ? 13 : 11;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        tips.clear();
        long now = Util.getMillis();
        int tick = (int) (now / FRAME_MILLIS);
        paintChrome();
        chrome.draw(graphics, 0, 0);
        if (!ClientQuests.loaded()) {
            graphics.drawCenteredString(font, Component.translatable("screen.rift_quests.loading"), width / 2, height / 2 - 4, SAND);
            return;
        }
        ensureSprites();
        ensureModel();
        clampPan();
        renderHeader(graphics, mouseX, mouseY, now);
        renderList(graphics, mouseX, mouseY, now);
        search.render(graphics, mouseX, mouseY, partialTick);
        renderMap(graphics, mouseX, mouseY, tick);
        if (selected >= 0 && selected < ClientQuests.book().quests().size()) {
            renderDetail(graphics, mouseX, mouseY, now);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void paintChrome() {
        if (!chrome.stale(List.of(width, height, list, mapPanel))) {
            return;
        }
        Canvas c = chrome.begin(width, height);
        c.rect(0, 0, width, height, 0xFF0C0705);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (Art.hash(x, y, 21) < 0.04) {
                    c.set(x, y, 0xFF150D09);
                }
            }
        }
        Art.panel(c, header.x(), header.y(), header.w(), header.h(), Art.PANEL_BRASS);
        Art.panel(c, list.x(), list.y(), list.w(), list.h(), Art.PANEL_ANDESITE);
        Art.panel(c, mapPanel.x(), mapPanel.y(), mapPanel.w(), mapPanel.h(), Art.PANEL_COPPER);
        chrome.commit();
    }

    private void ensureSprites() {
        if (spriteLayer.stale("sprites")) {
            Canvas c = spriteLayer.begin(42, 11);
            c.draw(Art.LOCK, Sprite.LOCK.u, 0);
            c.draw(Art.CHECK, Sprite.CHECK.u, 0);
            c.draw(Art.BANG, Sprite.BANG.u, 0);
            c.draw(Art.TEAM, Sprite.TEAM.u, 0);
            spriteLayer.commit();
        }
        if (plateLayer.stale("plate")) {
            plateLayer.begin(64, 64).draw(Art.PLATE, 0, 0);
            plateLayer.commit();
        }
    }

    private void sprite(GuiGraphics graphics, Sprite sprite, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(spriteLayer.id(), x, y, sprite.w, sprite.h, sprite.u, 0, sprite.w, sprite.h, 42, 11);
        RenderSystem.disableBlend();
    }

    private static FormattedCharSequence plain(String text) {
        return FormattedCharSequence.forward(text, Style.EMPTY);
    }

    private String fit(Component text, int maxWidth) {
        String plain = text.getString();
        if (font.width(plain) <= maxWidth) {
            return plain;
        }
        return font.plainSubstrByWidth(plain, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    private void flushTexts(GuiGraphics graphics) {
        Rect clip = null;
        for (TextOp op : texts) {
            if (!Objects.equals(op.clip(), clip)) {
                if (clip != null) {
                    graphics.disableScissor();
                }
                clip = op.clip();
                if (clip != null) {
                    graphics.enableScissor(clip.x(), clip.y(), clip.right(), clip.bottom());
                }
            }
            if (op.glow()) {
                graphics.drawString(font, op.text(), op.x() - 1, op.y(), HALO, false);
                graphics.drawString(font, op.text(), op.x() + 1, op.y(), HALO, false);
                graphics.drawString(font, op.text(), op.x(), op.y() - 1, HALO, false);
                graphics.drawString(font, op.text(), op.x(), op.y() + 1, HALO, false);
            }
            graphics.drawString(font, op.text(), op.x(), op.y(), op.colour(), op.shadow());
        }
        if (clip != null) {
            graphics.disableScissor();
        }
        texts.clear();
    }

    private void flushItems(GuiGraphics graphics) {
        for (ItemOp op : items) {
            if (op.clip() != null) {
                graphics.enableScissor(op.clip().x(), op.clip().y(), op.clip().right(), op.clip().bottom());
            }
            if (op.dim()) {
                RenderSystem.setShaderColor(0.62F, 0.56F, 0.5F, 1.0F);
            }
            graphics.renderFakeItem(op.stack(), op.x(), op.y());
            if (op.dim()) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            if (op.count() != null) {
                graphics.renderItemDecorations(font, op.stack(), op.x(), op.y(), op.count());
            }
            if (op.clip() != null) {
                graphics.disableScissor();
            }
        }
        items.clear();
    }

    private void renderHeader(GuiGraphics graphics, int mouseX, int mouseY, long now) {
        Canvas c = headerLayer.begin(header.w(), header.h());
        Art.crest(c, 6, 5);
        int tickets = ticketCount();
        String ticketText = String.valueOf(tickets);
        int boxW = 18 + font.width(ticketText) + 3;
        int boxX = header.w() - 6 - boxW;
        c.rect(boxX, 5, boxW, 16, 0xFF160C06);
        Art.ring(c, boxX, 5, boxW, 16, 0xFF5A3608);
        int total = chapter == null ? 0 : ClientQuests.questsOf(chapter).size();
        int done = chapter == null ? 0 : ClientQuests.done(chapter);
        String count = done + "/" + total;
        int countW = Math.max(24, font.width(count));
        int countX = boxX - 4 - countW;
        int dialX = countX - 4 - 30;
        float target = total == 0 ? 0.0F : done / (float) total;
        if (dialShown < 0) {
            dialShown = target;
        } else {
            dialShown += (target - dialShown) * Math.min(1.0F, (now - dialAt) / 160.0F);
            if (Math.abs(target - dialShown) < 0.002F) {
                dialShown = target;
            }
        }
        dialAt = now;
        Art.dial(c, dialX, 5, dialShown);
        BookView.ChapterView chapterView = chapterView();
        String title = chapterView == null ? "" : chapterView.title().getString().toUpperCase(Locale.ROOT);
        int brandW = Math.max(font.width(Component.translatable("screen.rift_quests.title")), font.width(Component.translatable("screen.rift_quests.motto")) / 2);
        int leftLimit = 26 + brandW + 8;
        int rightLimit = dialX - 8;
        int centre = header.w() / 2;
        int halfSpace = Math.min(centre - leftLimit, rightLimit - centre);
        int tubesW = title.length() * 9 - 1;
        Rect titleArea;
        if (!title.isEmpty() && tubesW <= halfSpace * 2) {
            int start = centre - tubesW / 2;
            for (int index = 0; index < title.length(); index++) {
                char character = title.charAt(index);
                int tubeX = start + index * 9;
                Art.nixieTube(c, tubeX, 5, character == ' ');
                if (character != ' ') {
                    String glyph = String.valueOf(character);
                    texts.add(new TextOp(plain(glyph), header.x() + tubeX + (8 - font.width(glyph) + 1) / 2, header.y() + 9, NIXIE, false, true, null));
                }
            }
            titleArea = new Rect(header.x() + start, header.y() + 5, tubesW, 16);
        } else {
            String shown = font.width(title) <= Math.max(0, halfSpace * 2) ? title : font.plainSubstrByWidth(title, Math.max(0, halfSpace * 2 - 12)) + "...";
            int shownW = font.width(shown);
            c.rect(centre - shownW / 2 - 4, 5, shownW + 8, 16, 0xFF120804);
            Art.ring(c, centre - shownW / 2 - 4, 5, shownW + 8, 16, 0xFF3D2414);
            texts.add(new TextOp(plain(shown), header.x() + centre - shownW / 2, header.y() + 9, NIXIE, false, true, null));
            titleArea = new Rect(header.x() + centre - shownW / 2 - 4, header.y() + 5, shownW + 8, 16);
        }
        headerLayer.commit();
        headerLayer.draw(graphics, header.x(), header.y());
        graphics.drawString(font, Component.translatable("screen.rift_quests.title"), header.x() + 26, header.y() + 5, LCOPPER, true);
        graphics.pose().pushPose();
        graphics.pose().translate(header.x() + 26, header.y() + 15, 0);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(font, Component.translatable("screen.rift_quests.motto"), 0, 0, SAND, false);
        graphics.pose().popPose();
        texts.add(new TextOp(plain(count), header.x() + countX + countW - font.width(count), header.y() + 9, NIXIE, false, true, null));
        texts.add(new TextOp(plain(ticketText), header.x() + boxX + 18, header.y() + 9, TICKET_TEXT, true, false, null));
        flushTexts(graphics);
        ItemStack ticket = new ItemStack(ModItems.RIFTWATCH_TICKET.get());
        graphics.renderFakeItem(ticket, header.x() + boxX + 1, header.y() + 5);
        tips.add(new Tip(new Rect(header.x() + boxX, header.y() + 5, boxW, 16),
                List.of(RainbowText.of(ticket.getHoverName().getString()), Component.translatable("screen.rift_quests.tickets", tickets).withStyle(ChatFormatting.GRAY)),
                ItemStack.EMPTY));
        tips.add(new Tip(new Rect(header.x() + dialX, header.y() + 5, countX + countW - dialX, 16),
                List.of(Component.translatable("screen.rift_quests.chapter_progress"), Component.translatable("screen.rift_quests.progress", done, total)
                        .withStyle(ChatFormatting.GRAY), Component.translatable("screen.rift_quests.book_progress", ClientQuests.totalDone(),
                        ClientQuests.book().quests().size()).withStyle(ChatFormatting.GRAY)), ItemStack.EMPTY));
        if (chapterView != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(chapterView.title());
            chapterView.description().ifPresent(description -> lines.add(description.copy().withStyle(ChatFormatting.GRAY)));
            tips.add(new Tip(titleArea, lines, ItemStack.EMPTY));
        }
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

    private Art.Lever leverOf(BookView.ChapterView view, long now) {
        if (!ClientQuests.chapterUnlocked(view)) {
            return Art.Lever.RUST;
        }
        boolean moving = now - leverAt < LEVER_MILLIS && (view.id().equals(chapter) || view.id().equals(leverFrom));
        if (moving) {
            return Art.Lever.MIDDLE;
        }
        return view.id().equals(chapter) ? Art.Lever.ON : Art.Lever.OFF;
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY, long now) {
        Canvas c = listLayer.begin(list.w(), list.h());
        int searchW = list.w() - 12;
        c.rect(6, 6, searchW, 15, 0xFF1D120C);
        c.rect(6, 6, searchW, 1, 0xFF0A0503);
        c.rect(6, 6, 1, 15, 0xFF0A0503);
        c.rect(6, 20, searchW, 1, 0xFF5C3E28);
        c.rect(5 + searchW, 7, 1, 14, 0xFF5C3E28);
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        Rect rows = new Rect(list.x() + 5, list.y() + 36, list.w() - 10, list.h() - 42);
        texts.add(new TextOp(Component.translatable(query.isEmpty() ? "screen.rift_quests.chapters" : "screen.rift_quests.results").getVisualOrderText(),
                list.x() + 8, list.y() + 25, COPPER, true, false, null));
        boolean inRows = rows.contains(mouseX, mouseY);
        c.clip(rows.x() - list.x(), rows.y() - list.y(), rows.w(), rows.h());
        int contentHeight;
        int rowWidth;
        if (query.isEmpty()) {
            List<BookView.ChapterView> chapters = ClientQuests.book().chapters();
            contentHeight = chapters.size() * ROW;
            listScroll = Mth.clamp(listScroll, 0, Math.max(0, contentHeight - rows.h()));
            rowWidth = rows.w() - (contentHeight > rows.h() ? 5 : 0);
            for (int index = 0; index < chapters.size(); index++) {
                int rowY = rows.y() + index * ROW - listScroll;
                if (rowY + ROW < rows.y() || rowY > rows.bottom()) {
                    continue;
                }
                chapterRow(c, chapters.get(index), new Rect(rows.x(), rowY, rowWidth, ROW), rows, inRows, mouseX, mouseY, now);
            }
        } else {
            List<Integer> matches = new ArrayList<>();
            for (int position = 0; position < ClientQuests.book().quests().size(); position++) {
                if (searchable(position) && ClientQuests.quest(position).title().getString().toLowerCase(Locale.ROOT).contains(query)) {
                    matches.add(position);
                }
            }
            contentHeight = Math.max(1, matches.size()) * RESULT_ROW;
            listScroll = Mth.clamp(listScroll, 0, Math.max(0, contentHeight - rows.h()));
            rowWidth = rows.w() - (contentHeight > rows.h() ? 5 : 0);
            if (matches.isEmpty()) {
                texts.add(new TextOp(Component.translatable("screen.rift_quests.no_results").getVisualOrderText(), rows.x() + 3, rows.y() + 4, DIM, false,
                        false, rows));
            }
            for (int index = 0; index < matches.size(); index++) {
                int position = matches.get(index);
                int rowY = rows.y() + index * RESULT_ROW - listScroll;
                if (rowY + RESULT_ROW < rows.y() || rowY > rows.bottom()) {
                    continue;
                }
                BookView.QuestView quest = ClientQuests.quest(position);
                Rect row = new Rect(rows.x(), rowY, rowWidth, RESULT_ROW - 1);
                boolean hover = inRows && row.contains(mouseX, mouseY);
                if (hover) {
                    c.rect(row.x() - list.x(), row.y() - list.y(), row.w(), row.h(), 0xFF110703);
                    Art.ring(c, row.x() - list.x(), row.y() - list.y(), row.w(), row.h(), 0xFF4F2C15);
                }
                if (!quest.icon().isEmpty()) {
                    items.add(new ItemOp(quest.icon().get(0), row.x() + 1, row.y(), null, ClientQuests.state(position) == QuestState.LOCKED, rows));
                }
                texts.add(new TextOp(plain(fit(quest.title(), row.w() - 22)), row.x() + 20, row.y() + 5, hover ? PAPER : stateColour(ClientQuests.state(position)),
                        false, false, rows));
                if (inRows) {
                    hits.add(new Hit(row, () -> focusQuest(position)));
                    tips.add(new Tip(row, nodeTooltip(position), ItemStack.EMPTY));
                }
            }
        }
        c.resetClip();
        if (contentHeight > rows.h()) {
            int trackX = rows.right() - 3 - list.x();
            int trackY = rows.y() - list.y();
            c.rect(trackX, trackY, 3, rows.h(), 0xFF47433E);
            int barHeight = Math.max(12, rows.h() * rows.h() / contentHeight);
            int barY = trackY + (rows.h() - barHeight) * listScroll / Math.max(1, contentHeight - rows.h());
            c.rect(trackX, barY, 3, barHeight, 0xFFB3ADA3);
            c.rect(trackX, barY, 1, barHeight, 0xFFECE7DE);
        }
        listLayer.commit();
        listLayer.draw(graphics, list.x(), list.y());
        flushTexts(graphics);
        flushItems(graphics);
    }

    private void chapterRow(Canvas c, BookView.ChapterView view, Rect row, Rect clip, boolean inRows, int mouseX, int mouseY, long now) {
        boolean current = view.id().equals(chapter);
        boolean unlocked = ClientQuests.chapterUnlocked(view);
        boolean hover = inRows && row.contains(mouseX, mouseY);
        int rx = row.x() - list.x();
        int ry = row.y() - list.y();
        if (current) {
            c.rect(rx, ry, row.w(), row.h(), 0xFF110703);
            Art.ring(c, rx, ry, row.w(), row.h(), 0xFF4F2C15);
            c.rect(rx + 1, ry + 1, row.w() - 2, 1, 0xFF1D0E06);
        } else if (hover) {
            c.rect(rx, ry, row.w(), row.h(), 0x40000000);
        }
        c.draw(Art.lever(leverOf(view, now)), rx + 2, ry + 2);
        int total = ClientQuests.questsOf(view.id()).size();
        int done = ClientQuests.done(view.id());
        int unclaimed = ClientQuests.unclaimed(view.id());
        int right = row.right() - 3;
        if (unlocked) {
            String count = done + "/" + total;
            int countW = font.width(count);
            right -= countW;
            int colour = current ? NIXIE : done == total && total > 0 ? OK : MCOPPER;
            texts.add(new TextOp(plain(count), right, row.y() + 3, colour, false, current, clip));
        } else {
            right -= 9;
            c.draw(Art.LOCK, right - list.x(), ry + 2);
        }
        if (unclaimed > 0 && unlocked) {
            right -= 9;
            c.draw(Art.BANG, right - list.x(), ry + 2);
        }
        int nameX = row.x() + 15;
        int nameColour = current ? NIXIE : !unlocked ? LOCKED_TEXT : hover ? PAPER : SAND;
        texts.add(new TextOp(plain(fit(view.title(), right - 4 - nameX)), nameX, row.y() + 3, nameColour, false, current, clip));
        if (inRows) {
            hits.add(new Hit(row, () -> switchChapter(view.id())));
            List<Component> lines = new ArrayList<>();
            lines.add(view.title());
            view.description().ifPresent(description -> lines.add(description.copy().withStyle(ChatFormatting.GRAY)));
            lines.add(Component.translatable("screen.rift_quests.progress", done, total).withStyle(ChatFormatting.GRAY));
            if (!unlocked) {
                lines.add(Component.translatable("screen.rift_quests.chapter_locked").withStyle(style -> style.withColor(COPPER)));
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

    private boolean searchable(int position) {
        BookView.QuestView quest = ClientQuests.quest(position);
        QuestState state = ClientQuests.state(position);
        if (quest.visibility() == Visibility.SECRET && !state.done()) {
            return false;
        }
        return !(quest.visibility() == Visibility.HIDDEN_UNTIL_UNLOCKED && state == QuestState.LOCKED);
    }

    private boolean visible(int position) {
        BookView.QuestView quest = ClientQuests.quest(position);
        return !(quest.visibility() == Visibility.HIDDEN_UNTIL_UNLOCKED && ClientQuests.state(position) == QuestState.LOCKED);
    }

    private boolean secret(int position) {
        return ClientQuests.quest(position).visibility() == Visibility.SECRET && !ClientQuests.state(position).done();
    }

    private Art.ShaftKind kindOf(Edge edge) {
        if (!ClientQuests.state(edge.from()).done()) {
            return Art.ShaftKind.RUST;
        }
        return ClientQuests.state(edge.to()) == QuestState.LOCKED ? Art.ShaftKind.IDLE : Art.ShaftKind.TURN;
    }

    private boolean inTurn(int position) {
        for (Edge edge : edges) {
            if (edge.to() == position && kindOf(edge) == Art.ShaftKind.TURN) {
                return true;
            }
        }
        return false;
    }

    private float fraction(int position) {
        BookView.QuestView quest = ClientQuests.quest(position);
        if (ClientQuests.state(position).done()) {
            return 1.0F;
        }
        long have = 0;
        long need = 0;
        for (int task = 0; task < quest.tasks().size(); task++) {
            long target = Math.max(1, quest.tasks().get(task).target());
            have += Math.min(ClientQuests.progress(position, task), target);
            need += target;
        }
        return need == 0 ? 0.0F : have / (float) need;
    }

    private void paintWorld(Canvas c, int tick) {
        Map<Long, int[]> bends = new HashMap<>();
        for (Art.ShaftKind kind : new Art.ShaftKind[] {Art.ShaftKind.RUST, Art.ShaftKind.IDLE, Art.ShaftKind.TURN}) {
            for (Edge edge : edges) {
                if (kindOf(edge) != kind) {
                    continue;
                }
                int offset = kind == Art.ShaftKind.TURN ? tick : 0;
                for (int[] segment : edge.segments()) {
                    if (segment[0] == 0) {
                        Art.shaftH(c, segment[1], segment[2], segment[3], kind, offset);
                    } else {
                        Art.shaftV(c, segment[1], segment[2], segment[3], kind, offset);
                    }
                }
                for (int[] bend : edge.bends()) {
                    long key = ((long) bend[0] << 32) | (bend[1] & 0xFFFFFFFFL);
                    int rank = 2 - kind.ordinal();
                    int[] previous = bends.get(key);
                    if (previous == null || rank > previous[3]) {
                        bends.put(key, new int[] {bend[0], bend[1], kind.ordinal(), rank});
                    }
                }
            }
        }
        for (int[] bend : bends.values()) {
            Art.gearbox(c, bend[0], bend[1], Art.ShaftKind.values()[bend[2]], tick);
        }
        for (Node node : nodes) {
            paintNode(c, node, tick);
        }
    }

    private void paintNode(Canvas c, Node node, int tick) {
        int position = node.position();
        QuestState state = ClientQuests.state(position);
        boolean locked = state == QuestState.LOCKED;
        boolean done = state.done();
        BookView.QuestView quest = ClientQuests.quest(position);
        int size = node.size();
        int x0 = node.x() - (size >> 1);
        int y0 = node.y() - (size >> 1);
        if (quest.size() == QuestSize.LARGE) {
            Canvas cog = Art.cog(Art.BIG_RING, done ? tick : 0, locked ? Art.COG_RUST : done ? Art.COG_WARM : Art.COG_WOOD);
            c.draw(cog, node.x() - (cog.width >> 1), node.y() - (cog.height >> 1));
        } else if (node.sides() != 0) {
            boolean turning = inTurn(position);
            Canvas cog = Art.cog(Art.SMALL_COG, turning ? tick : 0, locked ? Art.COG_RUST : turning ? Art.COG_WARM : Art.COG_WOOD);
            int half = cog.width >> 1;
            if ((node.sides() & LEFT) != 0) {
                c.draw(cog, x0 - half, node.y() - half);
            }
            if ((node.sides() & RIGHT) != 0) {
                c.draw(cog, x0 + size - 1 - half, node.y() - half);
            }
            if ((node.sides() & TOP) != 0) {
                c.draw(cog, node.x() - half, y0 - half);
            }
            if ((node.sides() & BOTTOM) != 0) {
                c.draw(cog, node.x() - half, y0 + size - 1 - half);
            }
        }
        if (state == QuestState.COMPLETE) {
            boolean on = ((tick >> 2) & 1) == 0;
            Art.ring(c, x0 - 1, y0 - 1, size + 2, size + 2, 0xFFFFCF6A);
            Art.ring(c, x0 - 2, y0 - 2, size + 4, size + 4, on ? 0xFFFF8A2A : 0xFF8A3F14);
            if (on) {
                Art.ring(c, x0 - 3, y0 - 3, size + 6, size + 6, 0x59FF8C28);
            }
        }
        c.draw(Art.casing(size == 22, locked, position % 13 + 1), x0, y0);
        if (state == QuestState.AVAILABLE) {
            boolean on = tick % 12 < 9;
            c.rect(node.x() - 2, y0 - 4, 5, 4, 0xFF120A05);
            c.rect(node.x() - 1, y0 - 3, 3, 2, on ? 0xFFFF9A3C : 0xFF5A2A0E);
            if (on) {
                c.set(node.x(), y0 - 3, 0xFFFFF0B0);
                c.rect(node.x() - 3, y0 - 5, 7, 1, 0x59FFAA3C);
            }
        }
        if (state == QuestState.IN_PROGRESS) {
            int gaugeY = y0 + size + 1;
            int inner = size - 2;
            int fill = Math.max(1, Math.round(inner * fraction(position)));
            c.rect(x0, gaugeY, size, 4, 0xFF120A05);
            c.rect(x0 + 1, gaugeY + 1, inner, 2, 0xFF2A1810);
            c.rect(x0 + 1, gaugeY + 1, fill, 1, 0xFFFFD27A);
            c.rect(x0 + 1, gaugeY + 2, fill, 1, 0xFFE07A2A);
            c.set(x0 + fill, gaugeY + 1, 0xFFFFF3CF);
        }
        if (position == hovered && position != selected) {
            Art.ring(c, x0 - 1, y0 - 1, size + 2, size + 2, PAPER);
        }
        if (position == selected) {
            if (quest.size() == QuestSize.LARGE) {
                Art.brackets(c, node.x() - 26, node.y() - 26, 52, 52, 0xFFFFE0A8);
            } else {
                Art.brackets(c, x0 - 4, y0 - 4, size + 8, size + 8, 0xFFFFE0A8);
            }
        }
    }

    private void renderMap(GuiGraphics graphics, int mouseX, int mouseY, int tick) {
        if (lightLayer.stale(List.of(view.w(), view.h()))) {
            lightLayer.begin(view.w(), view.h()).copyFrom(Art.light(view.w(), view.h()));
            lightLayer.commit();
            vignetteLayer.begin(view.w(), view.h()).copyFrom(Art.vignette(view.w(), view.h()));
            vignetteLayer.commit();
        }
        int g = guiScale();
        int px = (int) Math.round(panX);
        int py = (int) Math.round(panY);
        boolean overDetail = selected >= 0 && detail.contains(mouseX, mouseY);
        boolean mouseInMap = view.contains(mouseX, mouseY) && !overDetail;
        double localX = (mouseX * (double) g - view.x() * (double) g - px) / zoom;
        double localY = (mouseY * (double) g - view.y() * (double) g - py) / zoom;
        BookView.ChapterView chapterView = chapterView();
        boolean lockedChapter = chapterView != null && !ClientQuests.chapterUnlocked(chapterView);
        hovered = -1;
        if (mouseInMap && !lockedChapter && !dragging) {
            for (Node node : nodes) {
                if (Math.abs(localX - node.x()) <= node.half() && Math.abs(localY - node.y()) <= node.half()) {
                    hovered = node.position();
                }
            }
        }
        graphics.enableScissor(view.x(), view.y(), view.right(), view.bottom());
        int visibleX = Math.floorDiv(-px, zoom) - 2;
        int visibleY = Math.floorDiv(-py, zoom) - 2;
        int visibleW = view.w() * g / zoom + 5;
        int visibleH = view.h() * g / zoom + 5;
        graphics.pose().pushPose();
        worldPose(graphics, g, px, py);
        graphics.blit(plateLayer.id(), visibleX, visibleY, (float) visibleX, (float) visibleY, visibleW, visibleH, 64, 64);
        graphics.pose().popPose();
        lightLayer.draw(graphics, view.x(), view.y());
        if (lockedChapter) {
            renderLockedScene(graphics, chapterView);
        } else if (!nodes.isEmpty()) {
            Object key = List.of(Objects.toString(chapter), ClientQuests.revision(), hovered, selected);
            boolean full = !key.equals(worldKey);
            if (full || tick != worldTick) {
                Canvas canvas = worldLayer.begin(worldW, worldH);
                paintWorld(canvas, tick);
                worldLayer.commitRegion(visibleX, visibleY, visibleW, visibleH, full);
                worldKey = key;
                worldTick = tick;
            }
            graphics.pose().pushPose();
            worldPose(graphics, g, px, py);
            Layer.blit(graphics, worldLayer.id(), 0, 0, worldW, worldH);
            for (Node node : nodes) {
                int position = node.position();
                BookView.QuestView quest = ClientQuests.quest(position);
                if (secret(position)) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 150);
                    graphics.drawString(font, "?", node.x() - font.width("?") / 2, node.y() - 4, SAND, true);
                    graphics.pose().popPose();
                } else if (!quest.icon().isEmpty()) {
                    boolean dim = ClientQuests.state(position) == QuestState.LOCKED;
                    if (dim) {
                        RenderSystem.setShaderColor(0.62F, 0.56F, 0.5F, 1.0F);
                    }
                    graphics.renderFakeItem(quest.icon().get((int) (Util.getMillis() / 1000L % quest.icon().size())), node.x() - 8, node.y() - 8);
                    if (dim) {
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }
            }
            graphics.pose().translate(0, 0, 170);
            for (Node node : nodes) {
                paintBadges(graphics, node, tick);
            }
            graphics.pose().popPose();
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 175);
        vignetteLayer.draw(graphics, view.x(), view.y());
        graphics.pose().popPose();
        graphics.disableScissor();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 180);
        Rect recenter = new Rect(view.x() + 3, view.bottom() - 14, 11, 11);
        boolean recenterHover = recenter.contains(mouseX, mouseY) && !overDetail;
        Canvas button = buttonLayer.begin(13, 13);
        Art.button(button, 1, 1, 11, 11, Art.ButtonStyle.ANDESITE, recenterHover, false, false);
        Art.recenterIcon(button, 2, 2);
        buttonLayer.commit();
        buttonLayer.draw(graphics, recenter.x() - 1, recenter.y() - 1);
        if (!overDetail) {
            hits.add(new Hit(recenter, this::recenter));
            tips.add(new Tip(recenter, List.of(Component.translatable("screen.rift_quests.recenter")), ItemStack.EMPTY));
        }
        graphics.drawString(font, Component.translatable("screen.rift_quests.drag_hint"), view.x() + 18, view.bottom() - 12, 0x80C9A58A, false);
        String zoomText = Math.round(zoom * 100.0 / g) + "%";
        int zoomRight = selected >= 0 ? detail.x() - 4 : view.right() - 4;
        graphics.drawString(font, zoomText, zoomRight - font.width(zoomText), view.bottom() - 12, 0x80C9A58A, false);
        if (!lockedChapter && nodes.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.rift_quests.empty_chapter"), view.x() + effectiveWidth() / 2,
                    view.y() + view.h() / 2 - 4, SAND);
        }
        graphics.pose().popPose();
        if (hovered >= 0) {
            List<Component> lines = nodeTooltip(hovered);
            lines.add(Component.translatable("screen.rift_quests.click_details").withStyle(ChatFormatting.DARK_GRAY));
            tips.add(new Tip(new Rect(mouseX, mouseY, 1, 1), lines, ItemStack.EMPTY));
        }
    }

    private void worldPose(GuiGraphics graphics, int g, int px, int py) {
        graphics.pose().translate(view.x(), view.y(), 0);
        graphics.pose().scale(1.0F / g, 1.0F / g, 1.0F);
        graphics.pose().translate(px, py, 0);
        graphics.pose().scale(zoom, zoom, 1.0F);
    }

    private void paintBadges(GuiGraphics graphics, Node node, int tick) {
        int position = node.position();
        QuestState state = ClientQuests.state(position);
        int size = node.size();
        int x0 = node.x() - (size >> 1);
        int y0 = node.y() - (size >> 1);
        if (state == QuestState.LOCKED) {
            graphics.fill(node.x() - 8, node.y() - 8, node.x() + 8, node.y() + 8, 0x40140A04);
            sprite(graphics, Sprite.LOCK, x0 + size - 6, y0 + size - 8);
        } else if (state == QuestState.CLAIMED) {
            sprite(graphics, Sprite.CHECK, x0 + size - 6, y0 + size - 6);
        } else if (state == QuestState.COMPLETE) {
            sprite(graphics, Sprite.BANG, x0 + size - 4, y0 - 6 + ((tick >> 2) & 1));
        }
        if (ClientQuests.quest(position).team()) {
            sprite(graphics, Sprite.TEAM, x0 - 6, y0 + size - 5);
        }
    }

    private void renderLockedScene(GuiGraphics graphics, BookView.ChapterView chapterView) {
        int vw = view.w();
        int vh = view.h();
        int cy = vh * 41 / 100;
        if (lockedLayer.stale(List.of(vw, vh))) {
            Canvas c = lockedLayer.begin(vw, vh);
            int cx = vw / 2;
            Art.shaftH(c, 8, vw - 8, cy, Art.ShaftKind.RUST, 0);
            Art.gearbox(c, vw * 18 / 100, cy, Art.ShaftKind.RUST, 0);
            Art.gearbox(c, vw * 82 / 100, cy, Art.ShaftKind.RUST, 0);
            Canvas ring = Art.cog(Art.BIG_RING, 5, Art.COG_RUST);
            c.draw(ring, cx - (ring.width >> 1), cy - (ring.height >> 1));
            c.draw(Art.casing(false, true, 11), cx - 13, cy - 13);
            c.drawScaled(Art.LOCK, cx - 9, cy - 11, 2);
            Canvas cog = Art.cog(Art.SMALL_COG, 3, Art.COG_RUST);
            c.draw(cog, vw * 29 / 100 - (cog.width >> 1), cy - (cog.height >> 1));
            c.draw(cog, vw * 71 / 100 - (cog.width >> 1), cy - (cog.height >> 1));
            int tubesY = cy + 40;
            int start = vw / 2 - (6 * 9 - 1) / 2;
            for (int index = 0; index < 6; index++) {
                Art.nixieTube(c, start + index * 9, tubesY, false);
            }
            lockedLayer.commit();
        }
        lockedLayer.draw(graphics, view.x(), view.y());
        String word = Component.translatable("screen.rift_quests.locked").getString().toUpperCase(Locale.ROOT);
        int start = view.x() + vw / 2 - (6 * 9 - 1) / 2;
        for (int index = 0; index < Math.min(6, word.length()); index++) {
            String glyph = String.valueOf(word.charAt(index));
            texts.add(new TextOp(plain(glyph), start + index * 9 + (8 - font.width(glyph) + 1) / 2, view.y() + cy + 44, NIXIE, false, true, null));
        }
        int y = view.y() + cy + 62;
        int textW = Math.min(220, vw - 20);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("screen.rift_quests.chapter_locked"));
        for (ResourceLocation required : chapterView.unlock()) {
            int position = ClientQuests.indexOf(required);
            if (position >= 0 && !ClientQuests.state(position).done()) {
                lines.add(ClientQuests.quest(position).title());
            }
        }
        for (Component line : lines) {
            for (FormattedCharSequence part : font.split(line, textW)) {
                texts.add(new TextOp(part, view.x() + vw / 2 - font.width(part) / 2, y, SAND, false, false, null));
                y += 10;
            }
        }
        flushTexts(graphics);
    }

    private void recenter() {
        panX = 0;
        panY = 0;
        zoom = guiScale();
        clampPan();
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
        lines.add(Component.translatable("screen.rift_quests.size_line." + quest.size().name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        if (quest.team()) {
            lines.add(Component.translatable("screen.rift_quests.team_short", quest.teamMinPlayers()).withStyle(style -> style.withColor(0xE09A5A)));
        }
        return lines;
    }

    private Canvas detailBase(int w, int h) {
        if (detailBase == null || detailBase.width != w || detailBase.height != h) {
            detailBase = new Canvas(w, h);
            Art.panel(detailBase, 0, 0, w, h, Art.PANEL_BRASS);
        }
        return detailBase;
    }

    private void label(Canvas c, int px, int py, Component text, int x, int y, int w, Rect clip) {
        texts.add(new TextOp(text.getVisualOrderText(), x, y, COPPER, false, false, clip));
        int lineX = x + font.width(text) + 4 - px;
        c.rect(lineX, y + 4 - py, Math.max(0, x + w - px - lineX), 1, 0xFF4A2A16);
        c.rect(lineX, y + 5 - py, Math.max(0, x + w - px - lineX), 1, 0xFF120804);
    }

    private void renderDetail(GuiGraphics graphics, int mouseX, int mouseY, long now) {
        int position = selected;
        BookView.QuestView quest = ClientQuests.quest(position);
        QuestState state = ClientQuests.state(position);
        boolean hidden = secret(position);
        long since = now - detailOpenedAt;
        int slide = since < SLIDE_MILLIS ? 36 - 9 * (int) (since / (SLIDE_MILLIS / 4)) : 0;
        int px = detail.x() + slide;
        int py = detail.y();
        int pw = detail.w();
        int ph = detail.h();
        Canvas c = detailLayer.begin(pw, ph);
        c.copyFrom(detailBase(pw, ph));
        claimedSlots.clear();
        Rect close = new Rect(px + pw - 18, py + 7, 11, 11);
        Art.button(c, close.x() - px, close.y() - py, 11, 11, Art.ButtonStyle.ANDESITE, close.contains(mouseX, mouseY), false, false);
        texts.add(new TextOp(plain("x"), close.x() + 3, close.y() + 1, 0xFF241D18, false, false, null));
        hits.add(new Hit(close, () -> select(-1)));
        Canvas casing = Art.casing(quest.size() == QuestSize.SMALL, state == QuestState.LOCKED, position % 13 + 1);
        c.draw(casing, 8 + (26 - casing.width) / 2, 8 + (26 - casing.height) / 2);
        if (hidden) {
            texts.add(new TextOp(plain("?"), px + 21 - font.width("?") / 2, py + 17, SAND, true, false, null));
        } else if (!quest.icon().isEmpty()) {
            items.add(new ItemOp(quest.icon().get((int) (now / 1000L % quest.icon().size())), px + 13, py + 13, null, state == QuestState.LOCKED, null));
        }
        Component title = hidden ? Component.literal("???") : quest.title();
        int titleW = pw - 39 - 21;
        texts.add(new TextOp(plain(fit(title, titleW)), px + 39, py + 10, PAPER, true, false, null));
        if (font.width(title) > titleW) {
            tips.add(new Tip(new Rect(px + 39, py + 9, titleW, 10), List.of(title), ItemStack.EMPTY));
        }
        Component sizeText = Component.translatable("screen.rift_quests.size." + quest.size().name().toLowerCase(Locale.ROOT));
        texts.add(new TextOp(sizeText.getVisualOrderText(), px + 39, py + 23, SAND, false, false, null));
        String chip = Component.translatable("screen.rift_quests.chip." + state.name().toLowerCase(Locale.ROOT)).getString().toUpperCase(Locale.ROOT);
        int chipX = 39 + font.width(sizeText) + 5;
        int chipW = font.width(chip) + 5;
        if (chipX + chipW <= pw - 6) {
            Art.ring(c, chipX, 21, chipW, 12, stateColour(state));
            texts.add(new TextOp(plain(chip), px + chipX + 3, py + 23, stateColour(state), false, false, null));
        }
        Rect body = new Rect(px + 8, py + 38, pw - 8 - 9, ph - 38 - 26);
        boolean inBody = body.contains(mouseX, mouseY);
        c.clip(body.x() - px, body.y() - py, body.w(), body.h());
        int x = body.x();
        int w = body.w();
        int y = body.y() - detailScroll;
        if (quest.team()) {
            List<FormattedCharSequence> lines = new ArrayList<>(font.split(Component.translatable("screen.rift_quests.team_box", quest.teamMinPlayers(),
                    quest.teamRadius()), w - 24));
            if (ClientQuests.waitingForTeam(position)) {
                lines.addAll(font.split(Component.translatable("tracker.rift_quests.team"), w - 24));
            }
            int boxH = lines.size() * 9 + 6;
            c.rect(x - px, y - py, w, boxH, 0xFF1B0E07);
            Art.ring(c, x - px, y - py, w, boxH, 0xFF8A4B23);
            Art.ring(c, x - px + 1, y - py + 1, w - 2, boxH - 2, 0xFF2A160A);
            c.draw(Art.TEAM, x - px + 3, y - py + 3);
            for (int index = 0; index < lines.size(); index++) {
                texts.add(new TextOp(lines.get(index), x + 20, y + 3 + index * 9, index == 0 ? NIXIE : LCOPPER, false, false, body));
            }
            y += boxH + 5;
        }
        if (!hidden && quest.subtitle().isPresent()) {
            for (FormattedCharSequence line : font.split(quest.subtitle().get(), w)) {
                texts.add(new TextOp(line, x, y, LCOPPER, false, false, body));
                y += 9;
            }
            y += 2;
        }
        if (!hidden && quest.description().isPresent()) {
            for (FormattedCharSequence line : font.split(quest.description().get(), w)) {
                texts.add(new TextOp(line, x, y, SAND, false, false, body));
                y += 9;
            }
            y += 3;
        }
        if (!quest.dependencies().isEmpty()) {
            label(c, px, py, Component.translatable(quest.dependencies().size() > 1 && quest.dependencyMode() == DependencyMode.ANY
                    ? "screen.rift_quests.requires_any" : "screen.rift_quests.requires"), x, y, w, body);
            y += 11;
            for (ResourceLocation dependency : quest.dependencies()) {
                int from = ClientQuests.indexOf(dependency);
                if (from < 0) {
                    continue;
                }
                boolean done = ClientQuests.state(from).done();
                Rect row = new Rect(x, y, w, 10);
                boolean hover = inBody && row.contains(mouseX, mouseY) && visible(from);
                c.rect(x - px, y + 2 - py, 6, 6, 0xFF120804);
                c.rect(x - px + 1, y + 3 - py, 4, 4, done ? OK : 0xFF9A6240);
                Component name = secret(from) ? Component.literal("???") : ClientQuests.quest(from).title();
                texts.add(new TextOp(plain(fit(name, w - 10)), x + 9, y + 1, hover ? PAPER : done ? SAND : LOCK_CHIP, false, false, body));
                if (inBody && visible(from)) {
                    hits.add(new Hit(row, () -> focusQuest(from)));
                }
                y += 10;
            }
            y += 2;
        }
        label(c, px, py, Component.translatable("screen.rift_quests.tasks"), x, y, w, body);
        y += 11;
        boolean active = state == QuestState.AVAILABLE || state == QuestState.IN_PROGRESS;
        for (int task = 0; task < quest.tasks().size(); task++) {
            BookView.TaskView taskView = quest.tasks().get(task);
            long target = Math.max(1, taskView.target());
            long progress = state.done() ? target : Math.min(ClientQuests.progress(position, task), target);
            boolean taskDone = progress >= target;
            ItemStack icon = taskView.icons().isEmpty() ? ItemStack.EMPTY : taskView.icons().get((int) (now / 1000L % taskView.icons().size()));
            int textX = x;
            if (!icon.isEmpty()) {
                items.add(new ItemOp(icon, x, y, null, false, body));
                Rect slot = new Rect(x, y, 16, 16);
                if (inBody) {
                    tips.add(new Tip(slot, List.of(), icon));
                    hits.add(new Hit(slot, () -> EmiLink.showRecipes(icon)));
                }
                textX = x + 19;
            }
            String count = progress + "/" + target;
            int countW = font.width(count);
            int right = x + w;
            boolean actionable = active && !taskDone && taskView.action() != BookView.TaskAction.NONE;
            if (actionable) {
                Component actionText = Component.translatable(taskView.action() == BookView.TaskAction.CHECK ? "screen.rift_quests.check" : "screen.rift_quests.submit");
                int buttonW = font.width(actionText) + 10;
                Rect button = new Rect(right - buttonW, y + 2, buttonW, 12);
                boolean hover = inBody && button.contains(mouseX, mouseY);
                Art.button(c, button.x() - px, button.y() - py, button.w(), button.h(), Art.ButtonStyle.BRASS, hover, false, false);
                texts.add(new TextOp(actionText.getVisualOrderText(), button.x() + 5, button.y() + 2, BUTTON_DARK, false, false, body));
                String key = taskView.key();
                if (inBody) {
                    hits.add(new Hit(button, () -> PacketDistributor.sendToServer(new Payloads.TaskAction(quest.id(), key))));
                }
                right = button.x() - 4;
            }
            texts.add(new TextOp(plain(count), right - countW, y + 4, taskDone ? OK : LCOPPER, false, false, body));
            Component labelText = hidden ? Component.literal("???") : taskView.label();
            int labelW = right - countW - 4 - textX;
            texts.add(new TextOp(plain(fit(labelText, labelW)), textX, y + 4, PAPER, false, false, body));
            Rect labelArea = new Rect(textX, y, Math.max(1, labelW), 16);
            if (inBody && (font.width(labelText) > labelW || !taskView.details().isEmpty())) {
                List<Component> lines = new ArrayList<>();
                lines.add(labelText);
                lines.addAll(taskView.details());
                tips.add(new Tip(labelArea, lines, ItemStack.EMPTY));
            }
            Art.gauge(c, x - px, y + 17 - py, w, 7, progress / (double) target, taskDone);
            y += 28;
        }
        y += 1;
        label(c, px, py, Component.translatable("screen.rift_quests.rewards"), x, y, w, body);
        y += 12;
        y = rewards(c, px, py, quest.rewards(), position, true, x, y, w, body, inBody, mouseX, mouseY);
        int tickets = 0;
        for (BookView.RewardView reward : quest.rewards()) {
            if (reward.kind() == BookView.RewardKind.TICKET) {
                tickets += reward.amount();
            }
        }
        if (tickets > 0) {
            MutableComponent line = Component.literal("+" + tickets + " ").withStyle(style -> style.withColor(SAND))
                    .append(RainbowText.of(new ItemStack(ModItems.RIFTWATCH_TICKET.get()).getHoverName().getString()));
            if (quest.team()) {
                line.append(Component.literal(" ").append(Component.translatable("screen.rift_quests.first_clear")).withStyle(style -> style.withColor(DIM)));
            }
            for (FormattedCharSequence part : font.split(line, w)) {
                texts.add(new TextOp(part, x, y, SAND, false, false, body));
                y += 9;
            }
            y += 2;
        }
        if (quest.team() && !quest.teamRepeatRewards().isEmpty()) {
            texts.add(new TextOp(Component.translatable("screen.rift_quests.team_repeat").getVisualOrderText(), x, y, DIM, false, false, body));
            y += 11;
            y = rewards(c, px, py, quest.teamRepeatRewards(), position, false, x, y, w, body, inBody, mouseX, mouseY);
        }
        detailContent = y + detailScroll - body.y();
        detailScroll = Mth.clamp(detailScroll, 0, Math.max(0, detailContent - body.h()));
        c.resetClip();
        if (detailContent > body.h()) {
            int trackX = pw - 8;
            int trackY = body.y() - py;
            c.rect(trackX, trackY, 3, body.h(), 0xFF160C06);
            int barHeight = Math.max(12, body.h() * body.h() / detailContent);
            int barY = trackY + (body.h() - barHeight) * detailScroll / Math.max(1, detailContent - body.h());
            c.rect(trackX, barY, 3, barHeight, 0xFF8A4B23);
        }
        foot(c, px, py, pw, ph, quest, state, position, mouseX, mouseY, now);
        detailLayer.commit();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        detailLayer.draw(graphics, px, py);
        flushTexts(graphics);
        flushItems(graphics);
        if (!claimedSlots.isEmpty()) {
            graphics.pose().translate(0, 0, 220);
            graphics.enableScissor(body.x(), body.y(), body.right(), body.bottom());
            for (Rect slot : claimedSlots) {
                graphics.fill(slot.x() + 1, slot.y() + 1, slot.right() - 1, slot.bottom() - 1, 0x90000000);
                sprite(graphics, Sprite.CHECK, slot.x() + 5, slot.y() + 5);
            }
            graphics.disableScissor();
        }
        graphics.pose().popPose();
    }

    private void foot(Canvas c, int px, int py, int pw, int ph, BookView.QuestView quest, QuestState state, int position, int mouseX, int mouseY, long now) {
        int footY = py + ph - 8 - 13;
        int x = px + 8;
        int maxRight = px + pw - 8;
        if (state == QuestState.COMPLETE) {
            boolean ready = choicesReady(quest, position);
            Component text = Component.translatable("screen.rift_quests.claim");
            Rect button = new Rect(x, footY, font.width(text) + 12, 13);
            boolean hover = ready && button.contains(mouseX, mouseY);
            boolean glow = ready && (now / 600L) % 2L == 1L;
            Art.button(c, button.x() - px, button.y() - py, button.w(), button.h(), ready ? Art.ButtonStyle.BRASS : Art.ButtonStyle.DISABLED, hover, false, glow);
            texts.add(new TextOp(text.getVisualOrderText(), button.x() + 6, button.y() + 3, ready ? BUTTON_DARK : 0xFF4A4030, false, false, null));
            if (ready) {
                hits.add(new Hit(button, () -> PacketDistributor.sendToServer(new Payloads.Claim(quest.id(), Map.copyOf(choices)))));
            } else {
                tips.add(new Tip(button, List.of(Component.translatable("screen.rift_quests.choose_first")), ItemStack.EMPTY));
            }
            return;
        }
        if (state == QuestState.CLAIMED) {
            c.draw(Art.CHECK, x - px, footY + 2 - py);
            texts.add(new TextOp(plain(fit(Component.translatable("screen.rift_quests.foot.claimed"), maxRight - x - 13)), x + 13, footY + 3, OK, false, false, null));
            return;
        }
        if (state == QuestState.LOCKED) {
            c.draw(Art.LOCK, x - px, footY + 1 - py);
            texts.add(new TextOp(plain(fit(Component.translatable("screen.rift_quests.foot.locked"), maxRight - x - 13)), x + 13, footY + 3, LOCK_CHIP, false, false,
                    null));
            return;
        }
        boolean pinned = ClientQuests.isPinned(position);
        Component text = Component.translatable(pinned ? "screen.rift_quests.unpin" : "screen.rift_quests.pin");
        Rect button = new Rect(x, footY, font.width(text) + 12, 13);
        boolean hover = button.contains(mouseX, mouseY);
        Art.button(c, button.x() - px, button.y() - py, button.w(), button.h(), pinned ? Art.ButtonStyle.LIT : Art.ButtonStyle.ANDESITE, hover, false, false);
        texts.add(new TextOp(text.getVisualOrderText(), button.x() + 6, button.y() + 3, pinned ? BUTTON_DARK : BUTTON_GREY, false, false, null));
        hits.add(new Hit(button, () -> PacketDistributor.sendToServer(new Payloads.Pin(quest.id()))));
        tips.add(new Tip(button, List.of(Component.translatable("screen.rift_quests.pin_hint")), ItemStack.EMPTY));
        Component status;
        if (ClientQuests.waitingForTeam(position)) {
            status = Component.translatable("screen.rift_quests.foot.waiting");
        } else if (state == QuestState.IN_PROGRESS) {
            status = Component.translatable("screen.rift_quests.foot.progress", Math.round(fraction(position) * 100));
        } else {
            status = Component.translatable("screen.rift_quests.foot.ready");
        }
        texts.add(new TextOp(plain(fit(status, maxRight - button.right() - 5)), button.right() + 5, footY + 3, AMBER, false, false, null));
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

    private int rewards(Canvas c, int px, int py, List<BookView.RewardView> rewards, int position, boolean claimable, int x, int y, int w, Rect body,
            boolean inBody, int mouseX, int mouseY) {
        int slotX = x;
        for (int index = 0; index < rewards.size(); index++) {
            BookView.RewardView reward = rewards.get(index);
            boolean claimed = claimable && ClientQuests.claimed(position, index);
            if (reward.kind() == BookView.RewardKind.CHOICE) {
                if (slotX != x) {
                    slotX = x;
                    y += 20;
                }
                texts.add(new TextOp(plain(fit(reward.label(), w)), x, y + 1, claimed ? DIM : GOLD, false, false, body));
                y += 11;
                Integer picked = choices.get(reward.key());
                for (int option = 0; option < reward.options().size(); option++) {
                    if (slotX + 18 > x + w) {
                        slotX = x;
                        y += 20;
                    }
                    BookView.RewardView choice = reward.options().get(option);
                    Rect slot = new Rect(slotX, y, 18, 18);
                    rewardSlot(c, px, py, choice, slot, claimed, body, inBody, mouseX, mouseY);
                    if (picked != null && picked == option) {
                        Art.ring(c, slot.x() - 1 - px, slot.y() - 1 - py, 20, 20, 0xFFFFE0A8);
                    }
                    if (!claimed && claimable && ClientQuests.state(position) == QuestState.COMPLETE && inBody) {
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
            if (slotX + 18 > x + w) {
                slotX = x;
                y += 20;
            }
            rewardSlot(c, px, py, reward, new Rect(slotX, y, 18, 18), claimed, body, inBody, mouseX, mouseY);
            slotX += 20;
        }
        return slotX == x ? y : y + 22;
    }

    private void rewardSlot(Canvas c, int px, int py, BookView.RewardView reward, Rect slot, boolean claimed, Rect body, boolean inBody, int mouseX,
            int mouseY) {
        boolean ticket = reward.kind() == BookView.RewardKind.TICKET;
        Art.slot(c, slot.x() - px, slot.y() - py, ticket);
        ItemStack stack = reward.stacks().isEmpty() ? ItemStack.EMPTY : reward.stacks().get((int) (Util.getMillis() / 1000L % reward.stacks().size()));
        if (!stack.isEmpty()) {
            items.add(new ItemOp(stack, slot.x() + 1, slot.y() + 1, reward.amount() > 1 ? String.valueOf(reward.amount()) : null, false, body));
        }
        if (claimed) {
            claimedSlots.add(slot);
        }
        if (inBody && slot.contains(mouseX, mouseY)) {
            if ((reward.kind() == BookView.RewardKind.ITEM || ticket) && !stack.isEmpty()) {
                tips.add(new Tip(slot, List.of(), stack.copyWithCount(Math.max(1, Math.min(reward.amount(), stack.getMaxStackSize())))));
                hits.add(new Hit(slot, () -> EmiLink.showRecipes(stack)));
            } else {
                tips.add(new Tip(slot, List.of(reward.label()), ItemStack.EMPTY));
            }
        }
    }

    private Component stateLabel(QuestState state) {
        return Component.translatable("screen.rift_quests.state." + state.name().toLowerCase(Locale.ROOT));
    }

    private int stateColour(QuestState state) {
        return switch (state) {
            case LOCKED -> LOCK_CHIP;
            case AVAILABLE -> AMBER;
            case IN_PROGRESS -> LCOPPER;
            case COMPLETE -> GOLD;
            case CLAIMED -> OK;
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
        if (view != null && view.contains(mouseX, mouseY) && !(selected >= 0 && detail.contains(mouseX, mouseY))) {
            dragging = true;
            dragged = false;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            int g = guiScale();
            panX += dragX * g;
            panY += dragY * g;
            if (Math.abs(dragX) + Math.abs(dragY) > 0.0) {
                dragged = true;
            }
            clampPan();
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
        int g = guiScale();
        double localX = (mouseX * g - view.x() * (double) g - Math.round(panX)) / zoom;
        double localY = (mouseY * g - view.y() * (double) g - Math.round(panY)) / zoom;
        BookView.ChapterView chapterView = chapterView();
        if (chapterView != null && !ClientQuests.chapterUnlocked(chapterView)) {
            return -1;
        }
        for (Node node : nodes) {
            if (Math.abs(localX - node.x()) <= node.half() && Math.abs(localY - node.y()) <= node.half()) {
                return node.position();
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
        if (selected >= 0 && detail.contains(mouseX, mouseY)) {
            detailScroll -= (int) (scrollY * 12);
            return true;
        }
        if (view.contains(mouseX, mouseY)) {
            int g = guiScale();
            int next = Mth.clamp(zoom + (scrollY > 0 ? 1 : -1), 1, g * 2);
            if (next != zoom) {
                double anchorX = mouseX * g - view.x() * (double) g;
                double anchorY = mouseY * g - view.y() * (double) g;
                panX = anchorX - (anchorX - panX) * next / zoom;
                panY = anchorY - (anchorY - panY) * next / zoom;
                zoom = next;
                clampPan();
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
