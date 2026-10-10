package net.riftwatch.rift_quests.registry;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.riftwatch.rift_quests.RiftQuestsMod;
import net.riftwatch.rift_quests.block.SealOrreryBlock;
import net.riftwatch.rift_quests.block.SealOrreryBlockEntity;
import net.riftwatch.rift_quests.block.SealOrreryPartBlock;
import net.riftwatch.rift_quests.block.SealVeilBlock;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RiftQuestsMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RiftQuestsMod.MOD_ID);

    public static final DeferredBlock<SealOrreryBlock> SEAL_ORRERY = BLOCKS.registerBlock("seal_orrery", SealOrreryBlock::new, structureProperties());
    public static final DeferredBlock<SealOrreryPartBlock> SEAL_ORRERY_PART = BLOCKS.registerBlock("seal_orrery_part", SealOrreryPartBlock::new, structureProperties());
    public static final DeferredBlock<SealVeilBlock> SEAL_VEIL = BLOCKS.registerBlock("seal_veil", SealVeilBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .noOcclusion()
                    .lightLevel(state -> 6)
                    .sound(SoundType.AMETHYST)
                    .pushReaction(PushReaction.BLOCK)
                    .isViewBlocking((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false));

    public static final Supplier<BlockEntityType<SealOrreryBlockEntity>> SEAL_ORRERY_ENTITY = BLOCK_ENTITIES.register("seal_orrery",
            () -> BlockEntityType.Builder.of(SealOrreryBlockEntity::new, SEAL_ORRERY.get()).build(null));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties structureProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(-1.0F, 3600000.0F)
                .noLootTable()
                .noOcclusion()
                .sound(SoundType.METAL)
                .pushReaction(PushReaction.BLOCK)
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false);
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }
}
