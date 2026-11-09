package nl.juiced.guhs.feature.erfenis.sherlock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * Makelaar Guhrrit, who sold you the "opknapper" for one kaasknabbel ("Bouwkundig rapport: vads. Emotioneel rapport: heel
 * vads."). The tutorial (kern) uses him at the hekje of the village; the Sherlock story brings him back from holiday
 * (sunburn and a sombrero: {@link Kind#GUHRRIT_VAKANTIE}) in chapter 10 and in the courtroom.
 * <pre>
 * GuhNpcEntity g = Guhrrit.verschijn(level, hekje);                       // he stands there (one per village spot)
 * Guhrrit.bijKlik((speler, guhrrit) -&gt; {...; return true;});              // the tutorial hears a click on him
 * Guhrrit.zeg(speler, g, "erfenis_tutorial_1", List.of("erfenis.tut.guhrrit.0", "erfenis.tut.guhrrit.1"),
 *         new Praat.Optie(1, "erfenis.tut.ja"));                         // pages in the talking screen, answers to
 * Praat.luister("erfenis_tutorial_1", (speler, spreker, optie) -&gt; ...);   // ...the tutorial's own listener
 * Guhrrit.weg(g);                                                        // off on holiday (a little cloud)
 * (He is temporary like the whole cast: with nobody within 40 blocks he goes; call verschijn again when needed.)
 * </pre>
 * Standard lines of his that the tutorial may use: {@code sherlock.guhrrit.<0..5>} (see erfenis_sherlock.py).
 */
public final class Guhrrit {
    private static final List<BiPredicate<ServerPlayer, GuhNpcEntity>> LUISTERAARS = new ArrayList<>();
    /** How many standard lines {@code sherlock.guhrrit.<n>} there are. */
    public static final int ZINNEN = 6;

    private Guhrrit() {
    }

    /** Guhrrit (in his suit) stands at this block (feet) looking south; the one that already stands near it moves there. */
    @Nullable
    public static GuhNpcEntity verschijn(ServerLevel level, BlockPos pos) {
        return verschijn(level, pos, 0f, false);
    }

    /** Guhrrit at this block with this yaw; vakantie = with sunburn and sombrero. One Guhrrit per spot (shared, no owner). */
    @Nullable
    public static GuhNpcEntity verschijn(ServerLevel level, BlockPos pos, float yaw, boolean vakantie) {
        return verschijn(level, pos, yaw, vakantie, "guhrrit@" + pos.asLong());
    }

    /**
     * Guhrrit under a fixed key: the one that already stands under that key is reused (and walks over when he is far),
     * so a moving spot (the tutorial: in front of the player) never makes a second Guhrrit.
     */
    @Nullable
    public static GuhNpcEntity verschijn(ServerLevel level, BlockPos pos, float yaw, boolean vakantie, String sleutel) {
        if (nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.in(level)) {
            return null;   // (1.10: the makelaar never comes into the Vadsrijk)
        }
        Vec3 v = Vec3.atBottomCenterOf(pos);
        Entity e = Cast.zet(level, Cast.IEDEREEN, sleutel, "guhrrit", vakantie ? Kind.GUHRRIT_VAKANTIE : Kind.MAKELAAR_GUHRRIT, v, yaw, null, false);
        if (e instanceof GuhNpcEntity g) {
            g.setKind(vakantie ? Kind.GUHRRIT_VAKANTIE : Kind.MAKELAAR_GUHRRIT);
            return g;
        }
        return null;
    }

    /** He leaves (a little cloud of dust). */
    public static void weg(@Nullable Entity guhrrit) {
        if (guhrrit != null && guhrrit.isAlive()) {
            if (guhrrit.level() instanceof ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, guhrrit.getX(), guhrrit.getY() + 0.6, guhrrit.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            }
            guhrrit.discard();
        }
    }

    /** Hear clicks on Guhrrit; return true when you handled it (nobody did: one of his standard lines). */
    public static void bijKlik(BiPredicate<ServerPlayer, GuhNpcEntity> luisteraar) {
        LUISTERAARS.add(luisteraar);
    }

    /**
     * Guhrrit says these pages (lang keys) to the player in the talking screen; the answers come back to
     * {@code Praat.luister(sleutel, ...)} (-1 when read to the end / closed). sleutel: "&lt;pkg&gt;_&lt;x&gt;".
     */
    public static void zeg(ServerPlayer p, Entity guhrrit, String sleutel, List<String> tekstKeys, Praat.Optie... opties) {
        List<Praat.Regel> regels = new ArrayList<>();
        for (String k : tekstKeys) {
            regels.add(new Praat.Regel(guhrrit, "", k));
        }
        Praat.scene(p, sleutel, regels, opties);
    }

    static void klik(ServerPlayer p, Entity e) {
        if (e instanceof GuhNpcEntity g) {
            for (BiPredicate<ServerPlayer, GuhNpcEntity> l : LUISTERAARS) {
                if (l.test(p, g)) {
                    return;
                }
            }
        }
        Praat.open(p, e, Gesprek.VERHAAL, "sherlock.guhrrit." + p.getRandom().nextInt(ZINNEN), new Object[0],
                new Praat.Optie(9, "sherlock.optie.doei"));
    }

    /** (tests) the owner Guhrrit's spots belong to. */
    static UUID eigenaar() {
        return Cast.IEDEREEN;
    }
}
