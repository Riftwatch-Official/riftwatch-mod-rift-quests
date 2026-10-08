package net.riftwatch.rift_quests.progress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class PlayerQuestData implements INBTSerializable<CompoundTag> {
    public static final int MAX_PINNED = 3;
    private static final int FORMAT = 1;

    private final Map<ResourceLocation, QuestRecord> quests = new HashMap<>();
    private final List<ResourceLocation> pinned = new ArrayList<>();
    private boolean bookGiven;

    public Optional<QuestRecord> find(ResourceLocation quest) {
        return Optional.ofNullable(quests.get(quest));
    }

    public QuestRecord record(ResourceLocation quest) {
        return quests.computeIfAbsent(quest, id -> new QuestRecord());
    }

    public boolean isCompleted(ResourceLocation quest) {
        QuestRecord record = quests.get(quest);
        return record != null && record.completed();
    }

    public Map<ResourceLocation, QuestRecord> quests() {
        return quests;
    }

    public void reset(ResourceLocation quest) {
        quests.remove(quest);
    }

    public void resetAll() {
        quests.clear();
        pinned.clear();
    }

    public void migrate(ResourceLocation from, ResourceLocation to) {
        QuestRecord old = quests.remove(from);
        if (old != null) {
            record(to).copyFrom(old);
        }
        int index = pinned.indexOf(from);
        if (index >= 0) {
            if (pinned.contains(to)) {
                pinned.remove(index);
            } else {
                pinned.set(index, to);
            }
        }
    }

    public List<ResourceLocation> pinned() {
        return pinned;
    }

    public boolean togglePin(ResourceLocation quest) {
        if (pinned.remove(quest)) {
            return false;
        }
        pinned.add(quest);
        while (pinned.size() > MAX_PINNED) {
            pinned.removeFirst();
        }
        return true;
    }

    public boolean bookGiven() {
        return bookGiven;
    }

    public void markBookGiven() {
        bookGiven = true;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("format", FORMAT);
        CompoundTag questsTag = new CompoundTag();
        quests.forEach((id, record) -> {
            if (!record.isEmpty()) {
                questsTag.put(id.toString(), record.save());
            }
        });
        tag.put("quests", questsTag);
        ListTag pinnedTag = new ListTag();
        pinned.forEach(id -> pinnedTag.add(StringTag.valueOf(id.toString())));
        tag.put("pinned", pinnedTag);
        tag.putBoolean("book_given", bookGiven);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        quests.clear();
        pinned.clear();
        CompoundTag questsTag = tag.getCompound("quests");
        for (String key : questsTag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                quests.put(id, QuestRecord.load(questsTag.getCompound(key)));
            }
        }
        ListTag pinnedTag = tag.getList("pinned", Tag.TAG_STRING);
        for (int index = 0; index < pinnedTag.size(); index++) {
            ResourceLocation id = ResourceLocation.tryParse(pinnedTag.getString(index));
            if (id != null && !pinned.contains(id)) {
                pinned.add(id);
            }
        }
        bookGiven = tag.getBoolean("book_given");
    }
}
