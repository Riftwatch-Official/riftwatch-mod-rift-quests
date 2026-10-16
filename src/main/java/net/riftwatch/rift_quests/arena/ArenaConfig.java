package net.riftwatch.rift_quests.arena;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;

public record ArenaConfig(ResourceKey<Level> dimension, AABB area, Vec3 centre, Vec3 landing, double throneTop, List<Vec3> returnBeams,
                          Vec3 returnTo, ResourceLocation boss, int cooldownSeconds, int resetSeconds, double healthPerExtraPlayer,
                          ResourceLocation victoryQuest, String victoryTask) {
    public static final String FILE = "rift_quests-arena.json";

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
    }

    public static ArenaConfig read(Path file) throws IOException {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        JsonObject arena = root.getAsJsonObject("arena");
        Vec3 from = vec(arena.getAsJsonArray("from"));
        Vec3 to = vec(arena.getAsJsonArray("to"));
        List<Vec3> beams = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("returnBeams")) {
            beams.add(vec(element.getAsJsonArray()));
        }
        return new ArenaConfig(
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(root.get("dimension").getAsString())),
                new AABB(from, to),
                vec(root.getAsJsonArray("centre")),
                vec(root.getAsJsonArray("landing")),
                root.get("throneTop").getAsDouble(),
                List.copyOf(beams),
                vec(root.getAsJsonArray("returnTo")),
                ResourceLocation.parse(root.get("boss").getAsString()),
                root.get("cooldownSeconds").getAsInt(),
                root.get("resetSeconds").getAsInt(),
                root.get("healthPerExtraPlayer").getAsDouble(),
                ResourceLocation.parse(root.get("victoryQuest").getAsString()),
                root.get("victoryTask").getAsString());
    }

    private static Vec3 vec(JsonArray array) {
        return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }
}
