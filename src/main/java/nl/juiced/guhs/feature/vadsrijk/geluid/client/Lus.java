package nl.juiced.guhs.feature.vadsrijk.geluid.client;

import java.util.function.DoubleSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * T-GELUID: a piece of music or an ambience bed that plays "everywhere" (not from a spot), with its own fade in / fade out, a
 * live volume (the sliders can move while it plays) and ducking (the area music dips while a stinger plays).
 */
final class Lus extends AbstractTickableSoundInstance {
    private final DoubleSupplier doel;
    private final int inFade;
    private int leeftijd, uitFade = -1, uitLeeftijd;
    private float uitVan;
    /** 1 = normal; the ducking target (e.g. 0.3 while a stinger plays); the real factor glides there. */
    float duikDoel = 1f;
    private float duik = 1f;
    final String naam;

    Lus(String naam, SoundEvent event, SoundSource bron, DoubleSupplier doel, int inFade, boolean lus) {
        super(event, bron, RandomSource.create());
        this.naam = naam;
        this.doel = doel;
        this.inFade = Math.max(1, inFade);
        this.looping = lus;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = 0.0001f;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    /** Ended by itself, stopped, or never started (e.g. a missing file). */
    boolean klaar() {
        return isStopped() || leeftijd > 20 && !Minecraft.getInstance().getSoundManager().isActive(this);
    }

    boolean gaatUit() {
        return uitFade >= 0;
    }

    void uit(int ticks) {
        if (uitFade >= 0) {
            return;
        }
        uitVan = volume;
        uitFade = Math.max(1, ticks);
        uitLeeftijd = 0;
    }

    @Override
    public void tick() {
        leeftijd++;
        duik += (duikDoel - duik) * 0.08f;
        if (uitFade >= 0) {
            uitLeeftijd++;
            volume = Math.max(0.0001f, uitVan * (1f - uitLeeftijd / (float) uitFade));
            if (uitLeeftijd >= uitFade) {
                stop();
            }
            return;
        }
        volume = (float) Math.max(0.0001, doel.getAsDouble() * duik * Math.min(1f, leeftijd / (float) inFade));
    }
}
