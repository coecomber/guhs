package nl.juiced.guhs.feature.vadsrijk.bazen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.registries.DeferredItem;
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
 * Boss loot: the named Legendary items (each its own item + base type {@code baas_<naam>}, fixed name, an effect line, see
 * {@link BazenEffecten}), the two Guhddelijk items, the trophies, and what goes into a personal boss chest ({@link #kist}).
 * Everything from a boss chest is "gebonden" (no trading).
 */
public final class BazenLoot {
    private BazenLoot() {
    }

    /** One named item: its base type id, item, rarity, which boss, the drop chance. */
    public record Benoemd(String type, DeferredItem<Item> item, Zeldzaamheid zeldzaamheid, String baas, double kans) {
    }

    /** RijkItem + the effect line. */
    public static class BaasItem extends RijkItem {
        private final String type;

        public BaasItem(Properties p, String type) {
            super(p);
            this.type = type;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> t, TooltipFlag flag) {
            super.appendHoverText(stack, ctx, display, t, flag);
            t.accept(Component.translatable("vadsrijk.bazen.effect.titel").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable("vadsrijk.bazen.effect." + type).withStyle(ChatFormatting.YELLOW)));
        }
    }

    public static class Trofee extends Item {
        public Trofee(Properties p) {
            super(p);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> t, TooltipFlag flag) {
            t.accept(Component.translatable("vadsrijk.bazen.trofee.uitleg").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    private static DeferredItem<Item> wapen(String id, String type) {
        return BazenRegistry.ITEMS.registerItem(id, p -> new BaasItem(p, type), () -> new Item.Properties().stacksTo(1)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
    }

    private static DeferredItem<Item> harnas(String id, String type, Slot slot) {
        // visible on the player (kern's pink look)
        return BazenRegistry.ITEMS.registerItem(id, p -> new BaasItem(p, type),
                () -> nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry.harnasEigenschappen(slot, null));
    }

    private static DeferredItem<Item> trofeeItem(String id) {
        return BazenRegistry.ITEMS.registerItem(id, Trofee::new, () -> new Item.Properties().stacksTo(16));
    }

    public static final DeferredItem<Item> KLODDERKNOTS = wapen("vr_baas_klodderknots", "baas_klodderknots");
    public static final DeferredItem<Item> PLUISBOOG = wapen("vr_baas_pluisboog", "baas_pluisboog");
    public static final DeferredItem<Item> SPORENSTAF = wapen("vr_baas_sporenstaf", "baas_sporenstaf");
    public static final DeferredItem<Item> WORTELWAMBUIS = harnas("vr_baas_wortelwambuis", "baas_wortelwambuis", Slot.BORST);
    public static final DeferredItem<Item> LICHTZWAMHELM = harnas("vr_baas_lichtzwamhelm", "baas_lichtzwamhelm", Slot.HOOFD);
    public static final DeferredItem<Item> ROOKBOMLAARZEN = harnas("vr_baas_rookbomlaarzen", "baas_rookbomlaarzen", Slot.VOETEN);
    public static final DeferredItem<Item> KLODDERKROONTJE = harnas("vr_baas_klodderkroontje", "baas_klodderkroontje", Slot.HOOFD);
    public static final DeferredItem<Item> NEPPE_SNOR = harnas("vr_baas_neppe_snor", "baas_neppe_snor", Slot.HOOFD);
    public static final DeferredItem<Item> TROFEE_KONING = trofeeItem("vr_baas_trofee_koning_klodder");
    public static final DeferredItem<Item> TROFEE_OPPERZWAM = trofeeItem("vr_baas_trofee_opperzwam");
    public static final DeferredItem<Item> TROFEE_MIKA = trofeeItem("vr_baas_trofee_mika_spion");

    public static final List<Benoemd> BENOEMD = List.of(
            new Benoemd("baas_klodderknots", KLODDERKNOTS, Zeldzaamheid.LEGENDARISCH, "koning_klodder", 0.08),
            new Benoemd("baas_pluisboog", PLUISBOOG, Zeldzaamheid.LEGENDARISCH, "koning_klodder", 0.08),
            new Benoemd("baas_klodderkroontje", KLODDERKROONTJE, Zeldzaamheid.GUHDDELIJK, "koning_klodder", 0.02),
            new Benoemd("baas_sporenstaf", SPORENSTAF, Zeldzaamheid.LEGENDARISCH, "opperzwam", 0.10),
            new Benoemd("baas_wortelwambuis", WORTELWAMBUIS, Zeldzaamheid.LEGENDARISCH, "opperzwam", 0.10),
            new Benoemd("baas_lichtzwamhelm", LICHTZWAMHELM, Zeldzaamheid.LEGENDARISCH, "opperzwam", 0.10),
            new Benoemd("baas_rookbomlaarzen", ROOKBOMLAARZEN, Zeldzaamheid.LEGENDARISCH, "mika_spion", 0.15),
            new Benoemd("baas_neppe_snor", NEPPE_SNOR, Zeldzaamheid.GUHDDELIJK, "mika_spion", 0.03));

    /** Called from BazenRegistry (after the items exist as deferred holders: base types only hold the id string). */
    static void typen() {
        // weapons: soort = the class weapon kind (ridder: knots; jager: boog; magier: staf - klassen slice decides the list)
        BasisTypes.registreer(BasisType.wapen("baas_klodderknots", "guhs:vr_baas_klodderknots", "knots", Klassen.RIDDER, Element.WATER,
                6.5, 1.3, Stats.van(Stat.KRACHT, 4, Stat.VADS, 20), Stats.van(Stat.KRACHT, 0.5), 1));
        BasisTypes.registreer(BasisType.wapen("baas_pluisboog", "guhs:vr_baas_pluisboog", "boog", Klassen.JAGER, Element.WATER,
                6.5, 1.3, Stats.van(Stat.BEHENDIGHEID, 4, Stat.KRITIEK, 3), Stats.van(Stat.BEHENDIGHEID, 0.5), 1));
        BasisTypes.registreer(BasisType.wapen("baas_sporenstaf", "guhs:vr_baas_sporenstaf", "staf", Klassen.MAGIER, Element.NATUUR,
                6.5, 1.3, Stats.van(Stat.WIJSHEID, 6, Stat.HEALKRACHT, 5), Stats.van(Stat.WIJSHEID, 0.6), 1));
        BasisTypes.registreer(new BasisType("baas_wortelwambuis", "guhs:vr_baas_wortelwambuis", Slot.BORST, "harnas", null, Element.NATUUR, 0, 0,
                Stats.van(Stat.VERDEDIGING, 12, Stat.VADS, 40, Stat.ELEMENTWEERSTAND, 5), Stats.van(Stat.VERDEDIGING, 2, Stat.VADS, 6), 1));
        BasisTypes.registreer(BasisType.harnas("baas_lichtzwamhelm", "guhs:vr_baas_lichtzwamhelm", Slot.HOOFD, 7, 24, 1));
        BasisTypes.registreer(new BasisType("baas_rookbomlaarzen", "guhs:vr_baas_rookbomlaarzen", Slot.VOETEN, "harnas", null, Element.GEEN, 0, 0,
                Stats.van(Stat.VERDEDIGING, 5, Stat.LOOPSNELHEID, 6), Stats.van(Stat.VERDEDIGING, 1, Stat.VADS, 3), 1));
        BasisTypes.registreer(new BasisType("baas_klodderkroontje", "guhs:vr_baas_klodderkroontje", Slot.HOOFD, "harnas", null, Element.ROZE, 0, 0,
                Stats.van(Stat.VERDEDIGING, 6, Stat.VADS, 30, Stat.HEALKRACHT, 8), Stats.van(Stat.VERDEDIGING, 1.2, Stat.VADS, 5), 1));
        BasisTypes.registreer(new BasisType("baas_neppe_snor", "guhs:vr_baas_neppe_snor", Slot.HOOFD, "harnas", null, Element.GEEN, 0, 0,
                Stats.van(Stat.VERDEDIGING, 5, Stat.BEHENDIGHEID, 6, Stat.KRITIEK, 4), Stats.van(Stat.VERDEDIGING, 1, Stat.BEHENDIGHEID, 0.6), 1));
    }

    /** A rolled named item (its fixed rarity, this level), gebonden. */
    public static ItemStack maak(Benoemd b, int level, RandomSource r) {
        BasisType t = BasisTypes.van(b.type());
        if (t == null) {
            return ItemStack.EMPTY;
        }
        return Rollen.stack(Rollen.rol(t, level, b.zeldzaamheid(), r).metGebonden(true));
    }

    @Nullable
    public static Benoemd benoemd(String type) {
        for (Benoemd b : BENOEMD) {
            if (b.type().equals(type)) {
                return b;
            }
        }
        return null;
    }

    public static ItemStack trofee(String baas) {
        return new ItemStack(switch (baas) {
            case "opperzwam" -> TROFEE_OPPERZWAM.get();
            case "mika_spion" -> TROFEE_MIKA.get();
            default -> TROFEE_KONING.get();
        });
    }

    /** The table for the random gear part: economie's per-boss table when it exists, else kern's reference table. */
    @Nullable
    static LootTabel tabel(String baas) {
        LootTabel t = LootTabellen.van("economie:baas_" + baas);
        if (t == null) {
            t = LootTabellen.van("economie:zone1");
        }
        if (t == null) {
            t = LootTabellen.van("kern:klodderreus");
        }
        return t;
    }

    /**
     * What one player's chest holds: random gear (gebonden), named items by chance (the first kill per player guarantees one
     * Legendary of that boss; the Mika-spion's first kill also gives his Guhddelijk snor), maybe a trophy (always the first time).
     */
    public static LootTabel.Resultaat kist(String baas, ServerPlayer p, int level, RandomSource r) {
        List<ItemStack> items = new ArrayList<>();
        long munten = 0;
        LootTabel t = tabel(baas);
        if (t != null) {
            LootTabel.Resultaat basis = t.rol(level, r);
            items.addAll(basis.items());
            munten = basis.munten();
        }
        CompoundTag extra = Spelers.data(p).extra("bazen");
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
            boolean zeker = eerste && b.zeldzaamheid() == Zeldzaamheid.GUHDDELIJK && baas.equals("mika_spion");
            if (zeker || r.nextDouble() < b.kans()) {
                items.add(maak(b, level, r));
            }
        }
        if (eerste && !legendarisch.isEmpty()) {
            boolean alEen = items.stream().anyMatch(s -> {
                Uitrusting u = Rollen.van(s);
                return u != null && u.type().startsWith("baas_") && u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH;
            });
            if (!alEen) {
                items.add(maak(legendarisch.get(r.nextInt(legendarisch.size())), level, r));
            }
            p.sendSystemMessage(Component.translatable("vadsrijk.bazen.kist.eerste").withStyle(ChatFormatting.GOLD));
        }
        if (eerste || r.nextDouble() < 0.25) {
            items.add(trofee(baas));
        }
        // (I-MOUNTS: Koning Klodder's Klodderzadel is gone with the Klodderros; his rare mount loot is the sterrennacht egg below)
        // g-int (round 2, mounts): rare Pluimguh eggs (sterrennacht from Koning Klodder, the hidden gold one from the Opperzwam)
        pluimEi(baas, items, r);
        if (eerste) {
            extra.putBoolean(sleutel, true);
        }
        List<ItemStack> gebonden = new ArrayList<>();
        for (ItemStack s : items) {
            Uitrusting u = Rollen.van(s);
            if (u != null && !u.gebonden()) {
                s.set(KernRegistry.UITRUSTING.get(), u.metGebonden(true));
            }
            gebonden.add(s);
        }
        return new LootTabel.Resultaat(gebonden, munten);
    }

    /** Chance per chest of a sterrennacht / gold Pluimguh egg (g-mounts colours; mount screen: "baas" / hidden "goud"). */
    public static final double EI_STERRENNACHT_KANS = 0.05, EI_GOUD_KANS = 0.02;

    /** g-int: the Pluimguh egg drops of the zone-1 and zone-2 bosses (Z2BazenLoot calls this too). */
    public static void pluimEi(String baas, List<ItemStack> items, RandomSource r) {
        var kleur = switch (baas) {
            case "koning_klodder", "galmwolf" -> r.nextDouble() < EI_STERRENNACHT_KANS
                    ? nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur.STERRENNACHT : null;
            case "opperzwam", "spiegelzucht" -> r.nextDouble() < EI_GOUD_KANS
                    ? nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur.GOUD : null;
            default -> null;
        };
        if (kleur != null) {
            items.add(new ItemStack(nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul.PLUIMEI.get(kleur).get()));
        }
    }

    static String id(Zeldzaamheid z) {
        return z.name().toLowerCase(Locale.ROOT);
    }
}
