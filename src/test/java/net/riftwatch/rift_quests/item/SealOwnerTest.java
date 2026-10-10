package net.riftwatch.rift_quests.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.serialization.JsonOps;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SealOwnerTest {
    @Test
    void codecRoundTrip() {
        SealOwner owner = new SealOwner(UUID.fromString("00000000-0000-0000-0000-000000000042"), "Traveller");
        var json = SealOwner.CODEC.encodeStart(JsonOps.INSTANCE, owner).getOrThrow();
        assertEquals(owner, SealOwner.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }
}
