package net.riftwatch.rift_quests.client.cinematic;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.riftwatch.rift_quests.RiftQuestsMod;
import net.riftwatch.rift_quests.client.title.TitleOverlay;
import net.riftwatch.rift_quests.network.Payloads;

public final class ClientCinematic {
    private static final String ENTRY = "rift_monarch";
    private static final float EYE = 1.62F;
    private static final double HEAD = 6.6;
    private static final float ENTRY_LENGTH = 9.0F;
    private static final float SHORT_LENGTH = 3.0F;
    private static final float ENTRY_TITLE = 5.0F;
    private static final float SHORT_TITLE = 0.3F;
    private static final int SKIP_TICKS = 20;
    private static final float[][] ENTRY_FOV = {{0, 70}, {1.8F, 60}, {4.4F, 52}, {5.4F, 44}, {6.4F, 42}, {7.6F, 56}, {9.0F, 70}};
    private static final float[][] SHORT_FOV = {{0, 70}, {1.2F, 56}, {1.9F, 54}, {3.0F, 70}};
    private static final double[][] ENTRY_KEYS = {{1.8, 27.0, 7.0, 6.5}, {3.0, 15.5, 2.5, 5.5}, {4.4, 9.0, 1.4, 3.0}, {5.4, 8.1, 3.6, 5.1},
            {6.4, 8.4, 4.4, -5.1}, {7.6, 12.5, 5.5, -5.0}};

    record Cue(float time, SoundEvent sound, float pitch, float volume) {
    }

    private static final List<Cue> ENTRY_CUES = List.of(new Cue(2.4F, SoundEvents.BEACON_ACTIVATE, 1.3F, 0.5F), new Cue(3.0F, SoundEvents.BEACON_ACTIVATE, 1.5F, 0.5F),
            new Cue(3.6F, SoundEvents.BEACON_ACTIVATE, 1.7F, 0.5F), new Cue(4.0F, SoundEvents.ANVIL_LAND, 0.5F, 0.7F),
            new Cue(4.8F, SoundEvents.BEACON_POWER_SELECT, 0.6F, 0.8F));

    private static Payloads.Cinematic active;
    private static long startedAt;
    private static float length;
    private static boolean titleShown;
    private static int nextCue;
    private static int skipHeld;
    private static RemotePlayer camera;
    private static CameraType cameraType;
    private static Vec3 centre;
    private static Vec3 landing;
    private static Vec3 dir;
    private static Vec3 side;
    private static CinematicTrack track;
    private static Vec3 lookAt;
    private static Vec3 position;
    private static float yaw;
    private static float pitch;
    private static float roll;
    private static float fov;
    private static boolean veilPass;

    private ClientCinematic() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, event -> frame());
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeCameraAngles.class, event -> {
            if (active != null) {
                event.setYaw(yaw);
                event.setPitch(pitch);
                event.setRoll(roll);
            }
        });
        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFov.class, event -> {
            if (active != null) {
                event.setFOV(fov);
            }
        });
        NeoForge.EVENT_BUS.addListener(MovementInputUpdateEvent.class, event -> {
            if (active != null) {
                event.getInput().forwardImpulse = 0;
                event.getInput().leftImpulse = 0;
                event.getInput().jumping = false;
                event.getInput().shiftKeyDown = false;
            }
        });
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, event -> {
            if (active != null) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderHandEvent.class, event -> {
            if (active != null) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderHighlightEvent.Block.class, event -> {
            if (active != null) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(RenderGuiLayerEvent.Pre.class, event -> {
            if (active != null && !RiftQuestsMod.MOD_ID.equals(event.getName().getNamespace())) {
                event.setCanceled(true);
            }
        });
    }

    public static boolean veilPass() {
        return veilPass;
    }

    public static void setVeilPass(boolean open) {
        veilPass = open;
    }

    public static void clear() {
        if (active != null) {
            stop(false);
        }
        veilPass = false;
    }

    public static void start(Payloads.Cinematic payload) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        if (active != null) {
            stop(false);
        }
        active = payload;
        boolean entry = ENTRY.equals(payload.name());
        length = entry ? ENTRY_LENGTH : SHORT_LENGTH;
        startedAt = Util.getNanos();
        titleShown = false;
        nextCue = 0;
        skipHeld = 0;
        centre = new Vec3(payload.centreX(), payload.centreY(), payload.centreZ());
        landing = new Vec3(payload.landingX(), payload.landingY(), payload.landingZ());
        Vec3 flat = new Vec3(landing.x - centre.x, 0, landing.z - centre.z);
        dir = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
        side = new Vec3(-dir.z, 0, dir.x);
        track = entry ? entryTrack() : shortTrack();
        camera = new RemotePlayer(minecraft.level, player.getGameProfile());
        cameraType = minecraft.options.getCameraType();
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        minecraft.gameRenderer.setRenderHand(false);
        pose(0);
        place();
        minecraft.setCameraEntity(camera);
    }

    private static Vec3 local(double along, double up, double across) {
        return centre.add(dir.scale(along)).add(0, up, 0).add(side.scale(across));
    }

    private static Vec3 eye() {
        return landing.add(0, EYE, 0);
    }

    private static CinematicTrack entryTrack() {
        List<CinematicTrack.Key> keys = new ArrayList<>();
        keys.add(new CinematicTrack.Key(0, eye()));
        for (double[] key : ENTRY_KEYS) {
            keys.add(new CinematicTrack.Key((float) key[0], local(key[1], key[2], key[3])));
        }
        keys.add(new CinematicTrack.Key(ENTRY_LENGTH, eye()));
        return new CinematicTrack(List.copyOf(keys));
    }

    private static CinematicTrack shortTrack() {
        Vec3 out = new Vec3(landing.x - centre.x, 0, landing.z - centre.z);
        Vec3 away = out.lengthSqr() < 1.0E-4 ? dir : out.normalize();
        Vec3 high = eye().add(away.scale(4)).add(0, 3, 0);
        return new CinematicTrack(List.of(new CinematicTrack.Key(0, eye()), new CinematicTrack.Key(1.2F, high),
                new CinematicTrack.Key(1.9F, high.add(away.scale(-1))), new CinematicTrack.Key(SHORT_LENGTH, eye())));
    }

    private static float span(float t, float a, float b) {
        return Math.max(0, Math.min(1, (t - a) / (b - a)));
    }

    private static float smooth(float x) {
        return x * x * (3 - 2 * x);
    }

    private static double throneY(float t) {
        return active.throneTop() + (centre.y - active.throneTop()) * smooth(span(t, 1.8F, 4.0F));
    }

    private static Vec3 target(float t) {
        if (!ENTRY.equals(active.name())) {
            return centre.add(0, HEAD, 0);
        }
        if (t < 1.8F) {
            float k = smooth(span(t, 0, 1.8F));
            return local(-12 + 15.5 * k, 2 + 2 * span(t, 0, 1.8F), 0);
        }
        double head = throneY(t) - centre.y + HEAD;
        if (t < 4.4F) {
            return local(0, head - 1.5, 0);
        }
        if (t < 7.6F) {
            return local(0, head, 0);
        }
        return local(0, HEAD + (4.6 - HEAD) * span(t, 7.6F, 9.0F), 0);
    }

    private static void pose(float t) {
        boolean entry = ENTRY.equals(active.name());
        position = track.at(t);
        float shake = 0;
        if (entry && t > 5.8F && t < 6.5F) {
            shake = (1 - span(t, 5.8F, 6.5F)) * 0.18F;
        } else if (entry && t > 4.0F && t < 4.3F) {
            shake = 0.07F;
        }
        if (shake > 0) {
            position = position.add(Math.sin(t * 83) * shake, Math.cos(t * 71) * shake, Math.sin(t * 59) * shake);
        }
        lookAt = target(t);
        Vec3 d = lookAt.subtract(position);
        double flat = Math.sqrt(d.x * d.x + d.z * d.z);
        yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        pitch = (float) -Math.toDegrees(Math.atan2(d.y, flat));
        roll = shake * 12 * (float) Math.sin(t * 47);
        fov = CinematicTrack.scalar(entry ? ENTRY_FOV : SHORT_FOV, t);
    }

    private static void place() {
        camera.setPos(position.x, position.y - camera.getEyeHeight(), position.z);
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.setYHeadRot(yaw);
        camera.setOldPosAndRot();
    }

    private static float elapsed() {
        return (Util.getNanos() - startedAt) / 1.0E9F;
    }

    private static void frame() {
        if (active == null) {
            return;
        }
        float t = elapsed();
        if (t >= length) {
            stop(true);
            return;
        }
        pose(t);
        place();
        if (!titleShown && t >= (ENTRY.equals(active.name()) ? ENTRY_TITLE : SHORT_TITLE)) {
            titleShown = true;
            TitleOverlay.showAlways(ENTRY);
        }
        if (ENTRY.equals(active.name())) {
            while (nextCue < ENTRY_CUES.size() && ENTRY_CUES.get(nextCue).time() <= t) {
                Cue cue = ENTRY_CUES.get(nextCue++);
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(cue.sound(), cue.pitch(), cue.volume()));
            }
        }
    }

    private static void tick() {
        if (active == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !minecraft.player.isAlive() || camera.level() != minecraft.level) {
            stop(false);
            return;
        }
        if (active.skippable() && minecraft.options.keyShift.isDown()) {
            if (++skipHeld >= SKIP_TICKS) {
                PacketDistributor.sendToServer(new Payloads.CinematicSkip());
                stop(true);
            }
        } else {
            skipHeld = 0;
        }
    }

    private static void stop(boolean faceBoss) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null) {
            minecraft.setCameraEntity(player);
            if (faceBoss && centre != null) {
                Vec3 d = centre.add(0, HEAD - 2, 0).subtract(player.getEyePosition());
                double flat = Math.sqrt(d.x * d.x + d.z * d.z);
                player.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
                player.setXRot((float) -Math.toDegrees(Math.atan2(d.y, flat)));
                player.yRotO = player.getYRot();
                player.xRotO = player.getXRot();
            }
        }
        if (cameraType != null) {
            minecraft.options.setCameraType(cameraType);
        }
        minecraft.gameRenderer.setRenderHand(true);
        active = null;
        camera = null;
        cameraType = null;
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        if (active == null) {
            return;
        }
        float t = elapsed();
        float in = smooth(span(t, 0, 0.4F));
        float out = 1 - smooth(span(t, length - 0.4F, length));
        float k = Math.min(in, out);
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        int bar = Math.round(h * 0.11F * k);
        if (bar > 0) {
            graphics.fill(0, 0, w, bar, 0xFF000000);
            graphics.fill(0, h - bar, w, h, 0xFF000000);
        }
        if (active.skippable() && k > 0.9F) {
            Component hint = Component.translatable("message.rift_quests.cinematic.skip");
            int width = Minecraft.getInstance().font.width(hint);
            graphics.drawString(Minecraft.getInstance().font, hint, w - width - 10, h - bar / 2 - 4, skipHeld > 0 ? 0xFFCDB2FF : 0xFF8A7A6A, false);
        }
    }
}
