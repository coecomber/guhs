package nl.juiced.guhs.feature.vadsrijk.zone5.wezens;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * The zone-5 spawners: all in kern cave mode ({@link Spawners.Spawner#grot}: floor search inside a y-window), because everything
 * in Hoogvads is high up (y 176-240) or inside the whale. Per place:
 * <ul>
 *   <li>Proefarena: 4 Bezeten Oefenpoppen, 2 Sparringridders (fixed, never elite), leader Opperpop Strobaal;</li>
 *   <li>Buikgangen: Ribbengang 3 saboteurs + 4 mites, Kleppenzaal 2 saboteurs + 2 lontleggers + 3 mites (respawn 90 s, dungeon trash);</li>
 *   <li>Wolkrand: per fin 4 Wolkduivels, 2 Mistsliers, 2 harpooners; tail north lobe 3 Wolkduivels + 2 Mistsliers, leader Moeder Mist;</li>
 *   <li>Schuilnest: 3 Vermomde Mika's, 2 saboteurs, 2 lontleggers, leader Meester-Vermommer Feta.</li>
 * </ul>
 * ~45 alive at most in all of zone 5 (it is a city, combat is a side dish). Elites 6 % (kern affixes). Leaders: elite, lv max + 2,
 * respawn 5 min.
 */
public final class Z5WezensSpawners {
    public static final List<Spawners.Spawner> ALLE = new ArrayList<>();
    public static final Map<String, Integer> MAX_PER_PLEK = new LinkedHashMap<>();
    public static final double ELITE = 0.06;

    private Z5WezensSpawners() {
    }

    private static void s(String plek, String naam, BlockPos c, int straal, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int max, int respawn,
            int minLv, int maxLv, double elite, int onder, int boven) {
        Spawners.Spawner sp = new Spawners.Spawner("z5wezens:" + plek + "_" + naam, c, straal, type, max, respawn, minLv, maxLv, null, elite)
                .grot(c.getY() - onder, c.getY() + boven);
        Spawners.registreer(sp);
        ALLE.add(sp);
        MAX_PER_PLEK.merge(plek, max, Integer::sum);
    }

    public static void registreer() {
        if (!ALLE.isEmpty()) {
            return;
        }
        BlockPos pa = Z5WezensPlekken.PROEFARENA;
        s("proefarena", "oefenpop", pa, 8, Z5WezensRegistry.BEZETEN_OEFENPOP, 4, 900, 56, 57, ELITE, 4, 6);
        s("proefarena", "sparringridder", pa.offset(-6, 0, 6), 3, Z5WezensRegistry.SPARRINGRIDDER, 2, 400, 56, 56, 0, 4, 6);
        int by = 2, bb = 6;
        s("buik", "ribben_saboteur", Z5WezensPlekken.RIBBENGANG, 14, Z5WezensRegistry.MIKA_SABOTEUR, 3, 1800, 57, 58, ELITE, by, bb);
        s("buik", "ribben_mijt", Z5WezensPlekken.RIBBENGANG, 14, Z5WezensRegistry.GRAUWE_PLUISMIJT, 4, 1800, 57, 58, ELITE, by, bb);
        s("buik", "kleppen_saboteur", Z5WezensPlekken.KLEPPENZAAL, 6, Z5WezensRegistry.MIKA_SABOTEUR, 2, 1800, 58, 59, ELITE, by, bb);
        s("buik", "kleppen_lont", Z5WezensPlekken.KLEPPENZAAL, 6, Z5WezensRegistry.MIKA_LONTLEGGER, 2, 1800, 58, 59, ELITE, by, bb);
        s("buik", "kleppen_mijt", Z5WezensPlekken.KLEPPENZAAL, 6, Z5WezensRegistry.GRAUWE_PLUISMIJT, 3, 1800, 58, 59, ELITE, by, bb);
        for (String kant : List.of("zuid", "noord")) {
            BlockPos v = kant.equals("zuid") ? Z5WezensPlekken.ZUIDVIN : Z5WezensPlekken.NOORDVIN;
            s("wolkrand_" + kant, "wolkduivel", v, 12, Z5WezensRegistry.GRAUWE_WOLKDUIVEL, 4, 900, 58, 59, ELITE, 12, 10);
            s("wolkrand_" + kant, "mistslier", v, 12, Z5WezensRegistry.MISTSLIER, 2, 900, 58, 60, ELITE, 12, 10);
            s("wolkrand_" + kant, "harpoenier", v, 10, Z5WezensRegistry.MIKA_HARPOENIER, 2, 900, 58, 60, ELITE, 12, 10);
        }
        s("staartvlakte", "wolkduivel", Z5WezensPlekken.STAARTVLAKTE, 14, Z5WezensRegistry.GRAUWE_WOLKDUIVEL, 3, 900, 59, 60, ELITE, 10, 10);
        s("staartvlakte", "mistslier", Z5WezensPlekken.STAARTVLAKTE, 14, Z5WezensRegistry.MISTSLIER, 2, 900, 59, 60, ELITE, 10, 10);
        BlockPos sn = Z5WezensPlekken.SCHUILNEST;
        s("schuilnest", "vermomd", sn, 10, Z5WezensRegistry.VERMOMDE_MIKA, 3, 1200, 58, 60, ELITE, 2, 5);
        s("schuilnest", "saboteur", sn.offset(-6, 0, 0), 6, Z5WezensRegistry.MIKA_SABOTEUR, 2, 1200, 58, 59, ELITE, 2, 5);
        s("schuilnest", "lont", sn.offset(8, 0, 0), 6, Z5WezensRegistry.MIKA_LONTLEGGER, 2, 1200, 58, 59, ELITE, 2, 5);
        leider("strobaal", Z5WezensRegistry.BEZETEN_OEFENPOP, 59);
        leider("mistmoeder", Z5WezensRegistry.MISTSLIER, 62);
        leider("meestervermommer", Z5WezensRegistry.VERMOMDE_MIKA, 62);
    }

    private static void leider(String naam, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int lv) {
        BlockPos p = Z5WezensPlekken.LEIDERS.get(naam);
        Spawners.Spawner sp = new Spawners.Spawner("z5wezens:leider_" + naam, p, 3, type, 1, 6000, lv, lv, null, 1.0).grot(p.getY() - 10, p.getY() + 6);
        Spawners.registreer(sp);
        ALLE.add(sp);
    }

    public static int totaalMax() {
        int n = 0;
        for (Spawners.Spawner s : ALLE) {
            n += s.max();
        }
        return n;
    }
}
