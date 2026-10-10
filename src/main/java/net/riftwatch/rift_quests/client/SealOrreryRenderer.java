package net.riftwatch.rift_quests.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.riftwatch.rift_quests.RiftQuestsMod;
import net.riftwatch.rift_quests.block.SealOrreryBlockEntity;
import net.riftwatch.rift_quests.orrery.OrrerySpec;
import net.riftwatch.rift_quests.orrery.OrreryStructure;
import org.joml.Quaternionf;

public final class SealOrreryRenderer implements BlockEntityRenderer<SealOrreryBlockEntity> {
    private static final int WHITE = 0xFFFFFF;
    private static final int COPPER = 0xD98A4F;
    private static final int WARM = 0xFFE2B0;
    private static final int GEM_DARK = 0x3A3632;
    private static final int SHAFT_COLOUR = 0xFFE2B0;
    private static final float SHAFT_Y = 5.2F;
    private static final String PLATE_SUFFIX = "_plate";
    private static final String PILLAR_PREFIX = "pillar_";
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

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
        int light = LevelRenderer.getLightColor(blockEntity.getLevel(), blockEntity.getBlockPos().above(4));
        float seconds = ClientOrrery.seconds(partialTick);
        poseStack.pushPose();
        poseStack.translate(-anchor[0], -anchor[1], -anchor[2]);
        for (OrrerySpec.Part part : spec.parts()) {
            switch (part.type()) {
                case "box" -> drawBox(part, spec, poseStack, buffer, light);
                case "cylinder" -> drawCylinder(part, poseStack, buffer, light);
                case "sphere" -> drawCore(part, poseStack, buffer, seconds);
                case "ring" -> drawRing(part, spec, poseStack, buffer, light, partialTick);
                default -> {
                }
            }
        }
        drawBeams(spec, poseStack, buffer, seconds);
        poseStack.popPose();
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

    private static Quaternionf euler(double[] rot) {
        return new Quaternionf().rotationYXZ((float) Math.toRadians(rot[1]), (float) Math.toRadians(rot[0]), (float) Math.toRadians(rot[2]));
    }

    private static void drawBox(OrrerySpec.Part part, OrrerySpec spec, PoseStack poseStack, MultiBufferSource buffer, int light) {
        int rgb = WHITE;
        int packed = light;
        if (part.id().endsWith(PLATE_SUFFIX) && part.group().startsWith(PILLAR_PREFIX)) {
            int index = socketIndex(spec, part.group().substring(PILLAR_PREFIX.length()));
            if (index >= 0) {
                float glow = ClientOrrery.glow(index);
                rgb = mix(WHITE, spec.sockets().get(index).colour(), glow * 0.75F);
                packed = glow > 0.05F ? LightTexture.FULL_BRIGHT : light;
            }
        }
        poseStack.pushPose();
        poseStack.translate(part.center()[0], part.center()[1], part.center()[2]);
        poseStack.mulPose(euler(part.rot()));
        OrreryMesh.box(buffer.getBuffer(RenderType.entityCutoutNoCull(texture(part.material()))), poseStack.last(),
                (float) part.size()[0] / 2, (float) part.size()[1] / 2, (float) part.size()[2] / 2, rgb, packed);
        poseStack.popPose();
    }

    private static void drawCylinder(OrrerySpec.Part part, PoseStack poseStack, MultiBufferSource buffer, int light) {
        int rgb = WHITE;
        int packed = light;
        if (part.group().equals("inlay") && part.material().equals("brass")) {
            float level = ClientOrrery.level();
            rgb = mix(WHITE, COPPER, level * 0.8F);
            packed = level > 0.05F ? LightTexture.FULL_BRIGHT : light;
        }
        poseStack.pushPose();
        poseStack.translate(part.center()[0], part.center()[1], part.center()[2]);
        OrreryMesh.cylinder(buffer.getBuffer(RenderType.entityCutoutNoCull(texture(part.material()))), poseStack.last(),
                (float) part.radius(), (float) part.height(), part.segments(), rgb, packed);
        poseStack.popPose();
    }

    private static void drawCore(OrrerySpec.Part part, PoseStack poseStack, MultiBufferSource buffer, float seconds) {
        float level = ClientOrrery.level();
        boolean open = ClientOrrery.open();
        float pulse = open ? 0.12F + 0.05F * Mth.sin(seconds * 7F) : 0.04F * Mth.sin(seconds * 2F);
        int rgb = mix(0x000000, WARM, 0.35F + 0.65F * level + (open ? 0.2F * Mth.sin(seconds * 5F) : 0F));
        poseStack.pushPose();
        poseStack.translate(part.center()[0], part.center()[1], part.center()[2]);
        poseStack.scale(1F + pulse, 1F + pulse, 1F + pulse);
        OrreryMesh.sphere(buffer.getBuffer(RenderType.entityCutoutNoCull(texture(part.material()))), poseStack.last(),
                (float) part.radius(), 14, 20, rgb, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void drawRing(OrrerySpec.Part part, OrrerySpec spec, PoseStack poseStack, MultiBufferSource buffer, int light, float partialTick) {
        float angle = ClientOrrery.angleDegrees(part.dps(), partialTick);
        double[] axis = part.axis();
        poseStack.pushPose();
        poseStack.translate(part.center()[0], part.center()[1], part.center()[2]);
        poseStack.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(angle), (float) axis[0], (float) axis[1], (float) axis[2]).normalize());
        poseStack.mulPose(euler(part.rot()));
        OrreryMesh.ring(buffer.getBuffer(RenderType.entityCutoutNoCull(texture(part.material()))), poseStack.last(),
                (float) part.radius(), (float) part.width(), (float) part.thickness(), part.segments(), WHITE, light);
        if (part.gems() > 0) {
            VertexConsumer gems = buffer.getBuffer(RenderType.entityCutoutNoCull(texture("glass")));
            for (int index = 0; index < part.gems(); index++) {
                double a = index * 2 * Math.PI / part.gems();
                float glow = index < spec.sockets().size() ? ClientOrrery.glow(index) : 0F;
                int colour = index < spec.sockets().size() ? spec.sockets().get(index).colour() : WHITE;
                OrreryMesh.gem(gems, poseStack.last(), (float) (part.radius() * Math.cos(a)), (float) (part.radius() * Math.sin(a)),
                        mix(GEM_DARK, colour, glow), glow > 0.05F ? LightTexture.FULL_BRIGHT : light);
            }
        }
        poseStack.popPose();
    }

    private static void drawBeams(OrrerySpec spec, PoseStack poseStack, MultiBufferSource buffer, float seconds) {
        float level = ClientOrrery.level();
        double[] core = spec.core();
        VertexConsumer beams = buffer.getBuffer(RenderType.lightning());
        List<OrrerySpec.Socket> sockets = spec.sockets();
        for (int index = 0; index < sockets.size(); index++) {
            float glow = ClientOrrery.glow(index);
            if (glow < 0.02F) {
                continue;
            }
            double[] start = sockets.get(index).center();
            double dx = core[0] - start[0];
            double dy = core[1] - start[1];
            double dz = core[2] - start[2];
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            poseStack.pushPose();
            poseStack.translate((start[0] + core[0]) / 2, (start[1] + core[1]) / 2, (start[2] + core[2]) / 2);
            poseStack.mulPose(new Quaternionf().rotationYXZ((float) Math.atan2(dx, dz), (float) -Math.asin(dy / length), 0F));
            int alpha = Math.round(255 * glow * (0.55F + 0.25F * Mth.sin(seconds * 4F + index)));
            OrreryMesh.glowBox(beams, poseStack.last(), 0.035F, 0.035F, (float) length / 2, sockets.get(index).colour(), Mth.clamp(alpha, 0, 255));
            poseStack.popPose();
        }
        if (level > 0F) {
            float pulse = ClientOrrery.open() ? 0.12F + 0.06F * Mth.sin(seconds * 6F) : 0F;
            poseStack.pushPose();
            poseStack.translate(core[0], SHAFT_Y, core[2]);
            OrreryMesh.glowBox(beams, poseStack.last(), 0.14F, 1.6F, 0.14F, SHAFT_COLOUR, Mth.clamp(Math.round(255 * (level * 0.35F + pulse)), 0, 255));
            poseStack.popPose();
        }
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
