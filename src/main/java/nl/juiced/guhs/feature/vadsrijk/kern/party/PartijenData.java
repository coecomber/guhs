package nl.juiced.guhs.feature.vadsrijk.kern.party;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * W-PARTY: the parties across restarts (SavedData {@code guhs:vr_partijen} in the overworld). It holds a SNAPSHOT that is refreshed
 * at every party change ({@link #bewaar}), so clearing {@link Partijen} while the server stops can never save an empty file.
 * {@link #laad} at server start fills {@link Partijen} from it.
 */
public final class PartijenData extends SavedData {
    public static final SavedDataType<PartijenData> TYPE = GuhSavedData.tagType("vr_partijen", PartijenData::new, PartijenData::lees,
            d -> d.tag.copy());

    @Nullable
    private static PartijenData actief;
    private CompoundTag tag = new CompoundTag();

    public PartijenData() {
    }

    private static PartijenData lees(CompoundTag t) {
        PartijenData d = new PartijenData();
        d.tag = t.copy();
        return d;
    }

    /** Server start: the saved parties into {@link Partijen}. */
    public static void laad(MinecraftServer server) {
        actief = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        Partijen.uitTag(actief.tag);
    }

    public static void vergeet() {
        actief = null;
    }

    /** After a change: the snapshot is refreshed and marked dirty (saved with the world). */
    static void bewaar() {
        if (actief != null) {
            actief.tag = Partijen.naarTag();
            actief.setDirty();
        }
    }

    /** Gametests: the current snapshot. */
    public static CompoundTag snapshot() {
        return actief == null ? Partijen.naarTag() : actief.tag.copy();
    }
}
