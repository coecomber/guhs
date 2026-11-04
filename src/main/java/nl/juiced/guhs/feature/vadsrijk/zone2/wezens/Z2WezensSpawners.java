package nl.juiced.guhs.feature.vadsrijk.zone2.wezens;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * The zone-2 spawners, all in kern's {@link Spawners} (it ticks them). MMO density (ronde 14: ~60-90 creatures per area):
 * every area is covered by a jittered grid of spawners (~105 blocks apart, a fixed seed so it is the same every start), each a
 * kind drawn from the area's mix, max 2-4 alive, 30-45 s respawn, 6 % elites. All surface spawners use kern's floor search from
 * {@link Z2WezensPlekken#BOVEN_Y} down (the first floor from above = the snow, a ledge or a hanging bridge; "player near" measured
 * flat), so the mountains' heights never matter. No spawner comes near a safe spot ({@link Z2WezensPlekken#VEILIG}).
 * The Spiegelburcht adds use the room mode (floor search 24 down from the room spot).
 * <p>Balans 2026-11: no spawns next to the paths ({@link Z2WezensPlekken#PADEN}, registered in kern, which refuses any spot within
 * 5 of a path edge at the path's height); a grid spawner whose circle reaches a path is moved sideways off it (same kind and
 * max, so the per-area numbers stay the same).
 */
public final class Z2WezensSpawners {
    public static final double ELITE = 0.06;
    public static final int AFSTAND = 105;

    /** One kind in an area's mix: type, weight, max alive, respawn ticks, radius. */
    private record Soort(String naam, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int gewicht, int max, int respawn, int straal) {
    }

    /** Area id -> how many may be alive at most (the sum of the spawners' max), for the dev command and the gametest. */
    public static final Map<String, Integer> MAX_PER_GEBIED = new LinkedHashMap<>();
    public static final List<Spawners.Spawner> ALLE = new ArrayList<>();

    private Z2WezensSpawners() {
    }

    private static Soort s(String naam, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int gewicht, int max, int respawn, int straal) {
        return new Soort(naam, type, gewicht, max, respawn, straal);
    }

    /** How many grid spawners were moved off a path / could not be moved (the dev command and the gametest). */
    public static int verschoven, nietVerschoven;

    /** From Z2WezensRegistry.register. */
    public static void registreer() {
        Z2WezensPlekken.PADEN.forEach((naam, pad) -> Spawners.registreerPad("z2:" + naam, pad, Z2WezensPlekken.PAD_BREEDTE));
        verschoven = nietVerschoven = 0;
        gebied(Z2WezensPlekken.GALMHELLINGEN, 2001, List.of(
                s("splinterling", Z2WezensRegistry.SPLINTERLING, 3, 3, 700, 16),
                s("galmvleermuis", Z2WezensRegistry.GALMVLEERMUIS, 2, 3, 700, 16),
                s("sneeuwknabbelaar", Z2WezensRegistry.SNEEUWKNABBELAAR, 2, 4, 900, 18),
                s("kristalnaaldkruiper", Z2WezensRegistry.KRISTALNAALDKRUIPER, 2, 3, 700, 14)));
        gebied(Z2WezensPlekken.KNUFFELMEER, 2002, List.of(
                s("ijzelgeest", Z2WezensRegistry.IJZELGEEST, 3, 3, 700, 16),
                s("schotsbijter", Z2WezensRegistry.SCHOTSBIJTER, 2, 2, 800, 14),
                s("versteende_wacht", Z2WezensRegistry.VERSTEENDE_WACHT, 2, 2, 900, 12),
                s("ijspuffer", Z2WezensRegistry.IJSPUFFER, 3, 3, 700, 16)));
        gebied(Z2WezensPlekken.SPLINTERKLOOF, 2003, List.of(
                s("mika_mijnwerker", Z2WezensRegistry.MIKA_MIJNWERKER, 3, 3, 700, 16),
                s("mika_boorder", Z2WezensRegistry.MIKA_BOORDER, 2, 3, 700, 16),
                s("kloofgier", Z2WezensRegistry.KLOOFGIER, 2, 2, 800, 18),
                s("kristalspin", Z2WezensRegistry.KRISTALSPIN, 3, 3, 700, 14)));
        // (integration) the Splinterkloof ravine floor (y ~64, z -2810..-2745, z2-wereld's mob_kloof_bodem): the surface search
        // finds the rims/bridges, so the floor gets its own spawners with a y-range around it
        int bodem = 0, j = 0;
        for (int x = -540; x <= 540; x += 90) {
            BlockPos c = new BlockPos(x, 66, -2772);
            if (Z2WezensPlekken.veilig(c.getX(), c.getZ(), 20)) {
                continue;
            }
            boolean mika = j % 2 == 0;
            Spawners.Spawner sp = new Spawners.Spawner("z2wezens:kloofbodem_" + j++ + (mika ? "_mika_mijnwerker" : "_kristalspin"), c, 14,
                    mika ? Z2WezensRegistry.MIKA_MIJNWERKER : Z2WezensRegistry.KRISTALSPIN, 3, 700, 31, 33, null, ELITE).grot(58, 74);
            Spawners.registreer(sp);
            ALLE.add(sp);
            bodem += 3;
        }
        MAX_PER_GEBIED.put("splinterkloof_bodem", bodem);
        // De Spiegelburcht: the rooms (Z2WezensPlekken.BURCHT, from the real map)
        int burcht = 0;
        for (int i = 0; i < Z2WezensPlekken.BURCHT.length; i++) {
            BlockPos p = Z2WezensPlekken.BURCHT[i];
            boolean wacht = i % 2 == 0;
            Spawners.Spawner sp = new Spawners.Spawner("z2wezens:burcht_" + i + (wacht ? "_spiegelwacht" : "_scherfgeest"), p, 5,
                    wacht ? Z2WezensRegistry.SPIEGELWACHT : Z2WezensRegistry.SCHERFGEEST, 2, 900, 33, 35, null, ELITE).grot(p.getY() - 24, p.getY() + 1);
            Spawners.registreer(sp);
            ALLE.add(sp);
            burcht += 2;
        }
        MAX_PER_GEBIED.put("spiegelburcht", burcht);
    }

    /**
     * (balans 2026-11) A grid spawner centre moved straight away from the nearest path until its circle clears every path by
     * {@link Spawners#PAD_VRIJ} (this side, the other side, then 6 / 12 further), staying in its area and out of safe spots.
     * The centre itself when already clear; null when nothing fits (kern's spawn check still keeps it off the path).
     */
    @javax.annotation.Nullable
    static int[] naastPad(Z2WezensPlekken.Gebied g, int x, int z, int straal) {
        double nodig = straal + Spawners.PAD_VRIJ + 1;
        double[] d = Spawners.dichtstbijPad(x, z);
        if (d == null || d[2] >= nodig) {
            return new int[] {x, z};
        }
        double nx = x - d[0], nz = z - d[1], L = Math.hypot(nx, nz);
        if (L < 0.5) {
            nx = -d[4];
            nz = d[3];
        } else {
            nx /= L;
            nz /= L;
        }
        double breedte = L - d[2];
        for (int verder = 0; verder <= 12; verder += 6) {
            for (int kant = 1; kant >= -1; kant -= 2) {
                double a = breedte + nodig + verder;
                int tx = (int) Math.round(d[0] + kant * nx * a), tz = (int) Math.round(d[1] + kant * nz * a);
                if (Spawners.padAfstand(tx, tz) >= nodig && g.bevat(tx, tz) && !Z2WezensPlekken.veilig(tx, tz, straal + 8)) {
                    return new int[] {tx, tz};
                }
            }
        }
        return null;
    }

    private static void gebied(Z2WezensPlekken.Gebied g, long seed, List<Soort> mix) {
        Random r = new Random(seed);
        int som = mix.stream().mapToInt(Soort::gewicht).sum(), max = 0, i = 0;
        for (int x = g.minX() + AFSTAND / 2; x <= g.maxX(); x += AFSTAND) {
            for (int z = g.minZ() + AFSTAND / 2; z <= g.maxZ(); z += AFSTAND) {
                int px = x + r.nextInt(41) - 20, pz = z + r.nextInt(41) - 20;
                int k = r.nextInt(som);
                Soort s = mix.get(0);
                for (Soort m : mix) {
                    k -= m.gewicht();
                    if (k < 0) {
                        s = m;
                        break;
                    }
                }
                if (Z2WezensPlekken.veilig(px, pz, s.straal() + 8) || !g.bevat(px, pz)) {
                    continue;
                }
                int[] naast = naastPad(g, px, pz, s.straal());
                if (naast == null) {
                    nietVerschoven++;
                } else if (naast[0] != px || naast[1] != pz) {
                    verschoven++;
                    px = naast[0];
                    pz = naast[1];
                }
                BlockPos c = new BlockPos(px, (Z2WezensPlekken.BOVEN_Y + Z2WezensPlekken.ONDER_Y) / 2, pz);
                Spawners.Spawner sp = new Spawners.Spawner("z2wezens:" + g.id() + "_" + i++ + "_" + s.naam(), c, s.straal(), s.type(), s.max(), s.respawn(),
                        g.minLevel(), g.maxLevel(), null, ELITE).grot(Z2WezensPlekken.ONDER_Y, Z2WezensPlekken.BOVEN_Y);
                Spawners.registreer(sp);
                ALLE.add(sp);
                max += s.max();
            }
        }
        MAX_PER_GEBIED.put(g.id(), max);
    }
}
