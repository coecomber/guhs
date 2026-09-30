package nl.juiced.guhs.feature.tuintjes;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * The tuintjes on the Knus tab (section "tuintjes"): the counters, the milestones, and the tuinboek (the three plants,
 * each with its seeds and its harvest; an entry id is the item id).
 */
public final class TuintjesVoortgang {
    public static final String GEPLANT = "tuintjes.geplant", OOGST = "tuintjes.oogst", GEGOTEN = "tuintjes.gegoten",
            GUHS_GIETEN = "tuintjes.guhs_gieten", ZANG = "tuintjes.zang", FEESTBOEKET = "tuintjes.feestboeket";
    public static final String TUINBOEK = "tuinboek";
    public static final List<String> TUINBOEK_LIJST = List.of("knabbelzaadjes", "knabbelgraan", "theekruidzaadjes", "theekruid",
            "guhbloemzaadjes", "guhbloemetje");

    private static Supplier<ItemStack> stack(Supplier<? extends ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    static void register() {
        String t = "tuintjes";
        KnusVoortgang.mijlpaal(t, "tuintjes_eerste_oogst", OOGST, 1, stack(TuintjesFeature.GUH_BLOEMPOT, 2), "tuintjes_eerste_oogst");
        KnusVoortgang.mijlpaal(t, "tuintjes_gieter", GEGOTEN, 20, () -> new ItemStack(ModItems.clothingItem(GuhClothes.TUINHOEDJE)));
        KnusVoortgang.mijlpaal(t, "tuintjes_guhs", GUHS_GIETEN, 10, stack(TuintjesFeature.GUH_MOESTUINBAK, 2), "tuintjes_guh_goot");
        KnusVoortgang.mijlpaal(t, "tuintjes_zang", ZANG, 5, stack(TuintjesFeature.GUHBLOEMZAADJES, 6), "tuintjes_zang");
        KnusVoortgang.mijlpaal(t, "tuintjes_oogst", OOGST, 30, () -> new ItemStack(ModItems.clothingItem(GuhClothes.TUINSCHORTJE)));
        KnusVoortgang.mijlpaal(t, "tuintjes_feestboeket", FEESTBOEKET, 1, stack(ModItems.KAAS_KNABBELS, 12), "tuintjes_feestboeket");
        KnusVoortgang.verzameling(t, TUINBOEK, TUINBOEK_LIJST,
                item -> new ItemStack(BuiltInRegistries.ITEM.getValue(Guhs.id(item))));
    }

    static void geplant(ServerPlayer player, TuinPlant plant) {
        KnusVoortgang.tel(player, GEPLANT, 1);
        ontdek(player, BuiltInRegistries.ITEM.getKey(plant.zaadje()).getPath());
    }

    static void geoogst(ServerPlayer player, TuinPlant plant, int n) {
        KnusVoortgang.tel(player, OOGST, 1);
        ontdek(player, BuiltInRegistries.ITEM.getKey(plant.oogst()).getPath());
        toon(player, "tuintjes_eerste_oogst");
    }

    static void gegoten(ServerPlayer player, int n) {
        KnusVoortgang.tel(player, GEGOTEN, n);
    }

    static void guhGoot(ServerPlayer owner) {
        KnusVoortgang.tel(owner, GUHS_GIETEN, 1);
    }

    static void zang(ServerPlayer player, int n) {
        KnusVoortgang.tel(player, ZANG, n);
    }

    private static void ontdek(ServerPlayer player, String item) {
        KnusVoortgang.ontdek(player, TUINBOEK, item);
        if (KnusVoortgang.ontdekt(player, TUINBOEK).containsAll(TUINBOEK_LIJST)) {
            GuhAdvancements.grant(player, "tuintjes_tuinboek_vol");
            toon(player, "tuintjes_tuinboek_vol");
        }
    }

    /** Grants a shown advancement of the Knuffeldal tab (guhs:knuffeldal/&lt;name&gt;). */
    static void toon(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id("knuffeldal/" + name));
        if (holder == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    private TuintjesVoortgang() {
    }
}
