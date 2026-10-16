package net.riftwatch.rift_quests.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.riftwatch.rift_quests.registry.ModSounds;

public final class ArenaMusic {
    private static final float FADE_IN_TICKS = 40F;
    private static final float FADE_OUT_TICKS = 100F;

    private static Track playing;

    private ArenaMusic() {
    }

    public static void set(boolean on) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean audible = playing != null && minecraft.getSoundManager().isActive(playing);
        if (on) {
            if (audible) {
                playing.rising = true;
                return;
            }
            playing = new Track();
            minecraft.getSoundManager().play(playing);
        } else if (playing != null) {
            playing.rising = false;
        }
    }

    public static void clear() {
        if (playing != null) {
            Minecraft.getInstance().getSoundManager().stop(playing);
            playing = null;
        }
    }

    private static final class Track extends AbstractTickableSoundInstance {
        private boolean rising = true;
        private float level;

        private Track() {
            super(ModSounds.RIFT_MONARCH_MUSIC.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0F;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            level = rising ? Math.min(1F, level + 1F / FADE_IN_TICKS) : Math.max(0F, level - 1F / FADE_OUT_TICKS);
            volume = level * level;
            if (!rising && level <= 0F) {
                stop();
            }
        }
    }
}
