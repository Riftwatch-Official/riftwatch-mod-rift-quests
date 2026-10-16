package net.riftwatch.rift_quests.arena;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.riftwatch.rift_quests.network.Payloads;
import net.riftwatch.rift_quests.registry.ModAttachments;

public final class Cinematics {
    public static final String ENTRY = "rift_monarch";
    public static final String SHORT = "rift_monarch_short";
    public static final int ENTRY_TICKS = 180;
    public static final int SHORT_TICKS = 60;
    public static final int GRACE_TICKS = 30;

    record Session(String name, long endTick) {
    }

    private static final Map<UUID, Session> ACTIVE = new HashMap<>();

    private Cinematics() {
    }

    public static boolean canReceive(ServerPlayer player) {
        return player.connection.hasChannel(Payloads.Cinematic.TYPE);
    }

    public static void play(ServerPlayer player, String name, ArenaConfig config, double throneTop, boolean forceSkippable) {
        int length = ENTRY.equals(name) ? ENTRY_TICKS : SHORT_TICKS;
        boolean seen = player.getData(ModAttachments.CINEMATIC_SEEN);
        Vec3 landing = ENTRY.equals(name) ? new Vec3(player.getX(), config.centre().y, player.getZ()) : player.position();
        ACTIVE.put(player.getUUID(), new Session(name, player.server.getTickCount() + length + GRACE_TICKS));
        if (ENTRY.equals(name)) {
            player.setData(ModAttachments.CINEMATIC_SEEN, true);
        }
        PacketDistributor.sendToPlayer(player, new Payloads.Cinematic(name, config.centre().x, config.centre().y, config.centre().z,
                landing.x, landing.y, landing.z, throneTop, forceSkippable || seen));
    }

    public static boolean watching(ServerPlayer player) {
        Session session = ACTIVE.get(player.getUUID());
        if (session == null) {
            return false;
        }
        if (player.server.getTickCount() >= session.endTick()) {
            ACTIVE.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static void end(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
    }

    public static void endAll() {
        ACTIVE.clear();
    }
}
