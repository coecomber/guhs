package nl.juiced.guhs.feature.beauty;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nl.juiced.guhs.entity.GuhClothes;

/**
 * The themes of the guh beauty contest ("Winterpret", "Feestje!"...), and how well every piece of the loaner wardrobe
 * fits each theme: 3 = perfect, 2 = good, 1 = so-so, nothing = off theme. The wardrobe is exactly the pieces listed here.
 */
public enum ShowTheme {
    WINTER, FEEST, WERK, SPROOKJES, GRIEZELIG, ZOMER, GALA, KONINGSDAG, HOLLANDS_WEER;

    /** The slots the model is dressed in (the back is for backpacks, not for the catwalk). */
    public static final GuhClothes.Slot[] SLOTS = {GuhClothes.Slot.HEAD, GuhClothes.Slot.EYES, GuhClothes.Slot.NECK, GuhClothes.Slot.BODY};

    private static final Map<GuhClothes, Map<ShowTheme, Integer>> FIT = new LinkedHashMap<>();
    /** Pieces that belong together (a whole look): wearing two or more of one set pleases the style judge. */
    private static final List<List<GuhClothes>> SETS = new ArrayList<>();
    /** The Showster set (only from the Showguh's shop): the jury adores it, but it isn't in the loaner wardrobe. */
    public static final List<GuhClothes> SHOWSTER = List.of(GuhClothes.SHOWSTER_TIARA, GuhClothes.SHOWSTER_SJERP, GuhClothes.SHOWSTER_STRIK);

    private static void fit(GuhClothes clothes, Object... themeAndScore) {
        Map<ShowTheme, Integer> map = new EnumMap<>(ShowTheme.class);
        for (int i = 0; i < themeAndScore.length; i += 2) {
            map.put((ShowTheme) themeAndScore[i], (Integer) themeAndScore[i + 1]);
        }
        FIT.put(clothes, map);
    }

    static {
        // heads
        fit(GuhClothes.RAIN_HAT, HOLLANDS_WEER, 3, WERK, 1);
        fit(GuhClothes.PARTY_HAT, FEEST, 3, KONINGSDAG, 1);
        fit(GuhClothes.CHEF_HAT, WERK, 3);
        fit(GuhClothes.FIREFIGHTER_HELMET, WERK, 3);
        fit(GuhClothes.POLICE_CAP, WERK, 3);
        fit(GuhClothes.BUILDER_HELMET, WERK, 3);
        fit(GuhClothes.STRAW_HAT, ZOMER, 3, WERK, 1);
        fit(GuhClothes.SANTA_HAT, WINTER, 3, FEEST, 2);
        fit(GuhClothes.SINT_MITRE, WINTER, 2, FEEST, 2, SPROOKJES, 1);
        fit(GuhClothes.PIET_BERET, FEEST, 2, WINTER, 1);
        fit(GuhClothes.WITCH_HAT, GRIEZELIG, 3, SPROOKJES, 2);
        fit(GuhClothes.PUMPKIN_HEAD, GRIEZELIG, 3);
        fit(GuhClothes.ORANGE_CROWN, KONINGSDAG, 3, FEEST, 1);
        fit(GuhClothes.WIZARD_HAT, SPROOKJES, 3, GRIEZELIG, 1);
        fit(GuhClothes.KNIGHT_HELMET, SPROOKJES, 3);
        fit(GuhClothes.ROYAL_CROWN, SPROOKJES, 3, GALA, 2, KONINGSDAG, 2);
        fit(GuhClothes.PIRATE_HAT, SPROOKJES, 2, GRIEZELIG, 1, ZOMER, 1);
        fit(GuhClothes.KERMIS_HOED, FEEST, 3, GALA, 1);
        fit(GuhClothes.DETECTIVE_PET, WERK, 2, HOLLANDS_WEER, 1);
        fit(GuhClothes.KONING_KROON, GALA, 3, SPROOKJES, 3, KONINGSDAG, 2);
        // eyes
        fit(GuhClothes.SUNGLASSES, ZOMER, 3, FEEST, 1);
        fit(GuhClothes.HEART_GLASSES, FEEST, 3, ZOMER, 2);
        fit(GuhClothes.MONOCLE, GALA, 3, SPROOKJES, 1);
        fit(GuhClothes.EYEPATCH, SPROOKJES, 2, GRIEZELIG, 2);
        fit(GuhClothes.DETECTIVE_VERGROOTGLAS, WERK, 2, GRIEZELIG, 1);
        // necks
        fit(GuhClothes.RED_BOWTIE, GALA, 2, FEEST, 2, KONINGSDAG, 1);
        fit(GuhClothes.BLACK_BOWTIE, GALA, 3, FEEST, 1);
        fit(GuhClothes.STETHOSCOPE, WERK, 3);
        fit(GuhClothes.WINTER_SCARF, WINTER, 3, HOLLANDS_WEER, 2);
        fit(GuhClothes.KERMIS_STRIK, FEEST, 3, GALA, 1);
        fit(GuhClothes.KONING_KETTING, GALA, 3, SPROOKJES, 1, KONINGSDAG, 1);
        // bodies
        fit(GuhClothes.PINK_ONESIE, WINTER, 1, FEEST, 1);
        fit(GuhClothes.STRIPED_SWEATER, WINTER, 3, HOLLANDS_WEER, 1);
        fit(GuhClothes.RAINCOAT, HOLLANDS_WEER, 3, WERK, 1);
        fit(GuhClothes.CHEF_JACKET, WERK, 3);
        fit(GuhClothes.FIREFIGHTER_JACKET, WERK, 3);
        fit(GuhClothes.POLICE_UNIFORM, WERK, 3);
        fit(GuhClothes.DOCTOR_COAT, WERK, 3, GRIEZELIG, 1);
        fit(GuhClothes.SAFETY_VEST, WERK, 3, HOLLANDS_WEER, 1);
        fit(GuhClothes.OVERALLS, WERK, 2, ZOMER, 1);
        fit(GuhClothes.CHRISTMAS_SWEATER, WINTER, 3, FEEST, 1);
        fit(GuhClothes.GHOST_SHEET, GRIEZELIG, 3);
        fit(GuhClothes.ORANGE_SHIRT, KONINGSDAG, 3, ZOMER, 2);
        fit(GuhClothes.WIZARD_ROBE, SPROOKJES, 3, GRIEZELIG, 1);
        fit(GuhClothes.KNIGHT_ARMOUR, SPROOKJES, 3);
        fit(GuhClothes.ROYAL_CAPE, GALA, 3, SPROOKJES, 2, KONINGSDAG, 1);
        fit(GuhClothes.KERMIS_JASJE, FEEST, 3, GALA, 1);
        fit(GuhClothes.DETECTIVE_JAS, HOLLANDS_WEER, 2, WERK, 2, GRIEZELIG, 1);
        fit(GuhClothes.KONING_MANTEL, GALA, 3, SPROOKJES, 2, KONINGSDAG, 1);
        // the Showster set: not to borrow, but it knows what it's for
        fit(GuhClothes.SHOWSTER_TIARA, GALA, 3, SPROOKJES, 2, FEEST, 2);
        fit(GuhClothes.SHOWSTER_SJERP, GALA, 2, FEEST, 3, KONINGSDAG, 1);
        fit(GuhClothes.SHOWSTER_STRIK, GALA, 3, FEEST, 3, ZOMER, 1);

        SETS.add(List.of(GuhClothes.CHEF_HAT, GuhClothes.CHEF_JACKET));
        SETS.add(List.of(GuhClothes.RAIN_HAT, GuhClothes.RAINCOAT));
        SETS.add(List.of(GuhClothes.FIREFIGHTER_HELMET, GuhClothes.FIREFIGHTER_JACKET));
        SETS.add(List.of(GuhClothes.POLICE_CAP, GuhClothes.POLICE_UNIFORM));
        SETS.add(List.of(GuhClothes.DOCTOR_COAT, GuhClothes.STETHOSCOPE));
        SETS.add(List.of(GuhClothes.BUILDER_HELMET, GuhClothes.SAFETY_VEST));
        SETS.add(List.of(GuhClothes.STRAW_HAT, GuhClothes.OVERALLS));
        SETS.add(List.of(GuhClothes.SANTA_HAT, GuhClothes.CHRISTMAS_SWEATER, GuhClothes.WINTER_SCARF));
        SETS.add(List.of(GuhClothes.WITCH_HAT, GuhClothes.GHOST_SHEET, GuhClothes.PUMPKIN_HEAD));
        SETS.add(List.of(GuhClothes.ORANGE_CROWN, GuhClothes.ORANGE_SHIRT));
        SETS.add(List.of(GuhClothes.WIZARD_HAT, GuhClothes.WIZARD_ROBE));
        SETS.add(List.of(GuhClothes.KNIGHT_HELMET, GuhClothes.KNIGHT_ARMOUR));
        SETS.add(List.of(GuhClothes.ROYAL_CROWN, GuhClothes.ROYAL_CAPE, GuhClothes.MONOCLE));
        SETS.add(List.of(GuhClothes.PIRATE_HAT, GuhClothes.EYEPATCH));
        SETS.add(List.of(GuhClothes.KERMIS_HOED, GuhClothes.KERMIS_JASJE, GuhClothes.KERMIS_STRIK));
        SETS.add(List.of(GuhClothes.DETECTIVE_PET, GuhClothes.DETECTIVE_VERGROOTGLAS, GuhClothes.DETECTIVE_JAS));
        SETS.add(List.of(GuhClothes.KONING_KROON, GuhClothes.KONING_MANTEL, GuhClothes.KONING_KETTING));
        SETS.add(SHOWSTER);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** How well a piece fits this theme (0..3). */
    public int fit(GuhClothes clothes) {
        Map<ShowTheme, Integer> map = FIT.get(clothes);
        return map == null ? 0 : map.getOrDefault(this, 0);
    }

    /** The best fit a complete outfit can reach for this theme (the best loaner piece in every slot). */
    public int maxFit() {
        int total = 0;
        for (GuhClothes.Slot slot : SLOTS) {
            int best = 0;
            for (GuhClothes c : loaners()) {
                if (c.slot == slot) {
                    best = Math.max(best, fit(c));
                }
            }
            total += best;
        }
        return total;
    }

    /** Everything in the loaner wardrobe, in wardrobe order. */
    public static List<GuhClothes> loaners() {
        return FIT.keySet().stream().filter(c -> !SHOWSTER.contains(c)).toList();
    }

    public static boolean isLoaner(GuhClothes clothes) {
        return FIT.containsKey(clothes) && !SHOWSTER.contains(clothes);
    }

    /** Do at least two of these pieces make one look (a set)? */
    public static boolean hasSet(java.util.Collection<GuhClothes> worn) {
        for (List<GuhClothes> set : SETS) {
            if (worn.stream().filter(set::contains).count() >= 2) {
                return true;
            }
        }
        return false;
    }

    /** The worn pieces of the first set this outfit has two or more of (empty when there's none). */
    public static List<GuhClothes> matching(java.util.Collection<GuhClothes> worn) {
        for (List<GuhClothes> set : SETS) {
            List<GuhClothes> hit = worn.stream().filter(set::contains).toList();
            if (hit.size() >= 2) {
                return hit;
            }
        }
        return List.of();
    }

    public static ShowTheme byId(String id) {
        for (ShowTheme t : values()) {
            if (t.id().equals(id)) {
                return t;
            }
        }
        return WINTER;
    }
}
