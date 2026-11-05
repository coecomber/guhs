package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.loot;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
 * The Wolkwachtset (zone 5): helmet, cuirass, leg plates, boots, and with t-items' slots gloves + cape (6 pieces).
 * <ul>
 *   <li>2 pieces "Wolkwacht": Verdediging +10 and Vads +60;</li>
 *   <li>4 pieces "Ridderplicht": Schade tegen bazen +8 %;</li>
 *   <li>6 pieces "Wolkkussen": when a hit takes you below 30 % Vads, a cloud cushion catches you: a Knuffelschild of 20 % of your
 *       max Vads for 6 s (once per 45 s).</li>
 * </ul>
 * The stat part is {@link #stats}; the boss bonus and the cushion live in Z5WezensRegistry (Schade hooks).
 */
public final class Z5Sets {
    private static final EquipmentSlot[] HARNAS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<UUID, Long> KUSSEN = new ConcurrentHashMap<>();
    public static final double BAZEN_BONUS = 0.08;

    private Z5Sets() {
    }

    private static boolean telt(Uitrusting u, LivingEntity e) {
        return u != null && !u.verpluisd() && Z5Loot.WOLKWACHT.contains(u.type());
    }

    /** How many Wolkwacht pieces this entity wears (armour slots; on the server also the gloves/cape slots of t-items). */
    public static int stukken(LivingEntity e) {
        if (!(e instanceof Player p)) {
            return 0;
        }
        int n = 0;
        for (EquipmentSlot es : HARNAS) {
            ItemStack s = p.getItemBySlot(es);
            Uitrusting u = Rollen.van(s);
            if (telt(u, e) && u.basisType() != null && u.basisType().slot().vanilla == es) {
                n++;
            }
        }
        if (p instanceof ServerPlayer sp) {
            for (Slot s : new Slot[] {Z5Loot.HANDSCHOENEN, Z5Loot.CAPE}) {
                if (s != null) {
                    Uitrusting u = Rollen.van(Sieraden.get(sp, s));
                    if (telt(u, e) && u.level() <= Spelers.level(sp)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    public static Stats stats(Player p) {
        Stats s = new Stats();
        if (stukken(p) >= 2) {
            s.plus(Stat.VERDEDIGING, 10);
            s.plus(Stat.VADS, 60);
        }
        return s;
    }

    /** 6 pieces: may the cloud cushion catch this player now (once per 45 s)? Marks it used. */
    public static boolean kussen(ServerPlayer p) {
        long nu = p.level().getGameTime();
        Long vorige = KUSSEN.get(p.getUUID());
        if (vorige != null && nu - vorige < 900 && nu >= vorige) {
            return false;
        }
        KUSSEN.put(p.getUUID(), nu);
        return true;
    }
}
