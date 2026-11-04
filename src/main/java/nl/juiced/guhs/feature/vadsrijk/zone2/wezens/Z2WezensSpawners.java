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
        // (integration) the Splinterkloof ravine floor (y ~64, z2-wereld's kloof_bodem_N spots): the surface search
        // finds the rims/bridges, so the floor gets its own spawners with a y-range around it
        int bodem = 0, j = 0;
        for (int n = 1; nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.plek("kloof_bodem_" + n) != null; n++) {
            BlockPos c = nl.juiced.guhs.feature.vadsrijk.zone2.wereld.Z2WereldPlekken.plek("kloof_bodem_" + n).above();
            if (Z2WezensPlekken.veilig(c.getX(), c.getZ(), 20)) {
                continue;
            }
            boolean mika = j % 2 == 0;
            Spawners.Spawner sp = new Spawners.Spawner("z2wezens:kloofbodem_" + j++ + (mika ? "_mika_mijnwerker" : "_kristalspin"), c, 14,
                    mika ? Z2WezensRegistry.MIKA_MIJNWERKER : Z2WezensRegistry.KRISTALSPIN, 3, 700, 27, 29, null, ELITE).grot(58, 74);
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
                    wacht ? Z2WezensRegistry.SPIEGELWACHT : Z2WezensRegistry.SCHERFGEEST, 2, 900, 28, 30, null, ELITE).grot(p.getY() - 24, p.getY() + 1);
            Spawners.registreer(sp);
            ALLE.add(sp);
            burcht += 2;
        }
        MAX_PER_GEBIED.put("spiegelburcht", burcht);
    }

    /** One planned spawner (before registering). */
    private record Plan(int x, int z, Soort s, int straal) {
    }

    /**
     * (fix round 2) The compact zone: a jittered grid whose spacing is chosen per area so the area holds ~60-100 creatures
     * (MMO-dense); no spawner within its radius + 5 of a path (user rule: no spawns within ~5 blocks of a path) or near a
     * safe spot. A spawner close to a path gets a smaller radius (min 6) instead of being dropped.
     */
    private static void gebied(Z2WezensPlekken.Gebied g, long seed, List<Soort> mix) {
        List<Plan> plan = List.of();
        for (int afstand = 18; afstand <= 140; afstand += 3) {
            plan = plan(g, seed, mix, afstand);
            int max = plan.stream().mapToInt(p -> p.s().max()).sum();
            if (max <= 100) {
                break;
            }
        }
        int max = 0, i = 0;
        for (Plan p : plan) {
            Soort s = p.s();
            BlockPos c = new BlockPos(p.x(), (Z2WezensPlekken.BOVEN_Y + Z2WezensPlekken.ONDER_Y) / 2, p.z());
            Spawners.Spawner sp = new Spawners.Spawner("z2wezens:" + g.id() + "_" + i++ + "_" + s.naam(), c, p.straal(), s.type(), s.max(), s.respawn(),
                    g.minLevel(), g.maxLevel(), null, ELITE).grot(Z2WezensPlekken.ONDER_Y, Z2WezensPlekken.BOVEN_Y);
            Spawners.registreer(sp);
            ALLE.add(sp);
            max += s.max();
        }
        MAX_PER_GEBIED.put(g.id(), max);
    }

    private static List<Plan> plan(Z2WezensPlekken.Gebied g, long seed, List<Soort> mix, int afstand) {
        Random r = new Random(seed);
        int som = mix.stream().mapToInt(Soort::gewicht).sum();
        int jitter = Math.max(4, afstand / 3);
        List<Plan> uit = new ArrayList<>();
        for (int x = g.minX() + afstand / 2; x <= g.maxX(); x += afstand) {
            for (int z = g.minZ() + afstand / 2; z <= g.maxZ(); z += afstand) {
                int px = x + r.nextInt(2 * jitter + 1) - jitter, pz = z + r.nextInt(2 * jitter + 1) - jitter;
                int k = r.nextInt(som);
                Soort s = mix.get(0);
                for (Soort m : mix) {
                    k -= m.gewicht();
                    if (k < 0) {
                        s = m;
                        break;
                    }
                }
                // g-int: ONE rule with balans/kern: the circle stays PAD_VRIJ (5) from every path EDGE (kern Spawners.padAfstand)
                int straal = Math.min(s.straal(), (int) Math.floor(Spawners.padAfstand(px, pz) - Spawners.PAD_VRIJ));
                if (straal < 6 || Z2WezensPlekken.veilig(px, pz, straal + 8) || !g.bevat(px, pz)) {
                    continue;
                }
                uit.add(new Plan(px, pz, s, straal));
            }
        }
        return uit;
    }
}
