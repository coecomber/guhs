package nl.juiced.guhs.feature.theehuis;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Knus tab of the Knabbelthee-huisje (section "theehuis"): counters, milestones and the collection "theesoorten" (the
 * four teas, entry ids = item ids). Also grants the shown advancements of the Knuffeldal tab (guhs:knuffeldal/theehuis_*).
 */
public final class TheehuisVoortgang {
    public static final String ONDERDEEL = "theehuis";
    // counters
    public static final String KRANSJES = "theehuis.kransjes", ZELFGEBAKKEN = "theehuis.zelfgebakken", INGESCHONKEN = "theehuis.ingeschonken",
            GEZET = "theehuis.gezet", SOORTEN = "theehuis.soorten";
    /** The collection of teas. */
    public static final String THEESOORTEN = "theesoorten";
    public static final List<String> SOORTEN_LIJST = Arrays.stream(TheeBlocks.Soort.values()).map(TheeBlocks.Soort::id).toList();

    private static Supplier<ItemStack> stack(Supplier<? extends ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    static void register() {
        KnusVoortgang.mijlpaal(ONDERDEEL, "theehuis_eerste_kransje", KRANSJES, 1,
                () -> new ItemStack(TheehuisFeature.thee(TheeBlocks.Soort.GUHBLOEMENTHEE), 2), "theehuis_gezellig");
        KnusVoortgang.mijlpaal(ONDERDEEL, "theehuis_kransjes", KRANSJES, 5, stack(TheehuisFeature.THEEPOTJE_ITEM, 1));
        KnusVoortgang.mijlpaal(ONDERDEEL, "theehuis_zelfgebakken", ZELFGEBAKKEN, 5, stack(ModItems.GEFRITUURDE_KAASKNABBELS, 6), "theehuis_zelfgebakken");
        KnusVoortgang.mijlpaal(ONDERDEEL, "theehuis_ingeschonken", INGESCHONKEN, 30, stack(TheehuisFeature.THEETAFEL_ITEM, 1));
        KnusVoortgang.mijlpaal(ONDERDEEL, "theehuis_gezet", GEZET, 20, stack(ModItems.KAAS_KNABBELS, 16));
        KnusVoortgang.mijlpaal(ONDERDEEL, "theehuis_theesoorten", SOORTEN, SOORTEN_LIJST.size(), stack(TheehuisFeature.THEEPOTJE_ITEM, 2),
                "theehuis_theesoorten");
        KnusVoortgang.verzameling(ONDERDEEL, THEESOORTEN, SOORTEN_LIJST,
                item -> new ItemStack(TheehuisFeature.thee(TheeBlocks.Soort.valueOf(item.toUpperCase(java.util.Locale.ROOT)))));
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

    private TheehuisVoortgang() {
    }
}
