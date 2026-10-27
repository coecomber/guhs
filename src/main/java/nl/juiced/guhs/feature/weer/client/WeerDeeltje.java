package nl.juiced.guhs.feature.weer.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;

/**
 * 1.8.0: what the guh-weertjes are made of (the textures of the existing guh particles, but falling like weather). One
 * class, five ways to fall ({@link Soort}).
 */
public class WeerDeeltje extends SingleQuadParticle {
    public enum Soort {
        /** pluisjessneeuw: slow, wobbling fluff */
        PLUISJE,
        /** bellenbui: soap bubbles drifting down, now and then floating up again */
        ZEEPBEL,
        /** hartjesregen: little hearts tumbling down */
        HARTJE,
        /** bloesemwind: petals blown sideways by the wind */
        BLOESEM,
        /** sterrenregen: a falling star's glittering head (the WeerClient moves it; it only shines a moment) */
        STER
    }

    private final Soort soort;
    private final float fase;

    public WeerDeeltje(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, Soort soort) {
        super(level, x, y, z, sprites.get(level.getRandom()));
        this.soort = soort;
        this.fase = random.nextFloat() * 6.28f;
        hasPhysics = soort != Soort.STER;
        switch (soort) {
            case PLUISJE -> {
                lifetime = 120 + random.nextInt(60);
                quadSize = 0.07f + random.nextFloat() * 0.07f;
                gravity = 0f;
                yd = -0.035 - random.nextDouble() * 0.02;
            }
            case ZEEPBEL -> {
                lifetime = 140 + random.nextInt(60);
                quadSize = 0.1f + random.nextFloat() * 0.12f;
                gravity = 0f;
                yd = -0.025 - random.nextDouble() * 0.02;
                alpha = 0.85f;
            }
            case HARTJE -> {
                lifetime = 110 + random.nextInt(50);
                quadSize = 0.08f + random.nextFloat() * 0.06f;
                gravity = 0f;
                yd = -0.045 - random.nextDouble() * 0.02;
            }
            case BLOESEM -> {
                lifetime = 100 + random.nextInt(60);
                quadSize = 0.07f + random.nextFloat() * 0.05f;
                gravity = 0f;
                yd = -0.03 - random.nextDouble() * 0.02;
                xd = dx == 0 ? 0.12 + random.nextDouble() * 0.06 : dx;
                zd = dz == 0 ? 0.04 + random.nextDouble() * 0.03 : dz;
            }
            case STER -> {
                lifetime = 5 + random.nextInt(3);
                quadSize = 0.35f + random.nextFloat() * 0.15f;
                gravity = 0f;
                xd = yd = zd = 0;
            }
        }
        if (soort != Soort.BLOESEM && soort != Soort.STER) {
            xd = (random.nextDouble() - 0.5) * 0.02;
            zd = (random.nextDouble() - 0.5) * 0.02;
        }
    }

    @Override
    public void tick() {
        super.tick();
        float t = age / (float) lifetime;
        switch (soort) {
            case PLUISJE, HARTJE -> {
                xd += Math.sin(age * 0.08 + fase) * 0.0012;
                zd += Math.cos(age * 0.07 + fase) * 0.0012;
                if (soort == Soort.HARTJE) {
                    oRoll = roll;
                    roll = (float) Math.sin(age * 0.12 + fase) * 0.4f;
                }
                yd = Math.min(yd, -0.02);
            }
            case ZEEPBEL -> {
                xd += Math.sin(age * 0.05 + fase) * 0.0015;
                zd += Math.cos(age * 0.045 + fase) * 0.0015;
                if (onGround) {
                    remove();     // pop!
                }
            }
            case BLOESEM -> {
                oRoll = roll;
                roll += 0.08f;
                yd += Math.sin(age * 0.1 + fase) * 0.002;
            }
            case STER -> {
            }
        }
        if (onGround && soort != Soort.ZEEPBEL && age < lifetime - 20) {
            age = lifetime - 20;     // landed: it fades away
        }
        alpha = (soort == Soort.ZEEPBEL ? 0.85f : 1f) * Math.min(1f, Math.min(t * 8f, (1f - t) * 5f));
    }

    @Override
    protected int getLightCoords(float a) {
        return soort == Soort.STER ? 15728880 : super.getLightCoords(a);
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }
}
