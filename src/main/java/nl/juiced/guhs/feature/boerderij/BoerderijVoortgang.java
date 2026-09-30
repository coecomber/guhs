package nl.juiced.guhs.feature.boerderij;

import java.util.function.Supplier;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * The farm on the Knus tab (section "boerderij"): counters, milestones, and the advancements of the Knuffeldal tab
 * (guhs:knuffeldal/boerderij_*). Per-player farm data lives in {@link GuhQuests#saved} compound {@value #KEY}.
 */
public final class BoerderijVoortgang {
    public static final String KEY = "guhs_boerderij";
    // counters (KnusVoortgang.tel)
    public static final String AAIEN = "boerderij.aaien", BORSTELEN = "boerderij.borstelen", VOEREN = "boerderij.voeren",
            BLIJ = "boerderij.blij", PRODUCTEN = "boerderij.producten", SOORTEN = "boerderij.productsoorten", KLUSJES = "boerderij.klusjes";

    /** The three products, as bits (which ones a player ever got). */
    public enum Product {
        PLUISWOL, KNABBELEI, KAASMELK
    }

    private static Supplier<ItemStack> stack(Supplier<? extends ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    static void register() {
        String b = "boerderij";
        KnusVoortgang.mijlpaal(b, "boerderij_aaien", AAIEN, 10, stack(BoerderijFeature.KNABBELVOER, 8));
        KnusVoortgang.mijlpaal(b, "boerderij_borstelen", BORSTELEN, 10, stack(BoerderijFeature.PLUISWOLBLOK, 4));
        KnusVoortgang.mijlpaal(b, "boerderij_voeren", VOEREN, 10, stack(BoerderijFeature.GUH_VOERBAK, 1));
        KnusVoortgang.mijlpaal(b, "boerderij_blij", BLIJ, 5, stack(ModItems.KAAS_KNABBELS, 16), "boerderij_verzorgd");
        KnusVoortgang.mijlpaal(b, "boerderij_producten", PRODUCTEN, 25, stack(BoerderijFeature.KIPPENNESTJE, 2));
        KnusVoortgang.mijlpaal(b, "boerderij_alle_producten", SOORTEN, 3, () -> new ItemStack(ModItems.clothingItem(GuhClothes.BOERDERIJ_ZAKDOEK)),
                "boerderij_alle_producten");
        KnusVoortgang.mijlpaal(b, "boerderij_klusjes", KLUSJES, 7, () -> new ItemStack(ModItems.clothingItem(GuhClothes.BOERDERIJ_HOEDJE)),
                "boerderij_klusjes");
    }

    /** The player's own farm data (a live compound in their saved data). */
    public static CompoundTag data(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        if (!saved.contains(KEY, Tag.TAG_COMPOUND)) {
            saved.put(KEY, new CompoundTag());
        }
        return saved.getCompound(KEY);
    }

    /** A product came to this player (counts, the "all three" milestone and advancement). */
    public static void product(ServerPlayer player, Product product, int count) {
        KnusVoortgang.tel(player, PRODUCTEN, count);
        CompoundTag d = data(player);
        int bits = d.getInt("Producten") | (1 << product.ordinal());
        d.putInt("Producten", bits);
        KnusVoortgang.hoogste(player, SOORTEN, Integer.bitCount(bits));
        if (Integer.bitCount(bits) >= Product.values().length) {
            toon(player, "boerderij_alle_producten");
        }
    }

    /** An animal became content by this player's care. */
    public static void blij(ServerPlayer player) {
        KnusVoortgang.tel(player, BLIJ, 1);
        GuhAdvancements.grant(player, "boerderij_verzorgd");
        toon(player, "boerderij_verzorgd");
    }

    /** Grants a shown advancement of the Knuffeldal tab (guhs:knuffeldal/&lt;name&gt;). */
    public static void toon(ServerPlayer player, String name) {
        AdvancementHolder holder = player.server.getAdvancements().get(Guhs.id("knuffeldal/" + name));
        if (holder == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    private BoerderijVoortgang() {
    }
}
