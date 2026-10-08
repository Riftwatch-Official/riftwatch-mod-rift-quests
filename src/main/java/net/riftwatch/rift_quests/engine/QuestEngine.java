package net.riftwatch.rift_quests.engine;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToLongFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.riftwatch.rift_quests.book.Chapter;
import net.riftwatch.rift_quests.book.DependencyMode;
import net.riftwatch.rift_quests.book.Quest;
import net.riftwatch.rift_quests.book.QuestReward;
import net.riftwatch.rift_quests.book.QuestTask;
import net.riftwatch.rift_quests.book.Reward;
import net.riftwatch.rift_quests.book.Task;
import net.riftwatch.rift_quests.network.Payloads;
import net.riftwatch.rift_quests.network.QuestState;
import net.riftwatch.rift_quests.progress.PlayerQuestData;
import net.riftwatch.rift_quests.progress.QuestRecord;
import net.riftwatch.rift_quests.registry.ModAttachments;
import net.riftwatch.rift_quests.registry.ModItems;
import org.slf4j.Logger;

public final class QuestEngine {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int POLL_INTERVAL = 20;
    private static final int MAX_CASCADE = 16;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private static final class Session {
        private boolean dirty = true;
        private boolean pollNow = true;
        private List<Quest> active;
        private final Set<ResourceLocation> waitingForTeam = new HashSet<>();
        private long lastUseTick = -1;
    }

    private QuestEngine() {
    }

    private static Session session(ServerPlayer player) {
        return SESSIONS.computeIfAbsent(player.getUUID(), id -> new Session());
    }

    public static PlayerQuestData data(ServerPlayer player) {
        return ModAttachments.progress(player);
    }

    public static void bookActivated(MinecraftServer server) {
        Matchers.reset(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendBook(player);
            migrate(player);
            invalidate(player);
        }
    }

    public static void sendBook(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new Payloads.Book(ServerBook.current().bytes()));
        session(player).dirty = true;
    }

    public static void login(ServerPlayer player) {
        migrate(player);
        PlayerQuestData data = data(player);
        if (!data.bookGiven()) {
            give(player, new ItemStack(ModItems.QUEST_BOOK.get()));
            data.markBookGiven();
        }
        invalidate(player);
    }

    public static void logout(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    public static void invalidate(ServerPlayer player) {
        Session session = session(player);
        session.active = null;
        session.dirty = true;
        session.pollNow = true;
    }

    public static boolean usedThisTick(ServerPlayer player) {
        Session session = session(player);
        long tick = player.serverLevel().getGameTime();
        if (session.lastUseTick == tick) {
            return true;
        }
        session.lastUseTick = tick;
        return false;
    }

    public static void serverTick(MinecraftServer server) {
        int tick = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Session session = session(player);
            if (session.pollNow || (tick + (player.getId() & 0xFF)) % POLL_INTERVAL == 0) {
                session.pollNow = false;
                poll(player);
            }
            if (session.dirty) {
                session.dirty = false;
                PacketDistributor.sendToPlayer(player, progressPayload(player, session));
            }
        }
    }

    public static QuestState state(ServerBook book, PlayerQuestData data, Quest quest) {
        QuestRecord record = data.find(quest.id()).orElse(null);
        if (record != null && record.completed()) {
            return allClaimed(quest, record) ? QuestState.CLAIMED : QuestState.COMPLETE;
        }
        if (!unlocked(book, data, quest)) {
            return QuestState.LOCKED;
        }
        return record != null && !record.progressMap().isEmpty() ? QuestState.IN_PROGRESS : QuestState.AVAILABLE;
    }

    public static boolean unlocked(ServerBook book, PlayerQuestData data, Quest quest) {
        Chapter chapter = book.chapter(quest.chapter());
        if (chapter != null) {
            for (ResourceLocation required : chapter.unlock()) {
                if (!data.isCompleted(required)) {
                    return false;
                }
            }
        }
        if (quest.dependencies().isEmpty()) {
            return true;
        }
        if (quest.dependencyMode() == DependencyMode.ANY) {
            return quest.dependencies().stream().anyMatch(data::isCompleted);
        }
        return quest.dependencies().stream().allMatch(data::isCompleted);
    }

    private static boolean allClaimed(Quest quest, QuestRecord record) {
        for (QuestReward reward : quest.effectiveRewards()) {
            if (!record.claimed().contains(reward.key())) {
                return false;
            }
        }
        return true;
    }

    private static List<Quest> active(ServerPlayer player) {
        Session session = session(player);
        if (session.active == null) {
            ServerBook book = ServerBook.current();
            PlayerQuestData data = data(player);
            List<Quest> active = new ArrayList<>();
            for (Quest quest : book.quests()) {
                if (!data.isCompleted(quest.id()) && unlocked(book, data, quest)) {
                    active.add(quest);
                }
            }
            session.active = List.copyOf(active);
        }
        return session.active;
    }

    public static long target(QuestTask task) {
        return BookViews.target(task.task());
    }

    private static boolean taskDone(QuestRecord record, QuestTask task) {
        return record.progress(task.key()) >= target(task);
    }

    private static void poll(ServerPlayer player) {
        for (int round = 0; round < MAX_CASCADE; round++) {
            boolean completedAny = false;
            for (Quest quest : active(player)) {
                QuestRecord record = data(player).record(quest.id());
                boolean changed = false;
                for (QuestTask task : quest.tasks()) {
                    if (taskDone(record, task)) {
                        continue;
                    }
                    long measured = TaskChecks.measure(player, task, record);
                    if (measured >= 0) {
                        long value = Math.min(measured, target(task));
                        if (value != record.progress(task.key())) {
                            record.setProgress(task.key(), value);
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    session(player).dirty = true;
                }
                if (tryComplete(player, quest)) {
                    completedAny = true;
                }
            }
            if (!completedAny) {
                return;
            }
        }
    }

    public static void count(ServerPlayer player, ToLongFunction<Task> amount) {
        boolean completedAny = false;
        for (Quest quest : active(player)) {
            QuestRecord record = null;
            for (QuestTask task : quest.tasks()) {
                long add = amount.applyAsLong(task.task());
                if (add <= 0) {
                    continue;
                }
                if (record == null) {
                    record = data(player).record(quest.id());
                }
                if (taskDone(record, task)) {
                    continue;
                }
                record.setProgress(task.key(), Math.min(target(task), record.progress(task.key()) + add));
                session(player).dirty = true;
            }
            if (record != null && tryComplete(player, quest)) {
                completedAny = true;
            }
        }
        if (completedAny) {
            session(player).pollNow = true;
        }
    }

    public static void pollSoon(ServerPlayer player) {
        session(player).pollNow = true;
    }

    private static boolean tryComplete(ServerPlayer player, Quest quest) {
        PlayerQuestData data = data(player);
        QuestRecord record = data.record(quest.id());
        if (record.completed()) {
            return false;
        }
        for (QuestTask task : quest.tasks()) {
            if (!taskDone(record, task)) {
                return false;
            }
        }
        Session session = session(player);
        if (quest.team().isPresent()) {
            List<ServerPlayer> team = Teams.nearby(player, quest);
            if (team.size() < quest.team().get().minPlayers()) {
                if (session.waitingForTeam.add(quest.id())) {
                    session.dirty = true;
                }
                return false;
            }
            session.waitingForTeam.remove(quest.id());
            for (ServerPlayer member : team) {
                if (member == player) {
                    continue;
                }
                PlayerQuestData memberData = data(member);
                if (memberData.isCompleted(quest.id())) {
                    for (QuestReward reward : quest.effectiveTeamRepeatRewards()) {
                        Rewards.give(member, reward.reward(), -1);
                    }
                    member.sendSystemMessage(Component.literal("Team quest done again: ").append(BookTexts.title(quest))
                            .append(", reduced reward given.").withStyle(ChatFormatting.GOLD));
                } else {
                    QuestRecord memberRecord = memberData.record(quest.id());
                    for (QuestTask task : quest.tasks()) {
                        memberRecord.setProgress(task.key(), target(task));
                    }
                    complete(member, quest, memberRecord);
                }
            }
        }
        complete(player, quest, record);
        return true;
    }

    private static void complete(ServerPlayer player, Quest quest, QuestRecord record) {
        for (QuestTask task : quest.tasks()) {
            if (task.task() instanceof Task.Xp xp && xp.consume()) {
                player.giveExperienceLevels(-xp.level());
            }
        }
        record.complete(player.serverLevel().getGameTime());
        Session session = session(player);
        session.waitingForTeam.remove(quest.id());
        session.active = null;
        session.dirty = true;
        session.pollNow = true;
        ServerBook book = ServerBook.current();
        PacketDistributor.sendToPlayer(player, new Payloads.QuestCompleted(book.id(), book.indexOf(quest.id())));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.2F);
        LOGGER.info("{} completed quest {}", player.getGameProfile().getName(), quest.id());
    }

    public static void forceComplete(ServerPlayer player, Quest quest) {
        QuestRecord record = data(player).record(quest.id());
        if (record.completed()) {
            return;
        }
        for (QuestTask task : quest.tasks()) {
            record.setProgress(task.key(), target(task));
        }
        complete(player, quest, record);
    }

    public static void reset(ServerPlayer player, ResourceLocation quest) {
        data(player).reset(quest);
        invalidate(player);
    }

    public static void resetAll(ServerPlayer player) {
        data(player).resetAll();
        invalidate(player);
    }

    public static boolean addCustomProgress(ServerPlayer player, Quest quest, String taskKey, long amount) {
        if (!active(player).contains(quest)) {
            return false;
        }
        for (QuestTask task : quest.tasks()) {
            if (task.key().equals(taskKey) && task.task() instanceof Task.Custom) {
                QuestRecord record = data(player).record(quest.id());
                record.setProgress(task.key(), Math.min(target(task), record.progress(task.key()) + amount));
                session(player).dirty = true;
                tryComplete(player, quest);
                return true;
            }
        }
        return false;
    }

    public static void check(ServerPlayer player, ResourceLocation questId, String taskKey) {
        Quest quest = ServerBook.current().quest(questId);
        if (quest == null || !active(player).contains(quest)) {
            return;
        }
        for (QuestTask task : quest.tasks()) {
            if (!task.key().equals(taskKey)) {
                continue;
            }
            QuestRecord record = data(player).record(quest.id());
            if (task.task() instanceof Task.Checkmark) {
                record.setProgress(task.key(), 1);
            } else if (task.task() instanceof Task.Item item && item.mode() != Task.ItemMode.DETECT) {
                long missing = target(task) - record.progress(task.key());
                long taken = takeItems(player, item, missing);
                record.setProgress(task.key(), record.progress(task.key()) + taken);
            } else {
                return;
            }
            session(player).dirty = true;
            if (tryComplete(player, quest)) {
                session(player).pollNow = true;
            }
            return;
        }
    }

    private static long takeItems(ServerPlayer player, Task.Item item, long wanted) {
        Inventory inventory = player.getInventory();
        long taken = 0;
        for (int slot = 0; slot < inventory.getContainerSize() && taken < wanted; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (Matchers.item(item.items(), item.components(), stack)) {
                int amount = (int) Math.min(stack.getCount(), wanted - taken);
                stack.shrink(amount);
                taken += amount;
            }
        }
        if (taken > 0) {
            inventory.setChanged();
        }
        return taken;
    }

    public static void claim(ServerPlayer player, ResourceLocation questId, Map<String, Integer> choices) {
        Quest quest = ServerBook.current().quest(questId);
        if (quest == null) {
            return;
        }
        QuestRecord record = data(player).find(questId).orElse(null);
        if (record == null || !record.completed()) {
            return;
        }
        boolean given = false;
        for (QuestReward reward : quest.effectiveRewards()) {
            if (record.claimed().contains(reward.key())) {
                continue;
            }
            int choice = -1;
            if (reward.reward() instanceof Reward.Choice options) {
                Integer picked = choices.get(reward.key());
                if (picked == null || picked < 0 || picked >= options.options().size()) {
                    continue;
                }
                choice = picked;
            }
            record.claimed().add(reward.key());
            Rewards.give(player, reward.reward(), choice);
            given = true;
        }
        if (given) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.0F);
            session(player).dirty = true;
        }
    }

    public static void claimAll(ServerPlayer player) {
        for (Quest quest : ServerBook.current().quests()) {
            claim(player, quest.id(), Map.of());
        }
    }

    public static void pin(ServerPlayer player, ResourceLocation questId) {
        if (ServerBook.current().quest(questId) == null) {
            return;
        }
        data(player).togglePin(questId);
        session(player).dirty = true;
    }

    public static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    private static void migrate(ServerPlayer player) {
        PlayerQuestData data = data(player);
        for (Quest quest : ServerBook.current().quests()) {
            for (ResourceLocation old : quest.replaces()) {
                data.migrate(old, quest.id());
            }
            data.find(quest.id()).ifPresent(record -> {
                for (QuestTask task : quest.tasks()) {
                    task.replaces().forEach(old -> record.renameTask(old, task.key()));
                }
                for (QuestReward reward : quest.effectiveRewards()) {
                    reward.replaces().forEach(old -> record.renameReward(old, reward.key()));
                }
            });
        }
    }

    private static Payloads.Progress progressPayload(ServerPlayer player, Session session) {
        ServerBook book = ServerBook.current();
        PlayerQuestData data = data(player);
        List<Quest> quests = book.quests();
        byte[] states = new byte[quests.size()];
        List<Payloads.QuestEntry> entries = new ArrayList<>();
        for (int index = 0; index < quests.size(); index++) {
            Quest quest = quests.get(index);
            QuestState state = state(book, data, quest);
            states[index] = (byte) state.ordinal();
            QuestRecord record = data.find(quest.id()).orElse(null);
            boolean waiting = session.waitingForTeam.contains(quest.id());
            if (record == null || (state == QuestState.CLAIMED && !waiting) || (state == QuestState.LOCKED && record.progressMap().isEmpty())) {
                continue;
            }
            long[] progress = new long[quest.tasks().size()];
            for (int task = 0; task < progress.length; task++) {
                QuestTask questTask = quest.tasks().get(task);
                progress[task] = record.completed() ? target(questTask) : record.progress(questTask.key());
            }
            int claimedMask = 0;
            List<QuestReward> rewards = quest.effectiveRewards();
            for (int reward = 0; reward < rewards.size() && reward < 31; reward++) {
                if (record.claimed().contains(rewards.get(reward).key())) {
                    claimedMask |= 1 << reward;
                }
            }
            entries.add(new Payloads.QuestEntry(index, progress, claimedMask, waiting));
        }
        int[] pinned = data.pinned().stream().mapToInt(book::indexOf).filter(index -> index >= 0).toArray();
        return new Payloads.Progress(book.id(), states, List.copyOf(entries), pinned);
    }
}
