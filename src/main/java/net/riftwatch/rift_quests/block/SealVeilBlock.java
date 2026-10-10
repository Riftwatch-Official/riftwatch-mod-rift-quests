package net.riftwatch.rift_quests.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.riftwatch.rift_quests.orrery.OrreryLogic;

public final class SealVeilBlock extends TransparentBlock {
    public static final MapCodec<SealVeilBlock> CODEC = simpleCodec(SealVeilBlock::new);

    public SealVeilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SealVeilBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player && OrreryLogic.isOpen(player)) {
            return Shapes.empty();
        }
        return Shapes.block();
    }
}
