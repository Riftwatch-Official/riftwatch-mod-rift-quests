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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OrreryShapes {
    private static final String RESOURCE = "/assets/rift_quests/orrery/cells.json";
    private static final int OFFSET_BIAS = 64;
    private static final OrreryShapes INSTANCE = load();

    public record Cell(int x, int y, int z, int shape) {
    }

    private final List<VoxelShape> shapes;
    private final List<Cell> cells;
    private final Map<Long, Integer> shapeByOffset = new HashMap<>();
    private final List<List<Cell>> cellsByShape;

    private OrreryShapes(List<VoxelShape> shapes, List<Cell> cells) {
        this.shapes = shapes;
        this.cells = cells;
        List<List<Cell>> grouped = new ArrayList<>();
        for (int index = 0; index < shapes.size(); index++) {
            grouped.add(new ArrayList<>());
        }
        for (Cell cell : cells) {
            shapeByOffset.put(key(cell.x(), cell.y(), cell.z()), cell.shape());
            grouped.get(cell.shape()).add(cell);
        }
        this.cellsByShape = grouped.stream().map(List::copyOf).toList();
    }

    public static OrreryShapes get() {
        return INSTANCE;
    }

    public int shapeCount() {
        return shapes.size();
    }

    public VoxelShape shape(int shape) {
        return shapes.get(Math.max(0, Math.min(shape, shapes.size() - 1)));
    }

    public List<Cell> cells() {
        return cells;
    }

    public List<Cell> cellsOfShape(int shape) {
        return cellsByShape.get(shape);
    }

    public int shapeAt(int x, int y, int z) {
        return shapeByOffset.getOrDefault(key(x, y, z), -1);
    }

    private static long key(int x, int y, int z) {
        return ((long) (x + OFFSET_BIAS) << 32) | ((long) (y + OFFSET_BIAS) << 16) | (z + OFFSET_BIAS);
    }

    private static OrreryShapes load() {
        try (InputStream stream = OrreryShapes.class.getResourceAsStream(RESOURCE)) {
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

    private static OrreryShapes parse(JsonObject root) {
        double resolution = root.get("resolution").getAsInt();
        List<VoxelShape> shapes = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("classes")) {
            VoxelShape shape = Shapes.empty();
            for (JsonElement boxElement : element.getAsJsonArray()) {
                JsonArray box = boxElement.getAsJsonArray();
                shape = Shapes.or(shape, Shapes.box(
                        box.get(0).getAsInt() / resolution, box.get(1).getAsInt() / resolution, box.get(2).getAsInt() / resolution,
                        box.get(3).getAsInt() / resolution, box.get(4).getAsInt() / resolution, box.get(5).getAsInt() / resolution));
            }
            shapes.add(shape.optimize());
        }
        List<Cell> cells = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("cells")) {
            JsonArray cell = element.getAsJsonArray();
            cells.add(new Cell(cell.get(0).getAsInt(), cell.get(1).getAsInt(), cell.get(2).getAsInt(), cell.get(3).getAsInt()));
        }
        return new OrreryShapes(List.copyOf(shapes), List.copyOf(cells));
    }
}
