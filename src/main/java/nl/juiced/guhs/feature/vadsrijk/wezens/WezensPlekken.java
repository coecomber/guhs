package nl.juiced.guhs.feature.vadsrijk.wezens;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * Every coordinate the wezens slice uses (SLICE_BRIEF: one class). Fix round 1 ("leven"): the spawner plan
 * ({@link WezensSpawners}) is built from the real map: the generator's paths, ponds, cave tunnels and knots below are copies
 * of {@code tools/vadsrijk/wereld/kaart.py} (PADEN, VIJVERS, ARENA, SLAAPHEUVEL, GROT_INGANGEN, BERG ramp) and
 * {@code bouwsels.py} (GROT_KNOPEN, the tunnel wiggle). Keep them in step when the map layout changes. Stand heights come
 * from {@link WereldPlekken} where it has them.
 */
public final class WezensPlekken {
    private WezensPlekken() {
    }

    private static BlockPos p(int x, int z) {
        return new BlockPos(x, Vadsrijk.GROND + 1, z);
    }

    private static BlockPos w(String id, int dx, int dz) {
        return WereldPlekken.plek(id).offset(dx, 0, dz);
    }

    // ---- the paths (kaart.py PADEN after its needle-foot adjustment, world x/z; aligned to fix wild at integration) -----------------------------------------------------------------------------
    private static final String[] PAD_NAMEN = {"weide", "woud", "grotten", "grotten2", "grotten3", "grotten4", "berg", "weide2", "weide3", "slijm",
            "slijm2", "woud2", "druip"};
    private static final int[][][] PADEN = {
            {{30, -40}, {90, -40}, {220, -80}, {300, -20}, {360, 30}, {374, 90}},
            {{-60, 20}, {-130, 20}, {-220, 40}, {-350, 120}, {-430, 230}, {-300, 340}},
            {{-60, -70}, {-110, -120}, {-180, -220}, {-260, -300}, {-300, -220}},
            {{-260, -300}, {-420, -300}},
            {{-260, -300}, {-180, -360}, {-180, -480}},
            {{-260, -300}, {-380, -400}, {-460, -460}},
            {{119, -219}, {140, -270}, {190, -330}, {240, -375}, {290, -385}, {330, -366}},
            {{220, -80}, {230, -130}, {330, -200}},
            {{360, 30}, {480, -80}, {515, -222}},
            {{374, 90}, {410, 200}, {455, 362}},
            {{410, 200}, {330, 260}, {300, 262}},
            {{-220, 40}, {-300, -40}, {-420, 40}, {-496, -27}},
            {{-180, -360}, {-109, -493}},
    };

    public static List<int[][]> paden(String... namen) {
        List<int[][]> uit = new ArrayList<>();
        for (String n : namen) {
            for (int i = 0; i < PAD_NAMEN.length; i++) {
                if (PAD_NAMEN[i].equals(n)) {
                    uit.add(PADEN[i]);
                }
            }
        }
        return uit;
    }

    // ---- Klodderweide ------------------------------------------------------------------------------------------------------
    /** Ponds (x, z, r) (kaart.py VIJVERS). */
    public static final int[][] VIJVERS = {{330, -70, 30}, {470, 160, 25}, {250, 260, 22}, {520, 40, 20}, {300, 120, 14}};
    /** No-go circles (x, z, r): the Koning Klodder arena, the slime hill with the secret. */
    public static final int[] ARENA_KONING = {410, 90, 44};
    public static final int[] SLAAPHEUVEL = {300, 300, 50};
    public static final BlockPos[] MOEDERKLODDER = {p(230, 40), p(560, -240)};

    // ---- Druipgrotten ------------------------------------------------------------------------------------------------------
    /** Cave knots: centre x, y, z and radius (bouwsels.py GROT_KNOPEN; the floor is at about y - 0.7 r). */
    public static final int[][] KNOPEN = {{-320, 52, -300, 11}, {-400, 48, -380, 12}, {-260, 50, -420, 10}, {-460, 46, -470, 9}};
    /** The three cave mouths (x, z) (kaart.py GROT_INGANGEN), each leading to knot N1, N2, N3. */
    public static final int[][] GROT_INGANGEN = {{-300, -220}, {-420, -300}, {-180, -360}};

    /** A tunnel from a to b (centre line), with the generator's sideways wiggle. */
    public record Gang(double ax, double ay, double az, double bx, double by, double bz) {
        public double lengte() {
            return Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
        }

        /** The centre at t (0..1), as in bouwsels.grotten. */
        public int[] punt(double t) {
            double L = Math.max(1, lengte());
            double w = Math.sin(t * Math.PI * 3) * 5;
            double x = ax + (bx - ax) * t + w * (bz - az) / L;
            double y = ay + (by - ay) * t;
            double z = az + (bz - az) * t - w * (bx - ax) / L;
            return new int[] {(int) Math.round(x), (int) Math.floor(y), (int) Math.round(z)};
        }
    }

    public static List<Gang> gangen() {
        List<Gang> uit = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            BlockPos in = WereldPlekken.plek("grot_ingangen." + (i + 1));   // stand y = terrain + 1; the tunnel starts at terrain - 1
            int[] k = KNOPEN[i];
            uit.add(new Gang(in.getX(), in.getY() - 2, in.getZ(), k[0], k[1], k[2]));
        }
        int[][] paren = {{0, 1}, {1, 2}, {1, 3}, {0, 2}};
        for (int[] pr : paren) {
            int[] a = KNOPEN[pr[0]], b = KNOPEN[pr[1]];
            uit.add(new Gang(a[0], a[1], a[2], b[0], b[1], b[2]));
        }
        return uit;
    }

    // ---- Zwamtempel ----------------------------------------------------------------------------------------------------------
    public static final int[][] BERGPAD = PADEN[6];
    /** kaart.py (fix wild): the ramp runs from the fossil bridge deck (top block = the north landing's stand y - 1) to the foot
     * of the temple stairs (top block TEMPEL_PLATEAU - 7). */
    public static final int TEMPEL_PLATEAU = 151, BERG_TOP = TEMPEL_PLATEAU - 7;
    public static final int BERG_VOET = WereldPlekken.plek("fossielbrug.naar") != null ? WereldPlekken.plek("fossielbrug.naar").getY() - 1 : 72;

    /** The top-block height of the mountain path at fraction f (0..1) of its length. */
    public static double bergY(double f) {
        return BERG_VOET + (BERG_TOP - BERG_VOET) * Math.max(0, Math.min(1, f));
    }

    /** The (x, z) on the mountain path {@code langs} blocks from its start. */
    public static int[] bergPunt(double langs) {
        double acc = 0;
        for (int i = 0; i + 1 < BERGPAD.length; i++) {
            double ax = BERGPAD[i][0], az = BERGPAD[i][1], dx = BERGPAD[i + 1][0] - ax, dz = BERGPAD[i + 1][1] - az;
            double L = Math.hypot(dx, dz);
            if (acc + L >= langs) {
                double t = (langs - acc) / L;
                return new int[] {(int) Math.round(ax + t * dx), (int) Math.round(az + t * dz)};
            }
            acc += L;
        }
        return BERGPAD[BERGPAD.length - 1];
    }

    /** How far along the mountain path (in blocks) the point nearest to (x, z) lies. */
    public static double bergLangs(double x, double z) {
        double best = Double.MAX_VALUE, uit = 0, acc = 0;
        for (int i = 0; i + 1 < BERGPAD.length; i++) {
            double ax = BERGPAD[i][0], az = BERGPAD[i][1], dx = BERGPAD[i + 1][0] - ax, dz = BERGPAD[i + 1][1] - az;
            double L = Math.hypot(dx, dz);
            double t = Math.max(0, Math.min(1, ((x - ax) * dx + (z - az) * dz) / (L * L)));
            double d = Math.hypot(x - (ax + t * dx), z - (az + t * dz));
            if (d < best) {
                best = d;
                uit = acc + t * L;
            }
            acc += L;
        }
        return uit;
    }

    /** The voorhal (x 314..346, z -404..-394, stand y 152); the puzzle rooms and the Ridderzaal are bazen's (its waves). */
    public static final BlockPos[] TEMPELRIDDER = {w("zwamtempel_voorhal", -9, 0), w("zwamtempel_voorhal", 9, 0)};
    public static final BlockPos[] SPORENPRIESTER = {w("zwamtempel_voorhal", 0, -3)};

    // ---- Knuffelkamp: the Kampklodder's home at the rim of the camp pond (the Klodderreus arena ~(54,87,1069) r18) ------------
    public static final BlockPos KAMPKLODDER = new BlockPos(70, 87, 1052);
}
