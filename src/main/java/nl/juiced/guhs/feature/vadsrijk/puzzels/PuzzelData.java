package nl.juiced.guhs.feature.vadsrijk.puzzels;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * T-PUZZELS: where each puzzle was stamped in this world ({@code data/guhs_vadsrijk_puzzels.dat}, saved with the overworld):
 * the origin, the size it had (to clear it when the layout changes) and a fingerprint of the layout + anchor. A changed
 * fingerprint (new layout, or the anchor POI moved because the world layout shifted) = stamp again.
 */
public final class PuzzelData extends SavedData {
    static final SavedDataType<PuzzelData> TYPE = GuhSavedData.tagType("vadsrijk_puzzels", PuzzelData::new, PuzzelData::load, PuzzelData::save);

    public record Plek(BlockPos origin, int breed, int diep, int hoogte, int vingerafdruk) {
    }

    private final Map<String, Plek> plekken = new HashMap<>();

    public static PuzzelData get(MinecraftServer server) {
        return GuhSavedData.get(server.overworld(), TYPE, "guhs_vadsrijk_puzzels");
    }

    @Nullable
    public Plek plek(String id) {
        return plekken.get(id);
    }

    public void zet(String id, @Nullable Plek p) {
        if (p == null) {
            plekken.remove(id);
        } else {
            plekken.put(id, p);
        }
        setDirty();
    }

    private static PuzzelData load(CompoundTag t) {
        PuzzelData d = new PuzzelData();
        for (String k : t.keySet()) {
            if (t.get(k) instanceof CompoundTag c) {
                d.plekken.put(k, new Plek(new BlockPos(c.getIntOr("x", 0), c.getIntOr("y", 0), c.getIntOr("z", 0)), c.getIntOr("b", 0),
                        c.getIntOr("d", 0), c.getIntOr("h", 0), c.getIntOr("v", 0)));
            }
        }
        return d;
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        plekken.forEach((k, p) -> {
            CompoundTag c = new CompoundTag();
            c.putInt("x", p.origin().getX());
            c.putInt("y", p.origin().getY());
            c.putInt("z", p.origin().getZ());
            c.putInt("b", p.breed());
            c.putInt("d", p.diep());
            c.putInt("h", p.hoogte());
            c.putInt("v", p.vingerafdruk());
            t.put(k, c);
        });
        return t;
    }
}
