package nl.juiced.guhs.feature.vadsrijk.kern.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

/**
 * The boss music hook: while a boss bar shows, loop its music (pausing the normal music), if the sound exists in the resources.
 * Without an asset (now) nothing plays.
 */
final class BaasMuziek {
    @Nullable
    private static SimpleSoundInstance speelt;
    private static String huidig = "";

    private BaasMuziek() {
    }

    static void zet(String id) {
        if (id.equals(huidig)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (speelt != null) {
            mc.getSoundManager().stop(speelt);
            speelt = null;
        }
        huidig = id;
        Identifier loc = id.isEmpty() ? null : Identifier.tryParse(id);
        if (loc == null || mc.getSoundManager().getSoundEvent(loc) == null) {
            return;
        }
        mc.getMusicManager().stopPlaying();
        speelt = new SimpleSoundInstance(loc, SoundSource.MUSIC, 1f, 1f, SoundInstance.createUnseededRandom(), true, 0, SoundInstance.Attenuation.NONE,
                0, 0, 0, true);
        mc.getSoundManager().play(speelt);
    }
}
