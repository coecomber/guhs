package nl.juiced.guhs.feature.vadsrijk.zone4.bazen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

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

/**
 * Zone-4 boss loot (lv 54-56): 5 named Legendaries + 1 Guhddelijk, 2 trophies (deco, not gebonden). A chest = the economie boss
 * table (when there is one), one sure zone-4 piece (set piece or class weapon, Zeldzaam or better), the named rolls, and the
 * first kill per player guarantees a named Legendary + the trophy. Everything from a chest is gebonden except the trophies.
 * <ul>
 *   <li>De Grauwe Zeeslurf: Slurfstaf (Magier, Water), Zeekoeienhuid (borst), Lagunelampion (Guhddelijk oorbel, 2 %).</li>
 *   <li>Kootje de Vliegervanger: Oorlogsvliegerboog (Jager, Natuur), Kootjes Enterhaak (Ridder, Vuur),
 *       Kaasdoek van de Vliegervanger (hoofd).</li>
 * </ul>
 */
public final class Z4BazenLoot {
    private Z4BazenLoot() {
    }

    public record Benoemd(String type, DeferredItem<Item> item, Zeldzaamheid zeldzaamheid, String baas, double kans) {
    }

    private static DeferredItem<Item> wapen(String id, String type) {
        return Z4BazenRegistry.ITEMS.registerItem(id, p -> new BazenLoot.BaasItem(p, type), () -> new Item.Properties().stacksTo(1)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
    }

    private static DeferredItem<Item> harnas(String id, String type, Slot slot) {
        return Z4BazenRegistry.ITEMS.registerItem(id, p -> new BazenLoot.BaasItem(p, type), () -> KernRegistry.harnasEigenschappen(slot, null));
    }

    private static DeferredItem<Item> sieraad(String id, String type) {
        return Z4BazenRegistry.ITEMS.registerItem(id, p -> new BazenLoot.BaasItem(p, type), () -> new Item.Properties().stacksTo(1));
    }

    public static final DeferredItem<Item> SLURFSTAF = wapen("vr_z4_baas_slurfstaf", "z4baas_slurfstaf");
    public static final DeferredItem<Item> ZEEKOEIENHUID = harnas("vr_z4_baas_zeekoeienhuid", "z4baas_zeekoeienhuid", Slot.BORST);
    public static final DeferredItem<Item> LAGUNELAMPION = sieraad("vr_z4_baas_lagunelampion", "z4baas_lagunelampion");
    public static final DeferredItem<Item> OORLOGSVLIEGER = wapen("vr_z4_baas_oorlogsvliegerboog", "z4baas_oorlogsvliegerboog");
    public static final DeferredItem<Item> ENTERHAAK = wapen("vr_z4_baas_enterhaak", "z4baas_enterhaak");
    public static final DeferredItem<Item> KAASDOEK = harnas("vr_z4_baas_kaasdoek", "z4baas_kaasdoek", Slot.HOOFD);

    public static final List<String> BAZEN = List.of("zeeslurf", "vliegervanger");
    public static final List<DeferredItem<Item>> TROFEEEN = BAZEN.stream()
            .map(b -> Z4BazenRegistry.ITEMS.<Item>registerItem("vr_z4_baas_trofee_" + b, p -> new BazenLoot.Trofee(p), () -> new Item.Properties().stacksTo(16)))
            .toList();

    public static final List<Benoemd> BENOEMD = List.of(
            new Benoemd("z4baas_slurfstaf", SLURFSTAF, Zeldzaamheid.LEGENDARISCH, "zeeslurf", 0.12),
            new Benoemd("z4baas_zeekoeienhuid", ZEEKOEIENHUID, Zeldzaamheid.LEGENDARISCH, "zeeslurf", 0.10),
            new Benoemd("z4baas_lagunelampion", LAGUNELAMPION, Zeldzaamheid.GUHDDELIJK, "zeeslurf", 0.02),
            new Benoemd("z4baas_oorlogsvliegerboog", OORLOGSVLIEGER, Zeldzaamheid.LEGENDARISCH, "vliegervanger", 0.12),
            new Benoemd("z4baas_enterhaak", ENTERHAAK, Zeldzaamheid.LEGENDARISCH, "vliegervanger", 0.12),
            new Benoemd("z4baas_kaasdoek", KAASDOEK, Zeldzaamheid.LEGENDARISCH, "vliegervanger", 0.10));

    /** Base types (called from the registry after the items exist). Weapons a step above the zone-3 boss line. */
    static void typen() {
        BasisTypes.registreer(BasisType.wapen("z4baas_slurfstaf", "guhs:vr_z4_baas_slurfstaf", "staf", Klassen.MAGIER, Element.WATER,
                8.8, 1.7, Stats.van(Stat.WIJSHEID, 13, Stat.HEALKRACHT, 10), Stats.van(Stat.WIJSHEID, 0.95), 1));
        BasisTypes.registreer(new BasisType("z4baas_zeekoeienhuid", "guhs:vr_z4_baas_zeekoeienhuid", Slot.BORST, "harnas", null, Element.WATER, 0, 0,
                Stats.van(Stat.VERDEDIGING, 26, Stat.VADS, 140, Stat.ELEMENTWEERSTAND, 8), Stats.van(Stat.VERDEDIGING, 3.4, Stat.VADS, 12), 1));
        BasisTypes.registreer(BasisType.sieraad("z4baas_lagunelampion", "guhs:vr_z4_baas_lagunelampion", Slot.OORBEL,
                Stats.van(Stat.HEALKRACHT, 16, Stat.WIJSHEID, 10, Stat.ELEMENTSCHADE, 8), Stats.van(Stat.WIJSHEID, 0.6), 1));
        BasisTypes.registreer(BasisType.wapen("z4baas_oorlogsvliegerboog", "guhs:vr_z4_baas_oorlogsvliegerboog", "boog", Klassen.JAGER, Element.NATUUR,
                8.8, 1.7, Stats.van(Stat.BEHENDIGHEID, 12, Stat.KRITIEK, 5), Stats.van(Stat.BEHENDIGHEID, 0.95), 1));
        BasisTypes.registreer(BasisType.wapen("z4baas_enterhaak", "guhs:vr_z4_baas_enterhaak", "knots", Klassen.RIDDER, Element.VUUR,
                9.0, 1.75, Stats.van(Stat.KRACHT, 12, Stat.VERDEDIGING, 10), Stats.van(Stat.KRACHT, 0.95), 1));
        BasisTypes.registreer(new BasisType("z4baas_kaasdoek", "guhs:vr_z4_baas_kaasdoek", Slot.HOOFD, "harnas", null, Element.VUUR, 0, 0,
                Stats.van(Stat.VERDEDIGING, 16, Stat.KRITIEK, 6, Stat.LOOPSNELHEID, 6), Stats.van(Stat.VERDEDIGING, 2.2, Stat.KRITIEK, 0.15), 1));
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
        for (String id : List.of("economie:z4_baas_" + baas, "economie:zone4", "economie:zone3", "economie:zone2", "economie:zone1")) {
            LootTabel t = LootTabellen.van(id);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    /** One sure zone-4 piece: a set piece or a class weapon, Zeldzaam or better. */
    private static ItemStack zekerStuk(int level, RandomSource r) {
        List<String> pool = new ArrayList<>(nl.juiced.guhs.feature.vadsrijk.zone4.wezens.loot.Z4Loot.SCHELPEN);
        pool.addAll(nl.juiced.guhs.feature.vadsrijk.zone4.wezens.loot.Z4Loot.LAMPION);
        pool.addAll(nl.juiced.guhs.feature.vadsrijk.zone4.wezens.loot.Z4Loot.WAPENS);
        BasisType t = BasisTypes.van(pool.get(r.nextInt(pool.size())));
        if (t == null) {
            return ItemStack.EMPTY;
        }
        Zeldzaamheid z = Zeldzaamheid.rol(r, 4.0);
        if (z.ordinal() < Zeldzaamheid.ZELDZAAM.ordinal()) {
            z = Zeldzaamheid.ZELDZAAM;
        }
        return Rollen.stack(Rollen.rol(t, level, z, r));
    }

    /** One player's chest. */
    public static LootTabel.Resultaat kist(String baas, ServerPlayer p, int level, RandomSource r) {
        List<ItemStack> items = new ArrayList<>();
        long munten = 600 + r.nextInt(400);
        LootTabel t = tabel(baas);
        if (t != null) {
            LootTabel.Resultaat basis = t.rol(level, r);
            items.addAll(basis.items());
            munten += basis.munten();
        }
        items.add(zekerStuk(level, r));
        CompoundTag extra = Spelers.data(p).extra("z4bazen");
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
                    Component c = Component.translatable("vadsrijk.z4bazen.loot.guhddelijk", p.getDisplayName(),
                            Component.translatable("item.guhs." + b.item().getId().getPath())).withStyle(ChatFormatting.LIGHT_PURPLE);
                    sl.getServer().getPlayerList().broadcastSystemMessage(c, false);
                }
            }
        }
        if (eerste && !legendarisch.isEmpty()) {
            boolean alEen = items.stream().anyMatch(s -> {
                Uitrusting u = Rollen.van(s);
                return u != null && u.type().startsWith("z4baas_") && u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH;
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
