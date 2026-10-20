package nl.juiced.guhs.feature.safari.spullen.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.safari.spullen.SafariGeluid;

/**
 * The ambience of Safaripark Vadse Bergen (client only, S6): every zone its own soft loop (savanne crickets, jungle calls,
 * icy wind...), chosen by the biome under the player ({@code guhs:safari_<zone>}), crossfading when you walk into another
 * zone. In the entreedorp the cheerful lobby tune plays as well. Outside the safari dimension everything fades out.
 */
public final class SafariSfeer {
    private static final float SFEER_VOLUME = 0.55f, MUZIEK_VOLUME = 0.35f;
    private static final Map<SoundEvent, Lus> LUSSEN = new HashMap<>();

    public static void init(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(SafariSfeer::onTick);
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        String zone = null;
        if (level != null && mc.player != null && level.dimension() == Safari.DIM) {
            zone = zone(level.getBiome(mc.player.blockPosition()));
        }
        SoundEvent sfeer = zone == null ? null : SafariGeluid.SFEER.containsKey(zone) ? SafariGeluid.SFEER.get(zone).get() : null;
        SoundEvent muziek = "entree".equals(zone) ? SafariGeluid.MUZIEK_LOBBY.get() : null;
        if (mc.isPaused()) {
            return;
        }
        for (Lus l : LUSSEN.values()) {
            l.doel = 0f;
        }
        zet(mc, sfeer, SFEER_VOLUME);
        zet(mc, muziek, MUZIEK_VOLUME);
        LUSSEN.values().removeIf(l -> l.isStopped());
    }

    private static void zet(Minecraft mc, SoundEvent event, float volume) {
        if (event == null) {
            return;
        }
        Lus l = LUSSEN.get(event);
        if (l == null || l.isStopped() || !mc.getSoundManager().isActive(l)) {
            l = new Lus(event);
            LUSSEN.put(event, l);
            mc.getSoundManager().play(l);
        }
        l.doel = volume;
    }

    private static String zone(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.identifier().getPath()).filter(p -> p.startsWith("safari_")).map(p -> p.substring(7)).orElse(null);
    }

    /** A looping, non-positional ambience sound that eases its volume towards {@link #doel} and stops when silent. */
    private static final class Lus extends AbstractTickableSoundInstance {
        float doel = 0f;

        Lus(SoundEvent event) {
            super(event, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override
        public void tick() {
            volume = Mth.lerp(0.04f, volume, doel);
            if (doel == 0f && volume < 0.01f) {
                stop();
            }
        }
    }

    private SafariSfeer() {
    }
}
