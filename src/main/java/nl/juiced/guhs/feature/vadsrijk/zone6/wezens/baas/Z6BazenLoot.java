package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas;

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
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.GoudenVeren;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.loot.Z6Loot;

/**
 * Zone-6 boss loot (lv 63-68). A chest = the economie boss table (when there is one), one sure SET piece (field bosses: the
 * Karavaanwacht set; the dungeon: the Sterrenwacht set; Episch or better), the named rolls, Gouden Veren (field bosses: 2 on the
 * first kill, then 25 % for 1; the dungeon end boss 1 every time). The first kill per player guarantees a named Legendary + the
 * trophy. Everything from a chest is gebonden except the trophies.
 * <ul>
 *   <li>Koningin Wormelien: Wormelientand (Ridder knots), Duinzwemmerslaarzen (voeten), Wimperwaaier (oorbel, Guhddelijk 2 %).</li>
 *   <li>De Zoutkolos: Zingend Zoutzwaard (Ridder), Zoutsplinterboog (Jager), Zoutkristalkroon (hoofd).</li>
 *   <li>De Astrolabiumwachter: Astrolabiumring (ring). Kootje Glimmertje: Glimmertjes Dover (Magier staf).</li>
 *   <li>De Verzonken Sterrenguh: Sterrenkijkerkruisboog (Jager), Sterrenhartamulet (ketting).</li>
 * </ul>
 * The effects are in {@link Z6BazenEffecten}.
 */
public final class Z6BazenLoot {
    private Z6BazenLoot() {
    }

    public record Benoemd(String type, DeferredItem<Item> item, Zeldzaamheid zeldzaamheid, String baas, double kans) {
    }

    private static DeferredItem<Item> wapen(String id) {
        return Z6WezensRegistry.ITEMS.registerItem("vr_z6_baas_" + id, p -> new BazenLoot.BaasItem(p, "z6baas_" + id), () -> new Item.Properties().stacksTo(1)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
    }

    private static DeferredItem<Item> harnas(String id, Slot slot) {
        return Z6WezensRegistry.ITEMS.registerItem("vr_z6_baas_" + id, p -> new BazenLoot.BaasItem(p, "z6baas_" + id), () -> KernRegistry.harnasEigenschappen(slot, null));
    }

    private static DeferredItem<Item> sieraad(String id) {
        return Z6WezensRegistry.ITEMS.registerItem("vr_z6_baas_" + id, p -> new BazenLoot.BaasItem(p, "z6baas_" + id), () -> new Item.Properties().stacksTo(1));
    }

    public static final DeferredItem<Item> WORMELIENTAND = wapen("wormelientand");
    public static final DeferredItem<Item> DUINZWEMMERSLAARZEN = harnas("duinzwemmerslaarzen", Slot.VOETEN);
    public static final DeferredItem<Item> WIMPERWAAIER = sieraad("wimperwaaier");
    public static final DeferredItem<Item> ZOUTZWAARD = wapen("zingend_zoutzwaard");
    public static final DeferredItem<Item> ZOUTSPLINTERBOOG = wapen("zoutsplinterboog");
    public static final DeferredItem<Item> ZOUTKRISTALKROON = harnas("zoutkristalkroon", Slot.HOOFD);
    public static final DeferredItem<Item> ASTROLABIUMRING = sieraad("astrolabiumring");
    public static final DeferredItem<Item> REUZENDOVER = wapen("glimmertjes_dover");
    public static final DeferredItem<Item> STERRENKIJKER = wapen("sterrenkijkerkruisboog");
    public static final DeferredItem<Item> STERRENHART = sieraad("sterrenhartamulet");

    public static final List<String> BAZEN = List.of("wormelien", "zoutkolos", "astrolabiumwachter", "glimmertje", "sterrenguh");
    public static final List<String> MET_TROFEE = List.of("wormelien", "zoutkolos", "sterrenguh");
    public static final List<DeferredItem<Item>> TROFEEEN = MET_TROFEE.stream()
            .map(b -> Z6WezensRegistry.ITEMS.<Item>registerItem("vr_z6_baas_trofee_" + b, p -> new BazenLoot.Trofee(p), () -> new Item.Properties().stacksTo(16)))
            .toList();

    public static final List<Benoemd> BENOEMD = List.of(
            new Benoemd("z6baas_wormelientand", WORMELIENTAND, Zeldzaamheid.LEGENDARISCH, "wormelien", 0.12),
            new Benoemd("z6baas_duinzwemmerslaarzen", DUINZWEMMERSLAARZEN, Zeldzaamheid.LEGENDARISCH, "wormelien", 0.10),
            new Benoemd("z6baas_wimperwaaier", WIMPERWAAIER, Zeldzaamheid.GUHDDELIJK, "wormelien", 0.02),
            new Benoemd("z6baas_zingend_zoutzwaard", ZOUTZWAARD, Zeldzaamheid.LEGENDARISCH, "zoutkolos", 0.12),
            new Benoemd("z6baas_zoutsplinterboog", ZOUTSPLINTERBOOG, Zeldzaamheid.LEGENDARISCH, "zoutkolos", 0.12),
            new Benoemd("z6baas_zoutkristalkroon", ZOUTKRISTALKROON, Zeldzaamheid.LEGENDARISCH, "zoutkolos", 0.10),
            new Benoemd("z6baas_astrolabiumring", ASTROLABIUMRING, Zeldzaamheid.LEGENDARISCH, "astrolabiumwachter", 0.10),
            new Benoemd("z6baas_glimmertjes_dover", REUZENDOVER, Zeldzaamheid.LEGENDARISCH, "glimmertje", 0.12),
            new Benoemd("z6baas_sterrenkijkerkruisboog", STERRENKIJKER, Zeldzaamheid.LEGENDARISCH, "sterrenguh", 0.12),
            new Benoemd("z6baas_sterrenhartamulet", STERRENHART, Zeldzaamheid.LEGENDARISCH, "sterrenguh", 0.10));

    /** Base types: the two boss sets and the named items (from the registry after the items exist). */
    public static void typen() {
        // Karavaanwacht (field bosses): striped saddle cloth with brass buckles
        Z6Loot.stuk("karavaanwachtwanten", 0, 0, Stat.VERDEDIGING, 16, Stat.KRACHT, 8, Stat.BEHENDIGHEID, 8);
        Z6Loot.stuk("karavaanwachtcape", 0, 0, Stat.VADS, 80, Stat.ELEMENTWEERSTAND, 7, Stat.WIJSHEID, 6);
        Z6Loot.stuk("karavaanwachtarmband", 0, 0, Stat.AANVALSSNELHEID, 6, Stat.KRITIEK, 3, Stat.BAZENSCHADE, 2);
        Z6Loot.stuk("karavaanwachtlaarzen", 22, 68, Stat.LOOPSNELHEID, 7, Stat.KRACHT, 6, Stat.WIJSHEID, 6);
        // Sterrenwacht (dungeon): brass rings and a star-map cloak
        Z6Loot.stuk("sterrenwachtkap", 26, 74, Stat.WIJSHEID, 8, Stat.KRACHT, 8, Stat.KRITIEK, 3);
        Z6Loot.stuk("sterrenkaartmantel", 41, 120, Stat.VERDEDIGING, 10, Stat.ELEMENTSCHADE, 6);
        Z6Loot.stuk("sterrenwachtbroek", 35, 102, Stat.BEHENDIGHEID, 9, Stat.WIJSHEID, 9);
        Z6Loot.stuk("sterrenwachtlaarzen", 21, 66, Stat.LOOPSNELHEID, 6, Stat.COOLDOWNREDUCTIE, 3);
        Z6Loot.stuk("messinghandschoenen", 0, 0, Stat.KRITIEKSCHADE, 12, Stat.VERDEDIGING, 10);
        Z6Loot.stuk("sterrenkaartcape", 0, 0, Stat.VADS, 76, Stat.COOLDOWNREDUCTIE, 3, Stat.HEALKRACHT, 6);
        // the named items: a step above the zone-5 boss line
        BasisTypes.registreer(BasisType.wapen("z6baas_wormelientand", "guhs:vr_z6_baas_wormelientand", "knots", Klassen.RIDDER, Element.NATUUR,
                9.6, 1.9, Stats.van(Stat.KRACHT, 14, Stat.KRITIEKSCHADE, 12), Stats.van(Stat.KRACHT, 1.0), 1));
        BasisTypes.registreer(new BasisType("z6baas_duinzwemmerslaarzen", "guhs:vr_z6_baas_duinzwemmerslaarzen", Slot.VOETEN, "harnas", null, Element.NATUUR, 0, 0,
                Stats.van(Stat.VERDEDIGING, 22, Stat.VADS, 90, Stat.LOOPSNELHEID, 8), Stats.van(Stat.VERDEDIGING, 2.6, Stat.VADS, 9), 1));
        BasisTypes.registreer(BasisType.sieraad("z6baas_wimperwaaier", "guhs:vr_z6_baas_wimperwaaier", Slot.OORBEL,
                Stats.van(Stat.VADS, 120, Stat.ELEMENTWEERSTAND, 10, Stat.HEALKRACHT, 10), Stats.van(Stat.VADS, 6), 1));
        BasisTypes.registreer(BasisType.wapen("z6baas_zingend_zoutzwaard", "guhs:vr_z6_baas_zingend_zoutzwaard", "zwaard", Klassen.RIDDER, Element.WATER,
                9.4, 1.88, Stats.van(Stat.KRACHT, 13, Stat.VERDEDIGING, 12), Stats.van(Stat.KRACHT, 1.0), 1));
        BasisTypes.registreer(BasisType.wapen("z6baas_zoutsplinterboog", "guhs:vr_z6_baas_zoutsplinterboog", "boog", Klassen.JAGER, Element.WATER,
                9.4, 1.88, Stats.van(Stat.BEHENDIGHEID, 13, Stat.KRITIEK, 5), Stats.van(Stat.BEHENDIGHEID, 1.0), 1));
        BasisTypes.registreer(new BasisType("z6baas_zoutkristalkroon", "guhs:vr_z6_baas_zoutkristalkroon", Slot.HOOFD, "harnas", null, Element.WATER, 0, 0,
                Stats.van(Stat.VERDEDIGING, 20, Stat.WIJSHEID, 10, Stat.KRACHT, 10), Stats.van(Stat.VERDEDIGING, 2.4, Stat.WIJSHEID, 0.5), 1));
        BasisTypes.registreer(BasisType.sieraad("z6baas_astrolabiumring", "guhs:vr_z6_baas_astrolabiumring", Slot.RING,
                Stats.van(Stat.KRITIEK, 6, Stat.KRITIEKSCHADE, 14), Stats.van(Stat.KRITIEKSCHADE, 0.3), 1));
        BasisTypes.registreer(BasisType.wapen("z6baas_glimmertjes_dover", "guhs:vr_z6_baas_glimmertjes_dover", "staf", Klassen.MAGIER, Element.VUUR,
                9.4, 1.88, Stats.van(Stat.WIJSHEID, 14, Stat.ELEMENTSCHADE, 10), Stats.van(Stat.WIJSHEID, 1.0), 1));
        BasisTypes.registreer(BasisType.wapen("z6baas_sterrenkijkerkruisboog", "guhs:vr_z6_baas_sterrenkijkerkruisboog", "kruisboog", Klassen.JAGER, Element.ROZE,
                9.6, 1.9, Stats.van(Stat.BEHENDIGHEID, 12, Stat.KRITIEKSCHADE, 16), Stats.van(Stat.BEHENDIGHEID, 1.0), 1));
        BasisTypes.registreer(BasisType.sieraad("z6baas_sterrenhartamulet", "guhs:vr_z6_baas_sterrenhartamulet", Slot.KETTING,
                Stats.van(Stat.WIJSHEID, 12, Stat.ELEMENTSCHADE, 9, Stat.VADS, 80), Stats.van(Stat.WIJSHEID, 0.6), 1));
    }

    public static ItemStack maak(Benoemd b, int level, RandomSource r) {
        BasisType t = BasisTypes.van(b.type());
        if (t == null) {
            return ItemStack.EMPTY;
        }
        return Rollen.stack(Rollen.rol(t, level, b.zeldzaamheid(), r).metGebonden(true));
    }

    public static ItemStack trofee(String baas) {
        int i = MET_TROFEE.indexOf(baas);
        return i < 0 ? ItemStack.EMPTY : new ItemStack(TROFEEEN.get(i).get());
    }

    @Nullable
    static LootTabel tabel(String baas) {
        for (String id : List.of("economie:z6_baas_" + baas, "economie:zone6", "economie:zone5", "economie:zone4", "economie:zone3")) {
            LootTabel t = LootTabellen.van(id);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    private static boolean veldbaas(String baas) {
        return baas.equals("wormelien") || baas.equals("zoutkolos");
    }

    /** One sure set piece: the Karavaanwacht set (field bosses) or the Sterrenwacht set (the dungeon), Episch or better. */
    private static ItemStack zekerStuk(String baas, int level, RandomSource r) {
        List<String> pool = veldbaas(baas) ? Z6Loot.KARAVAANWACHT : Z6Loot.STERRENWACHT;
        BasisType t = BasisTypes.van(pool.get(r.nextInt(pool.size())));
        if (t == null) {
            return ItemStack.EMPTY;
        }
        Zeldzaamheid z = Zeldzaamheid.rol(r, 4.0);
        if (z.ordinal() < Zeldzaamheid.EPISCH.ordinal()) {
            z = Zeldzaamheid.EPISCH;
        }
        return Rollen.stack(Rollen.rol(t, level, z, r));
    }

    /** One player's chest. */
    public static LootTabel.Resultaat kist(String baas, ServerPlayer p, int level, RandomSource r) {
        List<ItemStack> items = new ArrayList<>();
        long munten = 900 + r.nextInt(600);
        LootTabel t = tabel(baas);
        if (t != null) {
            LootTabel.Resultaat basis = t.rol(level, r);
            items.addAll(basis.items());
            munten += basis.munten();
        }
        items.add(zekerStuk(baas, level, r));
        CompoundTag extra = Spelers.data(p).extra("z6wezens");
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
                    sl.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("vadsrijk.z6wezens.loot.guhddelijk", p.getDisplayName(),
                            Component.translatable("item.guhs." + b.item().getId().getPath())).withStyle(ChatFormatting.LIGHT_PURPLE), false);
                }
            }
        }
        if (eerste && !legendarisch.isEmpty()) {
            boolean alEen = items.stream().anyMatch(s -> {
                Uitrusting u = Rollen.van(s);
                return u != null && u.type().startsWith("z6baas_") && u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH;
            });
            if (!alEen) {
                items.add(maak(legendarisch.get(r.nextInt(legendarisch.size())), level, r));
            }
            p.sendSystemMessage(Component.translatable("vadsrijk.bazen.kist.eerste").withStyle(ChatFormatting.GOLD));
        }
        if (MET_TROFEE.contains(baas) && (eerste || r.nextDouble() < 0.25)) {
            items.add(trofee(baas));
        }
        // Gouden Veren: field bosses 2 on the first kill, then 1 at 25 %; the dungeon end boss 1 every time
        if (veldbaas(baas)) {
            GoudenVeren.geef(p, eerste ? 2 : r.nextDouble() < 0.25 ? 1 : 0);
        } else if (baas.equals("sterrenguh")) {
            GoudenVeren.geef(p, 1);
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
