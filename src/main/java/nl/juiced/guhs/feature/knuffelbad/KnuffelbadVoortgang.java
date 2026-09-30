package nl.juiced.guhs.feature.knuffelbad;

import java.util.function.Supplier;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Knuffelbad on the Knus tab of the Guhdex: milestones (found it, washes, each slide, ducks, a record) and the
 * badeendjes collection (the twelve special ducks). Also grants the shown advancements of the Knuffeldal tab.
 */
public final class KnuffelbadVoortgang {
    public static final String ONDERDEEL = "knuffelbad";
    // counters
    public static final String BEZOCHT = "knuffelbad.bezocht", WASSEN = "knuffelbad.wassen", EENDJES = "knuffelbad.eendjes", BESTE = "knuffelbad.beste";
    /** The collection of special ducks. */
    public static final String BADEENDJES = "badeendjes";

    public static String teller(Glijbaan g) {
        return "knuffelbad." + g.id();
    }

    private static Supplier<ItemStack> stack(Supplier<? extends ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    static void register() {
        String o = ONDERDEEL;
        KnusVoortgang.mijlpaal(o, "knuffelbad_gevonden", BEZOCHT, 1, stack(ModItems.KAAS_KNABBELS, 8), "knuffelbad_bezocht");
        KnusVoortgang.mijlpaal(o, "knuffelbad_eerste_was", WASSEN, 1, stack(KnuffelbadFeature.GUHSHAMPOO, 1));
        KnusVoortgang.mijlpaal(o, "knuffelbad_wassen", WASSEN, 10, () -> new ItemStack(ModItems.clothingItem(GuhClothes.BADMUTSJE)));
        KnusVoortgang.mijlpaal(o, "knuffelbad_roze_trechter", teller(Glijbaan.ROZE_TRECHTER), 1, stack(KnuffelbadFeature.EENDJESMUNT, 3));
        KnusVoortgang.mijlpaal(o, "knuffelbad_glimtunnel", teller(Glijbaan.GLIMTUNNEL), 1, stack(KnuffelbadFeature.EENDJESMUNT, 3));
        KnusVoortgang.mijlpaal(o, "knuffelbad_grote_plons", teller(Glijbaan.GROTE_PLONS), 1, stack(KnuffelbadFeature.EENDJESMUNT, 3));
        KnusVoortgang.mijlpaal(o, "knuffelbad_eendjes", EENDJES, 100, stack(KnuffelbadFeature.EENDJESMUNT, 10), "knuffelbad_eendjes_100");
        KnusVoortgang.mijlpaal(o, "knuffelbad_record", BESTE, 250, () -> new ItemStack(ModItems.clothingItem(GuhClothes.BADJASJE)), "knuffelbad_record");
        KnusVoortgang.verzameling(o, BADEENDJES, Eendsoort.SPECIAAL.stream().map(Eendsoort::id).toList(),
                id -> new ItemStack(KnuffelbadFeature.BADEENDJE_ITEM.get()));
    }

    /**
     * The Knus milestone "Het Knuffelbad gevonden" (and quest/knuffelbad_bezocht): on arrival in the structure, on
     * the first ride down a slide, or when you talk to Badmeester Bubbel. Idempotent.
     */
    public static void gevonden(ServerPlayer player) {
        if (KnusVoortgang.teller(player, BEZOCHT) < 1) {
            KnusVoortgang.hoogste(player, BEZOCHT, 1);
        }
    }

    /** Grants a shown advancement of the Knuffeldal tab (guhs:knuffeldal/&lt;name&gt;) that the game can't detect itself. */
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

    private KnuffelbadVoortgang() {
    }
}
