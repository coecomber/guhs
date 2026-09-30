package nl.juiced.guhs.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import nl.juiced.guhs.Guhs;
import org.slf4j.Logger;

/**
 * 26.1 SavedData helpers (1.21.5+: {@link SavedDataType} with a codec instead of {@code SavedData.Factory} + {@code save(tag, provider)}).
 * <p>
 * Pattern for an existing tag-based class:
 * <pre>
 * public static final SavedDataType&lt;Scorebord&gt; TYPE = GuhSavedData.tagType("scorebord", Scorebord::new, Scorebord::load, s -&gt; s.save(new CompoundTag()));
 * Scorebord data = GuhSavedData.get(server.overworld(), TYPE, "guhs_scorebord");   // was computeIfAbsent(new Factory(..), "guhs_scorebord")
 * </pre>
 * The file of a SavedDataType is {@code <dimension>/data/<namespace>/<path>.dat} (for the overworld
 * {@code <world>/dimensions/minecraft/overworld/data/guhs/<path>.dat}); 1.0.0 wrote {@code <world>/data/<legacy>.dat}
 * (overworld) or {@code <world>/dimensions/<ns>/<dim>/data/<legacy>.dat}. Vanilla's file fixer only moves vanilla files, so
 * {@link #get} moves the old file to the new place once (the first time the data is asked for) so 1.0.0 worlds keep it.
 * The "data" wrapper and DataVersion inside the file are the same as in 1.21.1.
 */
public final class GuhSavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Path> CHECKED = ConcurrentHashMap.newKeySet();

    private GuhSavedData() {
    }

    /** A SavedDataType {@code guhs:<path>} for a class that keeps its 1.21.1 {@code load(CompoundTag)} / {@code save(CompoundTag)}. */
    public static <T extends SavedData> SavedDataType<T> tagType(String path, Supplier<T> constructor, Function<CompoundTag, T> load,
                                                                 Function<T, CompoundTag> save) {
        return new SavedDataType<>(Guhs.id(path), constructor, CompoundTag.CODEC.xmap(load, save));
    }

    /** A SavedDataType {@code guhs:<path>} with a real codec. */
    public static <T extends SavedData> SavedDataType<T> type(String path, Supplier<T> constructor, Codec<T> codec) {
        return new SavedDataType<>(Guhs.id(path), constructor, codec);
    }

    /** {@code level.getDataStorage().computeIfAbsent(type)}, after moving a 1.0.0 file {@code <legacyName>.dat} to the new place. */
    public static <T extends SavedData> T get(ServerLevel level, SavedDataType<T> type, String legacyName) {
        migrate(level, type, legacyName);
        return level.getDataStorage().computeIfAbsent(type);
    }

    /** Moves {@code <legacyName>.dat} of 1.0.0 to the 26.1 place of {@code type} if the new file does not exist yet. */
    public static void migrate(ServerLevel level, SavedDataType<?> type, String legacyName) {
        Path root = level.getServer().getWorldPath(LevelResource.ROOT);
        Path dimData = DimensionType.getStorageFolder(level.dimension(), root).resolve("data");
        Path target = type.id().withSuffix(".dat").resolveAgainst(dimData);
        if (!CHECKED.add(target.toAbsolutePath().normalize())) {
            return;
        }
        Path legacy = level.dimension() == Level.OVERWORLD
                ? level.getServer().getWorldPath(LevelResource.DATA).resolve(legacyName + ".dat")
                : dimData.resolve(legacyName + ".dat");
        try {
            if (Files.exists(legacy) && !Files.exists(target)) {
                Files.createDirectories(target.getParent());
                Files.move(legacy, target, StandardCopyOption.ATOMIC_MOVE);
                LOGGER.info("Guhs: moved 1.0.0 saved data {} -> {}", legacy, target);
            }
        } catch (IOException e) {
            LOGGER.error("Guhs: could not move 1.0.0 saved data {} -> {}", legacy, target, e);
        }
    }
}
