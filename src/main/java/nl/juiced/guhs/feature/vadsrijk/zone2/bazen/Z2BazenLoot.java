package nl.juiced.guhs.feature.vadsrijk.zone2.bazen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

/**
 * Zone-2 boss loot: 6 named Legendary items (2 per boss; Spiegelzucht's and the Boormeester's roll at lv 32-33, the Galmwolf's at
 * his lv 24) and 2 Guhddelijk ones, the 3 trophies (deco, NOT gebonden so they can go home), and what goes into a personal chest
 * ({@link #kist}). The first kill per player guarantees one named Legendary + the trophy. Everything else is gebonden.
 * Item classes are zone-1's ({@link BazenLoot.BaasItem}: the effect line, {@link BazenLoot.Trofee}).
 */
public final class Z2BazenLoot {
    private Z2BazenLoot() {
    }

    public record Benoemd(String type, DeferredItem<Item> item, Zeldzaamheid zeldzaamheid, String baas, double kans) {
    }

    private static DeferredItem<Item> wapen(String id, String type) {
        return Z2BazenRegistry.ITEMS.registerItem(id, p -> new BazenLoot.BaasItem(p, type), () -> new Item.Properties().stacksTo(1)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
    }

    private static DeferredItem<Item> harnas(String id, String type, Slot slot) {
        return Z2BazenRegistry.ITEMS.registerItem(id, p -> new BazenLoot.BaasItem(p, type), () -> KernRegistry.harnasEigenschappen(slot, null));
    }

    private static DeferredItem<Item> trofeeItem(String id) {
        return Z2BazenRegistry.ITEMS.registerItem(id, BazenLoot.Trofee::new, () -> new Item.Properties().stacksTo(16));
    }

    public static final DeferredItem<Item> GALMKLAUW = wapen("vr_z2_baas_galmklauw", "z2baas_galmklauw");
    public static final DeferredItem<Item> IJSPELSMANTEL = harnas("vr_z2_baas_ijspelsmantel", "z2baas_ijspelsmantel", Slot.BORST);
    public static final DeferredItem<Item> SPIEGELSTAF = wapen("vr_z2_baas_spiegelstaf", "z2baas_spiegelstaf");
    public static final DeferredItem<Item> SCHADUWLOZE = wapen("vr_z2_baas_schaduwloze", "z2baas_schaduwloze");
    public static final DeferredItem<Item> SPIEGELKROON = harnas("vr_z2_baas_spiegelkroon", "z2baas_spiegelkroon", Slot.HOOFD);
    public static final DeferredItem<Item> BOORHAMER = wapen("vr_z2_baas_boorhamer", "z2baas_boorhamer");
    public static final DeferredItem<Item> STOOMLAARZEN = harnas("vr_z2_baas_stoomlaarzen", "z2baas_stoomlaarzen", Slot.VOETEN);
    public static final DeferredItem<Item> BOORBRIL = harnas("vr_z2_baas_boorbril", "z2baas_boorbril", Slot.HOOFD);
    public static final DeferredItem<Item> TROFEE_GALMWOLF = trofeeItem("vr_z2_baas_trofee_galmwolf");
    public static final DeferredItem<Item> TROFEE_SPIEGELZUCHT = trofeeItem("vr_z2_baas_trofee_spiegelzucht");
    public static final DeferredItem<Item> TROFEE_BOORMEESTER = trofeeItem("vr_z2_baas_trofee_boormeester");

    public static final List<Benoemd> BENOEMD = List.of(
            new Benoemd("z2baas_galmklauw", GALMKLAUW, Zeldzaamheid.LEGENDARISCH, "galmwolf", 0.08),
            new Benoemd("z2baas_ijspelsmantel", IJSPELSMANTEL, Zeldzaamheid.LEGENDARISCH, "galmwolf", 0.08),
            new Benoemd("z2baas_spiegelstaf", SPIEGELSTAF, Zeldzaamheid.LEGENDARISCH, "spiegelzucht", 0.10),
            new Benoemd("z2baas_schaduwloze", SCHADUWLOZE, Zeldzaamheid.LEGENDARISCH, "spiegelzucht", 0.10),
            new Benoemd("z2baas_spiegelkroon", SPIEGELKROON, Zeldzaamheid.GUHDDELIJK, "spiegelzucht", 0.02),
            new Benoemd("z2baas_boorhamer", BOORHAMER, Zeldzaamheid.LEGENDARISCH, "boormeester", 0.15),
            new Benoemd("z2baas_stoomlaarzen", STOOMLAARZEN, Zeldzaamheid.LEGENDARISCH, "boormeester", 0.15),
            new Benoemd("z2baas_boorbril", BOORBRIL, Zeldzaamheid.GUHDDELIJK, "boormeester", 0.03));

    /** Base types (only ids: called from the registry after the deferred items exist). */
    static void typen() {
        BasisTypes.registreer(BasisType.wapen("z2baas_galmklauw", "guhs:vr_z2_baas_galmklauw", "zwaard", Klassen.RIDDER, Element.WATER,
                14, 2.4, Stats.van(Stat.KRACHT, 6, Stat.VADS, 30), Stats.van(Stat.KRACHT, 0.6), 1));
        BasisTypes.registreer(new BasisType("z2baas_ijspelsmantel", "guhs:vr_z2_baas_ijspelsmantel", Slot.BORST, "harnas", null, Element.WATER, 0, 0,
                Stats.van(Stat.VERDEDIGING, 16, Stat.VADS, 60, Stat.ELEMENTWEERSTAND, 6), Stats.van(Stat.VERDEDIGING, 2.4, Stat.VADS, 8), 1));
        BasisTypes.registreer(BasisType.wapen("z2baas_spiegelstaf", "guhs:vr_z2_baas_spiegelstaf", "staf", Klassen.MAGIER, Element.ROZE,
                14, 2.6, Stats.van(Stat.WIJSHEID, 9, Stat.KRITIEK, 4), Stats.van(Stat.WIJSHEID, 0.7), 1));
        BasisTypes.registreer(BasisType.wapen("z2baas_schaduwloze", "guhs:vr_z2_baas_schaduwloze", "kruisboog", Klassen.JAGER, Element.NATUUR,
                15, 2.6, Stats.van(Stat.BEHENDIGHEID, 8, Stat.KRITIEKSCHADE, 10), Stats.van(Stat.BEHENDIGHEID, 0.7), 1));
        BasisTypes.registreer(new BasisType("z2baas_spiegelkroon", "guhs:vr_z2_baas_spiegelkroon", Slot.HOOFD, "harnas", null, Element.ROZE, 0, 0,
                Stats.van(Stat.VERDEDIGING, 9, Stat.VADS, 45, Stat.WIJSHEID, 6), Stats.van(Stat.VERDEDIGING, 1.6, Stat.VADS, 6), 1));
        BasisTypes.registreer(BasisType.wapen("z2baas_boorhamer", "guhs:vr_z2_baas_boorhamer", "hamer", Klassen.RIDDER, Element.VUUR,
                17, 2.8, Stats.van(Stat.KRACHT, 8, Stat.VERDEDIGING, 6), Stats.van(Stat.KRACHT, 0.7), 1));
        BasisTypes.registreer(new BasisType("z2baas_stoomlaarzen", "guhs:vr_z2_baas_stoomlaarzen", Slot.VOETEN, "harnas", null, Element.VUUR, 0, 0,
                Stats.van(Stat.VERDEDIGING, 8, Stat.LOOPSNELHEID, 8, Stat.VADS, 30), Stats.van(Stat.VERDEDIGING, 1.4, Stat.VADS, 5), 1));
        BasisTypes.registreer(new BasisType("z2baas_boorbril", "guhs:vr_z2_baas_boorbril", Slot.HOOFD, "harnas", null, Element.GEEN, 0, 0,
                Stats.van(Stat.VERDEDIGING, 8, Stat.KRITIEK, 6, Stat.KRITIEKSCHADE, 12), Stats.van(Stat.VERDEDIGING, 1.4, Stat.KRITIEK, 0.15), 1));
    }

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
            case "spiegelzucht" -> TROFEE_SPIEGELZUCHT.get();
            case "boormeester" -> TROFEE_BOORMEESTER.get();
            default -> TROFEE_GALMWOLF.get();
        });
    }

    /** The random gear part: economie's zone-2 tables when they exist, else zone 1's, else kern's reference table. */
    @Nullable
    static LootTabel tabel(String baas) {
        for (String id : List.of("economie:z2_baas_" + baas, "economie:baas_" + baas, "economie:zone2", "economie:zone1", "kern:klodderreus")) {
            LootTabel t = LootTabellen.van(id);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    /** One player's chest: random gear, named items by chance (first kill: a sure Legendary + trophy), all gebonden. */
    public static LootTabel.Resultaat kist(String baas, ServerPlayer p, int level, RandomSource r) {
        List<ItemStack> items = new ArrayList<>();
        long munten = 0;
        LootTabel t = tabel(baas);
        if (t != null) {
            LootTabel.Resultaat basis = t.rol(level, r);
            items.addAll(basis.items());
            munten = basis.munten();
        }
        CompoundTag extra = Spelers.data(p).extra("z2bazen");
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
            }
        }
        if (eerste && !legendarisch.isEmpty()) {
            boolean alEen = items.stream().anyMatch(s -> {
                Uitrusting u = Rollen.van(s);
                return u != null && u.type().startsWith("z2baas_") && u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH;
            });
            if (!alEen) {
                items.add(maak(legendarisch.get(r.nextInt(legendarisch.size())), level, r));
            }
            p.sendSystemMessage(Component.translatable("vadsrijk.bazen.kist.eerste").withStyle(ChatFormatting.GOLD));
        }
        if (eerste || r.nextDouble() < 0.25) {
            items.add(trofee(baas));
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
}
