package nl.juiced.guhs.feature.vadsrijk.items;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.Sieraden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonus;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonussen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Erfstuk;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;

/**
 * T-ITEMS: Erfstukken, gear that grows with you (R2: the Erfstukken model), the rules and the maths:
 * <ul>
 *   <li><b>Vertrouwd</b> (item xp): the weapon in your hand, your 4 armour pieces and your 7 virtual slots get xp for every
 *   creature you help cheer up (1, elite 3, boss 15). Levels 1-5 at {@link #DREMPELS}; each level is +2 % on the piece's base
 *   stats and weapon damage ({@code Rollen.ERF_FACTOR}). Level 5 = a gold line in the tooltip.</li>
 *   <li><b>Doorgroeien</b> (ascension) at the smid: only Legendarisch (2x) and Guhddelijk (unlimited, up to the level cap), with
 *   Vertrouwd 5: +{@code doorgroei_levels} item levels, the rolls move along to the same place (x{@code doorgroei_roll}) in the new
 *   range, Vertrouwd starts over, the piece becomes Gebonden. Costs Groeipluis (1 + times grown) and munten.</li>
 *   <li><b>Overvadsen</b> (optional): one bonus of an old piece moves onto another piece (replacing a bonus, or added when the
 *   target has room for its rarity), at the same place in the target's range; the old piece is used up. With
 *   {@code overvads_mislukking} chance a "lieve mislukking": the roll lands at 70 % of its place. The target is never lost.</li>
 *   <li><b>Polijsten</b>: one bonus gets a new value within its own range (rarer pieces land higher, as with rolling). Cheap.</li>
 * </ul>
 */
public final class Erfstukken {
    /** Total item xp for Vertrouwd 1..5. */
    public static final int[] DREMPELS = {40, 120, 280, 560, 1000};
    public static final int MAX_VERTROUWD = 5;

    private Erfstukken() {
    }

    // ---- Vertrouwd ---------------------------------------------------------------------------------------------------------

    public static int vertrouwd(int xp) {
        int l = 0;
        while (l < MAX_VERTROUWD && xp >= DREMPELS[l]) {
            l++;
        }
        return l;
    }

    public static int vertrouwd(Uitrusting u) {
        return vertrouwd(u.erf().xp());
    }

    /** Rollen.ERF_FACTOR: 1 + 2 % per Vertrouwd level. */
    public static double factor(Uitrusting u) {
        int l = vertrouwd(u);
        return l == 0 ? 1.0 : 1 + ItemsBalans.get("vertrouwd_per_level") * l;
    }

    /** xp a piece gets for one cheered-up creature. */
    public static int xpVoor(boolean elite, boolean baas) {
        int x = baas ? 15 : elite ? 3 : 1;
        return Math.max(1, (int) Math.round(x * ItemsBalans.get("vertrouwd_xp_schaal")));
    }

    /** The piece with {@code x} more item xp (no change past Vertrouwd 5). */
    public static Uitrusting metXp(Uitrusting u, int x) {
        if (vertrouwd(u) >= MAX_VERTROUWD) {
            return u;
        }
        return u.metErf(u.erf().metXp(Math.min(DREMPELS[MAX_VERTROUWD - 1], u.erf().xp() + x)));
    }

    static void verslagen(Haken.Verslagen v) {
        boolean baas = v.mob() instanceof VadsrijkBaas;
        int x = xpVoor(v.mob().elite(), baas);
        for (ServerPlayer p : v.helpers()) {
            if (Vadsrijk.in(p)) {
                geefXp(p, x);
            }
        }
    }

    /** Every worn piece (hand, armour, virtual slots) gets {@code x} item xp; a new Vertrouwd level gets a message. */
    public static void geefXp(ServerPlayer p, int x) {
        ItemStack hand = p.getMainHandItem();
        if (Rollen.van(hand) != null && Rollen.van(hand).basisType() != null && Rollen.van(hand).basisType().slot() == Slot.WAPEN) {
            groei(p, hand, x);
        }
        for (EquipmentSlot es : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = p.getItemBySlot(es);
            if (Rollen.van(s) != null) {
                groei(p, s, x);
            }
        }
        boolean sieraad = false;
        for (Slot sl : Slot.SIERADEN) {
            ItemStack s = Sieraden.get(p, sl);
            if (Rollen.van(s) != null) {
                ItemStack k = s.copy();
                if (groei(p, k, x)) {
                    Sieraden.zet(p, sl, k);
                    sieraad = true;
                }
            }
        }
        if (sieraad) {
            Sieraden.stuur(p);
        }
    }

    /** Changes the stack in place; true when its xp changed. */
    private static boolean groei(ServerPlayer p, ItemStack s, int x) {
        Uitrusting u = Rollen.van(s);
        if (u == null || u.verpluisd() || vertrouwd(u) >= MAX_VERTROUWD) {
            return false;
        }
        int voor = vertrouwd(u);
        Uitrusting na = metXp(u, x);
        s.set(KernRegistry.UITRUSTING.get(), na);
        int lv = vertrouwd(na);
        if (lv > voor) {
            p.sendSystemMessage(Component.translatable("vadsrijk.items.vertrouwd.omhoog", s.getHoverName(), lv)
                    .withStyle(lv >= MAX_VERTROUWD ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE));
            p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f + 0.1f * lv);
        }
        return true;
    }

    // ---- Doorgroeien -------------------------------------------------------------------------------------------------------

    /** Why this piece can't grow now ("" = it can). A lang key under vadsrijk.items.doorgroei.*. */
    public static String kanDoorgroeien(Uitrusting u) {
        if (u.verpluisd()) {
            return "vadsrijk.items.doorgroei.verpluisd";
        }
        if (u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH) {
            if (u.erf().groei() >= 2) {
                return "vadsrijk.items.doorgroei.op";
            }
        } else if (u.zeldzaamheid() != Zeldzaamheid.GUHDDELIJK) {
            return "vadsrijk.items.doorgroei.zeldzaamheid";
        }
        if (u.level() >= Levels.MAX) {
            return "vadsrijk.items.doorgroei.cap";
        }
        if (vertrouwd(u) < MAX_VERTROUWD) {
            return "vadsrijk.items.doorgroei.vertrouwd";
        }
        return "";
    }

    public static int nieuwLevel(Uitrusting u) {
        return Math.min(Levels.MAX, u.level() + (int) ItemsBalans.get("doorgroei_levels"));
    }

    public static int groeipluis(Uitrusting u) {
        return 1 + u.erf().groei();
    }

    public static long doorgroeiPrijs(Uitrusting u) {
        long p = (long) ItemsBalans.get("doorgroei_prijs") * Math.max(1, nieuwLevel(u) - u.level());
        return u.zeldzaamheid() == Zeldzaamheid.GUHDDELIJK ? p * 2 : p;
    }

    /** The grown piece (call only when {@link #kanDoorgroeien} is ""). */
    public static Uitrusting doorgroei(Uitrusting u) {
        int nl = nieuwLevel(u);
        double schaal = ItemsBalans.get("doorgroei_roll");
        List<Bonus> b = new ArrayList<>();
        for (Bonus x : u.bonussen()) {
            b.add(new Bonus(x.stat(), verplaats(x.stat(), x.waarde(), u.level(), nl, schaal)));
        }
        return u.metBonussen(b).metLevel(nl).metGebonden(true).metErf(new Erfstuk(0, u.erf().groei() + 1, u.erf().pity()));
    }

    /** Where {@code waarde} sits in its range at level {@code van} (0..1). */
    public static double plek(Stat s, double waarde, int van) {
        double[] a = Bonussen.bereik(s, van);
        return a[1] <= a[0] ? 1 : Math.max(0, Math.min(1, (waarde - a[0]) / (a[1] - a[0])));
    }

    /** The same place (x schaal) in the range at level {@code naar}. */
    public static double verplaats(Stat s, double waarde, int van, int naar, double schaal) {
        double[] b = Bonussen.bereik(s, naar);
        return Bonussen.rond(s, b[0] + (b[1] - b[0]) * plek(s, waarde, van) * schaal, b);
    }

    // ---- Overvadsen --------------------------------------------------------------------------------------------------------

    /**
     * Why bonus {@code i} of {@code offer} can't move onto {@code doel} at bonus {@code j} (-1 = add as a new one); "" = it can.
     */
    public static String kanOvervadsen(Uitrusting doel, Uitrusting offer, int i, int j) {
        if (doel.verpluisd() || offer.verpluisd()) {
            return "vadsrijk.items.overvads.verpluisd";
        }
        if (i < 0 || i >= offer.bonussen().size()) {
            return "vadsrijk.items.overvads.kies";
        }
        if (j >= doel.bonussen().size() || (j < 0 && doel.bonussen().size() >= doel.zeldzaamheid().maxBonus)) {
            return "vadsrijk.items.overvads.vol";
        }
        Stat s = offer.bonussen().get(i).stat();
        Slot slot = doel.basisType() == null ? null : doel.basisType().slot();
        if (!Bonussen.mag(s, slot)) {
            return "vadsrijk.items.overvads.past_niet";
        }
        for (int k = 0; k < doel.bonussen().size(); k++) {
            if (k != j && doel.bonussen().get(k).stat() == s) {
                return "vadsrijk.items.overvads.dubbel";
            }
        }
        return "";
    }

    /** The target after overvadsen ({@code lief} = the sweet failure: the roll at 70 % of its place). */
    public static Uitrusting overvads(Uitrusting doel, Uitrusting offer, int i, int j, boolean lief) {
        Bonus b = offer.bonussen().get(i);
        double v = verplaats(b.stat(), b.waarde(), offer.level(), doel.level(), lief ? 0.7 : 1.0);
        List<Bonus> l = new ArrayList<>(doel.bonussen());
        if (j < 0) {
            l.add(new Bonus(b.stat(), v));
        } else {
            l.set(j, new Bonus(b.stat(), v));
        }
        return doel.metBonussen(l);
    }

    public static long overvadsPrijs(Uitrusting doel) {
        return (long) ItemsBalans.get("overvads_prijs") + 20L * doel.level();
    }

    // ---- Polijsten ---------------------------------------------------------------------------------------------------------

    /** Bonus {@code j} with a fresh value within its range (the piece's level and rarity). */
    @Nullable
    public static Uitrusting polijst(Uitrusting u, int j, RandomSource r) {
        if (j < 0 || j >= u.bonussen().size() || u.verpluisd()) {
            return null;
        }
        List<Bonus> l = new ArrayList<>(u.bonussen());
        Stat s = l.get(j).stat();
        l.set(j, new Bonus(s, Bonussen.waarde(s, u.level(), u.zeldzaamheid(), r)));
        return u.metBonussen(l);
    }

    public static long polijstPrijs(Uitrusting u) {
        return (long) ItemsBalans.get("polijst_prijs") + 6L * u.level();
    }
}
