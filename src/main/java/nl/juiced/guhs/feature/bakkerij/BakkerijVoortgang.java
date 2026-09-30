package nl.juiced.guhs.feature.bakkerij;

import java.util.List;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.registry.ModItems;

/**
 * The bakery on the Knus tab of the Guhdex (section "bakkerij"): milestones for orders, combos, the highscore, your own
 * baking, perfect bakes and the feesttaart, and the receptenboek (the twelve recipes; the text of an entry tells its
 * dough, shape and topping). Also the shown advancements of the Knuffeldal tab (guhs:knuffeldal/bakkerij_*).
 */
public final class BakkerijVoortgang {
    public static final String SECTIE = "bakkerij";
    // counters (KnusVoortgang.tel / hoogste)
    public static final String BESTELLINGEN = "bakkerij.bestellingen", COMBO = "bakkerij.combo", HIGHSCORE = "bakkerij.highscore",
            ZELF_GEBAKKEN = "bakkerij.zelfgebakken", RECEPTEN = "bakkerij.recepten", PERFECT = "bakkerij.perfect", FEESTTAART = "bakkerij.feesttaart";
    public static final String RECEPTENBOEK = "receptenboek";

    static void register() {
        KnusVoortgang.mijlpaal(SECTIE, "bakkerij_eerste", BESTELLINGEN, 1, BakkerijFeature.stack(ModItems.KAAS_KNABBELS, 8));
        KnusVoortgang.mijlpaal(SECTIE, "bakkerij_bestellingen", BESTELLINGEN, 50, BakkerijFeature.stack(BakkerijFeature.BAKMUNT, 10));
        KnusVoortgang.mijlpaal(SECTIE, "bakkerij_combo", COMBO, 9, BakkerijFeature.stack(() -> BakkerijFeature.bakje(Recept.VADSDONUT), 6));
        KnusVoortgang.mijlpaal(SECTIE, "bakkerij_highscore", HIGHSCORE, 250, BakkerijFeature.stack(ModItems.GUH_TAART, 1), "bakkerij_highscore");
        KnusVoortgang.mijlpaal(SECTIE, "bakkerij_perfect", PERFECT, 25, BakkerijFeature.stack(() -> BakkerijFeature.bakje(Recept.STERRENKOEKJE), 8));
        KnusVoortgang.mijlpaal(SECTIE, "bakkerij_zelfgebakken", ZELF_GEBAKKEN, 30, BakkerijFeature.stack(BakkerijFeature.SCHOORSTEEN_ITEM, 2));
        KnusVoortgang.mijlpaal(SECTIE, "bakkerij_recepten", RECEPTEN, Recept.BOEK.size(), BakkerijFeature.stack(ModItems.VAHOEGE_VADS_INGOT, 2),
                "bakkerij_receptenboek_vol");
        KnusVoortgang.mijlpaal(SECTIE, "bakkerij_feesttaart", FEESTTAART, 1, BakkerijFeature.stack(BakkerijFeature.BAKMUNT, 5));
        KnusVoortgang.verzameling(SECTIE, RECEPTENBOEK, Recept.BOEK.stream().map(Recept::id).toList(), id -> {
            Recept r = Recept.byId(id);
            return r == null ? ItemStack.EMPTY : new ItemStack(BakkerijFeature.bakje(r));
        });
    }

    /** A recipe baked well: in the receptenboek (the first time: a toast, and the recipes counter goes up). */
    static void ontdek(ServerPlayer player, Recept recept) {
        if (recept.inBoek() && KnusVoortgang.ontdek(player, RECEPTENBOEK, recept.id())) {
            KnusVoortgang.tel(player, RECEPTEN, 1);
            if (KnusVoortgang.ontdekt(player, RECEPTENBOEK).size() >= Recept.BOEK.size()) {
                toon(player, "bakkerij_receptenboek_vol");
            }
        }
    }

    /** The recipes this player has in the receptenboek (in book order). */
    public static List<Recept> bekend(ServerPlayer player) {
        return Recept.BOEK.stream().filter(r -> KnusVoortgang.heeft(player, RECEPTENBOEK, r.id())).toList();
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

    private BakkerijVoortgang() {
    }
}
