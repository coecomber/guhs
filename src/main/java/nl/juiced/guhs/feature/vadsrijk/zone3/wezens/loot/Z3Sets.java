package nl.juiced.guhs.feature.vadsrijk.zone3.wezens.loot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * The three zone-3 creature sets (the zone-2 adapter pattern):
 * <ul>
 *   <li><b>Bloesemset</b> (behendigheid/kritiek): 2 "Bloesemvlucht" = +6 Kritiek; 4 "Stuifmeel" = your critical hits leave the
 *       creature Gesporen (3 s).</li>
 *   <li><b>Schorsset</b> (verdediging, ZONE3_PLAN §9.4): 2 "Schorshuid" = +12 Verdediging; 4 "Harsschild" = when a hit takes you
 *       below 50 % Vads, a resin shield (Knuffelschild, 15 % of your max Vads, 6 s) once per 30 s.</li>
 *   <li><b>Wortelset</b> (wijsheid/elementweerstand): 2 "Wortelvast" = +8 Elementweerstand; 4 "Wortellicht" = you carry your own
 *       light in the Wortelduister (a Nestlantaarn's circle) and do +10 % damage to rot creatures.</li>
 * </ul>
 * The effects live in Z3WezensRegistry (Schade hooks) and Lantaarnlicht.DRAGERS; the stat part is {@link #stats}.
 */
public final class Z3Sets {
    public static final String BLOESEM = "bloesem", SCHORS = "schors", WORTEL = "wortel";
    public static final List<String> SETS = List.of(BLOESEM, SCHORS, WORTEL);
    private static final EquipmentSlot[] HARNAS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<UUID, Long> HARSSCHILD = new HashMap<>();

    private Z3Sets() {
    }

    public static List<String> stukkenVan(String set) {
        return switch (set) {
            case BLOESEM -> Z3Loot.BLOESEM;
            case SCHORS -> Z3Loot.SCHORS;
            default -> Z3Loot.WORTEL;
        };
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
        return Math.min(4, n + nl.juiced.guhs.feature.vadsrijk.items.Sets.extraStukken(e, set));   // T-ITEMS: set gloves/capes
    }

    public static Stats stats(Player p) {
        Stats s = new Stats();
        if (stukken(p, BLOESEM) >= 2) {
            s.plus(Stat.KRITIEK, 6);
        }
        if (stukken(p, SCHORS) >= 2) {
            s.plus(Stat.VERDEDIGING, 12);
        }
        if (stukken(p, WORTEL) >= 2) {
            s.plus(Stat.ELEMENTWEERSTAND, 8);
        }
        return s;
    }

    /** Schorsset 4: may the resin shield go up now (30 s per player)? Marks it used. */
    public static boolean harsschild(ServerPlayer p) {
        long nu = p.level().getGameTime();
        Long vorige = HARSSCHILD.get(p.getUUID());
        if (vorige != null && nu - vorige < 600) {
            return false;
        }
        HARSSCHILD.put(p.getUUID(), nu);
        return true;
    }
}
