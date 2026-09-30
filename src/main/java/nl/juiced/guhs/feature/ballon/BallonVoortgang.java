package nl.juiced.guhs.feature.ballon;

import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.registry.ModItems;

/** The Knus tab of the Ballonfestival (section "ballon"): the ballonstempelkaart (8 viewpoints) and the milestones. */
public final class BallonVoortgang {
    public static final String ONDERDEEL = "ballon";

    private static Supplier<ItemStack> stack(Supplier<? extends ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    static void register() {
        String o = ONDERDEEL;
        KnusVoortgang.mijlpaal(o, "ballon_gevonden", BallonVlucht.GEVONDEN, 1, stack(ModItems.KAAS_KNABBELS, 8));
        KnusVoortgang.mijlpaal(o, "ballon_eerste_vlucht", BallonVlucht.VLUCHTEN, 1, stack(BallonFeature.MINI_LUCHTBALLON_ITEM, 1));
        KnusVoortgang.mijlpaal(o, "ballon_vier_vluchten", BallonVlucht.VLUCHTEN, 4, stack(ModItems.GUH_BALLON, 4));
        KnusVoortgang.mijlpaal(o, "ballon_alle_stempels", BallonVlucht.AANTAL_STEMPELS, BallonRoute.Uitzicht.values().length,
                stack(ModItems.VAHOEGE_VADS_INGOT, 2), "ballon_stempelkaart_vol");
        KnusVoortgang.mijlpaal(o, "ballon_tien_vluchten", BallonVlucht.VLUCHTEN, 10, stack(BallonFeature.BALLONMUNT, 6));
        KnusVoortgang.verzameling(o, BallonVlucht.STEMPELS, BallonRoute.Uitzicht.ids(), id -> switch (id) {
            case "guhgezichtveld" -> new ItemStack(Items.PINK_TULIP);
            case "wolkenpoort" -> new ItemStack(Items.WHITE_WOOL);
            case "hoogste_puntje" -> new ItemStack(Items.FEATHER);
            case "regenboogbocht" -> new ItemStack(Items.PRISMARINE_CRYSTALS);
            case "reuzenguh" -> new ItemStack(BallonFeature.MINI_LUCHTBALLON_ITEM.get());
            case "hartjeswolkje" -> new ItemStack(Items.PINK_DYE);
            case "brandersprong" -> new ItemStack(Items.BLAZE_POWDER);
            default -> new ItemStack(Items.SPYGLASS);
        });
    }

    private BallonVoortgang() {
    }
}
