package nl.juiced.guhs.feature.safari;

import java.util.List;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
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
        int zone = d.zoneIndex(p.zone().isEmpty() && soort != null ? soort.zone() : p.zone());
        if (zone >= 0 && d.zone(p.centrum().getX(), p.centrum().getZ()) < 0) {
            zone = -1;            // (a home whose centre lies outside the zone raster: no zone check rather than a stuck animal)
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
            double hoek = rnd.nextDouble() * Math.PI * 2, r = Math.sqrt(rnd.nextDouble()) * p.straal() * 0.7;
            int x = p.centrum().getX() + (int) Math.round(Math.cos(hoek) * r), z = p.centrum().getZ() + (int) Math.round(Math.sin(hoek) * r);
            if (zone >= 0 && d.zone(x, z) != zone) {
                x = p.centrum().getX();
                z = p.centrum().getZ();
            }
            level.getChunk(x >> 4, z >> 4);
            BlockPos plek = p.boom() ? boom(level, x, z) : dier.grond(level, x, z);
            dier.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, rnd.nextFloat() * 360f, 0f);
            if (level.addFreshEntity(dier)) {
                n++;
            }
        }
        return n;
    }

    /** On top of whatever stands here, leaves included (tree animals start on their branch). */
    private static BlockPos boom(ServerLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z), z);
    }

    private Populatie() {
    }
}
