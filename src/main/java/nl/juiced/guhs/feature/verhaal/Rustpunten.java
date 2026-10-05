package nl.juiced.guhs.feature.verhaal;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (verhaal engine, CONTRACT_130 §6.2.9): "the last rest point" of a player, per kind (soort: "ring" for the
 * Knabbelring trek; a course keeps its own flags). Saved with the player ({@code guhs_rustpunt_<soort>}), so it survives
 * logging out and dying. Seen by the Eye? {@code Rustpunten.terug(p, "ring")}.
 */
public final class Rustpunten {
    /** A saved rest point. */
    public record Punt(ResourceKey<Level> dim, Vec3 plek, float yaw) {
    }

    private static String key(String soort) {
        return "guhs_rustpunt_" + soort;
    }

    /** This is the player's rest point of that kind from now on. */
    public static void zet(ServerPlayer p, String soort, ResourceKey<Level> dim, Vec3 plek, float yaw) {
        CompoundTag t = new CompoundTag();
        t.putString("Dim", dim.identifier().toString());
        t.putDouble("X", plek.x);
        t.putDouble("Y", plek.y);
        t.putDouble("Z", plek.z);
        t.putFloat("Yaw", yaw);
        GuhQuests.saved(p).put(key(soort), t);
    }

    /** The player's rest point of that kind (null: none yet). */
    @Nullable
    public static Punt van(ServerPlayer p, String soort) {
        CompoundTag t = GuhQuests.saved(p).getCompoundOrEmpty(key(soort));
        Identifier dim = Identifier.tryParse(t.getStringOr("Dim", ""));
        if (t.isEmpty() || dim == null) {
            return null;
        }
        return new Punt(ResourceKey.create(Registries.DIMENSION, dim), new Vec3(t.getDoubleOr("X", 0), t.getDoubleOr("Y", 0), t.getDoubleOr("Z", 0)),
                t.getFloatOr("Yaw", 0f));
    }

    /** Puts the player back on their rest point of that kind ({@link Duwtje#terug}); false: they have none. */
    public static boolean terug(ServerPlayer p, String soort) {
        Punt punt = van(p, soort);
        if (punt == null || p.level().getServer().getLevel(punt.dim()) == null) {
            return false;
        }
        Duwtje.terug(p, punt.dim(), punt.plek(), punt.yaw());
        return true;
    }

    /** Forgets the rest point of that kind. */
    public static void wis(ServerPlayer p, String soort) {
        GuhQuests.saved(p).remove(key(soort));
    }

    private Rustpunten() {
    }
}
