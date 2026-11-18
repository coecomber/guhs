package nl.juiced.guhs.feature.guhwinter.plein;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * (Guhmension) what {@code /guhs winter inrichten} already did, so a second run skips the one-time parts, and which square
 * stands now ("" nothing, "winter", "raam"). The ground around the square is shaped only once ({@link #grondKlaar}).
 */
public final class GuhwinterPleinData extends SavedData {
    public static final SavedDataType<GuhwinterPleinData> TYPE = GuhSavedData.tagType("guhwinter_plein", GuhwinterPleinData::new,
            GuhwinterPleinData::load, GuhwinterPleinData::save);

    boolean portaal, reisguh, oudeTop, grondKlaar, aankomst, verstop;
    String plein = "";

    public static GuhwinterPleinData van(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Which square stands: "", "winter" or "raam". */
    public String plein() {
        return plein;
    }

    /** Did {@code /guhs winter inrichten} finish (portal moved, old peak restored, arrival built)? */
    public boolean ingericht() {
        return portaal && oudeTop && aankomst;
    }

    void zet(Runnable r) {
        r.run();
        setDirty();
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Portaal", portaal);
        t.putBoolean("Reisguh", reisguh);
        t.putBoolean("OudeTop", oudeTop);
        t.putBoolean("Grond", grondKlaar);
        t.putBoolean("Aankomst", aankomst);
        t.putBoolean("Verstop", verstop);
        t.putString("Plein", plein);
        return t;
    }

    private static GuhwinterPleinData load(CompoundTag t) {
        GuhwinterPleinData d = new GuhwinterPleinData();
        d.portaal = t.getBooleanOr("Portaal", false);
        d.reisguh = t.getBooleanOr("Reisguh", false);
        d.oudeTop = t.getBooleanOr("OudeTop", false);
        d.grondKlaar = t.getBooleanOr("Grond", false);
        d.aankomst = t.getBooleanOr("Aankomst", false);
        d.verstop = t.getBooleanOr("Verstop", false);
        d.plein = t.getStringOr("Plein", "");
        return d;
    }
}
