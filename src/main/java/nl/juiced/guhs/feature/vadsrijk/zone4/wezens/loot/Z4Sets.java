package nl.juiced.guhs.feature.vadsrijk.zone4.wezens.loot;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * The two zone-4 creature sets (the zone-2/3 adapter pattern). Clear stat names (user rule):
 * <ul>
 *   <li><b>Schelpenset</b>: 2 = "+12 Verdediging"; 4 = "Schade tegen bazen +8%".</li>
 *   <li><b>Lampionset</b>: 2 = "+8 Wijsheid"; 4 = "+10 Heal-kracht, en je geeft zelf licht in het donker" (the zone-3 lantern rule
 *       counts you as a light, like the Wortelset).</li>
 * </ul>
 * The boss-damage part lives in Z4WezensRegistry's Schade hook, the light in Lantaarnlicht.DRAGERS; the stats are {@link #stats}.
 */
public final class Z4Sets {
    public static final String SCHELPEN = "schelpen", LAMPION = "lampion";
    public static final List<String> SETS = List.of(SCHELPEN, LAMPION);
    public static final double BAAS_FACTOR = 1.08;
    private static final EquipmentSlot[] HARNAS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private Z4Sets() {
    }

    public static List<String> stukkenVan(String set) {
        return SCHELPEN.equals(set) ? Z4Loot.SCHELPEN : Z4Loot.LAMPION;
    }

    @Nullable
    public static String setVan(String basisType) {
        for (String s : SETS) {
            if (stukkenVan(s).contains(basisType)) {
                return s;
            }
        }
        return null;
    }

    public static int stukken(LivingEntity e, String set) {
        if (!(e instanceof Player p)) {
            return 0;
        }
        List<String> stukken = stukkenVan(set);
        int n = 0;
        for (EquipmentSlot es : HARNAS) {
            ItemStack s = p.getItemBySlot(es);
            Uitrusting u = Rollen.van(s);
            if (u != null && !u.verpluisd() && stukken.contains(u.type()) && u.basisType() != null && u.basisType().slot().vanilla == es) {
                n++;
            }
        }
        // t-items: the set's glove/cape in its virtual slot stands in for a missing piece (server side; the count stays max 4)
        if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
            for (Z4Loot.Nieuw x : Z4Loot.nieuweStukken()) {
                if (set.equals(x.set())) {
                    Uitrusting u = Rollen.van(nl.juiced.guhs.feature.vadsrijk.kern.Sieraden.get(sp, Z4Loot.slot(x.slot())));
                    if (u != null && !u.verpluisd() && x.id().equals(u.type())) {
                        n++;
                    }
                }
            }
        }
        return Math.min(4, n);
    }

    public static Stats stats(Player p) {
        Stats s = new Stats();
        if (stukken(p, SCHELPEN) >= 2) {
            s.plus(Stat.VERDEDIGING, 12);
        }
        if (stukken(p, LAMPION) >= 2) {
            s.plus(Stat.WIJSHEID, 8);
        }
        if (stukken(p, LAMPION) >= 4) {
            s.plus(Stat.HEALKRACHT, 10);
        }
        return s;
    }
}
