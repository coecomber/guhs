package nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot;

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
 * The two zone-2 sets (the same adapter pattern as zone 1's SetBonussen):
 * <ul>
 *   <li><b>Kristalset</b> (verdediging/wijsheid): 2 pieces "Kristalhuid" = +8 Elementweerstand, Bevroren lasts half as long and
 *       heavy hits on you while frozen do no extra damage; 4 pieces "Splinterslag" = 20 % chance your hit bursts into shards
 *       that do 25 % of it to every other creature within 3 blocks.</li>
 *   <li><b>Mika-buitset</b> (kracht/behendigheid): 2 pieces "Mijnlamp" = +5 Kritiek; 4 pieces "Boorkracht" = +15 % damage to
 *       versteende creatures and +25 % on the first hit on a creature that is still at full Vads.</li>
 * </ul>
 * The effects live in Z2WezensRegistry (Schade hooks) and {@link nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Bevroren}; the stat part is
 * {@link #stats} (kern Spelers.EXTRA_STATS).
 */
public final class Z2Sets {
    public static final String KRISTAL = "kristal", MIKA = "mika";
    public static final List<String> KRISTALSET = List.of("kristalkroon", "kristalborstplaat", "kristalbeenstukken", "kristalschoentjes");
    public static final List<String> MIKASET = List.of("mijnhelm", "buitvest", "boorbroek", "kaaslaarzen");
    private static final EquipmentSlot[] HARNAS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private Z2Sets() {
    }

    @Nullable
    public static String setVan(String basisType) {
        return KRISTALSET.contains(basisType) ? KRISTAL : MIKASET.contains(basisType) ? MIKA : null;
    }

    public static int stukken(LivingEntity e, String set) {
        if (!(e instanceof Player p)) {
            return 0;
        }
        List<String> stukken = KRISTAL.equals(set) ? KRISTALSET : MIKASET;
        int n = 0;
        for (EquipmentSlot es : HARNAS) {
            ItemStack s = p.getItemBySlot(es);
            Uitrusting u = Rollen.van(s);
            if (u != null && !u.verpluisd() && stukken.contains(u.type()) && u.basisType() != null && u.basisType().slot().vanilla == es) {
                n++;
            }
        }
        return n;
    }

    public static Stats stats(Player p) {
        Stats s = new Stats();
        if (stukken(p, KRISTAL) >= 2) {
            s.plus(Stat.ELEMENTWEERSTAND, 8);
        }
        if (stukken(p, MIKA) >= 2) {
            s.plus(Stat.KRITIEK, 5);
        }
        return s;
    }
}
