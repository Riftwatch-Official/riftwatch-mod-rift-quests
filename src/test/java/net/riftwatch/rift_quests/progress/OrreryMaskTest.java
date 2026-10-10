package net.riftwatch.rift_quests.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.riftwatch.rift_quests.orrery.OrreryLogic;
import org.junit.jupiter.api.Test;

class OrreryMaskTest {
    @Test
    void sealsAreTrackedPerBit() {
        PlayerQuestData data = new PlayerQuestData();
        assertFalse(data.hasOrrerySeal(3));
        data.insertOrrerySeal(3);
        assertTrue(data.hasOrrerySeal(3));
        assertFalse(data.hasOrrerySeal(4));
        assertEquals(8, data.orreryMask());
    }

    @Test
    void maskSurvivesSaving() {
        PlayerQuestData data = new PlayerQuestData();
        for (int bit = 0; bit < OrreryLogic.SEAL_COUNT; bit++) {
            data.insertOrrerySeal(bit);
        }
        CompoundTag saved = data.serializeNBT(null);
        PlayerQuestData loaded = new PlayerQuestData();
        loaded.deserializeNBT(null, saved);
        assertEquals(OrreryLogic.FULL, loaded.orreryMask());
    }

    @Test
    void questResetKeepsInsertedSeals() {
        PlayerQuestData data = new PlayerQuestData();
        data.insertOrrerySeal(0);
        data.resetAll();
        assertEquals(1, data.orreryMask());
        data.clearOrrery();
        assertEquals(0, data.orreryMask());
    }
}
