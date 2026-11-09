package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.emotes.Emote;

/**
 * U-LEVEN-STAD: street life and little events:
 * <ul>
 *   <li>{@link Wandelaar}: a guh walking its pet (a pieppiepmuisje, a guh-konijntje, a light-blue blob or a duckling) through town;</li>
 *   <li>{@link Muzikant}: a street musician (ukulele, soft notes) with now and then a dancing listener;</li>
 *   <li>{@link Kar}: the ice-cream cart (soup in snowy Kandijkom) riding along the streets and ringing its bell at stops;</li>
 *   <li>{@link Picknick}: guhs picnicking on a blanket on a lawn;</li>
 *   <li>{@link Tikkertje}: three child-guhs playing tag;</li>
 *   <li>{@link Terras}: 2-4 guhs on a bench/terrace eating, drinking and chatting with speech bubbles (or a little street chat).</li>
 * </ul>
 */
final class Straat {
    private Straat() {
    }

    /** A random walkable target 'min..max' blocks away, in this town. */
    @Nullable
    static Vec3 doelBij(ServerLevel sl, Steden.Stad stad, Vec3 van, double min, double max, double maxStap, RandomSource r) {
        for (int poging = 0; poging < 6; poging++) {
            double a = r.nextDouble() * Math.PI * 2, d = min + r.nextDouble() * (max - min);
            double x = van.x + Math.cos(a) * d, z = van.z + Math.sin(a) * d;
            double g = Plekken.grondY(sl, x, van.y, z);
            if (Double.isNaN(g)) {
                continue;
            }
            Vec3 n = new Vec3(x, g, z);
            if (Steden.op(n) == stad && Plekken.loopbaar(sl, van, n, maxStap)) {
                return n;
            }
        }
        return null;
    }

    /** A point that follows behind on a trail, 'afstand' blocks back. */
    static Vec3 achter(ArrayDeque<Vec3> spoor, Vec3 kop, double afstand) {
        Vec3[] p = spoor.toArray(new Vec3[0]);
        double gehad = 0;
        Vec3 uit = p.length == 0 ? kop : p[p.length - 1];
        for (int j = p.length - 1; j > 0; j--) {
            double s = p[j].distanceTo(p[j - 1]);
            if (gehad + s >= afstand) {
                return p[j].lerp(p[j - 1], (afstand - gehad) / s);
            }
            gehad += s;
            uit = p[j - 1];
        }
        return uit;
    }

    static void spoor(ArrayDeque<Vec3> spoor, Vec3 p) {
        if (spoor.isEmpty() || spoor.peekLast().distanceToSqr(p) > 0.04) {
            spoor.addLast(p);
            if (spoor.size() > 60) {
                spoor.removeFirst();
            }
        }
    }

    // =====================================================================================================================

    static final class Wandelaar extends Groep {
        static final String[][] HUISDIEREN = {   // model, texture, anim file, idle, walk, scale
                {"pieppiepmuisje", "textures/vadsrijk/vriendjes/muisje_roze.png", "pieppiepmuisje", "idle", "walk", "1.0"},
                {"pieppiepmuisje", "textures/entity/pieppiepmuisje.png", "pieppiepmuisje", "zit", "walk", "1.0"},
                {"guh_konijntje", "textures/entity/guh_konijntje_roze.png", "guh_konijntje", "idle", "walk", "0.8"},
                {"guh_konijntje", "textures/entity/guh_konijntje_wit.png", "guh_konijntje", "idle", "walk", "0.8"},
                {BlubLook.MODEL, BlubLook.TEX, BlubLook.ANIM, BlubLook.IDLE, BlubLook.HOP, "" + BlubLook.SCHAAL_HUISDIER},   // W-MODELLEN
                {"guh_eendje", "textures/entity/guh_eendje.png", "guh_eendje", "idle", "walk", "0.8"}};
        final LevenGuh guh;
        final LevenProp dier;
        final String[] soort;
        final Loper loper;
        final ArrayDeque<Vec3> spoor = new ArrayDeque<>();
        int pauze = 20, aaien;
        /**
         * W-LEVEN: how far the pet trails behind its guh, along the guh's path (was 1.2; the user: "kan daar iets meer ruimte
         * tussen? Ongeveer 1 blok").
         */
        static final double AFSTAND = 2.2;
        /** W-LEVEN: never closer than this to its guh (the guh turning back along its trail). */
        static final double MIN_AFSTAND = 1.2;

        private Wandelaar(Steden.Stad stad, LevenGuh guh, LevenProp dier, String[] soort) {
            super(StadsLeven.nieuweSleutel("wandelaar"), Soort.WANDELAAR, stad);
            this.guh = lid(guh);
            this.dier = lid(dier);
            this.soort = soort;
            this.loper = new Loper(guh.position(), 0.062);
            spoor.addLast(dier.position());   // W-LEVEN: the trail starts at the pet, so it keeps its distance from the first step
            spoor.addLast(guh.position());
        }

        /** W-LEVEN: a pet for a walker; mice only as often as the town's pieppiepmuisje multiplier allows. */
        static String[] huisdier(Steden.Stad stad, RandomSource r) {
            String[] s = HUISDIEREN[r.nextInt(HUISDIEREN.length)];
            while (s[0].equals(Beestjes.MUIS_MODEL) && r.nextDouble() >= stad.muizen()) {
                s = HUISDIEREN[r.nextInt(HUISDIEREN.length)];
            }
            return s;
        }

        /** W-LEVEN: where the pet starts: {@link #AFSTAND} beside its guh, on walkable ground (else right next to it). */
        static Vec3 naast(ServerLevel sl, Vec3 m) {
            for (double[] o : new double[][] {{AFSTAND, 0}, {-AFSTAND, 0}, {0, AFSTAND}, {0, -AFSTAND}}) {
                Vec3 n = new Vec3(m.x + o[0], m.y, m.z + o[1]);
                double g = Plekken.grondY(sl, n.x, m.y, n.z);
                if (!Double.isNaN(g) && Math.abs(g - m.y) <= 1 && Plekken.loopbaar(sl, m, new Vec3(n.x, g, n.z), 1.0)) {
                    return new Vec3(n.x, g, n.z);
                }
            }
            return m.add(0.8, 0, 0);
        }

        @Nullable
        static Wandelaar maak(ServerLevel sl, Steden.Stad stad, Vec3 bij, RandomSource r) {
            for (Plekken.Plek p : Plekken.rond(sl, bij, 14, Soort.WANDELAAR.zicht - 8, Plekken.Soort.GROND, r)) {
                Vec3 m = p.midden();
                if (Steden.op(m) != stad || !StadsLeven.vrij(m, 2)) {
                    continue;
                }
                String[] s = huisdier(stad, r);
                LevenGuh g = StadsLeven.maakGuh(sl, m, r.nextFloat() * 360, r, false, stad);
                if (g == null) {
                    return null;
                }
                LevenProp d = LevenProp.maak(sl, naast(sl, m), 0, s[0], s[1], s[2], s[3], Float.parseFloat(s[5]), false);
                if (d == null) {
                    StadsLeven.weg(g);
                    return null;
                }
                return new Wandelaar(stad, g, d, s);
            }
            return null;
        }

        @Override
        Vec3 midden() {
            return guh.position();
        }

        @Override
        @Nullable
        String klikRegel(LevenGuh g) {
            return "wandel";
        }

        @Override
        boolean tick(ServerLevel sl) {
            t++;
            RandomSource r = sl.getRandom();
            if (aaien > 0 && --aaien == 0) {
                guh.houd(null);
            }
            if (loper.beweegt()) {
                guh.stap(loper.stap(sl));
                if (!loper.beweegt()) {
                    pauze = 30 + r.nextInt(70);   // arrived: look around a bit
                }
            } else if (--pauze <= 0) {
                if (r.nextInt(4) == 0) {   // a little pat for the pet
                    pauze = 40;
                    aaien = 32;
                    guh.kijk((float) Math.toDegrees(Math.atan2(-(dier.getX() - guh.getX()), dier.getZ() - guh.getZ())));
                    guh.houd(Emote.AAIEN);
                    sl.sendParticles(ParticleTypes.HEART, dier.getX(), dier.getY() + 0.6, dier.getZ(), 1, 0.1, 0.1, 0.1, 0);
                } else {
                    Vec3 n = doelBij(sl, stad, loper.pos, 6, 14, 1.0, r);
                    if (n != null) {
                        loper.naar(n);
                    } else {
                        pauze = 20;
                    }
                }
            }
            spoor(spoor, guh.position());
            Vec3 p = achter(spoor, guh.position(), AFSTAND);
            // W-LEVEN: when the guh turns back along its own trail, the pet steps aside instead of being walked into
            double hx = p.x - guh.getX(), hz = p.z - guh.getZ();
            if (hx * hx + hz * hz < MIN_AFSTAND * MIN_AFSTAND) {
                double dx = dier.getX() - guh.getX(), dz = dier.getZ() - guh.getZ(), l = Math.sqrt(dx * dx + dz * dz);
                if (l < 1e-3) {
                    dx = 1;
                    dz = 0;
                    l = 1;
                }
                double nx = guh.getX() + dx / l * MIN_AFSTAND, nz = guh.getZ() + dz / l * MIN_AFSTAND;
                double g = Plekken.grondY(sl, nx, dier.getY(), nz);
                p = Double.isNaN(g) || Math.abs(g - dier.getY()) > 1 ? dier.position() : new Vec3(nx, g, nz);
            }
            boolean loopt = p.distanceToSqr(dier.position()) > 0.0009;
            Vec3 zet = (soort[4].equals("hop") || soort[0].equals(BlubLook.MODEL)) && loopt ? p.add(0, Math.abs(Math.sin(t * 0.35)) * 0.25, 0) : p;
            if (loopt) {
                dier.zet(zet);
            }
            dier.anim(loopt ? soort[4] : soort[3]);
            return true;
        }
    }

    // =====================================================================================================================

    static final class Muzikant extends Groep {
        /** A small cheerful tune (semitones from F#3: the note block scale), played on the bell and harp. */
        static final int[] DEUNTJE = {12, 14, 16, 19, 16, 14, 12, -1, 9, 12, 14, 12, 9, 7, 9, -1, 12, 14, 16, 19, 21, 19, 16, 14, 12, 14, 12, 9, 12, -1, -1, -1};
        final LevenGuh guh;
        @Nullable
        LevenGuh danser;
        int noot;

        private Muzikant(Steden.Stad stad, LevenGuh guh) {
            super("muzikant:" + stad.id(), Soort.MUZIKANT, stad);
            this.guh = lid(guh);
        }

        @Nullable
        static Muzikant maak(ServerLevel sl, Steden.Stad stad, Vec3 bij, RandomSource r) {
            if (StadsLeven.heeft("muzikant:" + stad.id())) {
                return null;
            }
            for (Plekken.Plek p : Plekken.rond(sl, bij, 10, Soort.MUZIKANT.zicht - 12, Plekken.Soort.GROND, r)) {
                Vec3 m = p.midden();
                if (Steden.op(m) != stad || !StadsLeven.vrij(m, 4) || !open(sl, p.pos(), 2)) {
                    continue;
                }
                LevenGuh g = StadsLeven.maakGuh(sl, m, r.nextFloat() * 360, r, false, stad);
                if (g == null) {
                    return null;
                }
                g.wear(r.nextBoolean() ? nl.juiced.guhs.entity.GuhClothes.KERMIS_HOED : nl.juiced.guhs.entity.GuhClothes.STRAW_HAT);
                g.houd(Emote.UKELELE);
                return new Muzikant(stad, g);
            }
            return null;
        }

        @Override
        Vec3 midden() {
            return guh.position();
        }

        @Override
        @Nullable
        String klikRegel(LevenGuh g) {
            return g == guh ? "muziek" : null;
        }

        @Override
        boolean tick(ServerLevel sl) {
            t++;
            RandomSource r = sl.getRandom();
            guh.houd(Emote.UKELELE);
            if (t % 5 == 0 && Babbels.iemandHoort(sl, guh.position())) {
                int n = DEUNTJE[noot++ % DEUNTJE.length];
                if (n >= 0) {
                    float pitch = (float) Math.pow(2, (n - 12) / 12.0);
                    sl.playSound(null, guh.getX(), guh.getY() + 0.5, guh.getZ(), noot % 4 == 1 ? SoundEvents.NOTE_BLOCK_BELL.value() : SoundEvents.NOTE_BLOCK_HARP.value(),
                            SoundSource.RECORDS, 0.16f, pitch);
                    sl.sendParticles(ParticleTypes.NOTE, guh.getX() + (r.nextDouble() - 0.5) * 0.6, guh.getY() + 1.1, guh.getZ() + (r.nextDouble() - 0.5) * 0.6,
                            0, n / 24.0, 0, 0, 1);
                }
            }
            if (t % 200 == 0 && danser == null && r.nextInt(2) == 0) {   // somebody comes to dance
                Vec3 v = new Vec3(Math.cos(guh.getYRot() * Mth.DEG_TO_RAD + Math.PI / 2), 0, Math.sin(guh.getYRot() * Mth.DEG_TO_RAD + Math.PI / 2));
                Vec3 plek = guh.position().add(v.scale(2.2));
                double g = Plekken.grondY(sl, plek.x, guh.getY(), plek.z);
                if (!Double.isNaN(g) && StadsLeven.vrij(new Vec3(plek.x, g, plek.z), 1)) {
                    danser = lid(StadsLeven.maakGuh(sl, new Vec3(plek.x, g, plek.z), 0, r, r.nextInt(3) == 0, stad));
                    if (danser != null) {
                        danser.kijk((float) Math.toDegrees(Math.atan2(-(guh.getX() - plek.x), guh.getZ() - plek.z)));
                        danser.houd(Emote.DANSEN);
                    }
                }
            }
            if (danser != null && t % 600 == 0) {   // ... and goes again
                leden.remove(danser);
                StadsLeven.weg(danser);
                danser = null;
            }
            if (t % 400 == 120) {
                Babbels.praat(sl, this, List.of(guh), Babbels.regel("muziek", Babbels.MUZIEK, r), 70);
            }
            return true;
        }

        /** A bit of open floor around (the musician stands on a square, not in a doorway). */
        static boolean open(ServerLevel sl, BlockPos p, int r) {
            int n = 0, goed = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    n++;
                    double g = Plekken.grondY(sl, p.getX() + dx + 0.5, p.getY(), p.getZ() + dz + 0.5);
                    if (!Double.isNaN(g) && Math.abs(g - p.getY()) < 0.6) {
                        goed++;
                    }
                }
            }
            return goed >= n * 0.85;
        }
    }

    // =====================================================================================================================

    static final class Kar extends Groep {
        final LevenProp kar;
        final Loper loper;
        int pauze = 60, roep;

        private Kar(Steden.Stad stad, LevenProp kar) {
            super("kar:" + stad.id(), Soort.KAR, stad);
            this.kar = lid(kar);
            this.loper = new Loper(kar.position(), 0.05);
        }

        @Nullable
        static Kar maak(ServerLevel sl, Steden.Stad stad, Vec3 bij, RandomSource r) {
            if (StadsLeven.heeft("kar:" + stad.id()) || StadsLeven.nacht(sl)) {
                return null;
            }
            for (Plekken.Plek p : Plekken.rond(sl, bij, 16, Soort.KAR.zicht - 10, Plekken.Soort.GROND, r)) {
                Vec3 m = p.midden();
                if (Steden.op(m) != stad || !StadsLeven.vrij(m, 3) || !Muzikant.open(sl, p.pos(), 1)) {
                    continue;
                }
                LevenProp k = LevenProp.maak(sl, m, r.nextFloat() * 360, "ijscoguh", "textures/entity/ijscoguh.png", "ijscoguh",
                        "animation.ijscoguh.idle", 0.7f, true);
                return k == null ? null : new Kar(stad, k);
            }
            return null;
        }

        @Override
        Vec3 midden() {
            return kar.position();
        }

        @Override
        boolean tick(ServerLevel sl) {
            t++;
            RandomSource r = sl.getRandom();
            if (StadsLeven.nacht(sl) && t % 100 == 0 && !Babbels.iemandHoort(sl, kar.position())) {
                return false;   // home for the night (when nobody is watching)
            }
            if (roep > 0 && --roep == 0) {
                kar.setCustomNameVisible(false);
                kar.setCustomName(null);
            }
            if (loper.beweegt()) {
                kar.zet(loper.stap(sl));
                if (!loper.beweegt()) {   // a stop: the bell and the call
                    pauze = 140 + r.nextInt(160);
                    if (Babbels.iemandHoort(sl, kar.position())) {
                        sl.playSound(null, kar.getX(), kar.getY() + 1, kar.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 0.35f, 1.5f);
                        sl.playSound(null, kar.getX(), kar.getY() + 1, kar.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 0.3f, 1.78f);
                        if (StadsLeven.bubbelsIn(stad) < Babbels.MAX_TEGELIJK) {
                            kar.setCustomName(Babbels.bubbel(stad.koud() ? Babbels.regel("kar.soep", Babbels.SOEP, r) : Babbels.regel("kar.ijs", Babbels.IJS, r)));
                            kar.setCustomNameVisible(true);
                            roep = 80;
                        }
                    }
                }
            } else if (--pauze <= 0) {
                Vec3 n = doelBij(sl, stad, loper.pos, 8, 16, 0.55, r);
                if (n != null) {
                    loper.naar(n);
                } else {
                    pauze = 30;
                }
            }
            return true;
        }
    }

    // =====================================================================================================================

    static final class Picknick extends Groep {
        final List<LevenGuh> guhs = new ArrayList<>();
        final Vec3 m;
        int eten;

        private Picknick(Steden.Stad stad, Vec3 m) {
            super(StadsLeven.nieuweSleutel("picknick"), Soort.PICKNICK, stad);
            this.m = m;
        }

        @Nullable
        static Picknick maak(ServerLevel sl, Steden.Stad stad, Vec3 bij, RandomSource r) {
            if (StadsLeven.nacht(sl) || StadsLeven.regen(sl)) {
                return null;
            }
            for (Plekken.Plek p : Plekken.rond(sl, bij, 12, Soort.PICKNICK.zicht - 8, Plekken.Soort.GRAS, r)) {
                Vec3 m = p.midden();
                if (Steden.op(m) != stad || !StadsLeven.vrij(m, 4) || !Muzikant.open(sl, p.pos(), 2)) {
                    continue;
                }
                Picknick pk = new Picknick(stad, m);
                LevenProp kleed = LevenProp.maak(sl, m.add(0, 0.01, 0), r.nextInt(4) * 90, "vadsrijk/stadsleven/sl_kleed",
                        "textures/vadsrijk/stadsleven/sl_kleed.png", "vadsrijk/stadsleven/sl_kleed", "", 1f, true);
                pk.lid(kleed);
                int n = 2 + r.nextInt(2);
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n + r.nextDouble() * 0.4;
                    Vec3 z = m.add(Math.cos(a) * 1.05, 0.05, Math.sin(a) * 1.05);
                    LevenGuh g = StadsLeven.maakGuh(sl, z, 0, r, i == 2, stad);
                    if (g == null) {
                        continue;
                    }
                    g.zit(true);
                    g.kijk((float) Math.toDegrees(Math.atan2(-(m.x - z.x), m.z - z.z)));
                    pk.guhs.add(pk.lid(g));
                }
                if (pk.guhs.isEmpty()) {
                    pk.weg();
                    return null;
                }
                pk.duur = 20 * (150 + r.nextInt(150));
                return pk;
            }
            return null;
        }

        @Override
        Vec3 midden() {
            return m;
        }

        @Override
        @Nullable
        String klikRegel(LevenGuh g) {
            return "picknick";
        }

        @Override
        boolean tick(ServerLevel sl) {
            t++;
            RandomSource r = sl.getRandom();
            if ((duur > 0 && t > duur || StadsLeven.nacht(sl) || StadsLeven.regen(sl)) && !Babbels.iemandHoort(sl, m)) {
                return false;
            }
            if (eten > 0 && --eten == 0) {
                for (LevenGuh g : guhs) {
                    g.houd(null);
                }
            }
            if (t % 160 == 40 && eten == 0 && r.nextInt(2) == 0) {
                LevenGuh g = guhs.get(r.nextInt(guhs.size()));
                g.houd(Emote.SMAKKEN);
                eten = 72;
            }
            if (t % 90 == 0) {
                Babbels.praat(sl, this, guhs, r.nextInt(3) == 0 ? Babbels.regel("picknick", Babbels.PICKNICK, r) : Babbels.kletsregel(stad, r), 70);
            }
            return true;
        }
    }

    // =====================================================================================================================

    static final class Tikkertje extends Groep {
        static final double VELD = 4.5;
        final List<LevenGuh> kids = new ArrayList<>();
        final List<Loper> lopers = new ArrayList<>();
        final Vec3 m;
        int tikker, wacht = 40;

        private Tikkertje(Steden.Stad stad, Vec3 m) {
            super(StadsLeven.nieuweSleutel("tikkertje"), Soort.TIKKERTJE, stad);
            this.m = m;
        }

        @Nullable
        static Tikkertje maak(ServerLevel sl, Steden.Stad stad, Vec3 bij, RandomSource r) {
            if (StadsLeven.nacht(sl) || StadsLeven.regen(sl)) {
                return null;
            }
            for (Plekken.Plek p : Plekken.rond(sl, bij, 12, Soort.TIKKERTJE.zicht - 8, r.nextBoolean() ? Plekken.Soort.GRAS : Plekken.Soort.GROND, r)) {
                Vec3 m = p.midden();
                if (Steden.op(m) != stad || !StadsLeven.vrij(m, 5) || !Muzikant.open(sl, p.pos(), 3)) {
                    continue;
                }
                Tikkertje tk = new Tikkertje(stad, m);
                for (int i = 0; i < 3; i++) {
                    double a = i * 2.1;
                    Vec3 z = m.add(Math.cos(a) * 2, 0, Math.sin(a) * 2);
                    double g = Plekken.grondY(sl, z.x, m.y, z.z);
                    if (Double.isNaN(g)) {
                        z = m;
                    } else {
                        z = new Vec3(z.x, g, z.z);
                    }
                    LevenGuh k = StadsLeven.maakGuh(sl, z, 0, r, true, stad);
                    if (k != null) {
                        tk.kids.add(tk.lid(k));
                        tk.lopers.add(new Loper(z, 0.15));
                    }
                }
                if (tk.kids.size() < 2) {
                    tk.weg();
                    return null;
                }
                tk.duur = 20 * (120 + r.nextInt(120));
                return tk;
            }
            return null;
        }

        @Override
        Vec3 midden() {
            return m;
        }

        @Override
        @Nullable
        String klikRegel(LevenGuh g) {
            return "kind";
        }

        @Override
        boolean tick(ServerLevel sl) {
            t++;
            RandomSource r = sl.getRandom();
            if ((duur > 0 && t > duur || StadsLeven.nacht(sl) || StadsLeven.regen(sl)) && !Babbels.iemandHoort(sl, m)) {
                return false;
            }
            Loper jager = lopers.get(tikker);
            for (int i = 0; i < kids.size(); i++) {
                Loper l = lopers.get(i);
                LevenGuh k = kids.get(i);
                if (i == tikker && wacht > 0) {
                    wacht--;   // counting to three
                    continue;
                }
                if (!l.beweegt() || t % 10 == i) {
                    Vec3 doel;
                    if (i == tikker) {   // chase the nearest
                        Loper prooi = null;
                        double best = 1e9;
                        for (int j = 0; j < lopers.size(); j++) {
                            double d = j == i ? 1e9 : lopers.get(j).pos.distanceToSqr(l.pos);
                            if (d < best) {
                                best = d;
                                prooi = lopers.get(j);
                            }
                        }
                        doel = prooi == null ? m : prooi.pos;
                        l.snelheid = 0.155;
                    } else {   // run away from the tagger, but stay on the field
                        Vec3 weg = l.pos.subtract(jager.pos).multiply(1, 0, 1);
                        if (weg.lengthSqr() < 1e-3) {
                            weg = new Vec3(r.nextDouble() - 0.5, 0, r.nextDouble() - 0.5);
                        }
                        Vec3 d = weg.normalize().scale(2.5).add(r.nextDouble() - 0.5, 0, r.nextDouble() - 0.5);
                        doel = l.pos.add(d);
                        Vec3 vanM = doel.subtract(m).multiply(1, 0, 1);
                        if (vanM.length() > VELD) {   // the edge: swerve round along it
                            doel = m.add(vanM.normalize().scale(VELD - 0.8)).add(new Vec3(-vanM.z, 0, vanM.x).normalize().scale(1.5));
                        }
                        l.snelheid = 0.13;
                    }
                    double g = Plekken.grondY(sl, doel.x, l.pos.y, doel.z);
                    l.naar(Double.isNaN(g) ? m : new Vec3(doel.x, g, doel.z));
                }
                k.stap(l.stap(sl));
            }
            // a tag!
            for (int j = 0; j < lopers.size(); j++) {
                if (j != tikker && wacht == 0 && lopers.get(j).pos.distanceToSqr(jager.pos) < 0.6 * 0.6) {
                    LevenGuh tikte = kids.get(tikker);
                    tikker = j;
                    wacht = 40;
                    if (Babbels.iemandHoort(sl, m)) {
                        sl.playSound(null, tikte.getX(), tikte.getY(), tikte.getZ(), Beestjes.geluid("creche.babygiechel"), SoundSource.NEUTRAL, 0.35f,
                                1.0f + r.nextFloat() * 0.3f);
                        if (StadsLeven.bubbelsIn(stad) < Babbels.MAX_TEGELIJK && !tikte.praat()) {
                            tikte.zeg(Babbels.bubbel(Component.translatable("vadsrijk.stadsleven.tikkie")), 50);
                        }
                    }
                    break;
                }
            }
            return true;
        }
    }

    // =====================================================================================================================

    static final class Terras extends Groep {
        final List<LevenGuh> guhs = new ArrayList<>();
        final Vec3 m;
        final boolean zitten;
        int eten;

        private Terras(String sleutel, Steden.Stad stad, Vec3 m, boolean zitten) {
            super(sleutel, Soort.TERRAS, stad);
            this.m = m;
            this.zitten = zitten;
        }

        @Nullable
        static Terras maak(ServerLevel sl, Steden.Stad stad, Vec3 bij, RandomSource r) {
            // a cluster of benches/chairs: 2-4 seats within 3 blocks
            List<Plekken.Plek> zitten = Plekken.rond(sl, bij, 4, Soort.TERRAS.zicht - 6, Plekken.Soort.ZIT, r);
            for (Plekken.Plek p : zitten) {
                String sleutel = "terras@" + p.pos().toShortString();
                if (Steden.op(p.midden()) != stad || StadsLeven.heeft(sleutel) || !StadsLeven.vrij(p.midden(), 1.2)) {
                    continue;
                }
                List<Plekken.Plek> cluster = new ArrayList<>();
                for (Plekken.Plek q : zitten) {
                    if (q.pos().distSqr(p.pos()) <= 9 && Math.abs(q.pos().getY() - p.pos().getY()) <= 1 && StadsLeven.vrij(q.midden(), 0.6)) {
                        cluster.add(q);
                    }
                }
                if (cluster.size() < 2) {
                    continue;
                }
                int n = Math.min(cluster.size(), 2 + r.nextInt(3));
                Vec3 m = Vec3.ZERO;
                Terras tr = new Terras(sleutel, stad, p.midden(), true);
                for (int i = 0; i < n; i++) {
                    Plekken.Plek q = cluster.get(i);
                    Vec3 z = new Vec3(q.pos().getX() + 0.5, q.pos().getY() + 0.3, q.pos().getZ() + 0.5);
                    LevenGuh g = StadsLeven.maakGuh(sl, z, q.yaw(), r, false, stad);
                    if (g == null) {
                        continue;
                    }
                    g.zit(true);
                    g.kijk(q.yaw());
                    tr.guhs.add(tr.lid(g));
                    m = m.add(z);
                }
                if (tr.guhs.size() < 2) {
                    tr.weg();
                    continue;
                }
                return tr;
            }
            // no benches here: a little street chat of 2-3 guhs standing together
            for (Plekken.Plek p : Plekken.rond(sl, bij, 10, Soort.TERRAS.zicht - 8, Plekken.Soort.GROND, r)) {
                Vec3 m = p.midden();
                if (Steden.op(m) != stad || !StadsLeven.vrij(m, 3) || !Muzikant.open(sl, p.pos(), 1)) {
                    continue;
                }
                Terras tr = new Terras(StadsLeven.nieuweSleutel("kletsen"), stad, m, false);
                int n = 2 + r.nextInt(2);
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n;
                    Vec3 z = m.add(Math.cos(a) * 0.9, 0, Math.sin(a) * 0.9);
                    double g = Plekken.grondY(sl, z.x, m.y, z.z);
                    if (Double.isNaN(g)) {
                        continue;
                    }
                    z = new Vec3(z.x, g, z.z);
                    LevenGuh gh = StadsLeven.maakGuh(sl, z, 0, r, false, stad);
                    if (gh == null) {
                        continue;
                    }
                    gh.kijk((float) Math.toDegrees(Math.atan2(-(m.x - z.x), m.z - z.z)));
                    tr.guhs.add(tr.lid(gh));
                }
                if (tr.guhs.size() < 2) {
                    tr.weg();
                    return null;
                }
                return tr;
            }
            return null;
        }

        @Override
        Vec3 midden() {
            return m;
        }

        @Override
        @Nullable
        String klikRegel(LevenGuh g) {
            return "klets";
        }

        @Override
        boolean tick(ServerLevel sl) {
            t++;
            RandomSource r = sl.getRandom();
            if (eten > 0 && --eten == 0) {
                for (LevenGuh g : guhs) {
                    g.houd(null);
                }
            }
            if (zitten && t % 140 == 70 && eten == 0 && r.nextInt(3) == 0) {   // a bite / a sip
                guhs.get(r.nextInt(guhs.size())).houd(Emote.SMAKKEN);
                eten = 72;
            }
            if (!zitten && t % 220 == 100 && eten == 0 && r.nextInt(3) == 0) {   // a laugh / a little dance while chatting
                guhs.get(r.nextInt(guhs.size())).houd(r.nextBoolean() ? Emote.HARTJES : Emote.VAHOEG);
                eten = 40;
            }
            if (t % 60 == 0) {
                Babbels.praat(sl, this, guhs, Babbels.kletsregel(stad, r), 80);
            }
            return true;
        }
    }
}
