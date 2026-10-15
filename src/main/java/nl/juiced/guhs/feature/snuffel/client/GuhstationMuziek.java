package nl.juiced.guhs.feature.snuffel.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.feature.snuffel.SnuffelFeature;

/**
 * The island tune ({@code guhs:music_disc.snuffeleiland}, the same sound as the music disc), VERY softly, while the
 * Guhstation's window is open and the two dogs play: it fades in when the window opens, fades out when it closes (also
 * when you press start) and stops. Not in the world (no position), looping, on the Jukebox/Note Blocks slider like the
 * disc itself. Never two at once: there is one instance, and a window that opens while the last one still fades out
 * takes that one over. The island itself stays without music; the disc is the only place where the tune plays at full
 * volume.
 */
public final class GuhstationMuziek extends AbstractTickableSoundInstance {
    /** How loud it gets (the disc plays at 1), and how many ticks the fades take. */
    public static final float ZACHT = 0.1f;
    private static final int IN_TICKS = 40, UIT_TICKS = 20;

    @Nullable
    private static GuhstationMuziek huidig;

    private boolean open = true;

    private GuhstationMuziek() {
        super(SnuffelFeature.MUZIEK.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = 0.001f;
    }

    /** The window opened (safe to call again, as a resize does). */
    static void aan() {
        Minecraft mc = Minecraft.getInstance();
        if (huidig != null && !huidig.isStopped() && mc.getSoundManager().isActive(huidig)) {
            huidig.open = true;
            return;
        }
        if (huidig != null) {
            // (stopped from outside, by a reload of the sounds or a change of world: make sure it is really gone)
            mc.getSoundManager().stop(huidig);
        }
        huidig = new GuhstationMuziek();
        mc.getSoundManager().play(huidig);
    }

    /** The window closed: fade out, then stop. */
    static void uit() {
        if (huidig != null) {
            huidig.open = false;
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (open && !(Minecraft.getInstance().screen instanceof GuhstationScherm)) {
            open = false;      // (the window went away without telling us)
        }
        if (open) {
            volume = Math.min(ZACHT, volume + ZACHT / IN_TICKS);
        } else {
            volume -= ZACHT / UIT_TICKS;
            if (volume <= 0f) {
                volume = 0f;
                stop();
                if (huidig == this) {
                    huidig = null;
                }
            }
        }
    }
}
