package net.riftwatch.rift_quests.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.riftwatch.rift_quests.RiftQuestsMod;
import net.riftwatch.rift_quests.block.SealOrreryBlockEntity;
import net.riftwatch.rift_quests.orrery.OrrerySpec;
import net.riftwatch.rift_quests.orrery.OrreryStructure;
import net.riftwatch.rift_quests.registry.ModItems;
import org.joml.Quaternionf;

public final class SealOrreryRenderer implements BlockEntityRenderer<SealOrreryBlockEntity> {
    private static final int BLACK = 0x000000;
    private static final int WHITE = 0xFFFFFF;
    private static final int WARM = 0xFFE2B0;
    private static final int BEAM_COLOUR = 0xFFC070;
    private static final int CONDUIT_OFF = 0x6A4A30;
    private static final int CONDUIT_ON = 0xFFC080;
    private static final int LEVEL_OFF = 0x8A7458;
    private static final float SLOT_DIM = 0.45F;
    private static final float SEAL_SIZE = 1.05F;
    private static final int LIGHT_SAMPLE_HEIGHT = 8;
    private static final float LIGHT_BOX_MARGIN = 1.04F;
    private static final float[] HALO_SCALES = {1.35F, 1.9F, 2.7F};
    private static final int[] HALO_ALPHA = {110, 55, 26};
    private static final float CORE_SPIN_Y = 0.45F;
    private static final float CORE_SPIN_X = 0.27F;
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static List<ItemStack> sealStacks;

    private record Look(int rgb, int light) {
    }

    private static List<ItemStack> sealStacks() {
        if (sealStacks == null) {
            sealStacks = ModItems.SEALS.stream().map(seal -> seal.get().getDefaultInstance()).toList();
        }
        return sealStacks;
    }

    private static ResourceLocation texture(String material) {
        return TEXTURES.computeIfAbsent(material,
                name -> ResourceLocation.fromNamespaceAndPath(RiftQuestsMod.MOD_ID, "textures/entity/orrery/" + name + ".png"));
    }

    @Override
    public void render(SealOrreryBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (blockEntity.getLevel() == null) {
            return;
        }
        OrrerySpec spec = OrrerySpec.get();
        int[] anchor = spec.controller();
        int light = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().above(LIGHT_SAMPLE_HEIGHT));
        float seconds = ClientOrrery.seconds(partialTick);
        poseStack.pushPose();
        poseStack.translate(-anchor[0], -anchor[1], -anchor[2]);
        for (OrrerySpec.Part part : spec.parts()) {
            switch (part.type()) {
                case "box" -> drawBox(part, spec, poseStack, buffer, light);
                case "voxels" -> drawVoxels(part, spec, poseStack, buffer, light, partialTick);
                case "core" -> drawCore(part, poseStack, buffer, seconds);
                case "beam" -> drawBeam(part, poseStack, buffer, seconds);
                default -> {
                }
            }
        }
        drawSeals(spec, blockEntity.getLevel(), poseStack, buffer, blockEntity.getBlockPos().hashCode());
        poseStack.popPose();
    }

    private static void drawSeals(OrrerySpec spec, Level level, PoseStack poseStack, MultiBufferSource buffer, int seed) {
        List<OrrerySpec.Socket> sockets = spec.sockets();
        List<ItemStack> stacks = sealStacks();
        float scale = (float) spec.scale();
        for (int index = 0; index < sockets.size(); index++) {
            float glow = ClientOrrery.glow(index);
            if (glow < 0.05F) {
                continue;
            }
            OrrerySpec.Socket socket = sockets.get(index);
            poseStack.pushPose();
            poseStack.translate(socket.center()[0], socket.center()[1], socket.center()[2]);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) socket.yaw() + 180F));
            float size = SEAL_SIZE * scale * glow;
            poseStack.scale(size, size, size);
            Minecraft.getInstance().getItemRenderer().renderStatic(stacks.get(index), ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, poseStack, buffer, level, seed + index);
            poseStack.popPose();
        }
    }

    private static int socketIndex(OrrerySpec spec, String id) {
        List<OrrerySpec.Socket> sockets = spec.sockets();
        for (int index = 0; index < sockets.size(); index++) {
            if (sockets.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private static int mix(int from, int to, float amount) {
        float t = Mth.clamp(amount, 0F, 1F);
        int r = Math.round(Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF));
        int g = Math.round(Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF));
        int b = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return (r << 16) | (g << 8) | b;
    }

    private static void rotate(PoseStack poseStack, double[] rot) {
        if (rot[0] != 0 || rot[1] != 0 || rot[2] != 0) {
            poseStack.mulPose(new Quaternionf().rotationYXZ((float) Math.toRadians(rot[1]), (float) Math.toRadians(rot[0]), (float) Math.toRadians(rot[2])));
        }
    }

    private static Look look(String tint, int socket, OrrerySpec spec, int light) {
        switch (tint) {
            case "colour" -> {
                float glow = socket >= 0 ? ClientOrrery.level() : 0F;
                int colour = socket >= 0 ? spec.sockets().get(socket).colour() : WHITE;
                return new Look(mix(mix(BLACK, colour, SLOT_DIM), colour, glow), glow > 0.05F ? LightTexture.FULL_BRIGHT : light);
            }
            case "conduit" -> {
                float glow = socket >= 0 ? ClientOrrery.level() : 0F;
                return new Look(mix(CONDUIT_OFF, CONDUIT_ON, glow), glow > 0.05F ? LightTexture.FULL_BRIGHT : light);
            }
            case "level" -> {
                return new Look(mix(LEVEL_OFF, WHITE, ClientOrrery.level()), LightTexture.FULL_BRIGHT);
            }
            default -> {
                return new Look(WHITE, light);
            }
        }
    }

    private static void drawBox(OrrerySpec.Part part, OrrerySpec spec, PoseStack poseStack, MultiBufferSource buffer, int light) {
        Look look = look(part.tint(), socketIndex(spec, part.socket()), spec, light);
        poseStack.pushPose();
        poseStack.translate(part.center()[0], part.center()[1], part.center()[2]);
        rotate(poseStack, part.rot());
        OrreryMesh.box(buffer.getBuffer(RenderType.entityCutoutNoCull(texture(part.material()))), poseStack.last(),
                (float) part.size()[0] / 2, (float) part.size()[1] / 2, (float) part.size()[2] / 2, look.rgb(), look.light());
        poseStack.popPose();
    }

    private static void drawVoxels(OrrerySpec.Part part, OrrerySpec spec, PoseStack poseStack, MultiBufferSource buffer, int light, float partialTick) {
        float angle = ClientOrrery.angleDegrees(part.dps(), partialTick);
        double[] axis = part.axis();
        poseStack.pushPose();
        poseStack.translate(part.center()[0], part.center()[1], part.center()[2]);
        poseStack.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(angle), (float) axis[0], (float) axis[1], (float) axis[2]).normalize());
        rotate(poseStack, part.rot());
        for (OrrerySpec.Cube cube : part.cubes()) {
            Look look = look(cube.tint(), cube.socket(), spec, light);
            poseStack.pushPose();
            poseStack.translate(cube.center()[0], cube.center()[1], cube.center()[2]);
            if (cube.yaw() != 0) {
                poseStack.mulPose(Axis.YP.rotationDegrees((float) cube.yaw()));
            }
            OrreryMesh.box(buffer.getBuffer(RenderType.entityCutoutNoCull(texture(cube.material()))), poseStack.last(),
                    (float) cube.size()[0] / 2, (float) cube.size()[1] / 2, (float) cube.size()[2] / 2, look.rgb(), look.light());
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void drawCore(OrrerySpec.Part part, PoseStack poseStack, MultiBufferSource buffer, float seconds) {
        float level = ClientOrrery.level();
        float burst = ClientOrrery.burst();
        float pulse = 0.04F * Mth.sin(seconds * 2F) * (1F - burst) + burst * (0.1F + 0.04F * Mth.sin(seconds * 7F));
        float half = (float) part.size()[0] / 2;
        int rgb = mix(mix(BLACK, WARM, 0.35F + 0.65F * level), WHITE, burst);
        poseStack.pushPose();
        poseStack.translate(part.center()[0], part.center()[1], part.center()[2]);
        poseStack.scale(1F + pulse, 1F + pulse, 1F + pulse);
        if (burst < 0.95F) {
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationYXZ(seconds * CORE_SPIN_Y, seconds * CORE_SPIN_X, 0F));
            OrreryMesh.box(buffer.getBuffer(RenderType.entityCutoutNoCull(texture(part.material()))), poseStack.last(), half, half, half, rgb, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }
        if (burst > 0.02F) {
            VertexConsumer light = buffer.getBuffer(RenderType.lightning());
            float box = half * LIGHT_BOX_MARGIN;
            OrreryMesh.glowBox(light, poseStack.last(), box, box, box, WHITE, Math.round(255 * Mth.clamp(burst * 1.4F, 0F, 1F)));
            for (int halo = 0; halo < HALO_SCALES.length; halo++) {
                float breathe = 1F + 0.06F * Mth.sin(seconds * (3F + halo) + halo);
                float size = box * HALO_SCALES[halo] * breathe;
                OrreryMesh.glowBox(light, poseStack.last(), size, size, size, WARM, Math.round(HALO_ALPHA[halo] * burst));
            }
        }
        poseStack.popPose();
    }

    private static void drawBeam(OrrerySpec.Part part, PoseStack poseStack, MultiBufferSource buffer, float seconds) {
        float level = ClientOrrery.level();
        float opacity = 0.14F + 0.3F * level + 0.35F * ClientOrrery.burst() + 0.04F * Mth.sin(seconds * 6F);
        float width = (0.6F + 0.5F * level + 0.25F * Mth.sin(seconds * 5F)) * (float) part.size()[0] / 2;
        poseStack.pushPose();
        poseStack.translate(part.center()[0], part.center()[1], part.center()[2]);
        OrreryMesh.glowBox(buffer.getBuffer(RenderType.lightning()), poseStack.last(), width, (float) part.size()[1] / 2, width, BEAM_COLOUR,
                Mth.clamp(Math.round(255 * opacity), 0, 255));
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SealOrreryBlockEntity blockEntity) {
        BlockPos origin = OrreryStructure.origin(blockEntity.getBlockPos());
        int[] size = OrrerySpec.get().size();
        return new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + size[0], origin.getY() + size[1], origin.getZ() + size[2]);
    }

    @Override
    public boolean shouldRenderOffScreen(SealOrreryBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
