package nl.juiced.guhs.feature.vadsrijk.wezens.loot;

import java.util.List;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * The two zone-1 sets (kern has no set support yet, so this is the slice's adapter; NOTITIES_WEZENS: kern can lift it).
 * <ul>
 *   <li><b>Klodderset</b> (behendigheid/slijm): 2 pieces "Glibberig" = 15 % chance a hit slides off; 4 pieces "Stuiterkracht"
 *       = +12 % damage and 20 % chance your hit makes the target Plakkerig.</li>
 *   <li><b>Zwamset</b> (wijsheid/sporen): 2 pieces "Sporenschild" = 10 % less damage; 4 pieces "Sporenregen" = +12 % spell
 *       damage and your spells give Gesporen.</li>
 * </ul>
 * The effects work now through Schade.VOOR_KLAP/NA_KLAP (WezensRegistry). {@link #stats} is the stat part for kern's
 * Spelers.stats (+4 Behendigheid / +4 Wijsheid at 2 pieces), not counted until kern adds it (one line).
 */
public final class SetBonussen {
    public static final String KLODDER = "klodder", ZWAM = "zwam";
    public static final List<String> KLODDERSET = List.of("klodderkapje", "klodderhesje", "klodderbroek", "klodderstuiters");
    public static final List<String> ZWAMSET = List.of("zwamhoed", "sporenmantel", "zwambroek", "mossloffen");
    private static final EquipmentSlot[] HARNAS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private SetBonussen() {
    }

    /** The set of a base type, or null. */
    public static String setVan(String basisType) {
        return KLODDERSET.contains(basisType) ? KLODDER : ZWAMSET.contains(basisType) ? ZWAM : null;
    }

    /** How many pieces of this set the entity wears (in the right slots, not verpluisd). */
    public static int stukken(LivingEntity e, String set) {
        if (!(e instanceof Player p)) {
            return 0;
        }
        List<String> stukken = KLODDER.equals(set) ? KLODDERSET : ZWAMSET;
        int n = 0;
        for (EquipmentSlot es : HARNAS) {
            ItemStack s = p.getItemBySlot(es);
            Uitrusting u = Rollen.van(s);
            if (u != null && !u.verpluisd() && stukken.contains(u.type()) && u.basisType() != null && u.basisType().slot().vanilla == es) {
                n++;
            }
        }
        return Math.min(4, n + nl.juiced.guhs.feature.vadsrijk.items.Sets.extraStukken(e, set));   // T-ITEMS: set gloves/capes
    }

    /** The stat part of the set bonuses (for kern: {@code s.plus(SetBonussen.stats(p))} in Spelers.stats). */
    public static Stats stats(Player p) {
        Stats s = new Stats();
        if (stukken(p, KLODDER) >= 2) {
            s.plus(Stat.BEHENDIGHEID, 4);
        }
        if (stukken(p, ZWAM) >= 2) {
            s.plus(Stat.WIJSHEID, 4);
        }
        return s;
    }
}
