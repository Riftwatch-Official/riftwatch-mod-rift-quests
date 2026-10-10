package net.riftwatch.rift_quests.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.riftwatch.rift_quests.registry.ModComponents;

public record SealOwner(UUID id, String name) {
    public static final Codec<SealOwner> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(SealOwner::id),
            Codec.STRING.fieldOf("name").forGetter(SealOwner::name)).apply(instance, SealOwner::new));

    public static final StreamCodec<ByteBuf, SealOwner> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SealOwner::id,
            ByteBufCodecs.STRING_UTF8, SealOwner::name,
            SealOwner::new);

    public static SealOwner of(Player player) {
        return new SealOwner(player.getUUID(), player.getGameProfile().getName());
    }

    public static boolean isBoundTo(ItemStack stack, UUID player) {
        SealOwner owner = stack.get(ModComponents.SEAL_OWNER.get());
        return owner != null && owner.id().equals(player);
    }
}
