package nl.juiced.guhs.feature.vadsrijk.zone5.droom;

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

/**
 * The Grauwe Droom's chest (T-DROOM, lv 60): the zone-5 boss roll {@code z5wezens:baas} (Wolkwacht pieces, jewellery, weapons; from
 * t-z5wezens) + three named Legendaries (one per class) + one Guhddelijk (Moeder Wolkwals Slaapmuts, 3 %) + the trophy "Een Fijne
 * Droom (in een potje)". The first cheer-up per player guarantees a named Legendary + the trophy. Everything gebonden but the
 * trophy. The pet (Droomwalvisje) comes from kern's G-PETS on the first kill.
 */
public final class DroomLoot {
    private DroomLoot() {
    }

    public record Benoemd(String type, DeferredItem<Item> item, Zeldzaamheid zeldzaamheid, double kans) {
    }

    private static DeferredItem<Item> wapen(String id, String type) {
        return Z5DroomRegistry.ITEMS.registerItem(id, p -> new BazenLoot.BaasItem(p, type), () -> new Item.Properties().stacksTo(1)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY));
    }

    public static final DeferredItem<Item> WIEGELIEDSTAF = wapen("vr_z5_droom_wiegeliedstaf", "z5droom_wiegeliedstaf");
    public static final DeferredItem<Item> DROMENZWAARD = wapen("vr_z5_droom_dromenzwaard", "z5droom_dromenzwaard");
    public static final DeferredItem<Item> STERRENBOOG = wapen("vr_z5_droom_sterrenboog", "z5droom_sterrenboog");
    public static final DeferredItem<Item> SLAAPMUTS = Z5DroomRegistry.ITEMS.registerItem("vr_z5_droom_slaapmuts",
            p -> new BazenLoot.BaasItem(p, "z5droom_slaapmuts"), () -> KernRegistry.harnasEigenschappen(Slot.HOOFD, null));
    public static final DeferredItem<BazenLoot.Trofee> TROFEE = Z5DroomRegistry.ITEMS.registerItem("vr_z5_droom_trofee", BazenLoot.Trofee::new,
            () -> new Item.Properties().stacksTo(16));

    public static final List<Benoemd> BENOEMD = List.of(
            new Benoemd("z5droom_wiegeliedstaf", WIEGELIEDSTAF, Zeldzaamheid.LEGENDARISCH, 0.12),
            new Benoemd("z5droom_dromenzwaard", DROMENZWAARD, Zeldzaamheid.LEGENDARISCH, 0.12),
            new Benoemd("z5droom_sterrenboog", STERRENBOOG, Zeldzaamheid.LEGENDARISCH, 0.12),
            new Benoemd("z5droom_slaapmuts", SLAAPMUTS, Zeldzaamheid.GUHDDELIJK, 0.03));

    /** Base types (a step above the zone-5 boss weapons: 9.0 + 1.8/lv). Clear stat names (user rule). */
    public static void typen() {
        BasisTypes.registreer(BasisType.wapen("z5droom_wiegeliedstaf", "guhs:vr_z5_droom_wiegeliedstaf", "staf", Klassen.MAGIER, Element.ROZE,
                9.0, 1.8, Stats.van(Stat.WIJSHEID, 14, Stat.HEALKRACHT, 16), Stats.van(Stat.WIJSHEID, 1.0), 1));
        BasisTypes.registreer(BasisType.wapen("z5droom_dromenzwaard", "guhs:vr_z5_droom_dromenzwaard", "zwaard", Klassen.RIDDER, Element.ROZE,
                9.0, 1.8, Stats.van(Stat.KRACHT, 14, Stat.VADS, 120), Stats.van(Stat.KRACHT, 1.0), 1));
        BasisTypes.registreer(BasisType.wapen("z5droom_sterrenboog", "guhs:vr_z5_droom_sterrenboog", "boog", Klassen.JAGER, Element.WATER,
                9.0, 1.8, Stats.van(Stat.BEHENDIGHEID, 14, Stat.KRITIEK, 8), Stats.van(Stat.BEHENDIGHEID, 1.0), 1));
        BasisTypes.registreer(new BasisType("z5droom_slaapmuts", "guhs:vr_z5_droom_slaapmuts", Slot.HOOFD, "harnas", null, Element.ROZE, 0, 0,
                Stats.van(Stat.VERDEDIGING, 24, Stat.VADS, 160, Stat.HEALKRACHT, 14, Stat.LOOPSNELHEID, 5), Stats.van(Stat.VERDEDIGING, 3.0, Stat.VADS, 14), 1));
    }

    public static ItemStack maak(Benoemd b, int level, RandomSource r) {
        BasisType t = BasisTypes.van(b.type());
        if (t == null) {
            return ItemStack.EMPTY;
        }
        return Rollen.stack(Rollen.rol(t, level, b.zeldzaamheid(), r).metGebonden(true));
    }

    /** One player's chest. */
    public static LootTabel.Resultaat kist(ServerPlayer p, int level, RandomSource r) {
        List<ItemStack> items = new ArrayList<>();
        long munten = 0;
        LootTabel t = LootTabellen.van("z5wezens:baas");
        if (t != null) {
            LootTabel.Resultaat basis = t.rol(level, r);
            items.addAll(basis.items());
            munten = basis.munten();
        }
        CompoundTag extra = Spelers.data(p).extra("z5droom");
        boolean eerste = !extra.getBooleanOr("eerste_kist", false);
        List<Benoemd> legendarisch = new ArrayList<>();
        for (Benoemd b : BENOEMD) {
            if (b.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH) {
                legendarisch.add(b);
            }
            if (r.nextDouble() < b.kans()) {
                items.add(maak(b, level, r));
                if (b.zeldzaamheid() == Zeldzaamheid.GUHDDELIJK && p.level() instanceof ServerLevel sl) {
                    sl.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("vadsrijk.z5droom.loot.guhddelijk", p.getDisplayName(),
                            Component.translatable("item.guhs." + b.item().getId().getPath())).withStyle(ChatFormatting.LIGHT_PURPLE), false);
                }
            }
        }
        if (eerste) {
            boolean alEen = items.stream().anyMatch(s -> {
                Uitrusting u = Rollen.van(s);
                return u != null && u.type().startsWith("z5droom_") && u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH;
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
            uit.add(new ItemStack(TROFEE.get()));
        }
        if (eerste) {
            extra.putBoolean("eerste_kist", true);
            Spelers.opgeslagen(p);
        }
        return new LootTabel.Resultaat(uit, munten);
    }
}
