package nl.juiced.guhs.feature.guhvillagers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The trades of the four 1.7.0 professions, as a table of ids: an item or enchantment that does not exist (yet) is simply
 * left out, so the table also works while the Spreukjes enchantments (guhs:mika_mepper, guhs:pluisval, ...) live in
 * another branch. Like ModVillagers: a villager gets two random trades of its level each time vanilla hands out trades
 * (called from ModVillagers#addTrades).
 * <p>
 * Spreukjesboekjes: the Toverguh sells vanilla enchanted books; a book trade names a few enchantments (id, level) and
 * one of the ones that exist is picked when the trade is made (the level is clamped to the enchantment's maximum).
 */
public final class GuhVillagerHandel {
    /** One trade: pay {@code wants} (and {@code wants2}) and get {@code gives}; or a Spreukjesboekje ({@code boekjes}). */
    public record Ruil(String wants, int wantCount, @Nullable String wants2, int want2Count, @Nullable String gives, int giveCount,
                       List<Boekje> boekjes, int maxUses, int xp) {
    }

    /** A possible enchantment on a Spreukjesboekje: its id, level and price in emeralds (plus one book). */
    public record Boekje(String id, int level, int prijs) {
    }

    private static Ruil verkoop(String item, int count, int emeralds, int maxUses, int xp) {
        return new Ruil("minecraft:emerald", emeralds, null, 0, item, count, List.of(), maxUses, xp);
    }

    private static Ruil koop(String item, int count, int emeralds, int maxUses, int xp) {
        return new Ruil(item, count, null, 0, "minecraft:emerald", emeralds, List.of(), maxUses, xp);
    }

    /** Slijpen: {@code count} of an item plus emeralds for something else. */
    private static Ruil slijp(String van, int vanCount, int emeralds, String naar, int naarCount, int maxUses, int xp) {
        return new Ruil(van, vanCount, emeralds > 0 ? "minecraft:emerald" : null, emeralds, naar, naarCount, List.of(), maxUses, xp);
    }

    private static Ruil boekje(int xp, Boekje... keuze) {
        return new Ruil("minecraft:emerald", 0, "minecraft:book", 1, null, 1, List.of(keuze), 12, xp);
    }

    private static Boekje b(String id, int level, int prijs) {
        return new Boekje(id.contains(":") ? id : "guhs:" + id, level, prijs);
    }

    /** The trades of one of the four professions per level 1-5 (index 0 = novice), or null for any other profession. */
    @Nullable
    public static List<List<Ruil>> tabel(Holder<VillagerProfession> beroep) {
        if (is(beroep, GuhVillagersFeature.TOVERGUH)) {
            return List.of(
                    List.of(koop("minecraft:paper", 24, 1, 16, 2),
                            boekje(2, b("pluisval", 1, 8), b("vadsvlijt", 1, 8), b("mika_mepper", 1, 9), b("minecraft:efficiency", 1, 6),
                                    b("minecraft:unbreaking", 1, 6), b("minecraft:protection", 1, 6)),
                            boekje(2, b("pluisval", 1, 8), b("mika_mepper", 1, 9), b("minecraft:sharpness", 1, 6), b("minecraft:power", 1, 6))),
                    List.of(koop("minecraft:book", 4, 1, 12, 10),
                            boekje(5, b("glitterzicht", 1, 12), b("bakvads", 1, 16), b("mika_mepper", 2, 14), b("minecraft:feather_falling", 2, 10),
                                    b("minecraft:sharpness", 2, 10), b("minecraft:power", 2, 10)),
                            verkoop("minecraft:lantern", 1, 1, 12, 5)),
                    List.of(koop("minecraft:lapis_lazuli", 12, 1, 12, 20),
                            boekje(10, b("aaizacht", 1, 20), b("kaasknabbelgeur", 1, 24), b("minecraft:fortune", 2, 18), b("minecraft:looting", 2, 18),
                                    b("minecraft:respiration", 2, 12), b("minecraft:depth_strider", 2, 12), b("vadsvlijt", 2, 14)),
                            verkoop("minecraft:bookshelf", 1, 3, 12, 10)),
                    List.of(koop("minecraft:writable_book", 2, 1, 12, 30),
                            boekje(15, b("spaarguhtje", 1, 32), b("minecraft:silk_touch", 1, 24), b("minecraft:swift_sneak", 2, 24),
                                    b("minecraft:efficiency", 4, 22), b("minecraft:protection", 4, 22), b("mika_mepper", 3, 22)),
                            boekje(15, b("aaizacht", 1, 20), b("kaasknabbelgeur", 1, 24), b("glitterzicht", 1, 12), b("bakvads", 1, 16))),
                    List.of(boekje(30, b("minecraft:mending", 1, 40), b("spaarguhtje", 1, 32), b("minecraft:unbreaking", 3, 24),
                                    b("minecraft:sharpness", 5, 30)),
                            verkoop("minecraft:name_tag", 1, 20, 12, 30)));
        }
        if (is(beroep, GuhVillagersFeature.VADSMONTEUR)) {
            return List.of(
                    List.of(koop("guhs:vahoege_vads", 3, 2, 12, 2), verkoop("guhs:guh_wire", 8, 2, 16, 1), koop("minecraft:redstone", 16, 1, 16, 2)),
                    List.of(verkoop("guhs:guh_wheel", 1, 8, 8, 10), verkoop("guhs:knabbelbatterij", 1, 10, 6, 10), koop("guhs:vahoege_vads", 2, 2, 12, 10)),
                    List.of(verkoop("guhs:guh_oven", 1, 12, 6, 15), verkoop("guhs:vadsmolen", 1, 14, 6, 15), koop("guhs:vahoege_vads_ingot", 1, 4, 12, 20)),
                    List.of(verkoop("guhs:oogster", 1, 18, 4, 20), verkoop("guhs:knutselmachine", 1, 20, 4, 20), verkoop("guhs:guh_wire", 24, 5, 8, 15)),
                    List.of(verkoop("guhs:neerzetter", 1, 24, 3, 30), verkoop("guhs:knabbelaar", 1, 24, 3, 30), verkoop("guhs:tekentafel", 1, 16, 3, 30)));
        }
        if (is(beroep, GuhVillagersFeature.MIJNWERKER)) {
            return List.of(
                    List.of(koop("minecraft:coal", 15, 1, 16, 2), verkoop("guhs:viltsteen", 16, 1, 16, 1), verkoop("guhs:knuffelsteen", 16, 1, 16, 1)),
                    List.of(koop("minecraft:raw_iron", 6, 1, 16, 10), koop("minecraft:raw_copper", 12, 1, 16, 10), verkoop("guhs:guhklei", 8, 1, 16, 5),
                            verkoop("guhs:roze_knuffelsteen", 12, 1, 16, 5)),
                    List.of(koop("minecraft:raw_gold", 4, 1, 12, 20), verkoop("guhs:guh_kristalsteen", 8, 1, 16, 10),
                            verkoop("minecraft:pink_dye", 8, 1, 16, 10), verkoop("guhs:park_lantaarn", 2, 2, 12, 10)),
                    List.of(koop("guhs:vahoegiet", 1, 6, 12, 30), koop("minecraft:lapis_lazuli", 8, 1, 16, 20), verkoop("guhs:guh_kristal_lamp", 2, 4, 12, 15),
                            verkoop("guhs:viltkiezel", 16, 1, 16, 10)),
                    List.of(koop("guhs:vahoegiet", 2, 13, 12, 30), verkoop("guhs:gebeitelde_viltsteen", 8, 2, 12, 30),
                            verkoop("guhs:lichtmosblok", 4, 3, 12, 30)));
        }
        if (is(beroep, GuhVillagersFeature.KRISTALSLIJPER)) {
            return List.of(
                    List.of(koop("guhs:guh_kristal", 6, 1, 16, 2), slijp("guhs:guh_kristal", 3, 1, "guhs:guh_kristal_cluster", 2, 16, 2)),
                    List.of(slijp("guhs:guh_kristal", 2, 1, "guhs:guh_kristal_lamp", 1, 12, 10), slijp("guhs:guh_kristal", 2, 1, "guhs:kristalpegelblok", 4, 12, 10),
                            verkoop("guhs:guh_kristal_blok", 1, 3, 12, 5)),
                    List.of(slijp("guhs:vahoegiet", 1, 2, "guhs:belletjeskristal", 4, 12, 20), verkoop("guhs:wolkenweide_kristal", 4, 3, 12, 15),
                            slijp("guhs:guh_kristal", 4, 0, "guhs:kristalpegel", 4, 12, 15)),
                    List.of(slijp("guhs:vahoegiet", 1, 1, "guhs:guh_kristal_lamp", 4, 12, 20), slijp("guhs:vahoegiet", 1, 2, "guhs:wolkenweide_kristalpunt", 4, 12, 20),
                            koop("guhs:vahoegiet", 1, 7, 12, 25)),
                    List.of(slijp("guhs:vahoegiet", 3, 8, "guhs:guh_kristal_verrekijker", 1, 3, 30), koop("guhs:vahoegiet", 1, 8, 12, 30)));
        }
        return null;
    }

    private static boolean is(Holder<VillagerProfession> beroep, DeferredHolder<VillagerProfession, VillagerProfession> b) {
        return beroep.is(b.getKey());
    }

    /**
     * Called by ModVillagers#addTrades: a villager of one of the four professions gets two random trades of its level.
     * Returns false for any other profession.
     */
    public static boolean addTrades(Villager villager) {
        VillagerData data = villager.getVillagerData();
        List<List<Ruil>> tabel = tabel(data.profession());
        if (tabel == null) {
            return false;
        }
        int level = Math.max(1, Math.min(tabel.size(), data.level()));
        List<Ruil> over = new ArrayList<>(tabel.get(level - 1));
        int added = 0;
        while (added < 2 && !over.isEmpty()) {
            MerchantOffer offer = offer(over.remove(villager.getRandom().nextInt(over.size())), villager.registryAccess(), villager.getRandom());
            if (offer != null) {
                villager.getOffers().add(offer);
                added++;
            }
        }
        return true;
    }

    /** The offer of a trade, or null when an item (or every enchantment of a Spreukjesboekje) doesn't exist. */
    @Nullable
    public static MerchantOffer offer(Ruil r, HolderLookup.Provider registries, RandomSource random) {
        if (!r.boekjes().isEmpty()) {
            List<Boekje> bestaand = new ArrayList<>();
            List<Holder<Enchantment>> holders = new ArrayList<>();
            HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
            for (Boekje b : r.boekjes()) {
                Identifier id = Identifier.tryParse(b.id());
                Optional<? extends Holder<Enchantment>> h = id == null ? Optional.empty() : lookup.get(ResourceKey.create(Registries.ENCHANTMENT, id));
                if (h.isPresent()) {
                    bestaand.add(b);
                    holders.add(h.get());
                }
            }
            if (bestaand.isEmpty()) {
                return null;
            }
            int i = random.nextInt(bestaand.size());
            Holder<Enchantment> ench = holders.get(i);
            int level = Math.max(1, Math.min(bestaand.get(i).level(), ench.value().getMaxLevel()));
            ItemStack boek = EnchantmentHelper.createBook(new EnchantmentInstance(ench, level));
            int prijs = Math.min(64, bestaand.get(i).prijs() + random.nextInt(5));
            return new MerchantOffer(new ItemCost(Items.EMERALD, prijs), Optional.of(new ItemCost(Items.BOOK, 1)), boek, r.maxUses(), r.xp(), 0.2f);
        }
        Item wants = item(r.wants());
        Item gives = r.gives() == null ? null : item(r.gives());
        if (wants == null || gives == null) {
            return null;
        }
        Optional<ItemCost> second = Optional.empty();
        if (r.wants2() != null && r.want2Count() > 0) {
            Item w2 = item(r.wants2());
            if (w2 == null) {
                return null;
            }
            second = Optional.of(new ItemCost(w2, r.want2Count()));
        }
        return new MerchantOffer(new ItemCost(wants, r.wantCount()), second, new ItemStack(gives, r.giveCount()), r.maxUses(), r.xp(), 0.05f);
    }

    /** The item with this id, or null when there is none (an id of another branch, a typo...). */
    @Nullable
    public static Item item(String id) {
        Identifier key = Identifier.tryParse(id);
        if (key == null) {
            return null;
        }
        Optional<Holder.Reference<Item>> item = BuiltInRegistries.ITEM.get(key);
        return item.map(Holder::value).filter(i -> i != Items.AIR).orElse(null);
    }

    private GuhVillagerHandel() {
    }
}
