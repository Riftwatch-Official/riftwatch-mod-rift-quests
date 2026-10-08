package net.riftwatch.rift_quests.book;

import com.google.gson.JsonElement;
import java.util.Optional;

public record Icon(IdRef item, Optional<JsonElement> components) {
}
