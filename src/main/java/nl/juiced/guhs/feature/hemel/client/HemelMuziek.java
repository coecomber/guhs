package nl.juiced.guhs.feature.hemel.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.hemel.HemelFeature;
import nl.juiced.guhs.feature.hemel.KnuffelhartBlockEntity;

/**
 * The Hemelkapelletje's soft music (hemel.muziek, a harp-and-celesta lullaby), looping while you are near the Knuffelhart:
 * it swells in as you come closer and fades away as you walk off (a little quieter while the heart still sleeps).
 */
public class HemelMuziek extends AbstractTickableSoundInstance {
    /** It starts within this distance and is gone beyond {@link #EIND}. */
    public static final double BEGIN = 24, EIND = 32, VOL = 8;

    private final KnuffelhartBlockEntity hart;
    private final Vec3 plek;
    private float doel;

    public HemelMuziek(KnuffelhartBlockEntity hart) {
        super(HemelFeature.MUZIEK.get(), SoundSource.RECORDS, RandomSource.create());
        this.hart = hart;
        this.plek = Vec3.atCenterOf(hart.getBlockPos());
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = plek.x;
        this.y = plek.y;
        this.z = plek.z;
        this.volume = 0.01f;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || hart.isRemoved() || p.level() != hart.getLevel()) {
            stop();
            return;
        }
        double d = p.position().distanceTo(plek);
        if (d > EIND) {
            stop();
            return;
        }
        float ver = (float) Mth.clamp((EIND - d) / (EIND - VOL), 0, 1);
        doel = ver * (HemelClient.klopt() ? 1f : 0.6f);
        volume += (doel - volume) * 0.05f;
        volume = Math.max(0.001f, volume);
    }

    void stopNu() {
        stop();
    }
}
