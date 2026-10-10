package net.riftwatch.rift_quests.orrery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.riftwatch.rift_quests.registry.ModItems;
import org.junit.jupiter.api.Test;

class OrreryShapesTest {
    @Test
    void controllerCellIsPartOfTheFootprint() {
        assertTrue(OrreryShapes.get().shapeAt(0, 0, 0) >= 0);
    }

    @Test
    void everyCellFitsTheDeclaredSize() {
        OrrerySpec spec = OrrerySpec.get();
        int[] anchor = spec.controller();
        int[] size = spec.size();
        for (OrreryShapes.Cell cell : OrreryShapes.get().cells()) {
            assertTrue(cell.x() + anchor[0] >= 0 && cell.x() + anchor[0] < size[0], "x " + cell.x());
            assertTrue(cell.y() + anchor[1] >= 0 && cell.y() + anchor[1] < size[1], "y " + cell.y());
            assertTrue(cell.z() + anchor[2] >= 0 && cell.z() + anchor[2] < size[2], "z " + cell.z());
            assertTrue(cell.shape() >= 0 && cell.shape() < OrreryShapes.get().shapeCount());
        }
    }

    @Test
    void offsetLookupMatchesTheCellList() {
        OrreryShapes shapes = OrreryShapes.get();
        for (OrreryShapes.Cell cell : shapes.cells()) {
            assertEquals(cell.shape(), shapes.shapeAt(cell.x(), cell.y(), cell.z()));
            assertTrue(shapes.cellsOfShape(cell.shape()).contains(cell));
        }
        assertEquals(-1, shapes.shapeAt(40, 0, 0));
    }

    @Test
    void everyShapeHasVolume() {
        OrreryShapes shapes = OrreryShapes.get();
        for (int shape = 0; shape < shapes.shapeCount(); shape++) {
            assertFalse(shapes.shape(shape).isEmpty(), "shape " + shape);
        }
    }

    @Test
    void socketsFollowTheSealOrder() {
        OrrerySpec spec = OrrerySpec.get();
        assertEquals(ModItems.SEAL_DISTRICTS.size(), spec.sockets().size());
        for (int index = 0; index < spec.sockets().size(); index++) {
            assertEquals(ModItems.SEAL_DISTRICTS.get(index), spec.sockets().get(index).id());
        }
    }
}
