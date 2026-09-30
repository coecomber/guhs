package nl.juiced.guhs.feature.kamperen;

import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.registry.ModItems;

/** The Knus tab of the kampeerplekjes (section "kamperen"): Opa Guh's verhalenbundel (12 stories) and the milestones. */
public final class KamperenVoortgang {
    public static final String ONDERDEEL = "kamperen";

    private static Supplier<ItemStack> stack(Supplier<? extends ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    static void register() {
        String o = ONDERDEEL;
        KnusVoortgang.mijlpaal(o, "kamperen_gevonden", Verhalen.GEVONDEN, 1, stack(ModItems.KAAS_KNABBELS, 8));
        KnusVoortgang.mijlpaal(o, "kamperen_eerste_verhaal", Verhalen.GEHOORD, 1, stack(KamperenFeature.SLAAPZAK_ITEM, 1));
        KnusVoortgang.mijlpaal(o, "kamperen_zes_verhalen", Verhalen.AANTAL, 6,
                () -> new ItemStack(ModItems.clothingItem(GuhClothes.SLAAPMUTSJE)));
        KnusVoortgang.mijlpaal(o, "kamperen_alle_verhalen", Verhalen.AANTAL, Verhalen.IDS.size(),
                () -> new ItemStack(ModItems.clothingItem(GuhClothes.PYJAMA_PAKJE)), "kamperen_verhalenbundel_vol");
        KnusVoortgang.mijlpaal(o, "kamperen_uitgeslapen", Verhalen.UITGESLAPEN, 1, stack(ModItems.GEFRITUURDE_KAASKNABBELS, 3));
        KnusVoortgang.mijlpaal(o, "kamperen_marshmallows", KampvuurMarshmallow.GEROOSTERD, 5, stack(ModItems.GEFRITUURDE_KAASKNABBELS, 2));
        KnusVoortgang.mijlpaal(o, "kamperen_pyjamafeest", Verhalen.PYJAMAGUHS, 5, stack(ModItems.GUH_BALLON, 3), "kamperen_pyjamafeest");
        KnusVoortgang.verzameling(o, Verhalen.BUNDEL, Verhalen.IDS, id -> switch (id) {
            case "eerste_kaasknabbel", "kaasregen" -> new ItemStack(ModItems.KAAS_KNABBELS.get());
            case "mika_die_niet_kon_delen", "mika_met_gouden_hart" -> new ItemStack(Items.GOLD_NUGGET);
            case "grote_knabbel" -> new ItemStack(Items.NETHER_STAR);
            case "wolkjes_eerste_vlucht" -> new ItemStack(Items.FEATHER);
            case "knabbelberg" -> new ItemStack(Items.ENDER_EYE);
            case "verdwaalde_babyguh" -> new ItemStack(Items.EGG);
            case "knuffeldal" -> new ItemStack(Items.PINK_WOOL);
            default -> new ItemStack(Items.BOOK);
        });
    }

    private KamperenVoortgang() {
    }
}
