package nl.juiced.guhs.feature.bio.blokkenwolk.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.juiced.guhs.feature.bio.blokkenwolk.BlokkenWolkSlice;
import nl.juiced.guhs.feature.bio.blokkenwolk.Waterval;

/**
 * Foam, mist and a soft rush at the foot of every BIG waterfall near the camera (the rule: {@link Waterval}). Client only;
 * the server knows nothing of it. It works for any water in any biome and dimension, so also for a waterfall a player
 * builds; where no water falls {@link Waterval#GROOT} blocks it does nothing at all.
 * <p>
 * How it finds the falls without scanning the neighbourhood every tick: every tick it looks down {@link #KOLOMMEN} columns
 * of a fixed walk over the square around the camera (half of them in the near square, so what is close is seen first) and
 * remembers the foot it finds per column. A column near you is looked at again about every two seconds, one further away
 * about every eight; a foot that is gone, or out of range, is forgotten. All the effects come from that small list.
 */
final class WatervalEffecten {
    /** The square that is searched: this far from the camera (blocks); the near square, searched four times as often. */
    static final int STRAAL = 24, DICHTBIJ = 12;
    /** Columns looked at per tick, and how far above and below the camera a column is searched. */
    static final int KOLOMMEN = 32, OMHOOG = 20, OMLAAG = 28;
    /** Feet remembered at most. */
    static final int MAX_VOETEN = 96;
    /** Foam and mist are made within this distance; the sound is heard within {@link #HOORBAAR}. */
    static final double ZICHTBAAR = 40, HOORBAAR = 30;

    private static final class Voet {
        final int x, y, z, hoogte;
        final boolean inWater;

        Voet(int x, int z, Waterval.Voet v) {
            this.x = x;
            this.y = v.y();
            this.z = z;
            this.hoogte = v.hoogte();
            this.inWater = v.inWater();
        }
    }

    private static final Long2ObjectMap<Voet> VOETEN = new Long2ObjectOpenHashMap<>();
    private static ClientLevel wereld;
    private static int ver, dichtbij, tik;
    private static Ruis ruis;

    private WatervalEffecten() {
    }

    private static long sleutel(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            VOETEN.clear();
            wereld = null;
            return;
        }
        if (mc.isPaused() || !mc.gameRenderer.getMainCamera().isInitialized()) {
            return;
        }
        if (level != wereld) {
            VOETEN.clear();
            wereld = level;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        int cx = Mth.floor(cam.x), cy = Mth.floor(cam.y), cz = Mth.floor(cam.z);
        tik++;
        zoek(level, cx, cy, cz);
        if (tik % 40 == 0) {
            int ver2 = (STRAAL + 8) * (STRAAL + 8);
            VOETEN.values().removeIf(v -> (v.x - cx) * (v.x - cx) + (v.z - cz) * (v.z - cz) > ver2 || Math.abs(v.y - cy) > OMLAAG + 8);
        }
        if (VOETEN.isEmpty()) {
            return;
        }
        double sterkte = 0, sx = 0, sy = 0, sz = 0;
        RandomSource r = level.getRandom();
        for (Voet v : VOETEN.values()) {
            double dx = v.x + 0.5 - cam.x, dy = v.y - cam.y, dz = v.z + 0.5 - cam.z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < ZICHTBAAR) {
                spat(level, v, r, d);
            }
            if (d < HOORBAAR) {
                double w = Math.min(v.hoogte, 12) / 12.0 * (1 - d / HOORBAAR) * (1 - d / HOORBAAR);
                sterkte += w;
                sx += (v.x + 0.5) * w;
                sy += v.y * w;
                sz += (v.z + 0.5) * w;
            }
        }
        if (sterkte > 0.001) {
            if (ruis == null || ruis.isStopped()) {
                ruis = new Ruis(sx / sterkte, sy / sterkte, sz / sterkte);
                mc.getSoundManager().play(ruis);
            }
            ruis.doel(sx / sterkte, sy / sterkte, sz / sterkte, (float) Math.min(0.6, 0.45 * Math.sqrt(sterkte)));
        }
    }

    /** This tick's columns: a walk that visits every column of the square once before it starts again. */
    private static void zoek(ClientLevel level, int cx, int cy, int cz) {
        int breedVer = 2 * STRAAL + 1, breedBij = 2 * DICHTBIJ + 1;
        for (int k = 0; k < KOLOMMEN; k++) {
            int dx, dz;
            if ((k & 1) == 0) {
                dichtbij = (dichtbij + 233) % (breedBij * breedBij);       // (233 and 1009 share no factor with the squares' sizes)
                dx = dichtbij % breedBij - DICHTBIJ;
                dz = dichtbij / breedBij - DICHTBIJ;
            } else {
                ver = (ver + 1009) % (breedVer * breedVer);
                dx = ver % breedVer - STRAAL;
                dz = ver / breedVer - STRAAL;
            }
            int x = cx + dx, z = cz + dz;
            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }
            // nothing in this column above its highest block or fluid: start there
            int boven = Math.min(cy + OMHOOG, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z));
            int onder = Math.max(cy - OMLAAG, level.getMinY());
            Waterval.Voet v = boven < onder ? null : Waterval.zoek(level, x, z, boven, onder);
            long sleutel = sleutel(x, z);
            if (v != null && v.groot()) {
                if (VOETEN.size() < MAX_VOETEN || VOETEN.containsKey(sleutel)) {
                    VOETEN.put(sleutel, new Voet(x, z, v));
                }
            } else {
                VOETEN.remove(sleutel);
            }
        }
    }

    /** Foam hopping away over the water at the foot, and now and then a puff of mist; less of both further away. */
    private static void spat(ClientLevel level, Voet v, RandomSource r, double afstand) {
        float kans = afstand < 16 ? 0.7f : afstand < 28 ? 0.35f : 0.15f;
        double y = v.y + (v.inWater ? 0.0 : 0.15);
        if (r.nextFloat() < kans) {
            double hoek = r.nextDouble() * Math.PI * 2, v0 = 0.04 + r.nextDouble() * 0.09;
            level.addParticle(BlokkenWolkSlice.WATERVAL_SCHUIM.get(), v.x + 0.15 + r.nextDouble() * 0.7, y + r.nextDouble() * 0.2,
                    v.z + 0.15 + r.nextDouble() * 0.7, Math.cos(hoek) * v0, 0.08 + r.nextDouble() * 0.12, Math.sin(hoek) * v0);
        }
        if (r.nextFloat() < kans * (0.04f + Math.min(v.hoogte, 12) * 0.008f)) {
            level.addParticle(BlokkenWolkSlice.WATERVAL_NEVEL.get(), v.x + r.nextDouble(), y + 0.3 + r.nextDouble() * 0.9, v.z + r.nextDouble(),
                    (r.nextDouble() - 0.5) * 0.02, 0.008 + r.nextDouble() * 0.012, (r.nextDouble() - 0.5) * 0.02);
        }
    }

    /** One soft loop for all the falls you can hear: it sits at their weighted middle and fades with their distance. */
    private static final class Ruis extends AbstractTickableSoundInstance {
        private double dx, dy, dz;
        private float doel;
        private int stil;

        Ruis(double x, double y, double z) {
            super(BlokkenWolkSlice.WATERVAL_RUIS.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.relative = false;
            this.attenuation = SoundInstance.Attenuation.NONE;   // (the distance is in the volume, see onTick)
            this.x = this.dx = x;
            this.y = this.dy = y;
            this.z = this.dz = z;
        }

        void doel(double x, double y, double z, float volume) {
            this.dx = x;
            this.dy = y;
            this.dz = z;
            this.doel = volume;
            this.stil = 0;
        }

        @Override
        public void tick() {
            if (Minecraft.getInstance().level != wereld || ++stil > 3) {
                doel = 0;       // (nobody set a target for a few ticks: no fall in earshot any more)
            }
            volume = Mth.lerp(0.08f, volume, doel);
            x = Mth.lerp(0.15, x, dx);
            y = Mth.lerp(0.15, y, dy);
            z = Mth.lerp(0.15, z, dz);
            if (doel == 0 && volume < 0.004f) {
                stop();
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
