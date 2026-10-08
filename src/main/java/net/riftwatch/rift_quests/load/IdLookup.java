package net.riftwatch.rift_quests.load;

import com.google.gson.JsonElement;
import java.util.Optional;
import net.riftwatch.rift_quests.book.IdRef;

public interface IdLookup {
    enum Status {
        OK,
        MISSING,
        EMPTY_TAG
    }

    Status status(IdKind kind, IdRef ref);

    Optional<String> textError(JsonElement text);

    Optional<String> componentsError(JsonElement components);

    Optional<String> nbtError(String nbt);
}
