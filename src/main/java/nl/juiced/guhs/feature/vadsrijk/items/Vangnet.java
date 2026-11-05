package nl.juiced.guhs.feature.vadsrijk.items;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.economie.SterRegels;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;

/**
 * T-ITEMS: the safety nets on top of the smid's stars ({@code economie.SterRegels}, R2: no Sterrenvangen minigame):
 * <ul>
 *   <li><b>Geduld van de smid (pity)</b>: every failed attempt in a row on a piece gives +{@code pity_per_mislukking} (5 %) on
 *   its next attempt, until one succeeds (then it starts over). Only on the way up to star {@code pity_tot_ster} (7): attempts
 *   from +7 on stay as exciting as before. The counter lives on the piece ({@code Erfstuk.pity}).</li>
 *   <li><b>Knuffeltalisman</b> (a consumable, never a slot): with the switch on in the smid screen, a failure that would cost a
 *   star costs nothing instead, and only THEN one talisman is used. It does not stop "verpluisd" (from +6, as before).</li>
 * </ul>
 * The chances the screen shows come from {@link #kans} (the same maths on both sides: the preview).
 */
public final class Vangnet {
    /** Who has the talisman switch on (server; off after a relog). */
    private static final Map<UUID, Boolean> TALISMAN_AAN = new ConcurrentHashMap<>();

    private Vangnet() {
    }

    /** The pity bonus (0..1) on the next attempt of this piece. */
    public static double pityBonus(Uitrusting u) {
        if (u.sterren() >= (int) ItemsBalans.get("pity_tot_ster")) {
            return 0;
        }
        return Math.max(0, u.erf().pity()) * ItemsBalans.get("pity_per_mislukking");
    }

    /** The success chance of the next star attempt, pity included (0..1). */
    public static double kans(Uitrusting u) {
        if (u.sterren() >= SterRegels.MAX || u.sterren() < 0) {
            return 0;
        }
        return Math.min(1, SterRegels.KANS[u.sterren()] + pityBonus(u));
    }

    /** The outcome with pity: the success die is lowered by the pity bonus (the same as a higher chance). */
    public static SterRegels.Uitkomst uitkomst(Uitrusting u, double dSucces, double dKwijt, double dPluis) {
        return SterRegels.uitkomst(u.sterren(), u.verpluisd(), dSucces - pityBonus(u), dKwijt, dPluis);
    }

    /** Smid.ster: roll with pity, then the talisman (if switched on and in the bag) turns a lost star into a plain failure. */
    public static SterRegels.Uitkomst rol(ServerPlayer p, Uitrusting u, RandomSource r) {
        SterRegels.Uitkomst k = uitkomst(u, r.nextDouble(), r.nextDouble(), r.nextDouble());
        if (k == SterRegels.Uitkomst.STER_KWIJT && talismanAan(p) && EcoUtil.tel(p.getInventory(), ItemsRegistry.KNUFFELTALISMAN.get()) > 0) {
            EcoUtil.neem(p.getInventory(), ItemsRegistry.KNUFFELTALISMAN.get(), 1);
            p.sendSystemMessage(Component.translatable("vadsrijk.items.talisman.beschermd").withStyle(ChatFormatting.LIGHT_PURPLE));
            EcoUtil.geluid(p, SoundEvents.TOTEM_USE, 0.5f, 1.6f);
            return SterRegels.Uitkomst.MISLUKT;
        }
        return k;
    }

    /** The piece after an attempt: success resets the pity, a failure below star 7 adds one. */
    public static Uitrusting naPoging(Uitrusting na, Uitrusting voor, SterRegels.Uitkomst k) {
        if (k == SterRegels.Uitkomst.KAN_NIET) {
            return na;
        }
        if (k == SterRegels.Uitkomst.GELUKT) {
            return na.erf().pity() == 0 ? na : na.metErf(na.erf().metPity(0));
        }
        if (voor.sterren() < (int) ItemsBalans.get("pity_tot_ster")) {
            return na.metErf(na.erf().metPity(voor.erf().pity() + 1));
        }
        return na;
    }

    public static boolean talismanAan(ServerPlayer p) {
        return TALISMAN_AAN.getOrDefault(p.getUUID(), false);
    }

    static void zetTalisman(ServerPlayer p, boolean aan) {
        TALISMAN_AAN.put(p.getUUID(), aan);
    }

    static void vergeet(UUID id) {
        TALISMAN_AAN.remove(id);
    }

    /** Brons for one Knuffeltalisman at the smid (a coin sink; the price rises with the piece's level). */
    public static long talismanPrijs(int level) {
        return (long) ItemsBalans.get("talisman_prijs") + 15L * Math.max(1, level);
    }
}
