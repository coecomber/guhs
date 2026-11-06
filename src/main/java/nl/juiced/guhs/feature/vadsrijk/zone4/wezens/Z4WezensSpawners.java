package nl.juiced.guhs.feature.vadsrijk.zone4.wezens;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.world.entity.EntityType;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * The zone-4 spawners (kern's {@link Spawners} ticks them): ONE small spawner per region of {@link Z4WezensPlekken}, each with
 * one kind picked by the island's mix (round-robin, so an island shows its variety), max 2-3 alive, a slow respawn (light
 * combat: ~40 creatures in the whole zone at most). Elites are rarer than elsewhere (4 %). Nothing spawns in a safe spot.
 */
public final class Z4WezensSpawners {
    public static final double ELITE = 0.04;

    record Soort(String naam, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int max, int respawn) {
    }

    public static final List<Spawners.Spawner> ALLE = new ArrayList<>();
    /** Island -> the sum of its spawners' max (dev command + gametest). */
    public static final Map<String, Integer> MAX_PER_EILAND = new LinkedHashMap<>();

    private static boolean geregistreerd;

    private Z4WezensSpawners() {
    }

    private static List<Soort> mix(String eiland) {
        Soort krab = new Soort("mopperkrab", Z4WezensRegistry.MOPPERKRAB, 3, 900);
        Soort meeuw = new Soort("mokmeeuw", Z4WezensRegistry.MOKMEEUW, 2, 900);
        Soort kwal = new Soort("lampionkwal", Z4WezensRegistry.LAMPIONKWAL, 2, 900);
        Soort vlieger = new Soort("bromvlieger", Z4WezensRegistry.BROMVLIEGER, 2, 900);
        Soort slak = new Soort("zeurslak", Z4WezensRegistry.ZEURSLAK, 2, 1000);
        Soort kobold = new Soort("koraalkobold", Z4WezensRegistry.KORAALKOBOLD, 3, 800);
        Soort rob = new Soort("pruilrob", Z4WezensRegistry.PRUILROB, 2, 1000);
        Soort pluis = new Soort("stekelpluis", Z4WezensRegistry.STEKELPLUIS, 2, 900);
        Soort mist = new Soort("mistmopje", Z4WezensRegistry.MISTMOPJE, 2, 900);
        Soort knecht = new Soort("vliegerknecht", Z4WezensRegistry.VLIEGERKNECHT, 2, 1200);
        return switch (eiland) {
            case "koraalbaai" -> List.of(kobold, krab, kwal, slak, pluis);
            case "vliegereiland" -> List.of(vlieger, meeuw, vlieger);
            case "bloemeneiland" -> List.of(slak, meeuw);
            case "mysterie" -> List.of(mist, kwal, mist);
            case "wrak" -> List.of(knecht, knecht, new Soort("bromvlieger", Z4WezensRegistry.BROMVLIEGER, 1, 1200), knecht);
            default -> List.of(krab, rob, pluis, krab, meeuw);   // rotsjes
        };
    }

    /** From Z4WezensRegistry.register (again from the dev command after the POI changed). */
    public static void registreer() {
        if (!geregistreerd) {
            geregistreerd = true;
            Spawners.VERBODEN.add((sl, p) -> Z4WezensPlekken.veilig(p.getX() + 0.5, p.getY(), p.getZ() + 0.5));
            Spawners.VEILIGE_PLEKKEN.addAll(Z4WezensPlekken.kampvuren());   // t-z4wereld's campfires (reflection; empty without it)
        }
        ALLE.clear();
        MAX_PER_EILAND.clear();
        Map<String, Integer> teller = new LinkedHashMap<>();
        for (Z4WezensPlekken.Regio r : Z4WezensPlekken.regios()) {
            List<Soort> m = mix(r.eiland());
            int i = teller.merge(r.eiland(), 1, Integer::sum) - 1;
            Soort s = m.get(i % m.size());
            Spawners.Spawner sp = new Spawners.Spawner("z4wezens:" + r.id() + "#" + s.naam(), r.centrum(), r.straal(), s.type(), s.max(), s.respawn(),
                    r.minLevel(), r.maxLevel(), r.eiland().equals("wrak") ? Element.VUUR : null, ELITE);
            if (r.grot()) {
                sp = sp.grot(r.minY(), r.maxY());   // the wreck's rooms below deck / the deck: floor search inside that height only
            }
            Spawners.registreer(sp);
            ALLE.add(sp);
            MAX_PER_EILAND.merge(r.eiland(), s.max(), Integer::sum);
        }
    }

    public static int maxTotaal() {
        return MAX_PER_EILAND.values().stream().mapToInt(Integer::intValue).sum();
    }
}
