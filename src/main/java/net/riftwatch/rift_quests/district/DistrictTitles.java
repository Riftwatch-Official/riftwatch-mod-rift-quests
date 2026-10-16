package net.riftwatch.rift_quests.district;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.riftwatch.rift_quests.network.Payloads;
import org.slf4j.Logger;

public final class DistrictTitles {
    public static final List<String> NAMES = List.of("castle_city", "desert_citadel", "ice_settlement", "jungle_city", "jungle_temple", "monastery",
            "pyramid_row", "river_oasis", "river_palace", "sky_fortress", "rift_monarch");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE = "rift_guard-districts.json";
    private static final int INTERVAL = 10;
    private static final double RELEASE_DISTANCE = 70;

    record Hull(String name, ResourceKey<Level> dimension, int x0, int z0, int x1, int z1) {
        boolean contains(double x, double z) {
            return x >= x0 && x < x1 + 1 && z >= z0 && z < z1 + 1;
        }

        double distance(double x, double z) {
            double dx = Math.max(0, Math.max(x0 - x, x - (x1 + 1)));
            double dz = Math.max(0, Math.max(z0 - z, z - (z1 + 1)));
            return Math.sqrt(dx * dx + dz * dz);
        }

        long area() {
            return (long) (x1 - x0 + 1) * (z1 - z0 + 1);
        }
    }

    private static volatile List<Hull> hulls = List.of();
    private static final Map<UUID, String> LAST = new HashMap<>();

    private DistrictTitles() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ServerStartedEvent.class, event -> load());
        bus.addListener(ServerStoppedEvent.class, event -> LAST.clear());
        bus.addListener(ServerTickEvent.Post.class, event -> tick(event.getServer()));
        bus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> LAST.remove(event.getEntity().getUUID()));
    }

    public static int load() {
        Path file = FMLPaths.CONFIGDIR.get().resolve(FILE);
        if (!Files.isRegularFile(file)) {
            hulls = List.of();
            LOGGER.info("District titles: no {}, no districts", FILE);
            return 0;
        }
        List<Hull> loaded = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("districts")) {
                JsonObject district = element.getAsJsonObject();
                String name = district.get("name").getAsString();
                if (!NAMES.contains(name)) {
                    continue;
                }
                JsonArray from = district.getAsJsonArray("from");
                JsonArray to = district.getAsJsonArray("to");
                int minX = Math.min(from.get(0).getAsInt(), to.get(0).getAsInt());
                int maxX = Math.max(from.get(0).getAsInt(), to.get(0).getAsInt());
                int minZ = Math.min(from.get(2).getAsInt(), to.get(2).getAsInt());
                int maxZ = Math.max(from.get(2).getAsInt(), to.get(2).getAsInt());
                ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(district.get("dimension").getAsString()));
                loaded.add(new Hull(name, dimension, (minX >> 4) << 4, (minZ >> 4) << 4, ((maxX >> 4) << 4) + 15, ((maxZ >> 4) << 4) + 15));
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("District titles: could not read {}", file, exception);
            return -1;
        }
        hulls = List.copyOf(loaded);
        LOGGER.info("District titles: {} districts", loaded.size());
        return loaded.size();
    }

    public static boolean send(ServerPlayer player, String district) {
        if (!player.connection.hasChannel(Payloads.DistrictTitle.TYPE)) {
            return false;
        }
        PacketDistributor.sendToPlayer(player, new Payloads.DistrictTitle(district));
        return true;
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % INTERVAL != 0) {
            return;
        }
        List<Hull> current = hulls;
        if (current.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            check(player, current);
        }
    }

    private static void check(ServerPlayer player, Collection<Hull> current) {
        ResourceKey<Level> dimension = player.level().dimension();
        double x = player.getX();
        double z = player.getZ();
        UUID id = player.getUUID();
        String last = LAST.get(id);
        if (last != null && !near(current, last, dimension, x, z)) {
            LAST.remove(id);
            last = null;
        }
        Hull inside = null;
        for (Hull hull : current) {
            if (hull.dimension().equals(dimension) && hull.contains(x, z) && (inside == null || hull.area() < inside.area())) {
                inside = hull;
            }
        }
        if (inside == null || inside.name().equals(last)) {
            return;
        }
        LAST.put(id, inside.name());
        send(player, inside.name());
    }

    private static boolean near(Collection<Hull> current, String name, ResourceKey<Level> dimension, double x, double z) {
        for (Hull hull : current) {
            if (hull.name().equals(name) && hull.dimension().equals(dimension) && hull.distance(x, z) <= RELEASE_DISTANCE) {
                return true;
            }
        }
        return false;
    }
}
