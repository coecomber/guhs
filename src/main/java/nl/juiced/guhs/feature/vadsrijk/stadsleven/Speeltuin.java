package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.emotes.Emote;
import org.slf4j.Logger;

/**
 * U-LEVEN-STAD: the pink playground of a hub town (stamped into the map by tools/vadsrijk/stadsleven/speeltuin.py; its corner
 * and floor y are in {@code data/guhs/vadsrijk_stadsleven/speeltuinen.txt}). The blocks are the map (fence ring, slide tower,
 * swing frame, sandpit, benches); the moving parts are props, and child-guhs (scale {@link LevenGuh#KIND}) play on everything
 * with a scripted movement: up the ladder and down the slide, the seesaw going up and down, the swings swinging, the
 * merry-go-round turning, digging in the sandpit, a grown-up on the bench. By day only; at night the playground sleeps.
 * The offsets below mirror the layout in speeltuin.py (13 x 10, F = floor y).
 */
final class Speeltuin extends Groep {
    private static final Logger LOG = com.mojang.logging.LogUtils.getLogger();
    static final String BESTAND = "/data/guhs/vadsrijk_stadsleven/speeltuinen.txt";
    static final String MAP = "vadsrijk/stadsleven/";
    @Nullable
    private static Map<String, BlockPos> alle;

    /** The playground corners per town, read once from the classpath. */
    static synchronized Map<String, BlockPos> alle() {
        if (alle == null) {
            Map<String, BlockPos> m = new LinkedHashMap<>();
            try (InputStream in = Speeltuin.class.getResourceAsStream(BESTAND)) {
                if (in != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    for (String regel; (regel = r.readLine()) != null;) {
                        String[] d = regel.strip().split("\\s+");
                        if (d.length == 5 && d[0].equals("speeltuin")) {
                            m.put(d[1], new BlockPos(Integer.parseInt(d[2]), Integer.parseInt(d[3]), Integer.parseInt(d[4])));
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("[vadsrijk stadsleven] could not read {}", BESTAND, e);
            }
            alle = m;
        }
        return alle;
    }

    /** Test: forget a test playground again. */
    static synchronized void vergeet(String stad) {
        alle();
        alle.remove(stad);
    }

    /** Test / dev: put a playground somewhere else (a gametest builds its own). */
    static synchronized void zet(String stad, BlockPos hoek) {
        alle();
        alle.put(stad, hoek);
    }

    // ---- the layout (keep in sync with speeltuin.py) ------------------------------------------------------------------------
    static final double WIP_X = 9.5, WIP_Z = 2.5, WIP_Y = 2.0, WIP_L = 1.6, WIP_MAX = 14;
    static final double[] SCHOMMEL_X = {9.5, 10.5};
    static final double SCHOMMEL_Z = 7.5, SCHOMMEL_Y = 4.45, SCHOMMEL_L = 2.56, SCHOMMEL_MAX = 30;
    static final double MOLEN_X = 3.5, MOLEN_Z = 6.5, MOLEN_R = 0.95;
    /** The slide loop: walk to the ladder, climb, onto the tower, slide, landing. */
    static final double[][] GLIJ = {{6.5, 1, 2.6}, {1.5, 1, 2.6}, {1.5, 1, 1.5}, {1.5, 3, 1.5}, {2.5, 3, 1.5}, {3.2, 3.0, 1.5}, {6.4, 1, 1.5}};
    static final double[][] ZAND = {{5.8, 1.0, 6.9}, {7.1, 1.0, 8.0}};
    static final double[][] BANK = {{1.5, 1.35, 8.5}, {2.5, 1.35, 8.5}};

    final BlockPos hoek;
    final Vec3 m;
    @Nullable
    LevenProp wip, molen;
    final LevenProp[] schommels = new LevenProp[2];
    // the children (null = that spot is empty)
    @Nullable
    LevenGuh wipA, wipB, molenA, molenB, zand;
    final LevenGuh[] schommelKind = new LevenGuh[2];
    final List<LevenGuh> glijers = new ArrayList<>();
    final List<double[]> glijStand = new ArrayList<>();   // per slider: {stage, progress}
    @Nullable
    LevenGuh ouder;
    boolean kinderen;
    final double[] fase = {0, 1.7};
    double molenYaw;

    private Speeltuin(Steden.Stad stad, BlockPos hoek) {
        super("speeltuin:" + stad.id(), Soort.SPEELTUIN, stad);
        this.hoek = hoek;
        this.m = new Vec3(hoek.getX() + 6.5, hoek.getY() + 1, hoek.getZ() + 5);
    }

    Vec3 w(double x, double y, double z) {
        return new Vec3(hoek.getX() + x, hoek.getY() + y, hoek.getZ() + z);
    }

    @Nullable
    static Speeltuin maak(ServerLevel sl, Steden.Stad stad, Vec3 bij, RandomSource r, StadsLeven.Niveau niveau) {
        BlockPos h = alle().get(stad.id());
        if (h == null || StadsLeven.heeft("speeltuin:" + stad.id())) {
            return null;
        }
        Speeltuin s = new Speeltuin(stad, h);
        if (s.m.distanceToSqr(bij) > Soort.SPEELTUIN.zicht * Soort.SPEELTUIN.zicht || !nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.klaar(sl, h)) {
            return null;
        }
        // the moving parts
        s.wip = s.lid(LevenProp.maak(sl, s.w(WIP_X, WIP_Y, WIP_Z), 90, MAP + "sl_wipwap", "textures/" + MAP + "sl_speeltoestel.png", MAP + "sl_wipwap",
                "", 1f, true));
        for (int i = 0; i < 2; i++) {
            s.schommels[i] = s.lid(LevenProp.maak(sl, s.w(SCHOMMEL_X[i], SCHOMMEL_Y, SCHOMMEL_Z), 0, MAP + "sl_schommel",
                    "textures/" + MAP + "sl_speeltoestel.png", MAP + "sl_schommel", "", 1f, true));
        }
        s.molen = s.lid(LevenProp.maak(sl, s.w(MOLEN_X, 1.0, MOLEN_Z), 0, MAP + "sl_draaimolen", "textures/" + MAP + "sl_speeltoestel.png",
                MAP + "sl_draaimolen", "", 1f, true));
        if (s.wip == null || s.molen == null || s.schommels[0] == null || s.schommels[1] == null) {
            s.weg();
            return null;
        }
        s.kinderenErbij(sl, r, niveau);
        return s;
    }

    /** By day: the children come (VOL: 9 children + a grown-up, ZACHT: 5). */
    void kinderenErbij(ServerLevel sl, RandomSource r, StadsLeven.Niveau niveau) {
        if (kinderen || StadsLeven.nacht(sl) || StadsLeven.regen(sl)) {
            return;
        }
        kinderen = true;
        boolean vol = niveau == StadsLeven.Niveau.VOL;
        wipA = kind(sl, r, w(WIP_X - WIP_L, WIP_Y, WIP_Z));
        wipB = kind(sl, r, w(WIP_X + WIP_L, WIP_Y, WIP_Z));
        schommelKind[0] = kind(sl, r, w(SCHOMMEL_X[0], 2, SCHOMMEL_Z));
        if (vol) {
            schommelKind[1] = kind(sl, r, w(SCHOMMEL_X[1], 2, SCHOMMEL_Z));
        }
        molenA = kind(sl, r, w(MOLEN_X + MOLEN_R, 1.2, MOLEN_Z));
        if (vol) {
            molenB = kind(sl, r, w(MOLEN_X - MOLEN_R, 1.2, MOLEN_Z));
            zand = kind(sl, r, w(ZAND[0][0], ZAND[0][1], ZAND[0][2]));
            ouder = lid(StadsLeven.maakGuh(sl, w(BANK[0][0], BANK[0][1], BANK[0][2]), 180, r, false, stad));
        }
        for (LevenGuh k : new LevenGuh[] {wipA, wipB, schommelKind[0], schommelKind[1], molenA, molenB, zand, ouder}) {
            if (k != null) {
                k.zit(true);
            }
        }
        if (ouder != null) {
            ouder.kijk(180);
        }
        int glij = vol ? 2 : 1;
        for (int i = 0; i < glij; i++) {
            double[] p = GLIJ[i * 3 % GLIJ.length];
            LevenGuh k = kind(sl, r, w(p[0], p[1], p[2]));
            if (k != null) {
                glijers.add(k);
                glijStand.add(new double[] {i * 3 % GLIJ.length, 0});
            }
        }
    }

    @Nullable
    private LevenGuh kind(ServerLevel sl, RandomSource r, Vec3 p) {
        return lid(StadsLeven.maakGuh(sl, p, 0, r, true, stad));
    }

    void kinderenWeg() {
        if (!kinderen) {
            return;
        }
        kinderen = false;
        List<LevenGuh> weg = new ArrayList<>(glijers);
        for (LevenGuh k : new LevenGuh[] {wipA, wipB, schommelKind[0], schommelKind[1], molenA, molenB, zand, ouder}) {
            if (k != null) {
                weg.add(k);
            }
        }
        for (LevenGuh k : weg) {
            leden.remove(k);
            StadsLeven.weg(k);
        }
        wipA = wipB = molenA = molenB = zand = ouder = null;
        schommelKind[0] = schommelKind[1] = null;
        glijers.clear();
        glijStand.clear();
    }

    int kinderenAantal() {
        int n = glijers.size();
        for (LevenGuh k : new LevenGuh[] {wipA, wipB, schommelKind[0], schommelKind[1], molenA, molenB, zand}) {
            if (k != null) {
                n++;
            }
        }
        return n;
    }

    @Override
    boolean heel(ServerLevel sl) {
        return wip != null && !wip.isRemoved() && molen != null && !molen.isRemoved() && super.heel(sl);
    }

    @Override
    Vec3 midden() {
        return m;
    }

    @Override
    @Nullable
    String klikRegel(LevenGuh g) {
        return g == ouder ? "klets" : "speel";
    }

    @Override
    boolean tick(ServerLevel sl) {
        t++;
        RandomSource r = sl.getRandom();
        boolean dag = !StadsLeven.nacht(sl) && !StadsLeven.regen(sl);
        if (!dag && kinderen && !Babbels.iemandHoort(sl, m)) {
            kinderenWeg();   // bedtime (when nobody is looking)
        } else if (dag && !kinderen && t % 100 == 0 && !Babbels.iemandHoort(sl, m)) {
            kinderenErbij(sl, r, StadsLeven.niveauIn(stad));
        }
        boolean speelt = kinderen;
        // the seesaw
        double hoek = speelt && wipA != null && wipB != null ? WIP_MAX * Math.sin(t * 0.075) : 0;
        wip.kantel((float) hoek);
        double dy = WIP_L * Math.sin(Math.toRadians(hoek));
        if (wipA != null) {
            wipA.stap(w(WIP_X - WIP_L + 0.15, WIP_Y + 0.12 + dy, WIP_Z));
            wipA.kijk(-90);
        }
        if (wipB != null) {
            wipB.stap(w(WIP_X + WIP_L - 0.15, WIP_Y + 0.12 - dy, WIP_Z));
            wipB.kijk(90);
        }
        // the swings
        for (int i = 0; i < 2; i++) {
            LevenGuh k = schommelKind[i];
            double th = k == null ? Math.sin(t * 0.02 + i) * 2 : SCHOMMEL_MAX * Math.sin(t * 0.098 + fase[i]);
            schommels[i].kantel((float) th);
            if (k != null) {
                double a = Math.toRadians(th);
                k.stap(w(SCHOMMEL_X[i], SCHOMMEL_Y - SCHOMMEL_L * Math.cos(a) + 0.02, SCHOMMEL_Z + SCHOMMEL_L * Math.sin(a)));
                k.kijk(0);
            }
        }
        // the merry-go-round
        molenYaw = Mth.wrapDegrees(molenYaw + (speelt ? 2.6 : 0.2));
        molen.setYRot((float) molenYaw);
        LevenGuh[] op = {molenA, molenB};
        for (int i = 0; i < 2; i++) {
            if (op[i] == null) {
                continue;
            }
            double yk = Math.toRadians(molenYaw - i * 180);
            double ox = MOLEN_R * Math.sin(yk), oz = -MOLEN_R * Math.cos(yk);
            op[i].stap(w(MOLEN_X + ox, 1.19, MOLEN_Z + oz));
            op[i].kijk((float) Math.toDegrees(Math.atan2(ox, -oz)));
        }
        // the sandpit
        if (zand != null && t % 30 == 0) {
            zand.stap(w(ZAND[0][0], ZAND[0][1], ZAND[0][2]));
            if (Babbels.iemandHoort(sl, m)) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), zand.getX(), zand.getY() + 0.2, zand.getZ(),
                        4, 0.2, 0.05, 0.2, 0.05);
            }
            if (r.nextInt(6) == 0) {
                zand.kijk(r.nextFloat() * 360);
            }
        }
        if (ouder != null) {
            ouder.stap(w(BANK[0][0], BANK[0][1], BANK[0][2]));
        }
        // the slide
        for (int i = 0; i < glijers.size(); i++) {
            glij(sl, glijers.get(i), glijStand.get(i));
        }
        // giggles and shouts
        if (speelt && t % 80 == 0) {
            List<LevenGuh> kids = new ArrayList<>(glijers);
            for (LevenGuh k : new LevenGuh[] {wipA, wipB, schommelKind[0], schommelKind[1], molenA, molenB}) {
                if (k != null) {
                    kids.add(k);
                }
            }
            if (!kids.isEmpty() && Babbels.iemandHoort(sl, m) && r.nextInt(3) == 0) {
                LevenGuh k = kids.get(r.nextInt(kids.size()));
                sl.playSound(null, k.getX(), k.getY(), k.getZ(), Beestjes.geluid("creche.babygiechel"), SoundSource.NEUTRAL, 0.3f, 1.0f + r.nextFloat() * 0.35f);
            }
            Babbels.praat(sl, this, kids, Babbels.regel("speel", Babbels.SPEEL, r), 60);
        }
        return true;
    }

    /** One child on the slide loop: walk, climb, step onto the tower, sit + slide down, a little cheer, again. */
    private void glij(ServerLevel sl, LevenGuh k, double[] st) {
        int i = (int) st[0];
        double[] a = GLIJ[i], b = GLIJ[(i + 1) % GLIJ.length];
        Vec3 va = w(a[0], a[1], a[2]), vb = w(b[0], b[1], b[2]);
        boolean glijdt = i == 5, klimt = i == 2, wacht = i == 6;
        double snelheid = glijdt ? 0.2 : klimt ? 0.07 : 0.1;
        double lengte = Math.max(0.1, va.distanceTo(vb));
        st[1] += snelheid / lengte;
        if (wacht) {   // at the bottom: a moment of joy
            if (st[1] < 0.02) {
                k.houd(Emote.VAHOEG);
            }
            if (st[1] >= 0.35) {
                k.houd(null);
            }
        }
        if (st[1] >= 1) {
            st[0] = (i + 1) % GLIJ.length;
            st[1] = 0;
            k.zit(st[0] == 5);
            return;
        }
        Vec3 p = va.lerp(vb, st[1]);
        if (wacht) {
            p = va;
        }
        Vec3 oud = k.position();
        k.setPos(p.x, p.y, p.z);
        if (klimt) {
            k.kijk(90);   // facing the ladder (the tower is east of it)
        } else if (p.distanceToSqr(oud) > 1e-4) {
            k.kijk((float) Math.toDegrees(Math.atan2(-(p.x - oud.x), p.z - oud.z)));
        }
        if (glijdt && st[1] > 0.95 && Babbels.iemandHoort(sl, p) && sl.getRandom().nextInt(2) == 0 && !k.praat()
                && StadsLeven.bubbelsIn(stad) < Babbels.MAX_TEGELIJK) {
            k.zeg(Babbels.bubbel(Component.translatable("vadsrijk.stadsleven.wiii")), 40);
        }
    }

    /** The playground of a town (null = none). */
    @Nullable
    static BlockPos van(String stad) {
        return alle().get(stad);
    }
}
