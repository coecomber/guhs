package nl.juiced.guhs.feature.vadsrijk.zone6.wezens;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * The zone-6 spawners (kern {@link Spawners}), per area a small mix, slow respawn, ~70 alive at most in the whole 900 x 600 zone (incl. the dungeon, the cave and the leaders):
 * <ul>
 *   <li>De Roze Duinen: Zandvlooien (swarms of 4), Fennekguhs (pairs), Springpiepen, Duinkruipers, a Lampendover;</li>
 *   <li>De Palmenkloven: Dadelaapjes, Gierguhs, Duinkruipers, Lampendovers;</li>
 *   <li>De Zoutvlakte: Spiegelslangen, Zoutkreeften, Duinkruipers; at NIGHT Zandgeestjes on their own spot;</li>
 *   <li>Het Ruïnedal: Ruïnewachters, Lampendovers, a Ringdrager, a Spiegelslang;</li>
 *   <li>De Moederoase: Gedoofde sterretjes, Ringdragers, a Lampendover;</li>
 *   <li>the dungeon rooms (cave mode): the Ingangshal and the Kaartenzaal (Lampendovers, a Ruïnewachter, a Ringdrager, a sterretje);</li>
 *   <li>De Zingende Zoutgrot: the elite De Zoutslang (max 1, 5 min);</li>
 *   <li>one named area leader per area (elite, lv max + 2..3, 5 min).</li>
 * </ul>
 * No spawns in the cities, the caravan camp or at the tower foot ({@link Z6WezensPlekken#veilig}), nor Zandgeestjes by day.
 */
public final class Z6WezensSpawners {
    public static final double ELITE = 0.05;
    public static final List<Spawners.Spawner> ALLE = new ArrayList<>();
    public static final Map<String, Integer> MAX_PER_GEBIED = new LinkedHashMap<>();
    private static boolean geregistreerd;

    record Soort(String naam, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int max, int respawn) {
        Soort metMax(int n) {
            return new Soort(naam, type, n, respawn);
        }
    }

    private Z6WezensSpawners() {
    }

    private static List<Soort> mix(String gebied) {
        Soort vlo = new Soort("zandvlooi", Z6WezensRegistry.ZANDVLOOI, 4, 700);
        Soort fennek = new Soort("fennekguh", Z6WezensRegistry.FENNEKGUH, 2, 900);
        Soort piep = new Soort("springpiep", Z6WezensRegistry.SPRINGPIEP, 2, 900);
        Soort kruiper = new Soort("duinkruiper", Z6WezensRegistry.DUINKRUIPER, 2, 900);
        Soort dover = new Soort("mika_lampendover", Z6WezensRegistry.MIKA_LAMPENDOVER, 2, 1000);
        Soort aap = new Soort("dadelaapje", Z6WezensRegistry.DADELAAPJE, 3, 900);
        Soort gier = new Soort("gierguh", Z6WezensRegistry.GIERGUH, 2, 1000);
        Soort slang = new Soort("spiegelslang", Z6WezensRegistry.SPIEGELSLANG, 2, 1000);
        Soort kreeft = new Soort("zoutkreeft", Z6WezensRegistry.ZOUTKREEFT, 3, 900);
        Soort wachter = new Soort("ruinewachter", Z6WezensRegistry.RUINEWACHTER, 3, 1100);
        Soort ring = new Soort("mika_ringdrager", Z6WezensRegistry.MIKA_RINGDRAGER, 1, 1400);
        Soort ster = new Soort("gedoofd_sterretje", Z6WezensRegistry.GEDOOFD_STERRETJE, 3, 1000);
        return switch (gebied) {
            case "duinen" -> List.of(vlo, fennek, piep, kruiper.metMax(2), dover.metMax(1));
            case "palmenkloven" -> List.of(aap, gier, kruiper);
            case "zoutvlakte" -> List.of(kreeft, slang, kruiper);
            case "ruinedal" -> List.of(wachter, dover, ring.metMax(1));
            default -> List.of(ster, ring.metMax(2), dover.metMax(1));
        };
    }

    /** From Z6WezensRegistry.register. */
    public static void registreer() {
        if (!geregistreerd) {
            geregistreerd = true;
            Spawners.VERBODEN.add((sl, p) -> Z6WezensPlekken.veilig(p.getX() + 0.5, p.getZ() + 0.5));
            // Zandgeestjes only at night (their spot is their own)
            Spawners.VERBODEN.add((sl, p) -> sl.isBrightOutside() && Z6WezensPlekken.ZOUT_NACHT.distToCenterSqr(p.getX(), Z6WezensPlekken.ZOUT_NACHT.getY(), p.getZ())
                    <= (Z6WezensPlekken.ZOUT_NACHT_STRAAL + 2) * (Z6WezensPlekken.ZOUT_NACHT_STRAAL + 2));
        }
        ALLE.clear();
        MAX_PER_GEBIED.clear();
        Map<String, Integer> teller = new LinkedHashMap<>();
        for (Z6WezensPlekken.Regio r : Z6WezensPlekken.REGIOS) {
            List<Soort> m = mix(r.gebied());
            Z6WezensPlekken.Gebied g = Z6WezensPlekken.gebied(r.gebied());
            int i = teller.merge(r.gebied(), 1, Integer::sum) - 1;
            for (int k = 0; k < 2; k++) {   // two kinds per spot (a mixed little group)
                Soort s = m.get((i + k) % m.size());
                s(r.gebied(), r.id() + "#" + s.naam(), r.centrum(), r.straal(), s.type(), Math.max(1, s.max() - k), s.respawn(), g.minLevel(), g.maxLevel(),
                        null, ELITE, false);
            }
        }
        s("zoutvlakte", "z6w_zout_nacht#zandgeestje", Z6WezensPlekken.ZOUT_NACHT, Z6WezensPlekken.ZOUT_NACHT_STRAAL, Z6WezensRegistry.ZANDGEESTJE, 4, 800,
                65, 66, null, ELITE, false);
        // the dungeon rooms (cave mode between the floor and the ceiling)
        s("kerker", "kerker_ingang#dover", Z6WezensPlekken.INGANGSHAL.offset(3, 0, 0), 4, Z6WezensRegistry.MIKA_LAMPENDOVER, 2, 1200, 66, 67, Element.VUUR, ELITE, true);
        s("kerker", "kerker_ingang#wachter", Z6WezensPlekken.INGANGSHAL.offset(5, 0, 3), 2, Z6WezensRegistry.RUINEWACHTER, 1, 1200, 66, 67, null, ELITE, true);
        s("kerker", "kerker_kaarten#dover", Z6WezensPlekken.KAARTENZAAL, 6, Z6WezensRegistry.MIKA_LAMPENDOVER, 2, 1200, 67, 67, Element.VUUR, ELITE, true);
        s("kerker", "kerker_kaarten#ring", Z6WezensPlekken.KAARTENZAAL.offset(4, 0, 0), 3, Z6WezensRegistry.MIKA_RINGDRAGER, 1, 1200, 67, 68, Element.VUUR, ELITE, true);
        s("kerker", "kerker_kaarten#ster", Z6WezensPlekken.KAARTENZAAL.offset(-4, 0, 3), 3, Z6WezensRegistry.GEDOOFD_STERRETJE, 1, 1200, 67, 67, null, ELITE, true);
        // the cave elite
        BlockPos e = Z6WezensPlekken.ZOUTGROT_ELITE;
        Spawners.Spawner zs = new Spawners.Spawner("z6wezens:grot_zoutslang", e, 4, Z6WezensRegistry.ZOUTSLANG, 1, 6000, 66, 66, Element.WATER, 1.0)
                .grot(e.getY() - 6, e.getY() + 6);
        Spawners.registreer(zs);
        ALLE.add(zs);
        MAX_PER_GEBIED.merge("zoutgrot", 1, Integer::sum);
        // the area leaders
        leider("oorschelp", Z6WezensRegistry.FENNEKGUH, 63);
        leider("pitje", Z6WezensRegistry.DADELAAPJE, 65);
        leider("korstje", Z6WezensRegistry.ZOUTKREEFT, 67);
        leider("oudstewachter", Z6WezensRegistry.RUINEWACHTER, 69);
        leider("brie", Z6WezensRegistry.MIKA_RINGDRAGER, 70);
    }

    private static void s(String gebied, String naam, BlockPos c, int straal, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int max, int respawn,
            int minLv, int maxLv, Element e, double elite, boolean grot) {
        Spawners.Spawner sp = new Spawners.Spawner("z6wezens:" + naam, c, straal, type, max, respawn, minLv, maxLv, e, elite);
        if (grot) {
            sp = sp.grot(c.getY() - 1, c.getY() + 4);
        }
        Spawners.registreer(sp);
        ALLE.add(sp);
        MAX_PER_GEBIED.merge(gebied, max, Integer::sum);
    }

    private static void leider(String naam, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int lv) {
        BlockPos p = Z6WezensPlekken.LEIDERS.get(naam);
        Spawners.Spawner sp = new Spawners.Spawner("z6wezens:leider_" + naam, p, 3, type, 1, 6000, lv, lv, null, 1.0);
        Spawners.registreer(sp);
        ALLE.add(sp);
        MAX_PER_GEBIED.merge("leiders", 1, Integer::sum);
    }

    public static int maxTotaal() {
        return MAX_PER_GEBIED.values().stream().mapToInt(Integer::intValue).sum();
    }
}
