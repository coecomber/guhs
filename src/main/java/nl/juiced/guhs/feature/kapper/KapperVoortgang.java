package nl.juiced.guhs.feature.kapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Knus tab of the kapper: section "kapper", the collection "kapsels" (the 8 hairstyles and the 8 dyes: entry id =
 * item id) and its milestones; plus the advancements (hidden quest ones for FTB, and the shown ones in the Knuffeldal
 * tab: kapper_eerste_kapsel, kapper_show, kapper_alle_kapsels).
 */
public final class KapperVoortgang {
    public static final String ONDERDEEL = "kapper", KAPSELS = "kapsels";
    // counters
    public static final String KLANTEN = "kapper.klanten", PERFECT = "kapper.perfect", RECORD = "kapper.record", SHOWS = "kapper.shows",
            EIGEN = "kapper.eigen", COLLECTIE = "kapper.collectie";
    /** A show with at least this many points is a real kappersshow (advancement kapper_show). */
    public static final int SHOW_DOEL = 120;
    public static final List<String> KAPSEL_LIJST = lijst();

    private static List<String> lijst() {
        List<String> all = new ArrayList<>();
        for (Kapsel k : Kapsel.values()) {
            all.add(k.id());
        }
        for (Haarverf v : Haarverf.values()) {
            all.add(v.id());
        }
        return List.copyOf(all);
    }

    private static Supplier<ItemStack> stack(Supplier<? extends ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    static void register() {
        String o = ONDERDEEL;
        KnusVoortgang.mijlpaal(o, "kapper_eerste_klant", KLANTEN, 1, stack(KapperFeature.KRULMUNT, 2), "kapper_eerste_klant");
        KnusVoortgang.mijlpaal(o, "kapper_klanten", KLANTEN, 30, stack(KapperFeature.HAARVERF.get(Haarverf.REGENBOOG), 1), "kapper_klanten");
        KnusVoortgang.mijlpaal(o, "kapper_perfect", PERFECT, 10, stack(KapperFeature.KRULMUNT, 5));
        KnusVoortgang.mijlpaal(o, "kapper_show", RECORD, SHOW_DOEL, stack(KapperFeature.KRULMUNT, 6), "kapper_show");
        KnusVoortgang.mijlpaal(o, "kapper_eigen_guh", EIGEN, 1, stack(KapperFeature.HAARVERF.get(Haarverf.ROZE), 1), "kapper_eigen_guh");
        KnusVoortgang.mijlpaal(o, "kapper_collectie", COLLECTIE, KAPSEL_LIJST.size(), stack(ModItems.GEFRITUURDE_KAASKNABBELS, 8), "kapper_alle_kapsels");
        KnusVoortgang.verzameling(o, KAPSELS, KAPSEL_LIJST, KapperVoortgang::icoon);
    }

    static ItemStack icoon(String entry) {
        Kapsel k = Kapsel.byId(entry);
        if (k != null) {
            return new ItemStack(ModItems.clothingItem(k.kleding));
        }
        Haarverf v = Haarverf.byId(entry);
        return v == null ? new ItemStack(KapperFeature.KRULMUNT.get()) : new ItemStack(KapperFeature.HAARVERF.get(v).get());
    }

    /** A hairstyle and/or dye for the collection; also counts it and grants the collection advancement when complete. */
    public static void ontdek(ServerPlayer player, @Nullable Kapsel kapsel, @Nullable Haarverf verf) {
        if (kapsel != null) {
            KnusVoortgang.ontdek(player, KAPSELS, kapsel.id());
        }
        if (verf != null) {
            KnusVoortgang.ontdek(player, KAPSELS, verf.id());
        }
        int n = KnusVoortgang.ontdekt(player, KAPSELS).size();
        KnusVoortgang.hoogste(player, COLLECTIE, n);
        if (n >= KAPSEL_LIJST.size()) {
            toon(player, "kapper_alle_kapsels");
        }
    }

    /** Your own tamed guh got a hairstyle or a dye. */
    static void eigenGuh(ServerPlayer player, @Nullable Kapsel kapsel, @Nullable Haarverf verf) {
        KnusVoortgang.tel(player, EIGEN, 1);
        GuhAdvancements.grant(player, "kapper_eigen_guh");
        if (verf != null) {
            GuhAdvancements.grant(player, "kapper_haarverf");
        }
        toon(player, "kapper_eerste_kapsel");
        ontdek(player, kapsel, verf);
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

    private KapperVoortgang() {
    }
}
