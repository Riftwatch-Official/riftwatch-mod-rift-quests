package net.riftwatch.rift_quests.orrery;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class OrrerySpec {
    private static final String RESOURCE = "/assets/rift_quests/orrery/orrery.json";
    private static final OrrerySpec INSTANCE = load();

    public record Cube(double[] center, double[] size, String material, int socket, double yaw, String tint) {
    }

    public record Part(String type, String id, double[] center, double[] size, double[] rot, String material, String group,
                       String socket, String tint, double[] axis, double dps, List<Cube> cubes) {
    }

    public record Socket(String id, double[] center, int colour, double yaw) {
    }

    private final int[] controller;
    private final int[] size;
    private final double[] core;
    private final double scale;
    private final List<Part> parts;
    private final List<Socket> sockets;

    private OrrerySpec(int[] controller, int[] size, double[] core, double scale, List<Part> parts, List<Socket> sockets) {
        this.controller = controller;
        this.size = size;
        this.core = core;
        this.scale = scale;
        this.parts = parts;
        this.sockets = sockets;
    }

    public static OrrerySpec get() {
        return INSTANCE;
    }

    public int[] controller() {
        return controller;
    }

    public int[] size() {
        return size;
    }

    public double[] core() {
        return core;
    }

    public double scale() {
        return scale;
    }

    public List<Part> parts() {
        return parts;
    }

    public List<Socket> sockets() {
        return sockets;
    }

    private static OrrerySpec load() {
        try (InputStream stream = OrrerySpec.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource " + RESOURCE);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static OrrerySpec parse(JsonObject root) {
        List<Part> parts = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("parts")) {
            parts.add(part(element.getAsJsonObject()));
        }
        JsonArray districts = root.getAsJsonArray("districts");
        List<Socket> sockets = new ArrayList<>();
        JsonArray socketArray = root.getAsJsonArray("sockets");
        for (int index = 0; index < socketArray.size(); index++) {
            JsonObject socket = socketArray.get(index).getAsJsonObject();
            String colour = districts.get(index).getAsJsonObject().get("colour").getAsString();
            sockets.add(new Socket(socket.get("id").getAsString(), doubles(socket.getAsJsonArray("center")), Integer.parseInt(colour.substring(1), 16), socket.get("yaw").getAsDouble()));
        }
        return new OrrerySpec(ints(root.getAsJsonArray("controller")), ints(root.getAsJsonArray("size")),
                doubles(root.getAsJsonArray("core")), root.get("scale").getAsDouble(), List.copyOf(parts), List.copyOf(sockets));
    }

    private static Part part(JsonObject json) {
        JsonObject anim = json.has("anim") ? json.getAsJsonObject("anim") : null;
        List<Cube> cubes = new ArrayList<>();
        if (json.has("cubes")) {
            for (JsonElement element : json.getAsJsonArray("cubes")) {
                cubes.add(cube(element.getAsJsonObject()));
            }
        }
        return new Part(
                json.get("type").getAsString(),
                json.get("id").getAsString(),
                json.has("center") ? doubles(json.getAsJsonArray("center")) : new double[3],
                json.has("size") ? doubles(json.getAsJsonArray("size")) : new double[3],
                json.has("rot") ? doubles(json.getAsJsonArray("rot")) : new double[3],
                text(json, "material"),
                json.get("group").getAsString(),
                text(json, "socket"),
                text(json, "tint"),
                anim == null ? new double[] {0, 1, 0} : doubles(anim.getAsJsonArray("axis")),
                anim == null ? 0 : anim.get("dps").getAsDouble(),
                List.copyOf(cubes));
    }

    private static Cube cube(JsonObject json) {
        return new Cube(
                doubles(json.getAsJsonArray("center")),
                doubles(json.getAsJsonArray("size")),
                json.get("material").getAsString(),
                json.has("socket") ? json.get("socket").getAsInt() : -1,
                json.has("yaw") ? json.get("yaw").getAsDouble() : 0,
                text(json, "tint"));
    }

    private static String text(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : "";
    }

    private static double[] doubles(JsonArray array) {
        double[] values = new double[array.size()];
        for (int index = 0; index < values.length; index++) {
            values[index] = array.get(index).getAsDouble();
        }
        return values;
    }

    private static int[] ints(JsonArray array) {
        int[] values = new int[array.size()];
        for (int index = 0; index < values.length; index++) {
            values[index] = array.get(index).getAsInt();
        }
        return values;
    }
}
