package nl.juiced.guhs.feature.vadsrijk.items;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * T-ITEMS: set gloves and capes. A set piece worn in the HANDSCHOENEN or CAPE slot counts as one piece of its set (the set
 * classes of wezens/z2-wezens/z3-wezens add {@link #extraStukken} to their own count, capped at 4: so a glove can stand in for a
 * missing helmet, and the 2- and 4-piece bonuses stay the same).
 */
public final class Sets {
    /** base type id -> set name (the set names of SetBonussen/Z2Sets/Z3Sets: klodder, zwam, kristal, mika, bloesem, schors, wortel). */
    static final Map<String, String> SET_VAN = new LinkedHashMap<>();

    private Sets() {
    }

    @Nullable
    public static String setVan(String basisType) {
        return SET_VAN.get(basisType);
    }

    /** How many of this set's gloves/capes the entity wears (virtual slots, not verpluisd, level allowed). */
    public static int extraStukken(LivingEntity e, String set) {
        if (!(e instanceof ServerPlayer p) || set == null) {
            return 0;
        }
        int n = 0;
        for (Slot s : new Slot[] {Slot.HANDSCHOENEN, Slot.CAPE}) {
            Uitrusting u = Rollen.van(Sieraden.get(p, s));
            if (u != null && !u.verpluisd() && set.equals(SET_VAN.get(u.type())) && u.level() <= Spelers.level(p)) {
                n++;
            }
        }
        return n;
    }
}
