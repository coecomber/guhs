package nl.juiced.guhs.feature.frituur;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.registry.ModItems;

/**
 * The snack recipes a player knows, per player and kept for good: a collection page in the Bakkerij section of the Knus tab
 * ({@link KnusVoortgang}). The ids are the ids of the items that come out ({@link #ALLE}). The Snackbar-guh calls {@link #leer}.
 */
public final class SnackRecepten {
    public static final String VERZAMELING = "snackrecepten", TELLER_KENNIS = "frituur.recepten", TELLER_HAP = "frituur.vetste_hap";
    /** Every recipe you can learn. */
    public static final List<String> ALLE = FrituurRecepten.ALLE.stream().filter(FrituurRecepten.Recept::leren).map(FrituurRecepten.Recept::id).toList();

    static void register() {
        KnusVoortgang.verzameling("bakkerij", VERZAMELING, ALLE, SnackRecepten::icoon);
        KnusVoortgang.mijlpaal("bakkerij", "frituur_recepten", TELLER_KENNIS, ALLE.size(), () -> new ItemStack(ModItems.MIKA_VET.get(), 4));
        KnusVoortgang.mijlpaal("bakkerij", "frituur_vetste_hap", TELLER_HAP, 1, () -> new ItemStack(FrituurFeature.DRIEDUBBEL.get(), 16));
    }

    /** Does this player know this recipe? (An id outside {@link #ALLE}: no.) */
    public static boolean kent(ServerPlayer player, String id) {
        return KnusVoortgang.heeft(player, VERZAMELING, id);
    }

    /** Teaches this recipe (nothing happens when it is known already or the id is not in {@link #ALLE}). */
    public static void leer(ServerPlayer player, String id) {
        if (ALLE.contains(id) && KnusVoortgang.ontdek(player, VERZAMELING, id)) {
            KnusVoortgang.hoogste(player, TELLER_KENNIS, bekend(player).size());
        }
    }

    /** The recipes this player knows. */
    public static Set<String> bekend(ServerPlayer player) {
        return KnusVoortgang.ontdekt(player, VERZAMELING);
    }

    /** The recipe with this id (null: there is none to learn). */
    @Nullable
    public static FrituurRecepten.Recept recept(String id) {
        return FrituurRecepten.ALLE.stream().filter(r -> r.leren() && r.id().equals(id)).findFirst().orElse(null);
    }

    private static ItemStack icoon(String id) {
        FrituurRecepten.Recept r = recept(id);
        return r == null ? new ItemStack(Items.PAPER) : new ItemStack(r.uit().get());
    }

    private SnackRecepten() {
    }
}
