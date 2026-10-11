package net.riftwatch.rift_quests.client.title;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.riftwatch.rift_quests.RiftQuestsMod;

record TitleLayout(String name, float size, float x0, float x1, float w, float y, float top, float bottom, float h, float cy,
                   Map<String, Layer> layers, Layer sub, List<Letter> letters, List<Letter> glyphs, float[] points3, float[] points4,
                   float[] cuts, float[] tabletBox, float[] stroke) {

    record Layer(ResourceLocation id, float x0, float y0, float x1, float y1) {
    }

    record Letter(int index, float x, float w, float cx, float s0, float s1, int k, float u) {
        boolean glyph() {
            return k >= 0;
        }
    }

    Layer layer(String key) {
        Layer layer = layers.get(key);
        if (layer == null) {
            throw new IllegalStateException("District title " + name + " has no layer " + key);
        }
        return layer;
    }

    Letter letter(int index) {
        return letters.get(index);
    }

    static TitleLayout load(String name) throws IOException {
        ResourceLocation json = ResourceLocation.fromNamespaceAndPath(RiftQuestsMod.MOD_ID, "titles/" + name + ".json");
        JsonObject root;
        try (Reader reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(json).openAsReader()) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        float[] crop = floats(root.getAsJsonArray("crop"));
        Map<String, Layer> layers = new HashMap<>();
        for (JsonElement element : root.getAsJsonArray("layers")) {
            String key = element.getAsString();
            layers.put(key, new Layer(texture(name, key), crop[0], crop[1], crop[2], crop[3]));
        }
        float[] subRect = floats(root.getAsJsonArray("sub"));
        Layer sub = new Layer(texture(name, "sub"), subRect[0], subRect[1], subRect[2], subRect[3]);
        List<Letter> letters = new ArrayList<>();
        JsonArray letterArray = root.getAsJsonArray("letters");
        for (int index = 0; index < letterArray.size(); index++) {
            JsonObject letter = letterArray.get(index).getAsJsonObject();
            letters.add(new Letter(index, f(letter, "x"), f(letter, "w"), f(letter, "cx"), f(letter, "s0"), f(letter, "s1"),
                    letter.get("k").getAsInt(), f(letter, "u")));
        }
        return new TitleLayout(name, f(root, "size"), f(root, "x0"), f(root, "x1"), f(root, "w"), f(root, "y"), f(root, "top"),
                f(root, "bottom"), f(root, "h"), f(root, "cy"), Map.copyOf(layers), sub, List.copyOf(letters),
                letters.stream().filter(Letter::glyph).toList(), floats(root.getAsJsonArray("points3")), floats(root.getAsJsonArray("points4")),
                optional(root, "cuts"), optional(root, "tablet_box"), optional(root, "stroke"));
    }

    private static ResourceLocation texture(String name, String key) {
        return ResourceLocation.fromNamespaceAndPath(RiftQuestsMod.MOD_ID, "textures/title/" + name + "/" + key + ".png");
    }

    private static float f(JsonObject object, String key) {
        return object.get(key).getAsFloat();
    }

    private static float[] optional(JsonObject object, String key) {
        return object.has(key) ? floats(object.getAsJsonArray(key)) : new float[0];
    }

    private static float[] floats(JsonArray array) {
        float[] values = new float[array.size()];
        for (int index = 0; index < values.length; index++) {
            values[index] = array.get(index).getAsFloat();
        }
        return values;
    }
}
