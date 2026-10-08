package net.riftwatch.rift_quests.network;

import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.riftwatch.rift_quests.book.DependencyMode;
import net.riftwatch.rift_quests.book.QuestSize;
import net.riftwatch.rift_quests.book.Visibility;

public record BookView(int id, List<ChapterView> chapters, List<QuestView> quests) {
    public static final BookView EMPTY = new BookView(0, List.of(), List.of());
    private static final int MAX_INFLATED = 32 * 1024 * 1024;

    public enum TaskAction {
        NONE,
        CHECK,
        SUBMIT
    }

    public enum RewardKind {
        ITEM,
        TICKET,
        XP,
        LOOT_TABLE,
        CHOICE,
        COMMAND
    }

    public record ChapterView(ResourceLocation id, Component title, Optional<Component> description, List<ItemStack> icon, List<ResourceLocation> unlock) {
    }

    public record TaskView(String key, Component label, long target, List<ItemStack> icons, TaskAction action, List<Component> details) {
    }

    public record RewardView(String key, RewardKind kind, Component label, List<ItemStack> stacks, int amount, List<RewardView> options) {
    }

    public record QuestView(
            ResourceLocation id,
            ResourceLocation chapter,
            Component title,
            Optional<Component> subtitle,
            Optional<Component> description,
            List<ItemStack> icon,
            QuestSize size,
            int x,
            int y,
            List<ResourceLocation> dependencies,
            DependencyMode dependencyMode,
            Visibility visibility,
            int teamMinPlayers,
            int teamRadius,
            List<TaskView> tasks,
            List<RewardView> rewards,
            List<RewardView> teamRepeatRewards) {

        public boolean team() {
            return teamMinPlayers > 0;
        }
    }

    public Map<ResourceLocation, Integer> indexById() {
        Map<ResourceLocation, Integer> index = new HashMap<>();
        for (int position = 0; position < quests.size(); position++) {
            index.put(quests.get(position).id(), position);
        }
        return index;
    }

    public byte[] toBytes(RegistryAccess access) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access, ConnectionType.NEOFORGE);
        try {
            write(buf);
            byte[] raw = new byte[buf.readableBytes()];
            buf.readBytes(raw);
            Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length / 4 + 64);
            byte[] chunk = new byte[8192];
            while (!deflater.finished()) {
                int length = deflater.deflate(chunk);
                out.write(chunk, 0, length);
            }
            deflater.end();
            return out.toByteArray();
        } finally {
            buf.release();
        }
    }

    public static BookView fromBytes(byte[] compressed, RegistryAccess access) throws DataFormatException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length * 4);
        byte[] chunk = new byte[8192];
        while (!inflater.finished()) {
            int length = inflater.inflate(chunk);
            if (length == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                break;
            }
            out.write(chunk, 0, length);
            if (out.size() > MAX_INFLATED) {
                inflater.end();
                throw new DataFormatException("quest book payload too large");
            }
        }
        inflater.end();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(out.toByteArray()), access, ConnectionType.NEOFORGE);
        try {
            return read(buf);
        } finally {
            buf.release();
        }
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(id);
        buf.writeVarInt(chapters.size());
        for (ChapterView chapter : chapters) {
            buf.writeResourceLocation(chapter.id());
            writeComponent(buf, chapter.title());
            writeOptionalComponent(buf, chapter.description());
            writeStacks(buf, chapter.icon());
            writeIds(buf, chapter.unlock());
        }
        buf.writeVarInt(quests.size());
        for (QuestView quest : quests) {
            buf.writeResourceLocation(quest.id());
            buf.writeResourceLocation(quest.chapter());
            writeComponent(buf, quest.title());
            writeOptionalComponent(buf, quest.subtitle());
            writeOptionalComponent(buf, quest.description());
            writeStacks(buf, quest.icon());
            buf.writeEnum(quest.size());
            buf.writeVarInt(quest.x());
            buf.writeVarInt(quest.y());
            writeIds(buf, quest.dependencies());
            buf.writeEnum(quest.dependencyMode());
            buf.writeEnum(quest.visibility());
            buf.writeVarInt(quest.teamMinPlayers());
            buf.writeVarInt(quest.teamRadius());
            buf.writeVarInt(quest.tasks().size());
            for (TaskView task : quest.tasks()) {
                buf.writeUtf(task.key());
                writeComponent(buf, task.label());
                buf.writeVarLong(task.target());
                writeStacks(buf, task.icons());
                buf.writeEnum(task.action());
                buf.writeVarInt(task.details().size());
                task.details().forEach(detail -> writeComponent(buf, detail));
            }
            writeRewards(buf, quest.rewards());
            writeRewards(buf, quest.teamRepeatRewards());
        }
    }

    private static BookView read(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        int chapterCount = buf.readVarInt();
        List<ChapterView> chapters = new ArrayList<>(chapterCount);
        for (int index = 0; index < chapterCount; index++) {
            chapters.add(new ChapterView(buf.readResourceLocation(), readComponent(buf), readOptionalComponent(buf), readStacks(buf), readIds(buf)));
        }
        int questCount = buf.readVarInt();
        List<QuestView> quests = new ArrayList<>(questCount);
        for (int index = 0; index < questCount; index++) {
            ResourceLocation questId = buf.readResourceLocation();
            ResourceLocation chapter = buf.readResourceLocation();
            Component title = readComponent(buf);
            Optional<Component> subtitle = readOptionalComponent(buf);
            Optional<Component> description = readOptionalComponent(buf);
            List<ItemStack> icon = readStacks(buf);
            QuestSize size = buf.readEnum(QuestSize.class);
            int x = buf.readVarInt();
            int y = buf.readVarInt();
            List<ResourceLocation> dependencies = readIds(buf);
            DependencyMode mode = buf.readEnum(DependencyMode.class);
            Visibility visibility = buf.readEnum(Visibility.class);
            int teamMin = buf.readVarInt();
            int teamRadius = buf.readVarInt();
            int taskCount = buf.readVarInt();
            List<TaskView> tasks = new ArrayList<>(taskCount);
            for (int taskIndex = 0; taskIndex < taskCount; taskIndex++) {
                String key = buf.readUtf();
                Component label = readComponent(buf);
                long target = buf.readVarLong();
                List<ItemStack> icons = readStacks(buf);
                TaskAction action = buf.readEnum(TaskAction.class);
                int detailCount = buf.readVarInt();
                List<Component> details = new ArrayList<>(detailCount);
                for (int detail = 0; detail < detailCount; detail++) {
                    details.add(readComponent(buf));
                }
                tasks.add(new TaskView(key, label, target, icons, action, List.copyOf(details)));
            }
            List<RewardView> rewards = readRewards(buf);
            List<RewardView> teamRepeat = readRewards(buf);
            quests.add(new QuestView(questId, chapter, title, subtitle, description, icon, size, x, y, dependencies, mode, visibility,
                    teamMin, teamRadius, List.copyOf(tasks), rewards, teamRepeat));
        }
        return new BookView(id, List.copyOf(chapters), List.copyOf(quests));
    }

    private static void writeRewards(RegistryFriendlyByteBuf buf, List<RewardView> rewards) {
        buf.writeVarInt(rewards.size());
        for (RewardView reward : rewards) {
            buf.writeUtf(reward.key());
            buf.writeEnum(reward.kind());
            writeComponent(buf, reward.label());
            writeStacks(buf, reward.stacks());
            buf.writeVarInt(reward.amount());
            writeRewards(buf, reward.options());
        }
    }

    private static List<RewardView> readRewards(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<RewardView> rewards = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            String key = buf.readUtf();
            RewardKind kind = buf.readEnum(RewardKind.class);
            Component label = readComponent(buf);
            List<ItemStack> stacks = readStacks(buf);
            int amount = buf.readVarInt();
            List<RewardView> options = readRewards(buf);
            rewards.add(new RewardView(key, kind, label, stacks, amount, options));
        }
        return List.copyOf(rewards);
    }

    private static void writeComponent(RegistryFriendlyByteBuf buf, Component component) {
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, component);
    }

    private static Component readComponent(RegistryFriendlyByteBuf buf) {
        return ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
    }

    private static void writeOptionalComponent(RegistryFriendlyByteBuf buf, Optional<Component> component) {
        buf.writeBoolean(component.isPresent());
        component.ifPresent(value -> writeComponent(buf, value));
    }

    private static Optional<Component> readOptionalComponent(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? Optional.of(readComponent(buf)) : Optional.empty();
    }

    private static void writeStacks(RegistryFriendlyByteBuf buf, List<ItemStack> stacks) {
        buf.writeVarInt(stacks.size());
        stacks.forEach(stack -> ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack));
    }

    private static List<ItemStack> readStacks(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            stacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        return List.copyOf(stacks);
    }

    private static void writeIds(RegistryFriendlyByteBuf buf, List<ResourceLocation> ids) {
        buf.writeVarInt(ids.size());
        ids.forEach(buf::writeResourceLocation);
    }

    private static List<ResourceLocation> readIds(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ResourceLocation> ids = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ids.add(buf.readResourceLocation());
        }
        return List.copyOf(ids);
    }
}
