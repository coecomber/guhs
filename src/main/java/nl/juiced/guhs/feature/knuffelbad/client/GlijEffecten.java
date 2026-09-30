package nl.juiced.guhs.feature.knuffelbad.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.knuffelbad.BadeendjeEntity;
import nl.juiced.guhs.feature.knuffelbad.GlijPad;
import nl.juiced.guhs.feature.knuffelbad.GlijRit;
import nl.juiced.guhs.feature.knuffelbad.Glijbaan;
import nl.juiced.guhs.feature.knuffelbad.KnuffelbadFeature;
import nl.juiced.guhs.feature.knuffelbad.ZwembandjeEntity;

/**
 * What you see and hear on a slide (every client tick of a zwembandje): water spraying off the ring, foam swirling in a
 * funnel, sparkles in the star tunnel, the rush of the water (a sound that follows your speed), the big PLONS at the end
 * (a fountain of drops, the camera shakes), and the ducks you pick up squeak at once (your own game sees it first).
 */
final class GlijEffecten {
    private static final class Staat {
        boolean plons;
        int vorigProf = -1;
        GlijGeluid geluid;
    }

    private static final Map<Integer, Staat> STATEN = new HashMap<>();

    private GlijEffecten() {
    }

    static void tick(ZwembandjeEntity ring) {
        Minecraft mc = Minecraft.getInstance();
        if (!(ring.level() instanceof ClientLevel level)) {
            return;
        }
        if (STATEN.size() > 16) {
            STATEN.keySet().removeIf(id -> level.getEntity(id) == null);
        }
        Staat staat = STATEN.computeIfAbsent(ring.getId(), id -> new Staat());
        GlijPad pad = ring.baan().pad();
        RandomSource r = level.getRandom();
        boolean glijdt = ring.fase() == ZwembandjeEntity.GLIJDT || ring.fase() == ZwembandjeEntity.PLONS;
        double s = pad.sAt(ring.tau);
        double v = glijdt ? pad.snelheid(ring.tau) : 0;
        GlijPad.Stand st = ring.stand(ring.tau, ring.lat);
        Vec3 m = st.midden(pad.ring);
        Vec3 t = st.tangent(), n = st.normal(), right = st.rechts();
        int fx = st.fx(), prof = st.prof();
        boolean eigen = ring.eigen;
        int dicht = eigen ? 1 : 2;

        if (!glijdt) {
            if (r.nextInt(6) == 0) {             // waiting at the top: a few bubbles round the ring
                level.addParticle(KnuffelbadFeature.ZEEPBELLETJE.get(), m.x + (r.nextDouble() - 0.5), m.y + 0.3, m.z + (r.nextDouble() - 0.5), 0, 0.02, 0);
            }
            return;
        }
        // water spraying off both sides of the ring (more the faster you go)
        if (prof != GlijPad.LUCHT && prof != GlijPad.WATER && v > 0.2 && ring.tickCount % dicht == 0) {
            int count = v > 0.8 ? 3 : v > 0.5 ? 2 : 1;
            for (int k = 0; k < count; k++) {
                double side = r.nextBoolean() ? 1 : -1;
                Vec3 p = m.add(right.scale(side * 0.55)).subtract(t.scale(0.3)).add(n.scale(-0.1));
                Vec3 sp = right.scale(side * (0.05 + v * 0.12)).add(n.scale(0.05 + v * 0.1)).subtract(t.scale(v * 0.3));
                level.addParticle(KnuffelbadFeature.PLONS.get(), p.x, p.y, p.z, sp.x, sp.y, sp.z);
            }
        }
        // round the funnel: pink foam swirling down to its mouth
        if ((fx & GlijPad.FX_DRAAI) != 0 && eigen && ring.tickCount % 2 == 0) {
            Vec3 p = m.add(t.scale(2 + r.nextDouble() * 3)).add(right.scale((r.nextDouble() - 0.5) * 3)).add(n.scale(0.1));
            level.addParticle(KnuffelbadFeature.SCHUIMVLOKJE.get(), p.x, p.y, p.z, t.x * 0.1, 0.01, t.z * 0.1);
        }
        // the star tunnel: sparkles all round you
        if ((fx & GlijPad.FX_STERREN) != 0 && eigen) {
            for (int k = 0; k < 2; k++) {
                Vec3 p = m.add(t.scale(1 + r.nextDouble() * 6)).add(right.scale((r.nextDouble() - 0.5) * 3)).add(n.scale(r.nextDouble() * 2.6));
                level.addParticle(KnuffelbadFeature.GLINSTERING.get(), p.x, p.y, p.z, 0, 0, 0);
            }
        }
        // falling: a whoosh
        if (prof == GlijPad.LUCHT && staat.vorigProf != GlijPad.LUCHT && eigen) {
            level.playLocalSound(m.x, m.y, m.z, net.minecraft.sounds.SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.5f, 1.4f, false);
        }
        staat.vorigProf = prof;
        // THE PLONS
        Double plonsS = pad.merk.get("plons");
        if (!staat.plons && plonsS != null && s >= plonsS) {
            staat.plons = true;
            plons(level, ring, m, pad, r);
        }
        if (eigen) {
            if (staat.geluid == null || staat.geluid.isStopped()) {
                staat.geluid = new GlijGeluid(ring);
                mc.getSoundManager().play(staat.geluid);
            }
            // the ducks you ride through: your own game picks them up at once (the server agrees a moment later)
            for (BadeendjeEntity duck : level.getEntitiesOfClass(BadeendjeEntity.class, new AABB(m, m).inflate(2.0))) {
                if (duck.vanRit() && !duck.lokaalGepakt && duck.position().add(0, 0.12, 0).distanceToSqr(m) < GlijRit.PAK * GlijRit.PAK) {
                    duck.lokaalGepakt = true;
                    level.playLocalSound(duck.getX(), duck.getY(), duck.getZ(), KnuffelbadFeature.EENDJE_PIEP.get(), SoundSource.PLAYERS, 1f,
                            0.95f + r.nextFloat() * 0.2f, false);
                    boolean bijzonder = duck.getSoort().speciaal();
                    for (int k = 0; k < (bijzonder ? 14 : 7); k++) {
                        level.addParticle(bijzonder ? KnuffelbadFeature.GLINSTERING.get() : KnuffelbadFeature.ZEEPBELLETJE.get(),
                                duck.getX() + (r.nextDouble() - 0.5) * 0.6, duck.getY() + 0.3 + r.nextDouble() * 0.4, duck.getZ() + (r.nextDouble() - 0.5) * 0.6,
                                (r.nextDouble() - 0.5) * 0.05, 0.05, (r.nextDouble() - 0.5) * 0.05);
                    }
                }
            }
        }
    }

    /** The big splash at the end: a fountain of drops (and foam in the foam bath), the sound, the camera shakes. */
    private static void plons(ClientLevel level, ZwembandjeEntity ring, Vec3 m, GlijPad pad, RandomSource r) {
        boolean groot = ring.glijbaan() == Glijbaan.GROTE_PLONS;
        boolean schuim = ring.glijbaan() == Glijbaan.ROZE_TRECHTER;
        int n = groot ? 140 : 80;
        for (int k = 0; k < n; k++) {
            double a = r.nextDouble() * Math.PI * 2, sp = 0.1 + r.nextDouble() * (groot ? 0.45 : 0.3);
            level.addParticle(schuim && k % 2 == 0 ? KnuffelbadFeature.SCHUIMVLOKJE.get() : KnuffelbadFeature.PLONS.get(), m.x, m.y + 0.2, m.z,
                    Math.cos(a) * sp, 0.25 + r.nextDouble() * (groot ? 0.6 : 0.4), Math.sin(a) * sp);
        }
        for (int k = 0; k < 40; k++) {
            level.addParticle(ParticleTypes.SPLASH, m.x + (r.nextDouble() - 0.5) * 3, m.y + 0.3, m.z + (r.nextDouble() - 0.5) * 3, 0, 0.2, 0);
        }
        if (ring.eigen) {
            level.playLocalSound(m.x, m.y, m.z, KnuffelbadFeature.PLONS_GELUID.get(), SoundSource.PLAYERS, groot ? 1.6f : 1.2f, groot ? 0.8f : 1f, false);
            if (schuim) {
                level.playLocalSound(m.x, m.y, m.z, KnuffelbadFeature.SCHUIM.get(), SoundSource.PLAYERS, 1f, 1f, false);
            }
            GlijCamera.schud(groot ? 1.6f : 1.0f);
        }
    }

    /** The rush of the water under the ring: louder and higher the faster you go; quiet in the air; gone at the end. */
    static final class GlijGeluid extends AbstractTickableSoundInstance {
        private final ZwembandjeEntity ring;

        GlijGeluid(ZwembandjeEntity ring) {
            super(KnuffelbadFeature.GLIJDEN.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.ring = ring;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.relative = false;
            this.x = ring.getX();
            this.y = ring.getY();
            this.z = ring.getZ();
        }

        @Override
        public void tick() {
            if (ring.isRemoved() || !ring.eigen || ring.fase() == ZwembandjeEntity.WACHT) {
                stop();
                return;
            }
            GlijPad pad = ring.baan().pad();
            double v = pad.snelheid(ring.tau);
            int prof = pad.profiel(pad.sAt(ring.tau));
            float doel = prof == GlijPad.LUCHT ? 0.15f : prof == GlijPad.WATER || ring.fase() == ZwembandjeEntity.PLONS ? 0f
                    : (float) Mth.clamp(0.3 + v * 0.6, 0.3, 0.95);
            volume = Mth.lerp(0.25f, volume, doel);
            pitch = (float) Mth.clamp(0.75 + v * 0.45, 0.7, 1.35);
            x = ring.getX();
            y = ring.getY();
            z = ring.getZ();
        }
    }
}
