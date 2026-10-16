package net.riftwatch.rift_quests.arena;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.riftwatch.rift_quests.book.Quest;
import net.riftwatch.rift_quests.engine.QuestEngine;
import net.riftwatch.rift_quests.engine.ServerBook;
import net.riftwatch.rift_quests.network.ClientBridge;
import net.riftwatch.rift_quests.network.Payloads;
import net.riftwatch.rift_quests.orrery.OrreryLogic;
import org.slf4j.Logger;

public final class Arena {
    public static final String BOSS_TAG = "rift_quests_arena_boss";
    public static final String PUPPET_TAG = "rift_quests_arena_puppet";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation HEALTH_SCALE = ResourceLocation.fromNamespaceAndPath("rift_quests", "arena_players");
    private static final int CHECK_INTERVAL = 10;
    private static final int DESCENT_START = 36;
    private static final int DESCENT_END = 80;
    private static final int RELEASE_TICK = 210;
    private static final int BEAM_HOLD_TICKS = 20;
    private static final int VICTORY_BEAM_TICKS = 6000;
    private static final double BEAM_RADIUS = 0.9;

    enum State { IDLE, INTRO, FIGHT, COOLDOWN }

    record Puppet(UUID entity, long endTick) {
    }

    private static volatile ArenaConfig config;
    private static State state = State.IDLE;
    private static UUID boss;
    private static long introStart;
    private static long lastEligible;
    private static long cooldownUntil;
    private static long victoryBeamUntil;
    private static boolean missingBossLogged;
    private static boolean bossKilled;
    private static final Set<UUID> SHOWN = new HashSet<>();
    private static final Set<UUID> DEBUG = new HashSet<>();
    private static final Set<UUID> DEBUG_PLAYED = new HashSet<>();
    private static final Map<UUID, Puppet> PUPPETS = new HashMap<>();
    private static final Map<UUID, Integer> BEAM_TIME = new HashMap<>();
    private static final Set<UUID> MUSIC = new HashSet<>();

    private Arena() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ServerStartedEvent.class, event -> load());
        bus.addListener(ServerStoppedEvent.class, event -> clear());
        bus.addListener(ServerTickEvent.Post.class, event -> tick(event.getServer()));
        bus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> forget(event.getEntity()));
        bus.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                Cinematics.end(player);
            }
        });
        bus.addListener(LivingDeathEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                Cinematics.end(player);
            } else if (event.getEntity().getUUID().equals(boss)) {
                bossKilled = true;
            }
        });
        bus.addListener(LivingIncomingDamageEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player && Cinematics.watching(player)) {
                event.setCanceled(true);
            }
        });
        bus.addListener(LivingChangeTargetEvent.class, event -> {
            if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer player && Cinematics.watching(player)) {
                event.setCanceled(true);
            }
        });
        bus.addListener(EntityJoinLevelEvent.class, event -> {
            Entity entity = event.getEntity();
            if (event.loadedFromDisk() && (entity.getTags().contains(BOSS_TAG) || entity.getTags().contains(PUPPET_TAG))
                    && !entity.getUUID().equals(boss) && !PUPPETS.containsKey(entity.getUUID())) {
                event.setCanceled(true);
            }
        });
    }

    public static int load() {
        Path file = ArenaConfig.path();
        if (!Files.isRegularFile(file)) {
            config = null;
            LOGGER.info("Arena: no {}, the final boss arena is off", ArenaConfig.FILE);
            return 0;
        }
        try {
            config = ArenaConfig.read(file);
        } catch (IOException | RuntimeException exception) {
            config = null;
            LOGGER.error("Arena: could not read {}", file, exception);
            return -1;
        }
        missingBossLogged = false;
        LOGGER.info("Arena: final boss arena at {} in {}", config.centre(), config.dimension().location());
        return 1;
    }

    public static ArenaConfig config() {
        return config;
    }

    public static boolean veilOpen(Player player) {
        if (player.level().isClientSide) {
            return ClientBridge.handler().orreryOpen() || ClientBridge.handler().veilPass();
        }
        return OrreryLogic.isOpen(player) || DEBUG.contains(player.getUUID());
    }

    public static boolean debugPass(ServerPlayer player) {
        return DEBUG.contains(player.getUUID());
    }

    public static void setDebug(ServerPlayer player, boolean on) {
        if (on) {
            DEBUG.add(player.getUUID());
        } else {
            DEBUG.remove(player.getUUID());
            DEBUG_PLAYED.remove(player.getUUID());
        }
        PacketDistributor.sendToPlayer(player, new Payloads.VeilPass(on));
    }

    public static boolean playDebug(ServerPlayer player) {
        ArenaConfig current = config;
        if (current == null || !inArena(player, current) || !Cinematics.canReceive(player)) {
            return false;
        }
        startDebug(player, current);
        return true;
    }

    public static void skipped(ServerPlayer player) {
        Cinematics.end(player);
    }

    public static String status() {
        ArenaConfig current = config;
        if (current == null) {
            return "Arena: off (no " + ArenaConfig.FILE + ")";
        }
        boolean registered = BuiltInRegistries.ENTITY_TYPE.containsKey(current.boss());
        return "Arena: " + state.name().toLowerCase() + ", boss " + current.boss() + (registered ? "" : " (not registered)")
                + ", " + DEBUG.size() + " debug passes, " + PUPPETS.size() + " puppets";
    }

    public static void reset(MinecraftServer server) {
        removeBoss(server);
        for (UUID id : new ArrayList<>(PUPPETS.keySet())) {
            discard(server, id);
        }
        PUPPETS.clear();
        state = State.IDLE;
        SHOWN.clear();
        cooldownUntil = 0;
    }

    private static void clear() {
        config = null;
        state = State.IDLE;
        boss = null;
        SHOWN.clear();
        DEBUG.clear();
        DEBUG_PLAYED.clear();
        PUPPETS.clear();
        BEAM_TIME.clear();
        MUSIC.clear();
        Cinematics.endAll();
    }

    private static void forget(Player player) {
        DEBUG.remove(player.getUUID());
        DEBUG_PLAYED.remove(player.getUUID());
        BEAM_TIME.remove(player.getUUID());
        MUSIC.remove(player.getUUID());
        SHOWN.remove(player.getUUID());
        if (player instanceof ServerPlayer serverPlayer) {
            Cinematics.end(serverPlayer);
        }
    }

    private static boolean inArena(ServerPlayer player, ArenaConfig current) {
        return player.level().dimension().equals(current.dimension()) && current.area().contains(player.position());
    }

    private static boolean landed(ServerPlayer player) {
        return player.onGround() && !player.isSpectator();
    }

    private static void tick(MinecraftServer server) {
        ArenaConfig current = config;
        if (current == null) {
            return;
        }
        ServerLevel level = server.getLevel(current.dimension());
        if (level == null) {
            return;
        }
        long now = server.getTickCount();
        tickPuppets(server, level, current, now);
        if (state == State.INTRO) {
            tween(level, current, now);
        }
        if (now % CHECK_INTERVAL != 0) {
            return;
        }
        List<ServerPlayer> eligible = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!inArena(player, current)) {
                DEBUG_PLAYED.remove(player.getUUID());
                continue;
            }
            beams(player, level, current, now);
            if (DEBUG.contains(player.getUUID())) {
                if (landed(player) && !DEBUG_PLAYED.contains(player.getUUID()) && !Cinematics.watching(player) && Cinematics.canReceive(player)) {
                    DEBUG_PLAYED.add(player.getUUID());
                    startDebug(player, current);
                }
                continue;
            }
            if (OrreryLogic.isOpen(player) && player.isAlive() && !player.isSpectator()) {
                eligible.add(player);
            }
        }
        if (!eligible.isEmpty()) {
            lastEligible = now;
        }
        showBeamParticles(level, current, now);
        switch (state) {
            case IDLE -> tryStart(server, level, current, eligible, now);
            case INTRO, FIGHT -> fight(server, level, current, eligible, now);
            case COOLDOWN -> {
                if (now >= cooldownUntil) {
                    state = State.IDLE;
                }
            }
        }
        music(server, level, current);
    }

    private static boolean hearsMusic(ServerPlayer player, ArenaConfig current) {
        if (!inArena(player, current) || player.isSpectator()) {
            return false;
        }
        if (DEBUG.contains(player.getUUID())) {
            return Cinematics.watching(player);
        }
        return state == State.INTRO || state == State.FIGHT;
    }

    private static void music(MinecraftServer server, ServerLevel level, ArenaConfig current) {
        Set<UUID> hearing = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (hearsMusic(player, current)) {
                hearing.add(player.getUUID());
                if (MUSIC.add(player.getUUID())) {
                    PacketDistributor.sendToPlayer(player, new Payloads.ArenaMusic(true));
                }
            }
        }
        for (UUID id : new ArrayList<>(MUSIC)) {
            if (hearing.contains(id)) {
                continue;
            }
            MUSIC.remove(id);
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                PacketDistributor.sendToPlayer(player, new Payloads.ArenaMusic(false));
            }
        }
    }

    private static void tryStart(MinecraftServer server, ServerLevel level, ArenaConfig current, List<ServerPlayer> eligible, long now) {
        List<ServerPlayer> ready = eligible.stream().filter(Arena::landed).toList();
        if (ready.isEmpty()) {
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(current.boss()).orElse(null);
        if (type == null) {
            if (!missingBossLogged) {
                LOGGER.warn("Arena: boss {} is not registered, the fight does not start", current.boss());
                missingBossLogged = true;
            }
            return;
        }
        Entity entity = spawn(level, type, current, BOSS_TAG);
        if (entity == null) {
            return;
        }
        boss = entity.getUUID();
        scaleHealth(entity, ready.size(), current);
        state = State.INTRO;
        introStart = now;
        SHOWN.clear();
        for (ServerPlayer player : ready) {
            if (Cinematics.canReceive(player)) {
                Cinematics.play(player, Cinematics.ENTRY, current, current.throneTop(), false);
            }
            SHOWN.add(player.getUUID());
        }
        LOGGER.info("Arena: the Rift Monarch arrives for {} players", ready.size());
    }

    private static void fight(MinecraftServer server, ServerLevel level, ArenaConfig current, List<ServerPlayer> eligible, long now) {
        if (bossKilled) {
            bossKilled = false;
            victory(level, current, eligible, now);
            return;
        }
        Entity entity = boss == null ? null : level.getEntity(boss);
        if (entity == null || !entity.isAlive()) {
            state = State.IDLE;
            boss = null;
            return;
        }
        if (state == State.INTRO && now - introStart >= RELEASE_TICK) {
            release(entity);
            state = State.FIGHT;
        }
        if (now - lastEligible > current.resetSeconds() * 20L) {
            LOGGER.info("Arena: no sealed player left, the Rift Monarch withdraws");
            removeBoss(server);
            state = State.IDLE;
            SHOWN.clear();
            return;
        }
        if (!current.area().contains(entity.position())) {
            entity.teleportTo(current.centre().x, current.centre().y, current.centre().z);
        }
        Set<UUID> present = new HashSet<>();
        for (ServerPlayer player : eligible) {
            present.add(player.getUUID());
        }
        SHOWN.retainAll(present);
        for (ServerPlayer player : eligible) {
            if (landed(player) && !SHOWN.contains(player.getUUID())) {
                SHOWN.add(player.getUUID());
                if (Cinematics.canReceive(player)) {
                    Cinematics.play(player, Cinematics.SHORT, current, current.centre().y, false);
                }
            }
        }
    }

    private static void victory(ServerLevel level, ArenaConfig current, List<ServerPlayer> eligible, long now) {
        LOGGER.info("Arena: the Rift Monarch has fallen, {} sealed players present", eligible.size());
        Quest quest = ServerBook.current().quest(current.victoryQuest());
        if (quest != null) {
            for (ServerPlayer player : eligible) {
                QuestEngine.addCustomProgress(player, quest, current.victoryTask(), 1);
            }
        }
        boss = null;
        state = State.COOLDOWN;
        bossKilled = false;
        cooldownUntil = now + current.cooldownSeconds() * 20L;
        victoryBeamUntil = now + VICTORY_BEAM_TICKS;
        SHOWN.clear();
    }

    private static Entity spawn(ServerLevel level, EntityType<?> type, ArenaConfig current, String tag) {
        Entity entity = type.create(level);
        if (entity == null) {
            return null;
        }
        Vec3 centre = current.centre();
        float yaw = facing(centre, current.landing());
        entity.moveTo(centre.x, current.throneTop(), centre.z, yaw, 0);
        entity.setYHeadRot(yaw);
        entity.addTag(tag);
        entity.setInvulnerable(true);
        entity.setNoGravity(true);
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(entity);
        return entity;
    }

    private static float facing(Vec3 from, Vec3 to) {
        return (float) (Math.atan2(to.z - from.z, to.x - from.x) * 180 / Math.PI) - 90F;
    }

    private static void scaleHealth(Entity entity, int players, ArenaConfig current) {
        if (!(entity instanceof LivingEntity living) || players <= 1) {
            return;
        }
        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.addPermanentModifier(new AttributeModifier(HEALTH_SCALE, current.healthPerExtraPlayer() * (players - 1), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            living.setHealth(living.getMaxHealth());
        }
    }

    private static double throneY(ArenaConfig current, double ticks) {
        double k = Math.max(0, Math.min(1, (ticks - DESCENT_START) / (double) (DESCENT_END - DESCENT_START)));
        double s = k * k * (3 - 2 * k);
        return current.throneTop() + (current.centre().y - current.throneTop()) * s;
    }

    private static void tween(ServerLevel level, ArenaConfig current, long now) {
        Entity entity = boss == null ? null : level.getEntity(boss);
        if (entity == null) {
            return;
        }
        place(entity, current, now - introStart);
    }

    private static void place(Entity entity, ArenaConfig current, long ticks) {
        Vec3 centre = current.centre();
        float yaw = facing(centre, current.landing());
        entity.moveTo(centre.x, throneY(current, ticks), centre.z, yaw, 0);
        entity.setYHeadRot(yaw);
        entity.setDeltaMovement(Vec3.ZERO);
    }

    private static void release(Entity entity) {
        entity.setInvulnerable(false);
        entity.setNoGravity(false);
        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
        }
    }

    private static void startDebug(ServerPlayer player, ArenaConfig current) {
        Cinematics.play(player, Cinematics.ENTRY, current, current.throneTop(), true);
        if (boss != null) {
            return;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(current.boss()).orElse(null);
        if (type == null) {
            return;
        }
        Entity puppet = spawn(player.serverLevel(), type, current, PUPPET_TAG);
        if (puppet != null) {
            PUPPETS.put(puppet.getUUID(), new Puppet(puppet.getUUID(), player.server.getTickCount() + Cinematics.ENTRY_TICKS));
        }
    }

    private static void tickPuppets(MinecraftServer server, ServerLevel level, ArenaConfig current, long now) {
        if (PUPPETS.isEmpty()) {
            return;
        }
        for (Puppet puppet : new ArrayList<>(PUPPETS.values())) {
            Entity entity = level.getEntity(puppet.entity());
            if (entity == null || now >= puppet.endTick()) {
                discard(server, puppet.entity());
                PUPPETS.remove(puppet.entity());
                continue;
            }
            place(entity, current, Cinematics.ENTRY_TICKS - (puppet.endTick() - now));
        }
    }

    private static void removeBoss(MinecraftServer server) {
        if (boss != null) {
            discard(server, boss);
        }
        boss = null;
    }

    private static void discard(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                entity.discard();
                return;
            }
        }
    }

    private static List<Vec3> activeBeams(ArenaConfig current, long now) {
        List<Vec3> beams = new ArrayList<>(current.returnBeams());
        if (now < victoryBeamUntil) {
            beams.add(current.centre());
        }
        return beams;
    }

    private static void beams(ServerPlayer player, ServerLevel level, ArenaConfig current, long now) {
        boolean inside = false;
        for (Vec3 beam : activeBeams(current, now)) {
            double dx = player.getX() - beam.x;
            double dz = player.getZ() - beam.z;
            double dy = player.getY() - beam.y;
            if (dx * dx + dz * dz <= BEAM_RADIUS * BEAM_RADIUS && dy > -0.5 && dy < 3) {
                inside = true;
            }
        }
        if (!inside || Cinematics.watching(player)) {
            BEAM_TIME.remove(player.getUUID());
            return;
        }
        int held = BEAM_TIME.merge(player.getUUID(), CHECK_INTERVAL, Integer::sum);
        if (held >= BEAM_HOLD_TICKS) {
            BEAM_TIME.remove(player.getUUID());
            Vec3 to = current.returnTo();
            player.teleportTo(level, to.x, to.y, to.z, player.getYRot(), player.getXRot());
            level.playSound(null, to.x, to.y, to.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.4F);
            DEBUG_PLAYED.remove(player.getUUID());
        }
    }

    private static void showBeamParticles(ServerLevel level, ArenaConfig current, long now) {
        for (Vec3 beam : activeBeams(current, now)) {
            boolean watched = false;
            for (ServerPlayer player : level.players()) {
                if (player.position().distanceToSqr(beam) < 64 * 64) {
                    watched = true;
                    break;
                }
            }
            if (watched) {
                level.sendParticles(ParticleTypes.END_ROD, beam.x, beam.y + 1.5, beam.z, 12, 0.15, 1.4, 0.15, 0.01);
            }
        }
    }
}
