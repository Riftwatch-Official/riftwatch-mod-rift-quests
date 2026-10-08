package net.riftwatch.rift_quests.progress;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class QuestRecord {
    public static final String BASE = "base:";
    public static final String MASK = "mask:";
    private static final String[] MEMORY_PREFIXES = {BASE, MASK};

    private final Map<String, Long> progress = new HashMap<>();
    private final Map<String, Long> memory = new HashMap<>();
    private final Set<String> claimed = new HashSet<>();
    private boolean completed;
    private long completedAt;

    public long progress(String task) {
        return progress.getOrDefault(task, 0L);
    }

    public void setProgress(String task, long value) {
        if (value == 0L) {
            progress.remove(task);
        } else {
            progress.put(task, value);
        }
    }

    public Map<String, Long> progressMap() {
        return progress;
    }

    public Map<String, Long> memory() {
        return memory;
    }

    public Set<String> claimed() {
        return claimed;
    }

    public boolean completed() {
        return completed;
    }

    public long completedAt() {
        return completedAt;
    }

    public void complete(long time) {
        completed = true;
        completedAt = time;
    }

    public boolean isEmpty() {
        return !completed && progress.isEmpty() && memory.isEmpty() && claimed.isEmpty();
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("progress", saveLongs(progress));
        tag.put("memory", saveLongs(memory));
        ListTag claimedList = new ListTag();
        claimed.stream().sorted().forEach(key -> claimedList.add(StringTag.valueOf(key)));
        tag.put("claimed", claimedList);
        tag.putBoolean("completed", completed);
        tag.putLong("completed_at", completedAt);
        return tag;
    }

    static QuestRecord load(CompoundTag tag) {
        QuestRecord record = new QuestRecord();
        loadLongs(tag.getCompound("progress"), record.progress);
        loadLongs(tag.getCompound("memory"), record.memory);
        ListTag claimedList = tag.getList("claimed", Tag.TAG_STRING);
        for (int index = 0; index < claimedList.size(); index++) {
            record.claimed.add(claimedList.getString(index));
        }
        record.completed = tag.getBoolean("completed");
        record.completedAt = tag.getLong("completed_at");
        return record;
    }

    public void renameTask(String from, String to) {
        Long value = progress.remove(from);
        if (value != null) {
            progress.merge(to, value, Math::max);
        }
        for (String prefix : MEMORY_PREFIXES) {
            Long remembered = memory.remove(prefix + from);
            if (remembered != null) {
                memory.putIfAbsent(prefix + to, remembered);
            }
        }
    }

    public void renameReward(String from, String to) {
        if (claimed.remove(from)) {
            claimed.add(to);
        }
    }

    void copyFrom(QuestRecord other) {
        progress.putAll(other.progress);
        memory.putAll(other.memory);
        claimed.addAll(other.claimed);
        completed = completed || other.completed;
        completedAt = Math.max(completedAt, other.completedAt);
    }

    private static CompoundTag saveLongs(Map<String, Long> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach(tag::putLong);
        return tag;
    }

    private static void loadLongs(CompoundTag tag, Map<String, Long> target) {
        for (String key : tag.getAllKeys()) {
            target.put(key, tag.getLong(key));
        }
    }
}
