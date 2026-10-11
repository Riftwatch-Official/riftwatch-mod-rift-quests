package net.riftwatch.rift_quests.orrery;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.riftwatch.rift_quests.block.SealOrreryPartBlock;
import net.riftwatch.rift_quests.registry.ModBlocks;

public final class OrreryStructure {
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private OrreryStructure() {
    }

    public static int[] size() {
        return OrrerySpec.get().size();
    }

    public static BlockPos origin(BlockPos controller) {
        int[] anchor = OrrerySpec.get().controller();
        return controller.offset(-anchor[0], -anchor[1], -anchor[2]);
    }

    public static boolean canPlace(Level level, BlockPos controller) {
        for (OrreryShapes.Cell cell : OrreryShapes.get().cells()) {
            BlockPos pos = controller.offset(cell.x(), cell.y(), cell.z());
            if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) {
                return false;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.canBeReplaced() && !state.is(ModBlocks.SEAL_ORRERY_PART.get())) {
                return false;
            }
        }
        return true;
    }

    public static void build(Level level, BlockPos controller) {
        BlockState part = ModBlocks.SEAL_ORRERY_PART.get().defaultBlockState();
        for (OrreryShapes.Cell cell : OrreryShapes.get().cells()) {
            if (cell.x() == 0 && cell.y() == 0 && cell.z() == 0) {
                continue;
            }
            BlockPos pos = controller.offset(cell.x(), cell.y(), cell.z());
            BlockState current = level.getBlockState(pos);
            if (current.canBeReplaced() || current.is(ModBlocks.SEAL_ORRERY_PART.get())) {
                level.setBlock(pos, part.setValue(SealOrreryPartBlock.SHAPE, cell.shape()), PLACE_FLAGS);
            }
        }
    }

    public static void removeParts(Level level, BlockPos controller) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (OrreryShapes.Cell cell : OrreryShapes.get().cells()) {
            BlockPos pos = controller.offset(cell.x(), cell.y(), cell.z());
            BlockState current = level.getBlockState(pos);
            if (current.is(ModBlocks.SEAL_ORRERY_PART.get()) && current.getValue(SealOrreryPartBlock.SHAPE) == cell.shape()) {
                level.setBlock(pos, air, PLACE_FLAGS);
            }
        }
    }

    public static boolean isOrphan(Level level, BlockPos part, int shape) {
        for (OrreryShapes.Cell cell : OrreryShapes.get().cellsOfShape(shape)) {
            BlockPos candidate = part.offset(-cell.x(), -cell.y(), -cell.z());
            if (!level.isLoaded(candidate) || level.getBlockState(candidate).is(ModBlocks.SEAL_ORRERY.get())) {
                return false;
            }
        }
        return true;
    }

    public static void removeStrays(Level level, BlockPos controller) {
        if (!level.isLoaded(controller) || !level.getBlockState(controller).is(ModBlocks.SEAL_ORRERY.get())) {
            return;
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos origin = origin(controller);
        int[] size = size();
        for (int x = 0; x < size[0]; x++) {
            for (int y = 0; y < size[1]; y++) {
                for (int z = 0; z < size[2]; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }
                    BlockState current = level.getBlockState(pos);
                    if (current.is(ModBlocks.SEAL_ORRERY_PART.get())
                            && current.getValue(SealOrreryPartBlock.SHAPE) != OrreryShapes.get().shapeAt(pos.getX() - controller.getX(), pos.getY() - controller.getY(), pos.getZ() - controller.getZ())
                            && isOrphan(level, pos, current.getValue(SealOrreryPartBlock.SHAPE))) {
                        level.setBlock(pos, air, PLACE_FLAGS);
                    }
                }
            }
        }
    }

    public static Optional<BlockPos> controllerOf(BlockGetter level, BlockPos part, int shape) {
        for (OrreryShapes.Cell cell : OrreryShapes.get().cellsOfShape(shape)) {
            BlockPos candidate = part.offset(-cell.x(), -cell.y(), -cell.z());
            if (level.getBlockState(candidate).is(ModBlocks.SEAL_ORRERY.get())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
