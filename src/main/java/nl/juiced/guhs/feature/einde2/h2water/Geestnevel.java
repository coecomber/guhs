package nl.juiced.guhs.feature.einde2.h2water;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 fix3-h2 (playtest 2, item 2: "de route is totaal niet duidelijk ... een soort roze rook waar we allemaal nog niet heen
 * kunnen"): <b>geestnevel</b>, walls of pink spirit mist across the paths of the Geestenwereld that the chapter does not want
 * the player on yet. The pattern is Guhdalfs sluier of the Knabbelring story ({@link Sluiers}): per player, a wall you can
 * see from afar, stepping in gives a gentle shove back out (deeper in: put back where you last stood outside), a line over
 * the hotbar at most every five seconds, and the wall dissolves for that player as soon as the story goes on. Never damage.
 * <ul>
 *   <li>At {@link E2H2Water#KOH} only the way to Koh is open: from the Meditatiecirkel west through the Gefluisterwoud to the
 *       old tree. The walls stand right where the other paths leave the start islet (north to the Witte Lotus, east to the
 *       Hemelvijver, south-east to the Theepaviljoen), so the player sees them from the arrival lawn, plus one where the
 *       path from the Witte Lotus reaches Koh's island.</li>
 *   <li>At {@link E2H2Water#HEMELVIJVER} the wall to the east lifts (Koh tells you to go there): back the way you came and on
 *       to the Hemelvijver; the paths on from the pond (to Fang, to the Theepaviljoen) are closed at the pond.</li>
 *   <li>Any other step, a finished chapter, or a spectator / creative operator: no walls.</li>
 * </ul>
 * The boxes are world blocks of the Geestenwereld build (template + {@link Geestenwereld#OORSPRONG}); every one stands across a
 * floating path, so the only way past is through it. Checked against the build with a walking search (reports/fix3_h2.md).
 */
public final class Geestnevel {
    /**
     * A wall of mist: its box (world blocks, inclusive) and the way back out (the side the player comes from: x, z of a
     * direction; a diagonal path has both).
     */
    public record Nevel(String id, int x0, int y0, int z0, int x1, int y1, int z1, int terugX, int terugZ) {
        public boolean binnen(Vec3 p) {
            return p.x >= x0 && p.x < x1 + 1 && p.y >= y0 && p.y < y1 + 1 && p.z >= z0 && p.z < z1 + 1;
        }

        /** How far in from the open side (blocks). */
        double diepte(Vec3 p) {
            double d = Double.MAX_VALUE;
            if (terugX < 0) {
                d = Math.min(d, p.x - x0);
            } else if (terugX > 0) {
                d = Math.min(d, x1 + 1 - p.x);
            }
            if (terugZ < 0) {
                d = Math.min(d, p.z - z0);
            } else if (terugZ > 0) {
                d = Math.min(d, z1 + 1 - p.z);
            }
            return d;
        }

        Vec3 terug() {
            return new Vec3(terugX, 0, terugZ).normalize();
        }

        /** A spot just outside the open side, at the height of the path (for a player who never stood outside). */
        Vec3 buiten(Vec3 p) {
            double x = terugX < 0 ? x0 - 1.5 : terugX > 0 ? x1 + 2.5 : p.x;
            double z = terugZ < 0 ? z0 - 1.5 : terugZ > 0 ? z1 + 2.5 : p.z;
            return new Vec3(x, p.y, z);
        }

        Vec3 midden() {
            return new Vec3((x0 + x1 + 1) / 2.0, (y0 + y1 + 1) / 2.0, (z0 + z1 + 1) / 2.0);
        }
    }

    // where the paths leave the Meditatiecirkel (the arrival lawn: world 0, 82, 22; the paths stand at 82)
    /** The path north to the Witte Lotus (and the Trap der meesters). */
    public static final Nevel LOTUS = new Nevel("lotus", -3, 80, -37, 5, 88, -34, 0, 1);
    /** The path east to the Hemelvijver. */
    public static final Nevel VIJVER = new Nevel("vijver", 33, 80, -5, 36, 88, 3, -1, 0);
    /** The path south-east to the Theepaviljoen. */
    public static final Nevel THEE = new Nevel("thee", 23, 80, 25, 28, 88, 31, -1, -1);
    /** Where the path from the Witte Lotus reaches Koh's island (east of the old tree, the path at 72). */
    public static final Nevel LOTUS_KOH = new Nevel("lotus_koh", -90, 70, -149, -86, 78, -140, -1, 0);
    /** At the Hemelvijver: the path north to Fang's Hemelpad (the rim at 97). */
    public static final Nevel VIJVER_FANG = new Nevel("vijver_fang", 168, 96, -42, 177, 104, -39, 0, 1);
    /** At the Hemelvijver: the path south to the Theepaviljoen. */
    public static final Nevel THEE_VIJVER = new Nevel("thee_vijver", 168, 95, 31, 175, 103, 34, 0, -1);
    public static final List<Nevel> ALLE = List.of(LOTUS, VIJVER, THEE, LOTUS_KOH, VIJVER_FANG, THEE_VIJVER);

    /** How deep a player may step in and only gets a shove (as Guhdalfs sluier); deeper: put back outside. */
    static final double DUW_DIEPTE = 1.5;
    /** The line over the hotbar comes at most once per this many ticks. */
    static final int BERICHT_TICKS = 100;
    /** Walls further than this are not drawn for the player (blocks). */
    static final double ZICHT = 80;
    public static final String BERICHT = "gui.guhs.einde2_h2_water.nevel";

    /** Pink spirit mist: big soft dust, two shades. */
    private static final ParticleOptions ROZE = new DustParticleOptions(0xFF8AD2, 3.6f), LICHT = new DustParticleOptions(0xFFC4EA, 2.6f);

    private record Buiten(Vec3 plek, float yaw) {
    }

    private static final Map<UUID, Buiten> BUITEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BERICHT_OP = new ConcurrentHashMap<>(), DUW_OP = new ConcurrentHashMap<>();

    private Geestnevel() {
    }

    /** The walls that are closed for this player at this step of chapter 2 (empty: none). */
    public static List<Nevel> dicht(int stap) {
        return switch (stap) {
            case E2H2Water.KOH -> List.of(LOTUS, VIJVER, THEE, LOTUS_KOH);
            case E2H2Water.HEMELVIJVER -> List.of(LOTUS, THEE, LOTUS_KOH, VIJVER_FANG, THEE_VIJVER);
            default -> List.of();
        };
    }

    /** The walls closed for this player now: only in the Geestenwereld (or a test's level), only while chapter 2 waits there. */
    static List<Nevel> dicht(ServerPlayer p) {
        Verhaallijn l = E2H2Water.lijn();
        if (!(Geestenwereld.in(p) || OVERAL) || !l.aanDeBeurt(p) || Sluiers.passeert(p)) {
            return List.of();
        }
        return dicht(l.stap(p));
    }

    /** (tests) the walls work in any level. */
    static volatile boolean OVERAL;

    @Nullable
    static Nevel waarIn(ServerPlayer p, List<Nevel> dicht) {
        Vec3 pos = p.position();
        for (Nevel n : dicht) {
            if (n.binnen(pos)) {
                return n;
            }
        }
        return null;
    }

    /** Every tick (the feature's tick listener): keeps the player out of the closed walls; every few ticks draws them. */
    static void tick(ServerPlayer p) {
        List<Nevel> dicht = dicht(p);
        if (dicht.isEmpty()) {
            return;
        }
        houdBuiten(p, dicht);
        if ((p.tickCount + p.getId()) % 4 == 0 && p.connection != null) {
            toon(p, dicht);
        }
    }

    /** (also for the tests) one tick of the walls: remembers where the player stood outside, sends them back out. */
    static void houdBuiten(ServerPlayer p, List<Nevel> dicht) {
        Nevel n = waarIn(p, dicht);
        if (n == null) {
            if (p.onGround() || !BUITEN.containsKey(p.getUUID())) {
                BUITEN.put(p.getUUID(), new Buiten(p.position(), p.getYRot()));
            }
            return;
        }
        if (Cutscenes.bezig(p) || !p.isAlive()) {
            return;
        }
        long nu = p.level().getGameTime();
        if (n.diepte(p.position()) <= DUW_DIEPTE && Duwtje.mag(p)) {
            Long geduwd = DUW_OP.get(p.getUUID());
            if (geduwd == null || nu - geduwd >= 6 || nu < geduwd) {
                DUW_OP.put(p.getUUID(), nu);
                Duwtje.duw(p, n.terug(), 0.6);
            }
        } else {
            Buiten b = BUITEN.get(p.getUUID());
            Vec3 naar = b != null && waarIn(p, List.of(n)) != null && !n.binnen(b.plek()) && b.plek().distanceToSqr(p.position()) < 32 * 32
                    ? b.plek() : n.buiten(p.position());
            Duwtje.terug(p, p.level().dimension(), naar, b != null ? b.yaw() : p.getYRot());
        }
        Long gezegd = BERICHT_OP.get(p.getUUID());
        if (gezegd == null || nu - gezegd >= BERICHT_TICKS || nu < gezegd) {
            BERICHT_OP.put(p.getUUID(), nu);
            p.sendOverlayMessage(Component.translatable(BERICHT).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The mist itself, for this player only: soft pink clouds that fill each closed wall within sight. */
    static void toon(ServerPlayer p, List<Nevel> dicht) {
        ServerLevel level = p.level();
        RandomSource r = p.getRandom();
        for (Nevel n : dicht) {
            Vec3 m = n.midden();
            if (m.distanceToSqr(p.position()) > ZICHT * ZICHT) {
                continue;
            }
            double bx = n.x1() + 1 - n.x0(), by = n.y1() + 1 - n.y0(), bz = n.z1() + 1 - n.z0();
            for (int i = 0; i < 4; i++) {
                double x = n.x0() + r.nextDouble() * bx, y = n.y0() + 1.5 + r.nextDouble() * (by - 2.5), z = n.z0() + r.nextDouble() * bz;
                level.sendParticles(p, i == 0 ? LICHT : ROZE, true, false, x, y, z, 5, bx / 5, by / 6, bz / 5, 0.0);
            }
        }
    }

    /** (logout, wipe) */
    static void vergeet(UUID speler) {
        BUITEN.remove(speler);
        BERICHT_OP.remove(speler);
        DUW_OP.remove(speler);
    }
}
