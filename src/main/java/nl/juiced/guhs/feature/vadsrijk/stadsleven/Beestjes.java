package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;

/**
 * U-LEVEN-STAD: the little animals of a guh town:
 * <ul>
 *   <li>{@link Diertje}: a pieppiepmuisje scurrying about, or (~30 %) a light-blue guh-blob bouncing (W-MODELLEN: the Blub-guh
 *       model of the dream and the tamable Blubs, see {@link BlubLook});</li>
 *   <li>{@link Kat}: a guh's cat on a roof (pink collar);</li>
 *   <li>{@link Eendjes}: a mother duck with three ducklings swimming in a line in a pond, canal or harbour;</li>
 *   <li>{@link Vogel}: a little bird landing on a lantern or a bench, pecking, and flying on.</li>
 * </ul>
 */
final class Beestjes {
    static final String MUIS_MODEL = "pieppiepmuisje", MUIS_TEX = "textures/entity/pieppiepmuisje.png",
            MUIS_ROZE = "textures/vadsrijk/vriendjes/muisje_roze.png";
    /** Share of blobs among the town critters. */
    static final float BLOB_KANS = 0.3f;

    private Beestjes() {
    }

    /**
     * W-LEVEN: the critter cap split in two. Blobs keep their old share ({@link #BLOB_KANS} of the DIERTJE cap: 3 at VOL); mice get
     * the rest times the town's multiplier ({@link Steden.Stad#muizen}: 0.3 -> 2 at VOL, Kandijkom 1.0 -> 6, as before).
     */
    static int blobMax(Steden.Stad stad, StadsLeven.Niveau n) {
        return Math.round(Groep.Soort.DIERTJE.max(n) * BLOB_KANS);
    }

    static int muisMax(Steden.Stad stad, StadsLeven.Niveau n) {
        int max = Groep.Soort.DIERTJE.max(n);
        return Math.min(max - blobMax(stad, n), (int) Math.round(max * (1 - BLOB_KANS) * stad.muizen()));
    }

    /** W-LEVEN: the next town critter: a blob, a mouse, or none (both at their cap). */
    @Nullable
    static Boolean volgendeIsBlob(Steden.Stad stad, StadsLeven.Niveau n, int blobs, int muizen, RandomSource r) {
        boolean blobMag = blobs < blobMax(stad, n), muisMag = muizen < muisMax(stad, n);
        if (!blobMag && !muisMag) {
            return null;
        }
        if (blobMag != muisMag) {
            return blobMag;
        }
        int bOver = blobMax(stad, n) - blobs, mOver = muisMax(stad, n) - muizen;
        return r.nextInt(bOver + mOver) < bOver;
    }

    static SoundEvent geluid(String id) {
        return SoundEvent.createVariableRangeEvent(Guhs.id(id));
    }

    static SoundEvent geluidMc(String id) {
        return SoundEvent.createVariableRangeEvent(net.minecraft.resources.Identifier.withDefaultNamespace(id));
    }

    // =====================================================================================================================

    /** A pieppiepmuisje or a light-blue guh-blob. */
    static final class Diertje extends Groep {
        final boolean blob;
        final LevenProp f;
        final Loper loper;
        int pauze;

        private Diertje(String sleutel, Steden.Stad stad, boolean blob, LevenProp f) {
            super(sleutel, Soort.DIERTJE, stad);
            this.blob = blob;
            this.f = lid(f);
            this.loper = new Loper(f.position(), blob ? 0.075 : 0.17);
            this.loper.huppel = blob ? 0.38 : 0.06;
            this.pauze = 20;
        }

        @Nullable
        static Diertje maak(ServerLevel sl, Steden.Stad stad, Vec3 bij, RandomSource r, boolean blob) {
            for (Plekken.Plek p : Plekken.rond(sl, bij, 6, Soort.DIERTJE.zicht - 6, Plekken.Soort.GROND, r)) {
                if (Steden.op(p.midden()) != stad) {
                    continue;
                }
                LevenProp f = blob
                        ? LevenProp.maak(sl, p.midden(), r.nextFloat() * 360, BlubLook.MODEL, BlubLook.textuur(r), BlubLook.ANIM, BlubLook.IDLE,   // W-MODELLEN
                                BlubLook.SCHAAL, false)
                        : LevenProp.maak(sl, p.midden(), r.nextFloat() * 360, MUIS_MODEL, r.nextInt(4) == 0 ? MUIS_ROZE : MUIS_TEX, MUIS_MODEL, "idle",
                                0.9f + r.nextFloat() * 0.2f, false);
                if (f == null) {
                    return null;
                }
                sl.sendParticles(blob ? ParticleTypes.SPLASH : ParticleTypes.POOF, p.midden().x, p.midden().y + 0.2, p.midden().z, 3, 0.15, 0.1, 0.15, 0.01);
                return new Diertje(StadsLeven.nieuweSleutel("diertje"), stad, blob, f);
            }
            return null;
        }

        @Override
        Vec3 midden() {
            return f.position();
        }

        @Override
        boolean tick(ServerLevel sl) {
            t++;
            RandomSource r = sl.getRandom();
            if (loper.beweegt()) {
                f.zet(loper.stap(sl));
                if (!loper.beweegt()) {
                    pauze = blob ? 30 + r.nextInt(70) : 15 + r.nextInt(60);
                    f.anim(blob ? (r.nextInt(3) == 0 ? BlubLook.BLIJ : BlubLook.IDLE) : pick(r, "idle", "eet", "zit", "piep", "blij"));
                }
                return true;
            }
            if (--pauze > 0) {
                return true;
            }
            // a new little dash / hop: 2-6 blocks, staying on the ground and in town
            for (int poging = 0; poging < 4; poging++) {
                double a = r.nextDouble() * Math.PI * 2, d = 2 + r.nextDouble() * 4;
                Vec3 van = loper.pos;
                Vec3 naar = new Vec3(van.x + Math.cos(a) * d, van.y, van.z + Math.sin(a) * d);
                double g = Plekken.grondY(sl, naar.x, van.y, naar.z);
                if (Double.isNaN(g)) {
                    continue;
                }
                naar = new Vec3(naar.x, g, naar.z);
                if (Steden.op(naar) == stad && Plekken.loopbaar(sl, van, naar, 1.0)) {
                    loper.naar(naar);
                    f.anim(blob ? BlubLook.HOP : "walk");   // W-MODELLEN
                    if (r.nextInt(blob ? 5 : 4) == 0 && Babbels.iemandHoort(sl, f.position())) {
                        sl.playSound(null, f.getX(), f.getY(), f.getZ(), blob ? geluid("blubguh.huppelen") : geluid("entity.pieppiepmuisje.ambient"),
                                SoundSource.NEUTRAL, 0.18f, blob ? 1.3f + r.nextFloat() * 0.3f : 1.1f + r.nextFloat() * 0.3f);
                    }
                    return true;
                }
            }
            pauze = 20;
            return true;
        }
    }

    static String pick(RandomSource r, String... s) {
        return s[r.nextInt(s.length)];
    }

    // =====================================================================================================================

    /** A guh's cat on a roof. */
    static final class Kat extends Groep {
        final LevenKat kat;
        int volgende;

        private Kat(Steden.Stad stad, LevenKat kat) {
            super(StadsLeven.nieuweSleutel("kat"), Soort.KAT, stad);
            this.kat = lid(kat);
            this.volgende = 200;
        }

        @Nullable
        static Kat maak(ServerLevel sl, Steden.Stad stad, Vec3 bij, RandomSource r) {
            for (Plekken.Plek p : Plekken.rond(sl, bij, 8, Soort.KAT.zicht - 4, Plekken.Soort.DAK, r)) {
                if (Steden.op(p.midden()) != stad || !StadsLeven.vrij(p.midden(), 3)) {
                    continue;
                }
                LevenKat k = StadslevenRegistry.KAT.get().create(sl, EntitySpawnReason.EVENT);
                if (k == null) {
                    return null;
                }
                k.kleed(sl);
                Vec3 m = p.midden();
                k.snapTo(m.x, m.y, m.z, r.nextFloat() * 360, 0);
                k.addTag(StadsLeven.TAG);
                StadsLeven.EIGEN.add(k.getUUID());
                if (!sl.addFreshEntity(k)) {
                    StadsLeven.EIGEN.remove(k.getUUID());
                    return null;
                }
                return new Kat(stad, k);
            }
            return null;
        }

        @Override
        Vec3 midden() {
            return kat.position();
        }

        @Override
        boolean tick(ServerLevel sl) {
            t++;
            if (t >= volgende) {
                RandomSource r = sl.getRandom();
                volgende = t + 160 + r.nextInt(300);
                int k = r.nextInt(5);
                if (k == 0) {   // stretch: stand up for a moment
                    kat.setInSittingPose(false);
                } else {
                    kat.setInSittingPose(true);
                    float yaw = kat.getYRot() + (r.nextFloat() - 0.5f) * 120;
                    kat.setYRot(yaw);
                    kat.setYHeadRot(yaw);
                    kat.setYBodyRot(yaw);
                }
                if (r.nextInt(4) == 0 && Babbels.iemandHoort(sl, kat.position())) {
                    sl.playSound(null, kat.getX(), kat.getY(), kat.getZ(), geluidMc("entity.cat.ambient"), SoundSource.NEUTRAL, 0.25f, 1.1f + r.nextFloat() * 0.2f);
                }
            }
            return true;
        }
    }

    // =====================================================================================================================

    /** A mother duck and her ducklings in a line on the water. */
    static final class Eendjes extends Groep {
        static final String MODEL = "guh_eendje", TEX = "textures/entity/guh_eendje.png", KUIKEN = "textures/entity/guh_eendje_kuiken.png";
        final LevenProp moeder;
        final List<LevenProp> kuikens = new ArrayList<>();
        final ArrayDeque<Vec3> spoor = new ArrayDeque<>();
        final double waterY;
        Vec3 pos;
        @Nullable
        Vec3 doel;
        int pauze = 40;

        private Eendjes(Steden.Stad stad, LevenProp moeder, double waterY) {
            super(StadsLeven.nieuweSleutel("eendjes"), Soort.EENDJES, stad);
            this.moeder = lid(moeder);
            this.waterY = waterY;
            this.pos = moeder.position();
        }

        @Nullable
        static Eendjes maak(ServerLevel sl, Steden.Stad stad, Vec3 bij, RandomSource r) {
            for (Plekken.Plek p : Plekken.rond(sl, bij, 8, Soort.EENDJES.zicht - 4, Plekken.Soort.WATER, r)) {
                if (Steden.op(p.midden()) != stad || waterRond(sl, p.pos(), 3) < 14) {
                    continue;
                }
                double y = p.pos().getY() + 0.82;
                Vec3 m = new Vec3(p.pos().getX() + 0.5, y, p.pos().getZ() + 0.5);
                LevenProp moe = LevenProp.maak(sl, m, r.nextFloat() * 360, MODEL, TEX, MODEL, "zwem", 1f, false);
                if (moe == null) {
                    return null;
                }
                Eendjes e = new Eendjes(stad, moe, y);
                for (int i = 0; i < 3; i++) {
                    LevenProp k = LevenProp.maak(sl, m, moe.getYRot(), MODEL, KUIKEN, MODEL, "zwem", 0.55f, false);
                    if (k != null) {
                        e.kuikens.add(e.lid(k));
                    }
                }
                return e;
            }
            return null;
        }

        static int waterRond(ServerLevel sl, BlockPos c, int r) {
            int n = 0;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Plekken.water(sl, c.offset(dx, 0, dz))) {
                        n++;
                    }
                }
            }
            return n;
        }

        private boolean natteLijn(ServerLevel sl, Vec3 a, Vec3 b) {
            double d = a.distanceTo(b);
            int n = Math.max(1, (int) Math.ceil(d / 0.5));
            int y = (int) Math.floor(waterY - 0.82);
            for (int i = 0; i <= n; i++) {
                Vec3 v = a.lerp(b, i / (double) n);
                if (!Plekken.water(sl, BlockPos.containing(v.x, y, v.z))) {
                    return false;
                }
                // the ducklings keep a little distance from the edge
                for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    if (!Plekken.water(sl, BlockPos.containing(v.x + o[0] * 0.7, y, v.z + o[1] * 0.7))) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        Vec3 midden() {
            return pos;
        }

        @Override
        boolean tick(ServerLevel sl) {
            t++;
            RandomSource r = sl.getRandom();
            if (doel == null) {
                if (--pauze <= 0) {
                    for (int poging = 0; poging < 6 && doel == null; poging++) {
                        double a = r.nextDouble() * Math.PI * 2, d = 2 + r.nextDouble() * 5;
                        Vec3 n = new Vec3(pos.x + Math.cos(a) * d, waterY, pos.z + Math.sin(a) * d);
                        if (natteLijn(sl, pos, n)) {
                            doel = n;
                        }
                    }
                    pauze = 30 + r.nextInt(60);
                }
            } else {
                Vec3 d = doel.subtract(pos);
                double l = d.horizontalDistance();
                if (l < 0.05) {
                    doel = null;
                } else {
                    pos = pos.add(d.scale(Math.min(1, 0.045 / l)));
                }
            }
            double bob = Math.sin(t * 0.12) * 0.03;
            moeder.zet(new Vec3(pos.x, waterY + bob, pos.z));
            if (spoor.isEmpty() || spoor.peekLast().distanceToSqr(pos) > 0.04) {
                spoor.addLast(pos);
                if (spoor.size() > 80) {
                    spoor.removeFirst();
                }
            }
            // the ducklings: 0.8 apart along the mother's wake
            Vec3[] punten = spoor.toArray(new Vec3[0]);
            for (int i = 0; i < kuikens.size(); i++) {
                double nodig = 0.8 * (i + 1), gehad = 0;
                Vec3 p = pos;
                for (int j = punten.length - 1; j > 0; j--) {
                    double s = punten[j].distanceTo(punten[j - 1]);
                    if (gehad + s >= nodig) {
                        p = punten[j].lerp(punten[j - 1], (nodig - gehad) / s);
                        break;
                    }
                    gehad += s;
                    p = punten[j - 1];
                }
                kuikens.get(i).zet(new Vec3(p.x, waterY + Math.sin(t * 0.15 + i) * 0.03 - 0.05, p.z));
            }
            if (t % 120 == 0 && r.nextInt(3) == 0 && Babbels.iemandHoort(sl, pos)) {
                sl.playSound(null, pos.x, pos.y, pos.z, geluid("waterdiertjes.kuiken_piep"), SoundSource.NEUTRAL, 0.22f, 1.2f + r.nextFloat() * 0.3f);
            }
            return true;
        }
    }

    // =====================================================================================================================

    /** A little bird: flies in, perches on a lantern or bench, pecks, flies on to the next one or away. */
    static final class Vogel extends Groep {
        static final String[][] SOORTEN = {
                {"pluisvinkje", "textures/entity/pluisvinkje.png", "animation.pluisvinkje.idle", "animation.pluisvinkje.fly", "animation.pluisvinkje.peck"},
                {"kaasmeesje", "textures/entity/kaasmeesje.png", "animation.kaasmeesje.idle", "animation.kaasmeesje.fly", "animation.kaasmeesje.peck"}};
        final LevenProp f;
        final String[] s;
        Vec3 van, naar;
        int k, duur;
        boolean vliegt, weg;
        int zit;
        int perches;

        private Vogel(Steden.Stad stad, LevenProp f, String[] s) {
            super(StadsLeven.nieuweSleutel("vogel"), Soort.VOGEL, stad);
            this.f = lid(f);
            this.s = s;
            this.van = f.position();
            this.naar = f.position();
        }

        @Nullable
        static Vec3 zitplek(ServerLevel sl, Steden.Stad stad, Vec3 bij, RandomSource r, double rMax) {
            Plekken.Soort soort = r.nextInt(3) == 0 ? Plekken.Soort.ZIT : Plekken.Soort.LAMP;
            for (Plekken.Plek p : Plekken.rond(sl, bij, 2, rMax, soort, r)) {
                if (Steden.op(p.midden()) != stad) {
                    continue;
                }
                BlockPos b = p.pos();
                if (soort == Plekken.Soort.ZIT) {   // on the backrest of a bench
                    return new Vec3(b.getX() + 0.5, b.getY() + 1.0, b.getZ() + 0.5);
                }
                double top = sl.getBlockState(b.below()).getShape(sl, b.below()).max(net.minecraft.core.Direction.Axis.Y);
                return new Vec3(b.getX() + 0.5, b.getY() - 1 + Math.max(0.5, top), b.getZ() + 0.5);
            }
            return null;
        }

        @Nullable
        static Vogel maak(ServerLevel sl, Steden.Stad stad, Vec3 bij, RandomSource r) {
            Vec3 zit = zitplek(sl, stad, bij, r, Soort.VOGEL.zicht - 6);
            if (zit == null) {
                return null;
            }
            String[] s = SOORTEN[r.nextInt(SOORTEN.length)];
            double a = r.nextDouble() * Math.PI * 2;
            Vec3 start = zit.add(Math.cos(a) * 14, 7, Math.sin(a) * 14);
            LevenProp f = LevenProp.maak(sl, start, 0, s[0], s[1], s[0], s[3], 1f, false);
            if (f == null) {
                return null;
            }
            Vogel v = new Vogel(stad, f, s);
            v.vlieg(start, zit);
            return v;
        }

        void vlieg(Vec3 a, Vec3 b) {
            van = a;
            naar = b;
            k = 0;
            duur = Math.max(10, (int) (a.distanceTo(b) / 0.28));
            vliegt = true;
            f.anim(s[3]);
        }

        @Override
        Vec3 midden() {
            return f.position();
        }

        @Override
        boolean tick(ServerLevel sl) {
            t++;
            RandomSource r = sl.getRandom();
            if (vliegt) {
                k++;
                double x = Math.min(1, k / (double) duur);
                double e = x * x * (3 - 2 * x);
                f.zet(van.lerp(naar, e).add(0, Math.sin(x * Math.PI) * 1.5, 0));
                if (x >= 1) {
                    vliegt = false;
                    if (weg) {
                        return false;
                    }
                    f.anim(s[2]);
                    zit = 120 + r.nextInt(260);
                    perches++;
                }
                return true;
            }
            if (t % 50 == 0) {
                f.anim(r.nextInt(3) == 0 ? s[4] : s[2]);
                if (r.nextInt(5) == 0) {
                    f.setYRot(f.getYRot() + (r.nextFloat() - 0.5f) * 140);
                }
                if (r.nextInt(6) == 0 && Babbels.iemandHoort(sl, f.position())) {
                    sl.playSound(null, f.getX(), f.getY(), f.getZ(), SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 0.2f, 1.7f + r.nextFloat() * 0.3f);
                }
            }
            if (--zit <= 0) {
                Vec3 nieuw = perches < 4 ? zitplek(sl, stad, f.position(), r, 18) : null;
                if (nieuw != null && nieuw.distanceToSqr(f.position()) > 4) {
                    vlieg(f.position(), nieuw);
                } else {   // off it goes
                    double a = r.nextDouble() * Math.PI * 2;
                    weg = true;
                    vlieg(f.position(), f.position().add(Math.cos(a) * 20, 12, Math.sin(a) * 20));
                }
            }
            return true;
        }
    }
}
