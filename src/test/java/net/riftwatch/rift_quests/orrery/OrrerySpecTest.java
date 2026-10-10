package net.riftwatch.rift_quests.orrery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.riftwatch.rift_quests.registry.ModItems;
import org.junit.jupiter.api.Test;

class OrrerySpecTest {
    private static final Set<String> TYPES = Set.of("box", "voxels", "core", "beam");
    private static final Set<String> TINTS = Set.of("", "colour", "conduit", "level");

    private static void assertTexture(String material) {
        assertNotNull(OrrerySpecTest.class.getResource("/assets/rift_quests/textures/entity/orrery/" + material + ".png"), "texture " + material);
    }

    @Test
    void everyPartUsesAKnownTypeAndTint() {
        for (OrrerySpec.Part part : OrrerySpec.get().parts()) {
            assertTrue(TYPES.contains(part.type()), part.id());
            assertTrue(TINTS.contains(part.tint()), part.id());
        }
    }

    @Test
    void everyMaterialHasATexture() {
        for (OrrerySpec.Part part : OrrerySpec.get().parts()) {
            if (part.type().equals("box") || part.type().equals("core")) {
                assertTexture(part.material());
            }
            for (OrrerySpec.Cube cube : part.cubes()) {
                assertTexture(cube.material());
            }
        }
    }

    @Test
    void socketReferencesPointAtASeal() {
        OrrerySpec spec = OrrerySpec.get();
        for (OrrerySpec.Part part : spec.parts()) {
            if (!part.socket().isEmpty()) {
                assertTrue(spec.sockets().stream().anyMatch(socket -> socket.id().equals(part.socket())), part.id());
            }
            for (OrrerySpec.Cube cube : part.cubes()) {
                assertTrue(cube.socket() < ModItems.SEAL_DISTRICTS.size(), part.id());
                assertTrue(cube.socket() >= 0 || !cube.tint().equals("colour"), part.id());
            }
        }
    }

    @Test
    void tintedPartsHaveASocketWhereTheTintNeedsOne() {
        for (OrrerySpec.Part part : OrrerySpec.get().parts()) {
            if (part.tint().equals("colour") || part.tint().equals("conduit")) {
                assertTrue(!part.socket().isEmpty(), part.id());
            }
        }
    }

    @Test
    void coreAndBeamsSitInsideTheFootprint() {
        OrrerySpec spec = OrrerySpec.get();
        int[] size = spec.size();
        assertEquals(3, spec.core().length);
        for (int axis = 0; axis < 3; axis++) {
            assertTrue(spec.core()[axis] > 0 && spec.core()[axis] < size[axis]);
        }
        assertEquals(1, spec.parts().stream().filter(part -> part.type().equals("core")).count());
        assertEquals(2, spec.parts().stream().filter(part -> part.type().equals("beam")).count());
    }
}
