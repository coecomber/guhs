package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenLoot;
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
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.loot.Z5Loot;

/**
 * Zone-5 boss loot (lv 59-60): 6 named Legendaries + 1 Guhddelijk, 3 trophies (deco, not gebonden). A chest = the base roll of
 * {@code z5wezens:baas} (Wolkwacht pieces, jewellery, weapons) + the boss's named drops; the first kill per player guarantees a named
 * Legendary + the trophy. Everything from a chest is gebonden except the trophy.
 */
public final class Z5BazenLoot {
    private Z5BazenLoot() {
    }

    public record Benoemd(String type, DeferredItem<Item> item, Zeldzaamheid zeldzaamheid, String baas, double kans) {
    }

    private static DeferredItem<Item> wapen(String id, String type) {
        return Z5WezensRegistry.ITEMS.registerItem(id, p -> new BazenLoot.BaasItem(p, type), () -> new Item.Properties().stacksTo(1)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
    }

    private static DeferredItem<Item> stuk(String id, String type, Slot slot) {
        return Z5WezensRegistry.ITEMS.registerItem(id, p -> new BazenLoot.BaasItem(p, type),
                () -> slot.harnas() ? KernRegistry.harnasEigenschappen(slot, null) : new Item.Properties().stacksTo(1));
    }

    /** The Mantel der Vermommingen is a CAPE with t-items, else a chest piece. */
    public static final Slot MANTEL_SLOT = Z5Loot.CAPE != null ? Z5Loot.CAPE : Slot.BORST;

    public static final DeferredItem<Item> HOEDENSPELD = wapen("vr_z5_baas_hoedenspeld", "z5baas_hoedenspeld");
    public static final DeferredItem<Item> MANTEL = stuk("vr_z5_baas_vermommingsmantel", "z5baas_vermommingsmantel", MANTEL_SLOT);
    public static final DeferredItem<Item> MOERSLEUTEL = wapen("vr_z5_baas_moersleutel", "z5baas_moersleutel");
    public static final DeferredItem<Item> KLEPPENSTAF = wapen("vr_z5_baas_kleppenstaf", "z5baas_kleppenstaf");
    public static final DeferredItem<Item> DONDERBOOG = wapen("vr_z5_baas_donderboog", "z5baas_donderboog");
    public static final DeferredItem<Item> ONWEERSTAF = wapen("vr_z5_baas_onweerstaf", "z5baas_onweerstaf");
    public static final DeferredItem<Item> REGENJASJE = stuk("vr_z5_baas_regenjasje", "z5baas_regenjasje", Slot.BORST);

    public static final List<String> BAZEN = List.of("zilverstaart", "gorgonzola", "donderbuik");
    public static final List<DeferredItem<BazenLoot.Trofee>> TROFEEEN = BAZEN.stream()
            .map(b -> Z5WezensRegistry.ITEMS.registerItem("vr_z5_baas_trofee_" + b, BazenLoot.Trofee::new, () -> new Item.Properties().stacksTo(16))).toList();

    public static final List<Benoemd> BENOEMD = List.of(
            new Benoemd("z5baas_hoedenspeld", HOEDENSPELD, Zeldzaamheid.LEGENDARISCH, "zilverstaart", 0.12),
            new Benoemd("z5baas_vermommingsmantel", MANTEL, Zeldzaamheid.LEGENDARISCH, "zilverstaart", 0.10),
            new Benoemd("z5baas_moersleutel", MOERSLEUTEL, Zeldzaamheid.LEGENDARISCH, "gorgonzola", 0.12),
            new Benoemd("z5baas_kleppenstaf", KLEPPENSTAF, Zeldzaamheid.LEGENDARISCH, "gorgonzola", 0.12),
            new Benoemd("z5baas_donderboog", DONDERBOOG, Zeldzaamheid.LEGENDARISCH, "donderbuik", 0.10),
            new Benoemd("z5baas_onweerstaf", ONWEERSTAF, Zeldzaamheid.LEGENDARISCH, "donderbuik", 0.10),
            new Benoemd("z5baas_regenjasje", REGENJASJE, Zeldzaamheid.GUHDDELIJK, "donderbuik", 0.02));

    /** Base types (after the items exist). A step above the zone-3 boss weapons (8.6 + 1.7/lv). */
    public static void typen() {
        Stat bazen = Z5Loot.stat("BAZENSCHADE");
        BasisTypes.registreer(BasisType.wapen("z5baas_hoedenspeld", "guhs:vr_z5_baas_hoedenspeld", "zwaard", Klassen.RIDDER, Element.VUUR,
                8.6, 1.7, Stats.van(Stat.KRACHT, 12, Stat.KRITIEK, 6), Stats.van(Stat.KRACHT, 0.95), 1));
        Stats mantel = Stats.van(Stat.VADS, 110, Stat.BEHENDIGHEID, 10, Stat.LOOPSNELHEID, 5);
        Stats mantelGroei = Stats.van(Stat.VADS, 10, Stat.BEHENDIGHEID, 0.6);
        if (MANTEL_SLOT == Slot.BORST) {
            mantel.plus(Stat.VERDEDIGING, 26);
            mantelGroei.plus(Stat.VERDEDIGING, 3.2);
        }
        BasisTypes.registreer(new BasisType("z5baas_vermommingsmantel", "guhs:vr_z5_baas_vermommingsmantel", MANTEL_SLOT,
                MANTEL_SLOT == Slot.BORST ? "harnas" : "cape", null, Element.VUUR, 0, 0, mantel, mantelGroei, 1));
        BasisTypes.registreer(BasisType.wapen("z5baas_moersleutel", "guhs:vr_z5_baas_moersleutel", "hamer", Klassen.RIDDER, Element.VUUR,
                8.8, 1.75, Stats.van(Stat.KRACHT, 13, Stat.VERDEDIGING, 10), Stats.van(Stat.KRACHT, 1.0), 1));
        BasisTypes.registreer(BasisType.wapen("z5baas_kleppenstaf", "guhs:vr_z5_baas_kleppenstaf", "staf", Klassen.MAGIER, Element.ROZE,
                8.6, 1.7, Stats.van(Stat.WIJSHEID, 13, Stat.HEALKRACHT, 14), Stats.van(Stat.WIJSHEID, 0.95), 1));
        Stats boog = Stats.van(Stat.BEHENDIGHEID, 13, Stat.KRITIEKSCHADE, 14);
        if (bazen != null) {
            boog.plus(bazen, 5);
        }
        BasisTypes.registreer(BasisType.wapen("z5baas_donderboog", "guhs:vr_z5_baas_donderboog", "boog", Klassen.JAGER, Element.WATER,
                8.8, 1.75, boog, Stats.van(Stat.BEHENDIGHEID, 1.0), 1));
        BasisTypes.registreer(BasisType.wapen("z5baas_onweerstaf", "guhs:vr_z5_baas_onweerstaf", "staf", Klassen.MAGIER, Element.WATER,
                8.8, 1.75, Stats.van(Stat.WIJSHEID, 13, Stat.ELEMENTSCHADE, 10), Stats.van(Stat.WIJSHEID, 1.0), 1));
        BasisTypes.registreer(new BasisType("z5baas_regenjasje", "guhs:vr_z5_baas_regenjasje", Slot.BORST, "harnas", null, Element.ROZE, 0, 0,
                Stats.van(Stat.VERDEDIGING, 30, Stat.VADS, 140, Stat.HEALKRACHT, 12, Stat.ELEMENTWEERSTAND, 10), Stats.van(Stat.VERDEDIGING, 3.6, Stat.VADS, 13), 1));
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

    /** One player's chest. */
    public static LootTabel.Resultaat kist(String baas, ServerPlayer p, int level, RandomSource r) {
        List<ItemStack> items = new ArrayList<>();
        long munten = 0;
        LootTabel t = LootTabellen.van("z5wezens:baas");
        if (t != null) {
            LootTabel.Resultaat basis = t.rol(level, r);
            items.addAll(basis.items());
            munten = basis.munten();
        }
        CompoundTag extra = Spelers.data(p).extra("z5wezens");
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
            if (r.nextDouble() < b.kans()) {
                items.add(maak(b, level, r));
                if (b.zeldzaamheid() == Zeldzaamheid.GUHDDELIJK && p.level() instanceof ServerLevel sl) {
                    sl.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("vadsrijk.z5wezens.loot.guhddelijk", p.getDisplayName(),
                            Component.translatable("item.guhs." + b.item().getId().getPath())).withStyle(ChatFormatting.LIGHT_PURPLE), false);
                }
            }
        }
        if (eerste && !legendarisch.isEmpty()) {
            boolean alEen = items.stream().anyMatch(s -> {
                Uitrusting u = Rollen.van(s);
                return u != null && u.type().startsWith("z5baas_") && u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH;
            });
            if (!alEen) {
                items.add(maak(legendarisch.get(r.nextInt(legendarisch.size())), level, r));
            }
            p.sendSystemMessage(Component.translatable("vadsrijk.bazen.kist.eerste").withStyle(ChatFormatting.GOLD));
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
        if (eerste || r.nextDouble() < 0.25) {
            uit.add(trofee(baas));
        }
        if (eerste) {
            extra.putBoolean(sleutel, true);
            Spelers.opgeslagen(p);
        }
        return new LootTabel.Resultaat(uit, munten);
    }
}
