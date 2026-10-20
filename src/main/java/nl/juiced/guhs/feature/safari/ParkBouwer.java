package nl.juiced.guhs.feature.safari;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.Stempel;
import org.slf4j.Logger;

/**
 * Stamps the park: the {@link Safari#TEGELS}² tile templates {@code guhs:safari/park_<tx>_<tz>} (written by the park
 * generator, SAFARI_CONTRACT.md), one tile every {@link #PAUZE} ticks, nearest to the entrance first, so the server never
 * hangs on the whole 768 x 768 at once. The park has a version ({@link ParkData#versie}); when the jar's is higher than the
 * stamped one, every tile is cleared (entities too) and stamped again, and then the animals are spawned anew
 * ({@link Populatie}). The progress is saved per tile, so a restart halfway goes on where it was.
 * <p>
 * A tile whose template is missing gets the test plateau instead: grass at y 63 (dirt below), a cross of water through the
 * park, and an oak fence along the park's edge, so the dimension and the animals can be tried before the park generator
 * delivers.
 */
public final class ParkBouwer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PAUZE = 5;
    private static int wacht;

    public static Identifier template(int tx, int tz) {
        return Guhs.id("safari/park_" + tx + "_" + tz);
    }

    /** Is the current park version completely stamped? */
    public static boolean klaar(MinecraftServer server) {
        SafariOpslag s = SafariOpslag.get(server);
        return s.gebouwd >= ParkData.get().versie && s.bezig == 0;
    }

    /** Server start (and a first visit): a newer park version starts a (re)build. */
    static void opStart(MinecraftServer server) {
        ParkData.vergeet();
        SafariOpslag s = SafariOpslag.get(server);
        int versie = ParkData.get().versie;
        if (s.bezig != 0 && s.bezig != versie) {         // (an unfinished stamping of an older version: start over)
            s.bezig = 0;
            s.klaar = 0;
        }
        if (s.bezig == 0 && s.gebouwd < versie && s.gebouwd > 0) {
            LOGGER.info("Safari: the park template is newer ({} -> {}), rebuilding the park", s.gebouwd, versie);
            begin(server, true);
        }
    }

    /** Starts stamping the current version (a rebuild clears every tile first). */
    public static void begin(MinecraftServer server, boolean herbouw) {
        SafariOpslag s = SafariOpslag.get(server);
        s.bezig = ParkData.get().versie;
        s.klaar = 0;
        s.herbouw = herbouw;
        s.setDirty();
        wacht = 0;
    }

    /**
     * Makes sure the tile of the entrance stands (stamping it right now when needed) and that the rest is on its way.
     * False when the dimension is missing.
     */
    public static boolean zorg(MinecraftServer server) {
        ServerLevel level = Safari.level(server);
        if (level == null) {
            return false;
        }
        SafariOpslag s = SafariOpslag.get(server);
        if (s.bezig == 0 && s.gebouwd < ParkData.get().versie) {
            begin(server, s.gebouwd > 0);
        }
        if (s.bezig != 0) {
            BlockPos in = ParkData.get().ingang;
            int t = tegel(in.getX() >> 7, in.getZ() >> 7);
            if (t >= 0 && (s.klaar & (1L << t)) == 0) {
                stempelTegel(level, t, s.herbouw);
                s.klaar |= 1L << t;
                s.setDirty();
            }
        }
        return true;
    }

    private static int tegel(int tx, int tz) {
        return tx < 0 || tz < 0 || tx >= Safari.TEGELS || tz >= Safari.TEGELS ? -1 : tz * Safari.TEGELS + tx;
    }

    static void tick(MinecraftServer server) {
        SafariOpslag s = SafariOpslag.get(server);
        if (s.bezig == 0) {
            Populatie.tick(server);
            return;
        }
        if (wacht-- > 0) {
            return;
        }
        wacht = PAUZE;
        ServerLevel level = Safari.level(server);
        if (level == null) {
            return;
        }
        int volgende = volgende(s.klaar);
        if (volgende < 0) {
            s.gebouwd = s.bezig;
            s.bezig = 0;
            s.klaar = 0;
            s.setDirty();
            LOGGER.info("Safari: park versie {} stamped", s.gebouwd);
            if (s.dierenVoor < s.gebouwd) {
                Populatie.begin(server);
            }
            return;
        }
        stempelTegel(level, volgende, s.herbouw);
        s.klaar |= 1L << volgende;
        s.setDirty();
    }

    /** The next tile to stamp: the nearest to the entrance that is not done yet (-1: all done). */
    private static int volgende(long klaar) {
        BlockPos in = ParkData.get().ingang;
        List<Integer> open = new ArrayList<>();
        for (int t = 0; t < Safari.TEGELS * Safari.TEGELS; t++) {
            if ((klaar & (1L << t)) == 0) {
                open.add(t);
            }
        }
        return open.stream().min(Comparator.comparingDouble(t -> {
            double cx = (t % Safari.TEGELS) * Safari.TEGEL + Safari.TEGEL / 2.0, cz = (t / Safari.TEGELS) * Safari.TEGEL + Safari.TEGEL / 2.0;
            return (cx - in.getX()) * (cx - in.getX()) + (cz - in.getZ()) * (cz - in.getZ());
        })).orElse(-1);
    }

    /** Stamps one tile (clearing it first on a rebuild). */
    static void stempelTegel(ServerLevel level, int t, boolean herbouw) {
        int tx = t % Safari.TEGELS, tz = t / Safari.TEGELS;
        BlockPos min = new BlockPos(tx * Safari.TEGEL, 0, tz * Safari.TEGEL);
        Vec3i maat = new Vec3i(Safari.TEGEL, Safari.HOOGTE, Safari.TEGEL);
        Stempel.laad(level, min, maat);
        if (herbouw) {
            Stempel.ruim(level, Stempel.doos(min, maat));
            Stempel.leeg(level, min, maat);
        }
        Identifier id = template(tx, tz);
        if (level.getStructureManager().get(id).isPresent()) {
            Stempel.plaats(level, id, min);
        } else {
            plateau(level, min);
        }
    }

    /** The test plateau for one tile (no template yet). */
    private static void plateau(ServerLevel level, BlockPos min) {
        ParkData d = ParkData.get();
        BlockState gras = Blocks.GRASS_BLOCK.defaultBlockState(), aarde = Blocks.DIRT.defaultBlockState(), water = Blocks.WATER.defaultBlockState(),
                zand = Blocks.SAND.defaultBlockState(), hek = Blocks.OAK_FENCE.defaultBlockState(), lucht = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int midden = Safari.MAAT / 2;
        for (int x = min.getX(); x < min.getX() + Safari.TEGEL; x++) {
            for (int z = min.getZ(); z < min.getZ() + Safari.TEGEL; z++) {
                int zone = d.zone(x, z);
                boolean nat = zone >= 0 && (Math.abs(x - midden) < 6 || Math.abs(z - midden) < 6);
                for (int y = 58; y <= 62; y++) {
                    level.setBlock(p.set(x, y, z), aarde, 2 | 16);
                }
                if (nat) {
                    level.setBlock(p.set(x, 60, z), zand, 2 | 16);
                    level.setBlock(p.set(x, 61, z), water, 2 | 16);
                    level.setBlock(p.set(x, 62, z), water, 2 | 16);
                    level.setBlock(p.set(x, 63, z), lucht, 2 | 16);
                } else {
                    level.setBlock(p.set(x, 63, z), gras, 2 | 16);
                }
                if (zone >= 0 && (d.zone(x + 1, z) < 0 || d.zone(x - 1, z) < 0 || d.zone(x, z + 1) < 0 || d.zone(x, z - 1) < 0)) {
                    level.setBlock(p.set(x, 64, z), hek, 2);
                    level.setBlock(p.set(x, 65, z), hek, 2);
                }
            }
        }
    }

    private ParkBouwer() {
    }
}
