package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenLoot;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.GoudenVeren;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Spul;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.RijkItem;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;

/**
 * Zone-3 boss loot (lv 38-50): 11 named Legendaries + 2 Guhddelijk items, 9 trophies (deco, NOT gebonden), the Vahoegzegel (the
 * Vahoeg-mode key) and the Gouden Veer (25 = the gold Pluimguh). The first kill per player guarantees a named Legendary + the
 * trophy. Vahoeg bosses drop Gouden Veren; the last boss of a Vahoeg run has 2 % for the gold Pluimguh itself (with a server-wide
 * line, like a Guhddelijk drop). Everything from a chest is gebonden except the trophies.
 */
public final class Z3BazenLoot {
    private Z3BazenLoot() {
    }

    public record Benoemd(String type, DeferredItem<Item> item, Zeldzaamheid zeldzaamheid, String baas, double kans) {
    }

    public static final int VEREN_NODIG = 25;

    private static DeferredItem<Item> wapen(String id, String type) {
        return Z3BazenRegistry.ITEMS.registerItem(id, p -> new BazenLoot.BaasItem(p, type), () -> new Item.Properties().stacksTo(1)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
    }

    private static DeferredItem<Item> harnas(String id, String type, Slot slot) {
        return Z3BazenRegistry.ITEMS.registerItem(id, p -> new BazenLoot.BaasItem(p, type), () -> KernRegistry.harnasEigenschappen(slot, null));
    }

    private static DeferredItem<Item> sieraad(String id, String type) {
        return Z3BazenRegistry.ITEMS.registerItem(id, p -> new BazenLoot.BaasItem(p, type), () -> new Item.Properties().stacksTo(1));
    }

    private static DeferredItem<Item> trofeeItem(String id) {
        return Z3BazenRegistry.ITEMS.registerItem(id, BazenLoot.Trofee::new, () -> new Item.Properties().stacksTo(16));
    }

    public static final DeferredItem<Item> TOUWKNOTS = wapen("vr_z3_baas_touwknots", "z3baas_touwknots");
    public static final DeferredItem<Item> LONTSTAF = wapen("vr_z3_baas_lontstaf", "z3baas_lontstaf");
    public static final DeferredItem<Item> ROQUEFORTSABEL = wapen("vr_z3_baas_roquefortsabel", "z3baas_roquefortsabel");
    public static final DeferredItem<Item> PARAPLU = wapen("vr_z3_baas_paraplu", "z3baas_paraplu");
    public static final DeferredItem<Item> KOOISLEUTEL = wapen("vr_z3_baas_kooisleutel", "z3baas_kooisleutel");
    public static final DeferredItem<Item> SOKKENCAPE = harnas("vr_z3_baas_sokkencape", "z3baas_sokkencape", Slot.BORST);
    public static final DeferredItem<Item> ROOFKLAUW = wapen("vr_z3_baas_roofklauw", "z3baas_roofklauw");
    public static final DeferredItem<Item> KRUINVEREN = harnas("vr_z3_baas_kruinveren", "z3baas_kruinveren", Slot.BORST);
    public static final DeferredItem<Item> HARTWORTEL = wapen("vr_z3_baas_hartwortel", "z3baas_hartwortel");
    public static final DeferredItem<Item> LANTAARNSTAF = wapen("vr_z3_baas_lantaarnstaf", "z3baas_lantaarnstaf");
    public static final DeferredItem<Item> SCHIMMELBREKER = wapen("vr_z3_baas_schimmelbreker", "z3baas_schimmelbreker");
    public static final DeferredItem<Item> BLEUMONOCLE = harnas("vr_z3_baas_bleumonocle", "z3baas_bleumonocle", Slot.HOOFD);
    public static final DeferredItem<Item> EIERWARMER = sieraad("vr_z3_baas_eierwarmer", "z3baas_eierwarmer");

    public static final List<String> BAZEN = List.of("kruinroofvogel", "taleggio", "brokkel", "roquefort", "piepvel", "wortelwurger", "nestwachter", "bleu",
            "vahoegnir");
    public static final List<DeferredItem<Item>> TROFEEEN = BAZEN.stream().map(b -> trofeeItem("vr_z3_baas_trofee_" + b)).toList();

    public static final List<Benoemd> BENOEMD = List.of(
            new Benoemd("z3baas_roofklauw", ROOFKLAUW, Zeldzaamheid.LEGENDARISCH, "kruinroofvogel", 0.10),
            new Benoemd("z3baas_kruinveren", KRUINVEREN, Zeldzaamheid.LEGENDARISCH, "kruinroofvogel", 0.10),
            new Benoemd("z3baas_touwknots", TOUWKNOTS, Zeldzaamheid.LEGENDARISCH, "taleggio", 0.12),
            new Benoemd("z3baas_lontstaf", LONTSTAF, Zeldzaamheid.LEGENDARISCH, "brokkel", 0.12),
            new Benoemd("z3baas_roquefortsabel", ROQUEFORTSABEL, Zeldzaamheid.LEGENDARISCH, "roquefort", 0.10),
            new Benoemd("z3baas_paraplu", PARAPLU, Zeldzaamheid.LEGENDARISCH, "roquefort", 0.10),
            new Benoemd("z3baas_kooisleutel", KOOISLEUTEL, Zeldzaamheid.LEGENDARISCH, "piepvel", 0.12),
            new Benoemd("z3baas_sokkencape", SOKKENCAPE, Zeldzaamheid.LEGENDARISCH, "piepvel", 0.10),
            new Benoemd("z3baas_hartwortel", HARTWORTEL, Zeldzaamheid.LEGENDARISCH, "wortelwurger", 0.12),
            new Benoemd("z3baas_lantaarnstaf", LANTAARNSTAF, Zeldzaamheid.LEGENDARISCH, "nestwachter", 0.12),
            new Benoemd("z3baas_schimmelbreker", SCHIMMELBREKER, Zeldzaamheid.LEGENDARISCH, "bleu", 0.10),
            new Benoemd("z3baas_bleumonocle", BLEUMONOCLE, Zeldzaamheid.GUHDDELIJK, "bleu", 0.02),
            new Benoemd("z3baas_eierwarmer", EIERWARMER, Zeldzaamheid.GUHDDELIJK, "vahoegnir", 0.03));

    /** Base types (ids only; called from the registry after the items exist). Weapons on the zone-2 rating line, a step up. */
    static void typen() {
        BasisTypes.registreer(BasisType.wapen("z3baas_roofklauw", "guhs:vr_z3_baas_roofklauw", "kruisboog", Klassen.JAGER, Element.NATUUR,
                7.6, 1.5, Stats.van(Stat.BEHENDIGHEID, 10, Stat.KRITIEK, 4), Stats.van(Stat.BEHENDIGHEID, 0.8), 1));
        BasisTypes.registreer(new BasisType("z3baas_kruinveren", "guhs:vr_z3_baas_kruinveren", Slot.BORST, "harnas", null, Element.NATUUR, 0, 0,
                Stats.van(Stat.VERDEDIGING, 20, Stat.VADS, 80, Stat.LOOPSNELHEID, 5), Stats.van(Stat.VERDEDIGING, 2.8, Stat.VADS, 9), 1));
        BasisTypes.registreer(BasisType.wapen("z3baas_touwknots", "guhs:vr_z3_baas_touwknots", "knots", Klassen.RIDDER, Element.NATUUR,
                7.6, 1.5, Stats.van(Stat.KRACHT, 9, Stat.VADS, 40), Stats.van(Stat.KRACHT, 0.8), 1));
        BasisTypes.registreer(BasisType.wapen("z3baas_lontstaf", "guhs:vr_z3_baas_lontstaf", "staf", Klassen.MAGIER, Element.VUUR,
                7.6, 1.5, Stats.van(Stat.WIJSHEID, 10, Stat.KRITIEK, 4), Stats.van(Stat.WIJSHEID, 0.8), 1));
        BasisTypes.registreer(BasisType.wapen("z3baas_roquefortsabel", "guhs:vr_z3_baas_roquefortsabel", "zwaard", Klassen.RIDDER, Element.VUUR,
                7.8, 1.5, Stats.van(Stat.KRACHT, 10, Stat.KRITIEKSCHADE, 12), Stats.van(Stat.KRACHT, 0.8), 1));
        BasisTypes.registreer(BasisType.wapen("z3baas_paraplu", "guhs:vr_z3_baas_paraplu", "boog", Klassen.JAGER, Element.VUUR,
                7.8, 1.5, Stats.van(Stat.BEHENDIGHEID, 10, Stat.LOOPSNELHEID, 6), Stats.van(Stat.BEHENDIGHEID, 0.8), 1));
        BasisTypes.registreer(BasisType.wapen("z3baas_kooisleutel", "guhs:vr_z3_baas_kooisleutel", "knots", Klassen.RIDDER, Element.ROZE,
                8.0, 1.55, Stats.van(Stat.KRACHT, 10, Stat.VERDEDIGING, 8), Stats.van(Stat.KRACHT, 0.85), 1));
        BasisTypes.registreer(new BasisType("z3baas_sokkencape", "guhs:vr_z3_baas_sokkencape", Slot.BORST, "harnas", null, Element.WATER, 0, 0,
                Stats.van(Stat.VERDEDIGING, 20, Stat.VADS, 90, Stat.ELEMENTWEERSTAND, 8), Stats.van(Stat.VERDEDIGING, 2.8, Stat.VADS, 10), 1));
        BasisTypes.registreer(BasisType.wapen("z3baas_hartwortel", "guhs:vr_z3_baas_hartwortel", "staf", Klassen.MAGIER, Element.NATUUR,
                8.2, 1.6, Stats.van(Stat.WIJSHEID, 12, Stat.VADS, 40), Stats.van(Stat.WIJSHEID, 0.9), 1));
        BasisTypes.registreer(BasisType.wapen("z3baas_lantaarnstaf", "guhs:vr_z3_baas_lantaarnstaf", "staf", Klassen.MAGIER, Element.ROZE,
                8.2, 1.6, Stats.van(Stat.WIJSHEID, 12, Stat.KRITIEK, 5), Stats.van(Stat.WIJSHEID, 0.9), 1));
        BasisTypes.registreer(BasisType.wapen("z3baas_schimmelbreker", "guhs:vr_z3_baas_schimmelbreker", "hamer", Klassen.RIDDER, Element.WATER,
                8.4, 1.65, Stats.van(Stat.KRACHT, 12, Stat.VERDEDIGING, 8), Stats.van(Stat.KRACHT, 0.9), 1));
        BasisTypes.registreer(new BasisType("z3baas_bleumonocle", "guhs:vr_z3_baas_bleumonocle", Slot.HOOFD, "harnas", null, Element.WATER, 0, 0,
                Stats.van(Stat.VERDEDIGING, 12, Stat.KRITIEK, 8, Stat.KRITIEKSCHADE, 16), Stats.van(Stat.VERDEDIGING, 1.8, Stat.KRITIEK, 0.2), 1));
        BasisTypes.registreer(BasisType.sieraad("z3baas_eierwarmer", "guhs:vr_z3_baas_eierwarmer", Slot.KETTING, Stats.van(Stat.VADS, 80, Stat.WIJSHEID, 8),
                Stats.van(Stat.VADS, 6), 1));
    }

    public static ItemStack maak(Benoemd b, int level, RandomSource r) {
        BasisType t = BasisTypes.van(b.type());
        if (t == null) {
            return ItemStack.EMPTY;
        }
        return Rollen.stack(Rollen.rol(t, level, b.zeldzaamheid(), r).metGebonden(true));
    }

    public static ItemStack trofee(String baas) {
        int i = BAZEN.indexOf(baas);
        return i < 0 ? ItemStack.EMPTY : new ItemStack(TROFEEEN.get(i).get());
    }

    @Nullable
    static LootTabel tabel(String baas) {
        for (String id : List.of("economie:z3_baas_" + baas, "economie:baas_" + baas, "economie:zone3", "economie:zone2", "economie:zone1", "kern:klodderreus")) {
            LootTabel t = LootTabellen.van(id);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    /** One player's chest. */
    public static LootTabel.Resultaat kist(String baas, ServerPlayer p, int level, RandomSource r, boolean vahoeg, boolean eindbaas) {
        List<ItemStack> items = new ArrayList<>();
        long munten = 0;
        LootTabel t = tabel(baas);
        if (t != null) {
            LootTabel.Resultaat basis = t.rol(level, r);
            items.addAll(basis.items());
            munten = basis.munten() * (vahoeg ? 2 : 1);
        }
        CompoundTag extra = Spelers.data(p).extra("z3bazen");
        String sleutel = "eerste_" + baas;
        boolean eerste = !extra.getBooleanOr(sleutel, false);
        List<Benoemd> legendarisch = new ArrayList<>();
        for (Benoemd b : BENOEMD) {
            if (!b.baas().equals(baas)) {
                continue;
            }
            if (b.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH) {
                legendarisch.add(b);
            }
            if (r.nextDouble() < b.kans() * (vahoeg ? 1.5 : 1)) {
                items.add(maak(b, level, r));
                if (b.zeldzaamheid() == Zeldzaamheid.GUHDDELIJK && p.level() instanceof ServerLevel sl) {
                    Component c = Component.translatable("vadsrijk.z3bazen.loot.guhddelijk", p.getDisplayName(), Component.translatable("item.guhs." + b.item().getId().getPath()))
                            .withStyle(ChatFormatting.LIGHT_PURPLE);
                    sl.getServer().getPlayerList().broadcastSystemMessage(c, false);
                }
            }
        }
        if (eerste && !legendarisch.isEmpty()) {
            boolean alEen = items.stream().anyMatch(s -> {
                Uitrusting u = Rollen.van(s);
                return u != null && u.type().startsWith("z3baas_") && u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH;
            });
            if (!alEen) {
                items.add(maak(legendarisch.get(r.nextInt(legendarisch.size())), level, r));
            }
            p.sendSystemMessage(Component.translatable("vadsrijk.bazen.kist.eerste").withStyle(ChatFormatting.GOLD));
        }
        if (eerste || r.nextDouble() < 0.25) {
            items.add(trofee(baas));
        }
        // the Vahoeg mode (z3-systemen's counters): Gouden Veren, and the 2 % gold Pluimguh roll at the end of a run
        if (vahoeg) {
            GoudenVeren.geef(p, eindbaas ? 2 + r.nextInt(2) : 1 + r.nextInt(2));
            if (eindbaas) {
                GoudenVeren.rolGoudenPluimguh(p);
            }
        }
        if (!vahoeg && List.of("taleggio", "brokkel", "roquefort", "wortelwurger", "nestwachter", "bleu").contains(baas) && r.nextDouble() < 0.2) {
            items.add(new ItemStack(Z3Spul.VAHOEGZEGEL.get()));   // a seal now and then (also sold for Jachtpenningen)
        }
        if (eerste) {
            extra.putBoolean(sleutel, true);
        }
        List<ItemStack> uit = new ArrayList<>();
        for (ItemStack s : items) {
            Uitrusting u = Rollen.van(s);
            if (u != null && !u.gebonden()) {
                s.set(KernRegistry.UITRUSTING.get(), u.metGebonden(true));
            }
            if (!s.isEmpty()) {
                uit.add(s);
            }
        }
        return new LootTabel.Resultaat(uit, munten);
    }

    static {
        RijkItem.class.getName();   // (kern loaded first)
    }
}
