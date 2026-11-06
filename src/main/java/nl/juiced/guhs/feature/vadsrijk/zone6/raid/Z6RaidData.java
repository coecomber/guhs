package nl.juiced.guhs.feature.vadsrijk.zone6.raid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * U-Z6RAID: what the raid remembers server-wide ({@code data/guhs/vadsrijk_z6raid.dat}): which version of the binnentoren
 * template is placed, whether the stars above the desert burn again (the first clear ever lights them for good), how many
 * clears there were. The running raid itself (group, floor, queue) is NOT saved: a restart frees the tower (like parties).
 */
public final class Z6RaidData extends SavedData {
    static final SavedDataType<Z6RaidData> TYPE = GuhSavedData.tagType("vadsrijk_z6raid", Z6RaidData::new, Z6RaidData::load, Z6RaidData::save);

    public int bouwVersie;
    public boolean sterrenAan;
    public int clears;

    public static Z6RaidData get(MinecraftServer server) {
        return GuhSavedData.get(server.overworld(), TYPE, "guhs_vadsrijk_z6raid");
    }

    public void zet(Runnable r) {
        r.run();
        setDirty();
    }

    private static Z6RaidData load(CompoundTag t) {
        Z6RaidData d = new Z6RaidData();
        d.bouwVersie = t.getIntOr("bouw", 0);
        d.sterrenAan = t.getBooleanOr("sterren", false);
        d.clears = t.getIntOr("clears", 0);
        return d;
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("bouw", bouwVersie);
        t.putBoolean("sterren", sterrenAan);
        t.putInt("clears", clears);
        return t;
    }
}
