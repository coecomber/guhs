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
    /** (Round 5) The map ids of the kaartwand in the entreegebouw, in kaartwand.json's order (a restamp reuses them). */
    final java.util.List<Integer> kaarten = new java.util.ArrayList<>();
    /** (Round 5) The park version whose kaartwand hangs (the frames go up once the wall's chunks are loaded). */
    int kaartwand;

    public static SafariOpslag get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int generatie() {
        return generatie;
    }

    /** The map id of the k-th map of the kaartwand (a fresh one the first time). */
    net.minecraft.world.level.saveddata.maps.MapId kaart(int k, net.minecraft.server.level.ServerLevel level) {
        while (kaarten.size() <= k) {
            kaarten.add(level.getFreeMapId().id());
            setDirty();
        }
        return new net.minecraft.world.level.saveddata.maps.MapId(kaarten.get(k));
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("Gebouwd", gebouwd);
        t.putInt("Bezig", bezig);
        t.putLong("Klaar", klaar);
        t.putBoolean("Herbouw", herbouw);
        t.putInt("Generatie", generatie);
        t.putInt("DierenVoor", dierenVoor);
        t.putIntArray("Kaarten", kaarten.stream().mapToInt(Integer::intValue).toArray());
        t.putInt("Kaartwand", kaartwand);
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
        s.kaartwand = t.getIntOr("Kaartwand", 0);
        t.getIntArray("Kaarten").ifPresent(a -> {
            for (int id : a) {
                s.kaarten.add(id);
            }
        });
        return s;
    }
}
