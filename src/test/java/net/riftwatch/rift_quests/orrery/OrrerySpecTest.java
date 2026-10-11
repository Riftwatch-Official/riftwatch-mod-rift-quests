package net.riftwatch.rift_quests.orrery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
    void everyHitboxVolumeIsCoveredByAVisibleBox() {
        OrrerySpec spec = OrrerySpec.get();
        int[] anchor = spec.controller();
        List<OrrerySpec.Part> boxes = spec.parts().stream().filter(part -> part.type().equals("box")).toList();
        double epsilon = 1e-6;
        for (OrreryShapes.Cell cell : OrreryShapes.get().cells()) {
            for (AABB hitbox : OrreryShapes.get().shape(cell.shape()).toAabbs()) {
                double x = cell.x() + anchor[0] + (hitbox.minX + hitbox.maxX) / 2;
                double y = cell.y() + anchor[1] + (hitbox.minY + hitbox.maxY) / 2;
                double z = cell.z() + anchor[2] + (hitbox.minZ + hitbox.maxZ) / 2;
                assertTrue(boxes.stream().anyMatch(part -> Math.abs(x - part.center()[0]) <= part.size()[0] / 2 + epsilon
                        && Math.abs(y - part.center()[1]) <= part.size()[1] / 2 + epsilon
                        && Math.abs(z - part.center()[2]) <= part.size()[2] / 2 + epsilon),
                        "invisible hitbox at " + cell);
            }
        }
    }

    @Test
    void everyPillarIsFoundAtItsOwnSocketAndNowhereElse() {
        BlockPos controller = new BlockPos(100, 70, 100);
        BlockPos origin = OrreryStructure.origin(controller);
        List<OrrerySpec.Socket> sockets = OrrerySpec.get().sockets();
        for (int index = 0; index < sockets.size(); index++) {
            double[] centre = sockets.get(index).center();
            Vec3 tip = new Vec3(origin.getX() + centre[0], origin.getY() + centre[1], origin.getZ() + centre[2]);
            assertEquals(index, OrreryLogic.pillarAt(controller, tip));
        }
        double[] core = OrrerySpec.get().core();
        assertEquals(-1, OrreryLogic.pillarAt(controller, new Vec3(origin.getX() + core[0], origin.getY() + core[1], origin.getZ() + core[2])));
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
