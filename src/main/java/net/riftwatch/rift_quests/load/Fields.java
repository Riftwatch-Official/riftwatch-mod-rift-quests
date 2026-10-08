package net.riftwatch.rift_quests.load;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.riftwatch.rift_quests.book.IdRef;
import net.riftwatch.rift_quests.book.Text;

public final class Fields {
    private final JsonObject object;
    private final String file;
    private final String path;
    private final Problems problems;
    private final Set<String> used = new HashSet<>();

    private Fields(JsonObject object, String file, String path, Problems problems) {
        this.object = object;
        this.file = file;
        this.path = path;
        this.problems = problems;
    }

    public static Optional<Fields> of(JsonElement element, String file, String path, Problems problems) {
        if (element == null || !element.isJsonObject()) {
            problems.error(file, path, "must be a JSON object");
            return Optional.empty();
        }
        return Optional.of(new Fields(element.getAsJsonObject(), file, path, problems));
    }

    public String file() {
        return file;
    }

    public String path() {
        return path;
    }

    public Problems problems() {
        return problems;
    }

    public String at(String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    public boolean has(String key) {
        return object.has(key);
    }

    public void error(String key, String message) {
        problems.error(file, key == null ? path : at(key), message);
    }

    public Optional<JsonElement> raw(String key, boolean required) {
        used.add(key);
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            if (required) {
                error(key, "is required");
            }
            return Optional.empty();
        }
        return Optional.of(element);
    }

    public Optional<String> string(String key, boolean required) {
        Optional<JsonElement> element = raw(key, required);
        if (element.isEmpty()) {
            return Optional.empty();
        }
        if (!(element.get() instanceof JsonPrimitive primitive) || !primitive.isString()) {
            error(key, "must be a string");
            return Optional.empty();
        }
        String value = primitive.getAsString();
        if (value.isBlank()) {
            error(key, "must not be empty");
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public Optional<Integer> integer(String key, boolean required, int min, int max) {
        Optional<JsonElement> element = raw(key, required);
        if (element.isEmpty()) {
            return Optional.empty();
        }
        return asInteger(element.get(), at(key), min, max);
    }

    public Optional<Integer> asInteger(JsonElement element, String field, int min, int max) {
        if (!(element instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
            problems.error(file, field, "must be a whole number");
            return Optional.empty();
        }
        BigDecimal value = primitive.getAsBigDecimal();
        if (value.stripTrailingZeros().scale() > 0) {
            problems.error(file, field, "must be a whole number");
            return Optional.empty();
        }
        if (value.compareTo(BigDecimal.valueOf(min)) < 0 || value.compareTo(BigDecimal.valueOf(max)) > 0) {
            problems.error(file, field, "must be between " + min + " and " + max);
            return Optional.empty();
        }
        return Optional.of(value.intValueExact());
    }

    public Optional<Boolean> bool(String key) {
        Optional<JsonElement> element = raw(key, false);
        if (element.isEmpty()) {
            return Optional.empty();
        }
        if (!(element.get() instanceof JsonPrimitive primitive) || !primitive.isBoolean()) {
            error(key, "must be true or false");
            return Optional.empty();
        }
        return Optional.of(primitive.getAsBoolean());
    }

    public <E extends Enum<E>> Optional<E> enumValue(String key, Class<E> type, boolean required) {
        Optional<String> value = string(key, required);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        for (E constant : type.getEnumConstants()) {
            if (constant.name().toLowerCase(Locale.ROOT).equals(value.get())) {
                return Optional.of(constant);
            }
        }
        error(key, "must be one of " + names(type) + ", not \"" + value.get() + "\"");
        return Optional.empty();
    }

    public Optional<JsonArray> array(String key, boolean required) {
        Optional<JsonElement> element = raw(key, required);
        if (element.isEmpty()) {
            return Optional.empty();
        }
        if (!element.get().isJsonArray()) {
            error(key, "must be a list");
            return Optional.empty();
        }
        return Optional.of(element.get().getAsJsonArray());
    }

    public Optional<JsonObject> objectValue(String key, boolean required) {
        Optional<JsonElement> element = raw(key, required);
        if (element.isEmpty()) {
            return Optional.empty();
        }
        if (!element.get().isJsonObject()) {
            error(key, "must be a JSON object");
            return Optional.empty();
        }
        return Optional.of(element.get().getAsJsonObject());
    }

    public Optional<Fields> object(String key, boolean required) {
        return objectValue(key, required).map(value -> new Fields(value, file, at(key), problems));
    }

    public Optional<Text> text(String key, boolean required) {
        Optional<JsonElement> element = raw(key, required);
        if (element.isEmpty()) {
            return Optional.empty();
        }
        JsonElement value = element.get();
        if (value instanceof JsonPrimitive primitive && primitive.isString()) {
            if (primitive.getAsString().isBlank()) {
                error(key, "must not be empty");
                return Optional.empty();
            }
            return Optional.of(new Text(value));
        }
        if (value.isJsonObject() || value.isJsonArray()) {
            return Optional.of(new Text(value));
        }
        error(key, "must be a string or a text component");
        return Optional.empty();
    }

    public Optional<ResourceLocation> id(String key, boolean required) {
        return string(key, required).flatMap(value -> parseId(value, at(key)));
    }

    public Optional<ResourceLocation> parseId(String value, String field) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            problems.error(file, field, "\"" + value + "\" is not a valid resource location");
            return Optional.empty();
        }
        return Optional.of(id);
    }

    public Optional<IdRef> parseRef(JsonElement element, String field, boolean tagAllowed) {
        if (!(element instanceof JsonPrimitive primitive) || !primitive.isString()) {
            problems.error(file, field, "must be a string id");
            return Optional.empty();
        }
        String value = primitive.getAsString();
        boolean tag = value.startsWith("#");
        if (tag && !tagAllowed) {
            problems.error(file, field, "a tag is not allowed here");
            return Optional.empty();
        }
        return parseId(tag ? value.substring(1) : value, field).map(id -> new IdRef(id, tag));
    }

    public Optional<IdRef> ref(String key, boolean required, boolean tagAllowed) {
        Optional<JsonElement> element = raw(key, required);
        return element.flatMap(value -> parseRef(value, at(key), tagAllowed));
    }

    public Optional<List<IdRef>> refs(String singular, String plural, boolean required, boolean tagAllowed) {
        boolean hasSingular = has(singular);
        boolean hasPlural = has(plural);
        if (hasSingular && hasPlural) {
            used.add(singular);
            used.add(plural);
            error(null, "use either \"" + singular + "\" or \"" + plural + "\", not both");
            return Optional.empty();
        }
        if (hasSingular) {
            return ref(singular, true, tagAllowed).map(List::of);
        }
        if (!hasPlural) {
            if (required) {
                error(null, "needs \"" + singular + "\" or \"" + plural + "\"");
            }
            return Optional.empty();
        }
        Optional<JsonArray> array = array(plural, true);
        if (array.isEmpty()) {
            return Optional.empty();
        }
        if (array.get().isEmpty()) {
            error(plural, "must not be empty");
            return Optional.empty();
        }
        List<IdRef> result = new ArrayList<>();
        boolean valid = true;
        for (int index = 0; index < array.get().size(); index++) {
            String field = at(plural) + "[" + index + "]";
            Optional<IdRef> ref = parseRef(array.get().get(index), field, tagAllowed);
            if (ref.isEmpty()) {
                valid = false;
            } else if (result.contains(ref.get())) {
                problems.error(file, field, ref.get() + " is listed twice");
                valid = false;
            } else {
                result.add(ref.get());
            }
        }
        return valid ? Optional.of(List.copyOf(result)) : Optional.empty();
    }

    public Optional<List<ResourceLocation>> ids(String singular, String plural, boolean required) {
        return refs(singular, plural, required, false).map(list -> list.stream().map(IdRef::id).toList());
    }

    public Optional<List<String>> strings(String key) {
        Optional<JsonArray> array = array(key, false);
        if (array.isEmpty()) {
            return Optional.of(List.of());
        }
        List<String> result = new ArrayList<>();
        for (int index = 0; index < array.get().size(); index++) {
            JsonElement element = array.get().get(index);
            if (!(element instanceof JsonPrimitive primitive) || !primitive.isString() || primitive.getAsString().isBlank()) {
                problems.error(file, at(key) + "[" + index + "]", "must be a non-empty string");
                return Optional.empty();
            }
            result.add(primitive.getAsString());
        }
        return Optional.of(List.copyOf(result));
    }

    public void finish() {
        for (String key : object.keySet()) {
            if (!used.contains(key)) {
                error(key, "unknown field");
            }
        }
    }

    private static <E extends Enum<E>> String names(Class<E> type) {
        List<String> names = new ArrayList<>();
        for (E constant : type.getEnumConstants()) {
            names.add(constant.name().toLowerCase(Locale.ROOT));
        }
        return String.join(", ", names);
    }
}
