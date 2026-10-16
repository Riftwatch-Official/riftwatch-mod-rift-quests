package net.riftwatch.rift_quests.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.riftwatch.rift_quests.RiftQuestsMod;

public final class Payloads {
    private static final int MAX_BOOK_BYTES = 1_000_000;

    private Payloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(RiftQuestsMod.MOD_ID, path));
    }

    public record Book(byte[] data) implements CustomPacketPayload {
        public static final Type<Book> TYPE = Payloads.typeOf("book");
        public static final StreamCodec<RegistryFriendlyByteBuf, Book> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeByteArray(payload.data()),
                buf -> new Book(buf.readByteArray(MAX_BOOK_BYTES)));

        @Override
        public Type<Book> type() {
            return TYPE;
        }
    }

    public record QuestEntry(int quest, long[] progress, int claimedMask, boolean waitingForTeam) {
    }

    public record Progress(int book, byte[] states, List<QuestEntry> entries, int[] pinned) implements CustomPacketPayload {
        public static final Type<Progress> TYPE = Payloads.typeOf("progress");
        public static final StreamCodec<RegistryFriendlyByteBuf, Progress> CODEC = StreamCodec.of(Progress::write, Progress::read);

        private static void write(RegistryFriendlyByteBuf buf, Progress payload) {
            buf.writeVarInt(payload.book());
            buf.writeByteArray(payload.states());
            buf.writeVarInt(payload.entries().size());
            for (QuestEntry entry : payload.entries()) {
                buf.writeVarInt(entry.quest());
                buf.writeVarInt(entry.progress().length);
                for (long value : entry.progress()) {
                    buf.writeVarLong(value);
                }
                buf.writeVarInt(entry.claimedMask());
                buf.writeBoolean(entry.waitingForTeam());
            }
            buf.writeVarIntArray(payload.pinned());
        }

        private static Progress read(RegistryFriendlyByteBuf buf) {
            int book = buf.readVarInt();
            byte[] states = buf.readByteArray(65_536);
            int count = buf.readVarInt();
            List<QuestEntry> entries = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                int quest = buf.readVarInt();
                long[] progress = new long[buf.readVarInt()];
                for (int task = 0; task < progress.length; task++) {
                    progress[task] = buf.readVarLong();
                }
                entries.add(new QuestEntry(quest, progress, buf.readVarInt(), buf.readBoolean()));
            }
            return new Progress(book, states, List.copyOf(entries), buf.readVarIntArray(16));
        }

        @Override
        public Type<Progress> type() {
            return TYPE;
        }
    }

    public record QuestCompleted(int book, int quest) implements CustomPacketPayload {
        public static final Type<QuestCompleted> TYPE = Payloads.typeOf("quest_completed");
        public static final StreamCodec<RegistryFriendlyByteBuf, QuestCompleted> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.book());
                    buf.writeVarInt(payload.quest());
                },
                buf -> new QuestCompleted(buf.readVarInt(), buf.readVarInt()));

        @Override
        public Type<QuestCompleted> type() {
            return TYPE;
        }
    }

    public record OrreryState(long pillars) implements CustomPacketPayload {
        public static final Type<OrreryState> TYPE = Payloads.typeOf("orrery_state");
        public static final StreamCodec<RegistryFriendlyByteBuf, OrreryState> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeVarLong(payload.pillars()),
                buf -> new OrreryState(buf.readVarLong()));

        @Override
        public Type<OrreryState> type() {
            return TYPE;
        }
    }

    public record DistrictTitle(String district) implements CustomPacketPayload {
        public static final Type<DistrictTitle> TYPE = Payloads.typeOf("district_title");
        public static final StreamCodec<RegistryFriendlyByteBuf, DistrictTitle> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeUtf(payload.district(), 64),
                buf -> new DistrictTitle(buf.readUtf(64)));

        @Override
        public Type<DistrictTitle> type() {
            return TYPE;
        }
    }

    public record Cinematic(String name, double centreX, double centreY, double centreZ, double landingX, double landingY, double landingZ,
                            double throneTop, boolean skippable) implements CustomPacketPayload {
        public static final Type<Cinematic> TYPE = Payloads.typeOf("cinematic");
        public static final StreamCodec<RegistryFriendlyByteBuf, Cinematic> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.name(), 64);
                    buf.writeDouble(payload.centreX());
                    buf.writeDouble(payload.centreY());
                    buf.writeDouble(payload.centreZ());
                    buf.writeDouble(payload.landingX());
                    buf.writeDouble(payload.landingY());
                    buf.writeDouble(payload.landingZ());
                    buf.writeDouble(payload.throneTop());
                    buf.writeBoolean(payload.skippable());
                },
                buf -> new Cinematic(buf.readUtf(64), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                        buf.readDouble(), buf.readDouble(), buf.readBoolean()));

        @Override
        public Type<Cinematic> type() {
            return TYPE;
        }
    }

    public record VeilPass(boolean open) implements CustomPacketPayload {
        public static final Type<VeilPass> TYPE = Payloads.typeOf("veil_pass");
        public static final StreamCodec<RegistryFriendlyByteBuf, VeilPass> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBoolean(payload.open()),
                buf -> new VeilPass(buf.readBoolean()));

        @Override
        public Type<VeilPass> type() {
            return TYPE;
        }
    }

    public record CinematicSkip() implements CustomPacketPayload {
        public static final Type<CinematicSkip> TYPE = Payloads.typeOf("cinematic_skip");
        public static final StreamCodec<RegistryFriendlyByteBuf, CinematicSkip> CODEC = StreamCodec.unit(new CinematicSkip());

        @Override
        public Type<CinematicSkip> type() {
            return TYPE;
        }
    }

    public record OpenBook() implements CustomPacketPayload {
        public static final Type<OpenBook> TYPE = Payloads.typeOf("open_book");
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenBook> CODEC = StreamCodec.unit(new OpenBook());

        @Override
        public Type<OpenBook> type() {
            return TYPE;
        }
    }

    public record Claim(ResourceLocation quest, Map<String, Integer> choices) implements CustomPacketPayload {
        public static final Type<Claim> TYPE = Payloads.typeOf("claim");
        public static final StreamCodec<RegistryFriendlyByteBuf, Claim> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeResourceLocation(payload.quest());
                    buf.writeMap(payload.choices(), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
                },
                buf -> new Claim(buf.readResourceLocation(), buf.readMap(LinkedHashMap::new, FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt)));

        @Override
        public Type<Claim> type() {
            return TYPE;
        }
    }

    public record TaskAction(ResourceLocation quest, String task) implements CustomPacketPayload {
        public static final Type<TaskAction> TYPE = Payloads.typeOf("task_action");
        public static final StreamCodec<RegistryFriendlyByteBuf, TaskAction> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeResourceLocation(payload.quest());
                    buf.writeUtf(payload.task());
                },
                buf -> new TaskAction(buf.readResourceLocation(), buf.readUtf()));

        @Override
        public Type<TaskAction> type() {
            return TYPE;
        }
    }

    public record Pin(ResourceLocation quest) implements CustomPacketPayload {
        public static final Type<Pin> TYPE = Payloads.typeOf("pin");
        public static final StreamCodec<RegistryFriendlyByteBuf, Pin> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeResourceLocation(payload.quest()),
                buf -> new Pin(buf.readResourceLocation()));

        @Override
        public Type<Pin> type() {
            return TYPE;
        }
    }
}
