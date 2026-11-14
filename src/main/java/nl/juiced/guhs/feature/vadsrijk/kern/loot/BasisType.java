package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;

/**
 * A loot base type (~30 in zone 1): one registered item per base type ({@code itemId}, e.g. "guhs:vr_kussenzwaard"); the rolls
 * live in the {@link Uitrusting} component. Weapons do {@code schade + schadePerLevel * level}; base stats are
 * {@code basis + perLevel * level}, times the rarity factor. {@code soort} is the weapon kind a class accepts
 * (zwaard/knots/hamer/boog/staf...) or "harnas". {@code klasse} null = every class (armour).
 */
public record BasisType(String id, String itemId, Slot slot, String soort, @Nullable String klasse, Element element, double schade,
        double schadePerLevel, Stats basis, Stats perLevel, int flavours) {
    public Stats statsOp(int level) {
        Stats s = basis.kopie();
        perLevel.alles().forEach((k, v) -> s.plus(k, v * level));
        return s;
    }

    public double schadeOp(int level) {
        return schade + schadePerLevel * level;
    }

    public String naamKey() {
        return "vadsrijk.loot." + id;
    }

    public String flavourKey(int i) {
        return "vadsrijk.loot." + id + ".flavour." + i;
    }

    /**
     * 1.10.2 (user decision "D"): how much of its base a main stat on armour and jewellery grows per item level, for every zone's
     * set pieces. Was 0.25. 1.10.3 schade-audit (user decision): Kracht / Behendigheid / Wijsheid grow {@link #AANVALSSTAT_GROEI}
     * 0.10 (a lv-35 mage had 125 Wijsheid from three Kristal armour bases); an extra Vads / Verdediging line keeps this 0.15.
     */
    public static final double HOOFD_GROEI = 0.15;
    public static final double AANVALSSTAT_GROEI = 0.10;

    /** The per-level growth share of a main stat ({@link Stat#hoofd}) on set armour/jewellery: 0.10 for Kracht/Behendigheid/Wijsheid, else 0.15. */
    public static double hoofdGroei(nl.juiced.guhs.feature.vadsrijk.kern.Stat s) {
        return switch (s) {
            case KRACHT, BEHENDIGHEID, WIJSHEID -> AANVALSSTAT_GROEI;
            default -> HOOFD_GROEI;
        };
    }

    public static BasisType wapen(String id, String itemId, String soort, String klasse, Element e, double schade, double perLevel, Stats basis,
            Stats groei, int flavours) {
        return new BasisType(id, itemId, Slot.WAPEN, soort, klasse, e, schade, perLevel, basis, groei, flavours);
    }

    /** A piece of jewellery (RING, KETTING, OORBEL): only side stats, they grow with the level. */
    public static BasisType sieraad(String id, String itemId, Slot slot, Stats basis, Stats perLevel, int flavours) {
        return new BasisType(id, itemId, slot, "sieraad", null, Element.GEEN, 0, 0, basis, perLevel, flavours);
    }

    public static BasisType harnas(String id, String itemId, Slot slot, double verdediging, double vads, int flavours) {
        return new BasisType(id, itemId, slot, "harnas", null, Element.GEEN, 0, 0, Stats.van(Stat.VERDEDIGING, verdediging, Stat.VADS, vads),
                Stats.van(Stat.VERDEDIGING, verdediging * 0.25, Stat.VADS, vads * 0.2), flavours);
    }
}
