package nl.juiced.guhs.feature.safari;

import java.util.List;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;

/**
 * The park's animals: for every home of plekken.json its grown-ups (half of them males, rounded up) and young, spread over the
 * home. A respawn raises the {@link SafariOpslag#generatie generation} first, so every animal of before removes itself as soon
 * as its chunk is loaded; then the homes are filled {@link #PER_TICK} at a time (their chunks are loaded for that).
 * Runs by itself once the park of a new version is stamped, and on {@code /guhs safari dieren}.
 */
public final class Populatie {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PER_TICK = 6;
    /** The next home to fill (-1: nothing to do). Not saved: an interrupted fill starts over on the next start. */
    private static int volgende = -1;

    public static void begin(MinecraftServer server) {
        SafariOpslag s = SafariOpslag.get(server);
        s.generatie++;
        s.setDirty();
        volgende = 0;
    }

    public static boolean bezig() {
        return volgende >= 0;
    }

    static void opStart(MinecraftServer server) {
        volgende = -1;
        SafariOpslag s = SafariOpslag.get(server);
        if (s.bezig == 0 && s.gebouwd >= ParkData.get().versie && s.dierenVoor < s.gebouwd) {
            begin(server);
        }
    }

    static void tick(MinecraftServer server) {
        if (volgende < 0) {
            return;
        }
        ServerLevel level = Safari.level(server);
        if (level == null) {
            volgende = -1;
            return;
        }
        List<ParkData.Plek> plekken = ParkData.get().plekken;
        SafariOpslag s = SafariOpslag.get(server);
        for (int n = 0; n < PER_TICK && volgende < plekken.size(); n++, volgende++) {
            vul(level, plekken.get(volgende), s.generatie);
        }
        if (volgende >= plekken.size()) {
            volgende = -1;
            s.dierenVoor = Math.max(s.gebouwd, ParkData.get().versie);
            s.setDirty();
            LOGGER.info("Safari: {} homes filled (generation {})", plekken.size(), s.generatie);
        }
    }

    /** Puts the family of one home in the world; returns how many animals. */
    public static int vul(ServerLevel level, ParkData.Plek p, int generatie) {
        ParkData d = ParkData.get();
        ParkData.Soort soort = d.soort(p.soort());
        // the zone of the home is where its centre lies (outside the raster: no zone check rather than a stuck animal)
        int zone = d.zone(p.centrum().getX(), p.centrum().getZ());
        String gevraagd = p.zone().isEmpty() && soort != null ? soort.zone() : p.zone();
        if (zone >= 0 && !gevraagd.isEmpty() && zone != d.zoneIndex(gevraagd) && !d.testPlateau) {
            LOGGER.warn("Safari: home {} of {} lies in zone {}, not in {}", p.id(), p.soort(), d.zoneNaam(zone), gevraagd);
        }
        RandomSource rnd = RandomSource.create(p.id() * 7919L + generatie);
        int n = 0;
        int mannen = (p.volwassenen() + 1) / 2;
        for (int i = 0; i < p.volwassenen() + p.jongen(); i++) {
            String variant = i < mannen ? "man" : i < p.volwassenen() ? "vrouw" : "baby";
            SafariDier dier = SafariFeature.DIER.get().create(level, EntitySpawnReason.STRUCTURE);
            if (dier == null) {
                continue;
            }
            dier.zet(p.soort(), variant, p.centrum(), p.straal(), zone, p.id(), generatie);
            nl.juiced.guhs.feature.safari.gedrag.Bewoners.Bewoner bewoner = nl.juiced.guhs.feature.safari.gedrag.Bewoners.voor(p, i);
            if (bewoner != null) {
                dier.zetBewoner(bewoner.id(), bewoner.naam());      // round 3: a "bekende bewoner" (name tag, Safari dex list)
            }
            double hoek = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * p.straal() * 0.7;
            int x = p.centrum().getX() + (int) Math.round(Math.cos(hoek) * r), z = p.centrum().getZ() + (int) Math.round(Math.sin(hoek) * r);
            if (zone >= 0 && d.zone(x, z) != zone) {
                x = p.centrum().getX();
                z = p.centrum().getZ();
            }
            level.getChunk(x >> 4, z >> 4);
            BlockPos plek = null;
            if (soort != null && soort.hangt()) {
                // the sloth hangs under a low branch near its home (visible from the ground and the paths)
                plek = hangPlek(level, x, z, Mth.ceil(dier.getBbHeight()));
                if (plek != null) {
                    dier.hang(plek);
                }
            }
            if (plek == null) {
                plek = p.boom() ? boom(level, x, z) : dier.grond(level, x, z);
            }
            dier.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, rnd.nextFloat() * 360f, 0f);
            if (level.addFreshEntity(dier)) {
                n++;
            }
        }
        return n;
    }

    /** A spot to hang under the lowest leaves near x,z (2..14 above the ground, with room below): its feet position. */
    @javax.annotation.Nullable
    static BlockPos hangPlek(ServerLevel level, int x, int z, int hoogte) {
        for (int r = 0; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int cx = x + dx, cz = z + dz;
                    level.getChunk(cx >> 4, cz >> 4);
                    int grond = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
                    for (int y = grond + 2; y < grond + 14; y++) {
                        BlockPos b = new BlockPos(cx, y, cz);
                        if (level.getBlockState(b).is(BlockTags.LEAVES)) {
                            int voet = y - hoogte;
                            boolean vrij = voet > grond;
                            for (int k = voet; vrij && k < y; k++) {
                                vrij = level.getBlockState(new BlockPos(cx, k, cz)).isAir();
                            }
                            if (vrij) {
                                return new BlockPos(cx, voet, cz);
                            }
                            break;
                        }
                        if (!level.getBlockState(b).isAir()) {
                            break;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** On top of whatever stands here, leaves included (tree animals start on their branch). */
    private static BlockPos boom(ServerLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z), z);
    }

    private Populatie() {
    }
}
