package nl.juiced.guhs.feature.safari;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * The world's safari state (SavedData {@code guhs:safari}, with the overworld): which park version is stamped, which tiles
 * of the current stamping are done, and the generation of the animals (an animal of an older generation removes itself
 * when its chunk loads, see {@link SafariDier}).
 */
public final class SafariOpslag extends SavedData {
    public static final SavedDataType<SafariOpslag> TYPE = GuhSavedData.tagType("safari", SafariOpslag::new, SafariOpslag::load, SafariOpslag::save);

    /** The park version the stamped park is from (0: never stamped). */
    int gebouwd;
    /** The version being stamped now (0: none); with {@link #klaar} the tiles of it that are done (bit per tile). */
    int bezig;
    long klaar;
    /** True when the stamping in progress clears the tiles first (a rebuild). */
    boolean herbouw;
    /** The generation of the animals: raised on every respawn of the populations. */
    int generatie;
    /** The park version the populations were spawned for (0: never). */
    int dierenVoor;

    public static SafariOpslag get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int generatie() {
        return generatie;
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("Gebouwd", gebouwd);
        t.putInt("Bezig", bezig);
        t.putLong("Klaar", klaar);
        t.putBoolean("Herbouw", herbouw);
        t.putInt("Generatie", generatie);
        t.putInt("DierenVoor", dierenVoor);
        return t;
    }

    private static SafariOpslag load(CompoundTag t) {
        SafariOpslag s = new SafariOpslag();
        s.gebouwd = t.getIntOr("Gebouwd", 0);
        s.bezig = t.getIntOr("Bezig", 0);
        s.klaar = t.getLongOr("Klaar", 0L);
        s.herbouw = t.getBooleanOr("Herbouw", false);
        s.generatie = t.getIntOr("Generatie", 0);
        s.dierenVoor = t.getIntOr("DierenVoor", 0);
        return s;
    }
}
