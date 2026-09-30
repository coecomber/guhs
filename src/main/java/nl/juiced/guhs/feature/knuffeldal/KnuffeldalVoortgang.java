package nl.juiced.guhs.feature.knuffeldal;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Knus tab of the knuffeldal feature: the sections "knuffeldal" (the town, its friends, the Grote Knusfeest) and
 * "seizoenen" (the seizoensplakboek and a milestone per season). Counters and collection ids live here.
 */
public final class KnuffeldalVoortgang {
    // counters (KnusVoortgang.tel)
    public static final String STADJE = "knuffeldal.stadje", VRIENDJES = "knuffeldal.vriendjes", COCOTJE = "knuffeldal.cocotje",
            KNUSFEEST = "knuffeldal.knusfeest", TAAKJES = "knuffeldal.taakjes", KRUIMEL_MIKAS = "knuffeldal.kruimel_mikas",
            FINALE = "knuffeldal.finale", SEIZOENSFEESTEN = "knuffeldal.seizoensfeesten", PLUISGUH = "knuffeldal.pluisguh";
    public static final String SEIZOEN_ALLE = "seizoenen.alle", BLADERHOOPJES = "seizoenen.bladerhoopjes";

    /** The collection of friends in the town: the characters and the six residents (their names). */
    public static final String VRIENDJES_BOEK = "knuffelvriendjes";
    public static final List<String> BEWONERS = List.of("pluisje", "knabbeltje", "dikkie", "mollie", "sproetje", "bolletje");
    public static final List<String> VRIENDJES_LIJST = List.of("burgemeester", "cocotje", "opa_guh", "pluisje", "knabbeltje", "dikkie", "mollie",
            "sproetje", "bolletje");
    /** The seizoensplakboek: two entries per season. */
    public static final String PLAKBOEK = "seizoensplakboek";
    public static final List<String> PLAKBOEK_LIJST = List.of("lente_kransje", "lente_guh", "zomer_hoedje", "zomer_guh", "herfst_hoopje",
            "herfst_guh", "winter_sneeuwpop", "winter_guh");

    private static Supplier<ItemStack> stack(Supplier<? extends ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    static void register() {
        String kd = "knuffeldal";
        KnusVoortgang.mijlpaal(kd, "knuffeldal_gevonden", STADJE, 1, stack(ModItems.KAAS_KNABBELS, 8), "knuffeldal_stadje_bezocht");
        KnusVoortgang.mijlpaal(kd, "knuffeldal_vriendjes", VRIENDJES, 6, stack(KnuffeldalFeature.SEIZOENSBLOEMBAK, 2));
        KnusVoortgang.mijlpaal(kd, "knuffeldal_cocotje", COCOTJE, 1, () -> ItemStack.EMPTY, "knuffeldal_cocotje");
        KnusVoortgang.mijlpaal(kd, "knuffeldal_knusfeest", KNUSFEEST, 1, stack(ModItems.GEFRITUURDE_KAASKNABBELS, 4));
        KnusVoortgang.mijlpaal(kd, "knuffeldal_taakjes", TAAKJES, 6, stack(ModItems.KAAS_KNABBELS, 16));
        KnusVoortgang.mijlpaal(kd, "knuffeldal_kruimelmikas", KRUIMEL_MIKAS, 3, stack(ModItems.GUH_BALLON, 3));
        KnusVoortgang.mijlpaal(kd, "knuffeldal_finale", FINALE, 1, stack(ModItems.VAHOEGE_VADS_INGOT, 2));
        KnusVoortgang.mijlpaal(kd, "knuffeldal_seizoensfeesten", SEIZOENSFEESTEN, 4, stack(ModItems.GEFRITUURDE_KAASKNABBELS, 16));
        KnusVoortgang.mijlpaal(kd, "knuffeldal_pluisguh", PLUISGUH, 1, stack(KnuffeldalFeature.PLUIZENBOOM_ZAAILING, 3));
        KnusVoortgang.verzameling(kd, VRIENDJES_BOEK, VRIENDJES_LIJST, item -> switch (item) {
            case "burgemeester" -> new ItemStack(KnuffeldalFeature.KNUSFEESTLIJSTJE.get());
            case "cocotje" -> new ItemStack(ModItems.KAAS_KNABBELS.get());
            case "opa_guh" -> new ItemStack(Items.CAMPFIRE);
            default -> new ItemStack(nl.juiced.guhs.registry.ModItems.GUH_SPAWN_EGG.get());
        });

        String sz = "seizoenen";
        KnusVoortgang.mijlpaal(sz, "seizoenen_lente", "seizoenen.lente", 2, stack(nl.juiced.guhs.registry.ModBlocks.GUHBLOESEM_SAPLING, 2));
        KnusVoortgang.mijlpaal(sz, "seizoenen_zomer", "seizoenen.zomer", 2, stack(ModItems.KAASKNABBEL_MILKSHAKE, 1));
        KnusVoortgang.mijlpaal(sz, "seizoenen_herfst", "seizoenen.herfst", 2, stack(KnuffeldalFeature.PLUIZENBOOM_ZAAILING, 2));
        KnusVoortgang.mijlpaal(sz, "seizoenen_winter", "seizoenen.winter", 2, stack(ModItems.GEFRITUURDE_KAASKNABBELS, 8));
        KnusVoortgang.mijlpaal(sz, "seizoenen_alle", SEIZOEN_ALLE, 4, stack(KnuffeldalFeature.SEIZOENSBLOEMBAK, 4), "knuffeldal_seizoen_alle");
        KnusVoortgang.mijlpaal(sz, "seizoenen_bladerhoopjes", BLADERHOOPJES, 10, stack(KnuffeldalFeature.BLADERHOOPJE, 8));
        KnusVoortgang.verzameling(sz, PLAKBOEK, PLAKBOEK_LIJST, item -> switch (item) {
            case "lente_kransje", "lente_guh" -> new ItemStack(ModItems.clothingItem(GuhClothes.BLOESEMKRANSJE));
            case "zomer_hoedje", "zomer_guh" -> new ItemStack(ModItems.clothingItem(GuhClothes.ZONNEHOEDJE));
            case "herfst_hoopje", "herfst_guh" -> new ItemStack(KnuffeldalFeature.BLADERHOOPJE.get());
            case "winter_sneeuwpop" -> new ItemStack(KnuffeldalFeature.SNEEUWGUHKOPJE.get());
            default -> new ItemStack(ModItems.clothingItem(GuhClothes.KNUS_SJAALTJE));
        });
    }

    private KnuffeldalVoortgang() {
    }
}
