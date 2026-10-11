package net.riftwatch.rift_quests.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.riftwatch.rift_quests.orrery.OrreryLogic;
import net.riftwatch.rift_quests.orrery.OrreryPillars;
import org.junit.jupiter.api.Test;

class OrreryMaskTest {
    @Test
    void anySealFitsAnyPillar() {
        PlayerQuestData data = new PlayerQuestData();
        assertFalse(data.hasOrrerySeal(3));
        data.insertOrrerySeal(3, 7);
        assertTrue(data.hasOrrerySeal(3));
        assertFalse(data.hasOrrerySeal(7));
        assertEquals(3, data.orreryPillarSeal(7));
        assertEquals(OrreryPillars.EMPTY, data.orreryPillarSeal(3));
        assertEquals(8, data.orreryMask());
    }

    @Test
    void everyArrangementSurvivesSaving() {
        PlayerQuestData data = new PlayerQuestData();
        for (int seal = 0; seal < OrreryLogic.SEAL_COUNT; seal++) {
            data.insertOrrerySeal(seal, OrreryLogic.SEAL_COUNT - 1 - seal);
        }
        CompoundTag saved = data.serializeNBT(null);
        PlayerQuestData loaded = new PlayerQuestData();
        loaded.deserializeNBT(null, saved);
        assertEquals(OrreryLogic.FULL, loaded.orreryMask());
        for (int pillar = 0; pillar < OrreryLogic.SEAL_COUNT; pillar++) {
            assertEquals(OrreryLogic.SEAL_COUNT - 1 - pillar, loaded.orreryPillarSeal(pillar));
        }
    }

    @Test
    void olderSavesKeepEachSealOnItsOwnPillar() {
        CompoundTag old = new PlayerQuestData().serializeNBT(null);
        old.remove("orrery_pillars");
        old.putInt("orrery_seals", 0b100101);
        PlayerQuestData loaded = new PlayerQuestData();
        loaded.deserializeNBT(null, old);
        assertEquals(0b100101, loaded.orreryMask());
        assertEquals(0, loaded.orreryPillarSeal(0));
        assertEquals(2, loaded.orreryPillarSeal(2));
        assertEquals(5, loaded.orreryPillarSeal(5));
        assertEquals(OrreryPillars.EMPTY, loaded.orreryPillarSeal(1));
    }

    @Test
    void questResetKeepsInsertedSeals() {
        PlayerQuestData data = new PlayerQuestData();
        data.insertOrrerySeal(0, 4);
        data.resetAll();
        assertEquals(1, data.orreryMask());
        data.clearOrrery();
        assertEquals(0, data.orreryMask());
    }
}
