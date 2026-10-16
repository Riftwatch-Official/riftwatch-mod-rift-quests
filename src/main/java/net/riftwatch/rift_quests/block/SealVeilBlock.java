package net.riftwatch.rift_quests.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.riftwatch.rift_quests.arena.Arena;

public final class SealVeilBlock extends TransparentBlock {
    public static final MapCodec<SealVeilBlock> CODEC = simpleCodec(SealVeilBlock::new);
    private static final int DESCENT_TICKS = 300;

    public SealVeilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SealVeilBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player && Arena.veilOpen(player)) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player) || !Arena.veilOpen(player)) {
            return;
        }
        if (!level.getBlockState(pos.below()).isAir()) {
            return;
        }
        MobEffectInstance current = player.getEffect(MobEffects.SLOW_FALLING);
        if (current == null || current.getDuration() < DESCENT_TICKS - 20) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, DESCENT_TICKS, 0, false, false, true));
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 24, 0.4, 0.6, 0.4, 0.02);
        }
    }
}
