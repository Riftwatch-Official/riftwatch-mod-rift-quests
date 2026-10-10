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

    public record Part(String type, String id, double[] center, double[] size, double[] rot, double radius, double height,
                       int segments, String material, boolean glow, String group, double width, double thickness,
                       double[] axis, double dps, int gems) {
    }

    public record Socket(String id, double[] center, int colour) {
    }

    private final int[] controller;
    private final int[] size;
    private final double[] core;
    private final List<Part> parts;
    private final List<Socket> sockets;

    private OrrerySpec(int[] controller, int[] size, double[] core, List<Part> parts, List<Socket> sockets) {
        this.controller = controller;
        this.size = size;
        this.core = core;
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
            sockets.add(new Socket(socket.get("id").getAsString(), doubles(socket.getAsJsonArray("center")), Integer.parseInt(colour.substring(1), 16)));
        }
        return new OrrerySpec(ints(root.getAsJsonArray("controller")), ints(root.getAsJsonArray("size")),
                doubles(root.getAsJsonArray("core")), List.copyOf(parts), List.copyOf(sockets));
    }

    private static Part part(JsonObject json) {
        JsonObject anim = json.has("anim") ? json.getAsJsonObject("anim") : null;
        return new Part(
                json.get("type").getAsString(),
                json.get("id").getAsString(),
                json.has("center") ? doubles(json.getAsJsonArray("center")) : new double[3],
                json.has("size") ? doubles(json.getAsJsonArray("size")) : new double[3],
                json.has("rot") ? doubles(json.getAsJsonArray("rot")) : new double[3],
                number(json, "radius"),
                number(json, "height"),
                json.has("segments") ? json.get("segments").getAsInt() : 0,
                json.get("material").getAsString(),
                json.has("glow") && json.get("glow").getAsBoolean(),
                json.get("group").getAsString(),
                number(json, "width"),
                number(json, "thickness"),
                anim == null ? new double[] {0, 1, 0} : doubles(anim.getAsJsonArray("axis")),
                anim == null ? 0 : anim.get("dps").getAsDouble(),
                json.has("gems") ? json.get("gems").getAsInt() : 0);
    }

    private static double number(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsDouble() : 0;
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
