package nl.juiced.guhs.feature.elftocht;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;

/**
 * The tour's advancements in the tab "De Grote Guhspelen" (tools/features/elftocht.py writes them):
 * {@code grote_guhspelen/elftocht_gevonden} (you skated into the tour: a location trigger), and granted by the game:
 * {@code elftocht_eerste_rit} (your first start), {@code elftocht_uitgereden} (finished: the challenge),
 * {@code elftocht_snel} (under the last speed mark), {@code elftocht_kleding} (the four tour clothes unlocked).
 */
public final class ElftochtVoortgang {
    public static final GuhClothes[] KLEDING = {GuhClothes.ELFTOCHT_SCHAATSMUTS, GuhClothes.ELFTOCHT_TRUITJE, GuhClothes.ELFTOCHT_SJAAL,
            GuhClothes.ELFTOCHT_OORWARMERS};

    /** Grants guhs:grote_guhspelen/&lt;name&gt; (true when it was new). */
    public static boolean grant(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id("grote_guhspelen/" + name));
        if (holder == null || player.getAdvancements().getOrStartProgress(holder).isDone()) {
            return false;
        }
        player.getAdvancements().award(holder, "done");
        return true;
    }

    public static boolean heeft(ServerPlayer player, String name) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id("grote_guhspelen/" + name));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** All four tour clothes unlocked: "Warm ingepakt". */
    public static void kleding(ServerPlayer player) {
        for (GuhClothes c : KLEDING) {
            if (!KledingUnlocks.heeft(player, c)) {
                return;
            }
        }
        grant(player, "elftocht_kleding");
    }

    private ElftochtVoortgang() {
    }
}
