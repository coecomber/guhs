package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.util.RandomSource;

/**
 * 1101-balans: the loot pool of a zone. Quest rewards, dungeon chests and cave elites/treasures used to pick from ALL base types
 * of all zones (how Gorgonzola's moersleutel, a zone-5 boss Legendary, dropped in zone 1). Now they pick only from the base types
 * in the zone's own creature tables ({@code wezens:*} for zone 1, {@code z2wezens:*} .. {@code z6wezens:*}), without named
 * boss/raid/dream items ({@link #benoemd}: those only drop from their own boss) and without craft-only Maatwerk ({@code mw_*}).
 * <p>The tables are read at call time (items' {@code lootErbij} adds pieces at server start, so a cache could miss them).
 */
public final class ZonePools {
    public static final int ZONES = 6;

    private ZonePools() {
    }

    /** The creature-table prefix of a zone: "wezens:" (zone 1), "z2wezens:" .. "z6wezens:". */
    public static String prefix(int zone) {
        return zone <= 1 ? "wezens:" : "z" + zone + "wezens:";
    }

    /** A named boss/raid/dream item ({@code baas_*}, {@code zNbaas_*}, {@code z5droom_*}, {@code z6raid_*}). */
    public static boolean benoemd(String id) {
        return id.startsWith("baas_") || id.matches("z\\dbaas_.*") || id.startsWith("z5droom_") || id.startsWith("z6raid_");
    }

    /** Craft-only (Maatwerk). */
    public static boolean maatwerk(String id) {
        return id.startsWith("mw_");
    }

    /** Is this a creature table of the zone (not a contract/weapons list)? */
    static boolean wezensTabel(String tabelId, int zone) {
        if (!tabelId.startsWith(prefix(zone))) {
            return false;
        }
        String naam = tabelId.substring(prefix(zone).length());
        return !naam.startsWith("contract_") && !naam.equals("wapens");
    }

    /** Every base type id of the zone's pool (all classes), in registration-table order. */
    public static Set<String> ids(int zone) {
        int z = Math.max(1, Math.min(ZONES, zone));
        Set<String> uit = new LinkedHashSet<>();
        for (Map.Entry<String, LootTabel> e : LootTabellen.alle().entrySet()) {
            if (!wezensTabel(e.getKey(), z)) {
                continue;
            }
            for (LootTabel.Regel r : e.getValue().regels()) {
                String id = r.basisType();
                if (!benoemd(id) && !maatwerk(id) && BasisTypes.van(id) != null) {
                    uit.add(id);
                }
            }
        }
        return uit;
    }

    /** The pool for a player of class {@code klasse}: the zone's types that are not a weapon of another class. */
    public static List<BasisType> voorZone(int zone, @Nullable String klasse) {
        List<BasisType> l = new ArrayList<>();
        for (String id : ids(zone)) {
            BasisType t = BasisTypes.van(id);
            if (t != null && (t.slot() != Slot.WAPEN || klasse == null || klasse.equals(t.klasse()))) {
                l.add(t);
            }
        }
        return l;
    }

    /** A random piece of the zone's pool for that class (null when the pool is empty). */
    @Nullable
    public static BasisType stuk(int zone, @Nullable String klasse, RandomSource r) {
        List<BasisType> l = voorZone(zone, klasse);
        return l.isEmpty() ? null : l.get(r.nextInt(l.size()));
    }

    /**
     * The zone a base type belongs to, for the one-time cleanup: named items by their prefix ({@code baas_} = 1, {@code zN...} = N),
     * else the LOWEST zone whose creature tables drop it (zone 2's tables also drop zone 1's weapons); 0 = unknown (crafted,
     * quest-only, economy...: never treated as out of tier).
     */
    public static int zoneVan(String id) {
        if (maatwerk(id)) {
            return 0;
        }
        if (id.startsWith("baas_")) {
            return 1;
        }
        if (benoemd(id)) {
            return id.charAt(1) - '0';
        }
        for (int z = 1; z <= ZONES; z++) {
            for (Map.Entry<String, LootTabel> e : LootTabellen.alle().entrySet()) {
                if (wezensTabel(e.getKey(), z)) {
                    for (LootTabel.Regel r : e.getValue().regels()) {
                        if (r.basisType().equals(id)) {
                            return z;
                        }
                    }
                }
            }
        }
        return 0;
    }
}
