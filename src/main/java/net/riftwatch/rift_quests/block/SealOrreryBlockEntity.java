package net.riftwatch.rift_quests.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.riftwatch.rift_quests.orrery.OrreryStructure;
import net.riftwatch.rift_quests.registry.ModBlocks;

public final class SealOrreryBlockEntity extends BlockEntity {
    public SealOrreryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SEAL_ORRERY_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> OrreryStructure.removeStrays(serverLevel, getBlockPos()));
        }
    }
}
