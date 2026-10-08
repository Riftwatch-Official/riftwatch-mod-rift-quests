package net.riftwatch.rift_quests.load;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class QuestBookLoader extends SimplePreparableReloadListener<QuestBookLoader.Files> {
    private static final FileToIdConverter FILES = FileToIdConverter.json(QuestBookParser.DIRECTORY);
    private static final TypeAdapter<JsonElement> JSON = new Gson().getAdapter(JsonElement.class);

    public record Files(Map<ResourceLocation, JsonElement> json, List<Problem> problems) {
        public static final Files EMPTY = new Files(Map.of(), List.of());
    }

    @Override
    protected Files prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> json = new LinkedHashMap<>();
        List<Problem> problems = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Resource> entry : FILES.listMatchingResources(manager).entrySet()) {
            ResourceLocation fileId = FILES.fileToId(entry.getKey());
            String file = QuestBookParser.displayPath(fileId) + " (pack " + entry.getValue().sourcePackId() + ")";
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonReader jsonReader = new JsonReader(reader);
                JsonElement element = JSON.read(jsonReader);
                if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                    problems.add(new Problem(Problem.Severity.ERROR, file, "", "unexpected content after the JSON value"));
                    continue;
                }
                json.put(fileId, element);
            } catch (IOException | RuntimeException exception) {
                problems.add(new Problem(Problem.Severity.ERROR, file, "", "invalid JSON: " + exception.getMessage()));
            }
        }
        return new Files(Map.copyOf(json), List.copyOf(problems));
    }

    @Override
    protected void apply(Files files, ResourceManager manager, ProfilerFiller profiler) {
        QuestBooks.stage(files);
    }
}
