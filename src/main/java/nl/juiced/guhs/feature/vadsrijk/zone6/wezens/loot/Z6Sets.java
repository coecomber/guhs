package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.loot;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * The four zone-6 sets (clear stat names, user rule). The stat part is {@link #stats}; the damage parts live in
 * Z6WezensRegistry's Schade hooks.
 * <ul>
 *   <li><b>Duinwandelaar</b> (6): 2 = Verdediging +14 en Vads +80; 4 = Schade tegen bazen +8%; 6 = Loopsnelheid +8%.</li>
 *   <li><b>Duinzijde</b> (6): 2 = Wijsheid +10; 4 = Heal-kracht +12; 6 = Elementschade +6%.</li>
 *   <li><b>Karavaanwacht</b> (4, Episch): 2 = Vads +120; 4 = Schade tegen bazen +10%.</li>
 *   <li><b>Sterrenwacht</b> (6, Episch): 2 = Kritiek +4%; 4 = Cooldownreductie +6%; 6 = "Sterretje": every 20 s your next hit does
 *       +40% damage (a little star over your head shows it is ready).</li>
 * </ul>
 * Pieces count in the armour slots and (server) in the virtual gloves / cape / armband slots of kern.Sieraden.
 */
public final class Z6Sets {
    public static final String DUINWANDELAAR = "duinwandelaar", DUINZIJDE = "duinzijde", KARAVAANWACHT = "karavaanwacht", STERRENWACHT = "sterrenwacht";
    public static final List<String> SETS = List.of(DUINWANDELAAR, DUINZIJDE, KARAVAANWACHT, STERRENWACHT);
    public static final double BAZEN_4 = 1.08, KARAVAAN_BAZEN_4 = 1.10, STERRETJE = 1.4;
    public static final int STERRETJE_TICKS = 20 * 20;
    private static final EquipmentSlot[] HARNAS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Slot[] VIRTUEEL = {Slot.HANDSCHOENEN, Slot.CAPE, Slot.ARMBAND};
    private static final Map<UUID, Long> STER = new ConcurrentHashMap<>();

    private Z6Sets() {
    }

    public static List<String> stukkenVan(String set) {
        return switch (set) {
            case DUINWANDELAAR -> Z6Loot.DUINWANDELAAR;
            case DUINZIJDE -> Z6Loot.DUINZIJDE;
            case KARAVAANWACHT -> Z6Loot.KARAVAANWACHT;
            default -> Z6Loot.STERRENWACHT;
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
        if (p instanceof ServerPlayer sp) {
            for (Slot s : VIRTUEEL) {
                Uitrusting u = Rollen.van(Sieraden.get(sp, s));
                if (u != null && !u.verpluisd() && stukken.contains(u.type()) && u.level() <= Spelers.level(sp)) {
                    n++;
                }
            }
        }
        return n;
    }

    public static Stats stats(Player p) {
        Stats s = new Stats();
        int dw = stukken(p, DUINWANDELAAR), dz = stukken(p, DUINZIJDE), kw = stukken(p, KARAVAANWACHT), sw = stukken(p, STERRENWACHT);
        if (dw >= 2) {
            s.plus(Stat.VERDEDIGING, 14);
            s.plus(Stat.VADS, 80);
        }
        if (dw >= 6) {
            s.plus(Stat.LOOPSNELHEID, 8);
        }
        if (dz >= 2) {
            s.plus(Stat.WIJSHEID, 10);
        }
        if (dz >= 4) {
            s.plus(Stat.HEALKRACHT, 12);
        }
        if (dz >= 6) {
            s.plus(Stat.ELEMENTSCHADE, 6);
        }
        if (kw >= 2) {
            s.plus(Stat.VADS, 120);
        }
        if (sw >= 2) {
            s.plus(Stat.KRITIEK, 4);
        }
        if (sw >= 4) {
            s.plus(Stat.COOLDOWNREDUCTIE, 6);
        }
        return s;
    }

    /** The boss-damage factor of this player's sets (Duinwandelaar 4, Karavaanwacht 4). */
    public static double bazenFactor(Player p) {
        double f = 1;
        if (stukken(p, DUINWANDELAAR) >= 4) {
            f *= BAZEN_4;
        }
        if (stukken(p, KARAVAANWACHT) >= 4) {
            f *= KARAVAAN_BAZEN_4;
        }
        return f;
    }

    /** Sterrenwacht 6: is the little star ready (and use it)? */
    public static boolean sterretje(ServerPlayer p) {
        if (stukken(p, STERRENWACHT) < 6) {
            return false;
        }
        long nu = p.level().getGameTime();
        Long vorig = STER.get(p.getUUID());
        if (vorig != null && nu - vorig < STERRETJE_TICKS && nu >= vorig) {
            return false;
        }
        STER.put(p.getUUID(), nu);
        return true;
    }

    /** Sterrenwacht 6: ready now (for the little star over your head; does not use it)? */
    public static boolean sterretjeKlaar(ServerPlayer p) {
        Long vorig = STER.get(p.getUUID());
        long nu = p.level().getGameTime();
        return stukken(p, STERRENWACHT) >= 6 && (vorig == null || nu - vorig >= STERRETJE_TICKS || nu < vorig);
    }
}
