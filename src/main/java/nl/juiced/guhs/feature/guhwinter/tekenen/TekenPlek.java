package nl.juiced.guhs.feature.guhwinter.tekenen;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import nl.juiced.guhs.feature.guhwinter.plein.GuhwinterAnkers;

/**
 * Where the kerstboom stands: plein's anchor {@code kerstboom} (the base centre of the tree, in guhs:guhmension), through
 * {@link GuhwinterAnkers} (1.10.9 integration; it used to read the json itself).
 */
public final class TekenPlek {
    private TekenPlek() {
    }

    /** The kerstboom anchor (in guhs:guhmension), or null when the plaza has none. */
    @Nullable
    public static BlockPos kerstboom(MinecraftServer server) {
        GuhwinterAnkers.Anker a = GuhwinterAnkers.zoek("kerstboom");
        return a == null ? null : a.pos();
    }

    /** (kept for TekenenFeature's ServerStoppedEvent; the anchors are read once from the mod jar) */
    public static void vergeet() {
    }
}
