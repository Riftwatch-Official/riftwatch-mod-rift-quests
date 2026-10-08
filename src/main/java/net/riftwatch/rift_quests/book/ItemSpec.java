package net.riftwatch.rift_quests.book;

import com.google.gson.JsonElement;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record ItemSpec(ResourceLocation item, Optional<JsonElement> components) {
}
