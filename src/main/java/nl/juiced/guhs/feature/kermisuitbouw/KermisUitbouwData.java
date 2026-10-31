package nl.juiced.guhs.feature.kermisuitbouw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Per dimension: the kermissen with an extension (by template origin), the version stamped there, and so the boxes that are
 * protected (known from the moment the work starts, also when the Kermis-guh's chunk is not loaded).
 */
public final class KermisUitbouwData extends SavedData {
    public static final SavedDataType<KermisUitbouwData> TYPE = GuhSavedData.tagType("kermis_uitbouw", KermisUitbouwData::new,
            KermisUitbouwData::load, KermisUitbouwData::save);

    private record Stand(KermisUitbouw.Plek plek, int versie) {
    }

    private final Map<Long, Stand> kermissen = new HashMap<>();
    private final List<BoundingBox> dozen = new ArrayList<>();

    public static KermisUitbouwData van(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** The version stamped at this kermis (0: nothing yet). */
    public int versie(KermisUitbouw.Plek plek) {
        Stand s = kermissen.get(plek.oorsprong().asLong());
        return s == null ? 0 : s.versie();
    }

    void zet(KermisUitbouw.Plek plek, int versie) {
        if (kermissen.put(plek.oorsprong().asLong(), new Stand(plek, versie)) == null) {
            dozen.addAll(plek.dozen());
        }
        setDirty();
    }

    /** Does this spot belong to an extension? */
    public boolean beschermd(BlockPos pos) {
        for (int i = 0; i < dozen.size(); i++) {
            if (dozen.get(i).isInside(pos)) {
                return true;
            }
        }
        return false;
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        ListTag lijst = new ListTag();
        for (Stand s : kermissen.values()) {
            CompoundTag c = new CompoundTag();
            c.putLong("Oorsprong", s.plek().oorsprong().asLong());
            c.putString("Draai", s.plek().draai().name());
            c.putInt("Versie", s.versie());
            lijst.add(c);
        }
        t.put("Kermissen", lijst);
        return t;
    }

    private static KermisUitbouwData load(CompoundTag t) {
        KermisUitbouwData d = new KermisUitbouwData();
        ListTag lijst = t.getListOrEmpty("Kermissen");
        for (int i = 0; i < lijst.size(); i++) {
            CompoundTag c = lijst.getCompoundOrEmpty(i);
            Rotation draai;
            try {
                draai = Rotation.valueOf(c.getStringOr("Draai", "NONE"));
            } catch (IllegalArgumentException e) {
                draai = Rotation.NONE;
            }
            d.zet(new KermisUitbouw.Plek(BlockPos.of(c.getLongOr("Oorsprong", 0L)), draai), c.getIntOr("Versie", 0));
        }
        return d;
    }
}
