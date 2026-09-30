package nl.juiced.guhs.feature.creche;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Knus tab of the Knuffelcreche (section "creche"): counters, milestones and the collection "slaapliedjes" (the four
 * songs of {@link Slaapliedje}). Also grants the shown advancements of the Knuffeldal tab (guhs:knuffeldal/creche_*).
 */
public final class CrecheVoortgang {
    public static final String ONDERDEEL = "creche";
    // counters
    public static final String RONDES = "creche.verzorgrondes", INGESTOPT = "creche.ingestopt", TERUG = "creche.terug",
            RECORD = "creche.record", LIEDJES = "creche.liedjes", SPELLETJES = "creche.spelletjes";
    /** The collection of lullabies. */
    public static final String SLAAPLIEDJES = "slaapliedjes";
    public static final List<String> LIEDJES_LIJST = Arrays.stream(Slaapliedje.values()).map(Slaapliedje::id).toList();

    private static Supplier<ItemStack> stack(Supplier<? extends ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    static void register() {
        KnusVoortgang.mijlpaal(ONDERDEEL, "creche_eerste_ronde", RONDES, 1, stack(CrecheFeature.SPEENMUNT, 3), "creche_ingestopt");
        KnusVoortgang.mijlpaal(ONDERDEEL, "creche_ingestopt", INGESTOPT, 15, stack(CrecheFeature.GUH_WIEGJE_ITEM, 1));
        KnusVoortgang.mijlpaal(ONDERDEEL, "creche_spelletje", SPELLETJES, 1, stack(CrecheFeature.SPEENMUNT, 2), "creche_terugbrengen");
        KnusVoortgang.mijlpaal(ONDERDEEL, "creche_terug", TERUG, 40, stack(CrecheFeature.SPEENMUNT, 6));
        KnusVoortgang.mijlpaal(ONDERDEEL, "creche_record", RECORD, 200, () -> new ItemStack(ModItems.clothingItem(GuhClothes.ROMPERTJE)));
        KnusVoortgang.mijlpaal(ONDERDEEL, "creche_liedjes", LIEDJES, LIEDJES_LIJST.size(),
                () -> new ItemStack(ModItems.clothingItem(GuhClothes.SPEENKETTINKJE)), "creche_slaapliedjes");
        KnusVoortgang.verzameling(ONDERDEEL, SLAAPLIEDJES, LIEDJES_LIJST, item -> new ItemStack(switch (item) {
            case "sterretjes" -> Items.GLOWSTONE_DUST;
            case "maantje" -> Items.CLOCK;
            case "knabbeltje" -> ModItems.KAAS_KNABBELS.get();
            default -> Items.WHITE_WOOL;
        }));
    }

    /** Grants a shown advancement of the Knuffeldal tab (guhs:knuffeldal/&lt;name&gt;). */
    static void toon(ServerPlayer player, String name) {
        AdvancementHolder holder = player.server.getAdvancements().get(Guhs.id("knuffeldal/" + name));
        if (holder == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    private CrecheVoortgang() {
    }
}
