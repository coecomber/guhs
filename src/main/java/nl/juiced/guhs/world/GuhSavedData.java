package nl.juiced.guhs.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import nl.juiced.guhs.Guhs;

/**
 * 26.1 port: saved data helper for all of Guhs' {@link SavedData} classes.
 * <ul>
 *     <li>{@link #type}: builds a {@link SavedDataType} from the old 1.21.1 style {@code load(CompoundTag, Provider)} /
 *     {@code save(CompoundTag, Provider)} pair (the codec wraps the whole tag, so the file content stays the same as in
 *     1.0.0). The id is {@code guhs:<old name without the "guhs_" prefix>}.</li>
 *     <li>{@link #get}: {@code level.getDataStorage().computeIfAbsent(type)}, but first moves a 1.0.0 file
 *     ({@code <old name>.dat}) to the new 26.1 place ({@code <dimension>/data/guhs/<path>.dat}) once, so old worlds keep
 *     their bank, highscores, nests, ...</li>
 * </ul>
 * Usage:
 * <pre>
 * static final SavedDataType&lt;Data&gt; TYPE = GuhSavedData.type("guhs_reisguhs", Data::new, Data::load, Data::save);
 * Data d = GuhSavedData.get(server.overworld(), TYPE);
 * </pre>
 */
public final class GuhSavedData {
    /** new id -> old 1.21.1 file name (without .dat). */
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final Map<Identifier, String> LEGACY = new ConcurrentHashMap<>();
    /** "folder|id" pairs already checked in this run. */
    private static final Set<String> CHECKED = ConcurrentHashMap.newKeySet();

    private GuhSavedData() {
    }

    /** A saved data type keeping the 1.21.1 tag layout; {@code oldName} is the 1.21.1 name ("guhs_reisguhs"). */
    public static <T extends SavedData> SavedDataType<T> type(String oldName, Supplier<T> constructor,
            BiFunction<CompoundTag, HolderLookup.Provider, T> load, BiFunction<T, HolderLookup.Provider, CompoundTag> save) {
        Identifier id = Guhs.id(oldName.startsWith("guhs_") ? oldName.substring("guhs_".length()) : oldName);
        LEGACY.put(id, oldName);
        return new SavedDataType<>(id, level -> constructor.get(), level -> codec(level, load, save));
    }

    /** Same, for a load/save pair that does not need the registries. */
    public static <T extends SavedData> SavedDataType<T> simpleType(String oldName, Supplier<T> constructor,
            java.util.function.Function<CompoundTag, T> load, java.util.function.Function<T, CompoundTag> save) {
        return type(oldName, constructor, (tag, regs) -> load.apply(tag), (data, regs) -> save.apply(data));
    }

    private static <T> Codec<T> codec(@Nullable ServerLevel level, BiFunction<CompoundTag, HolderLookup.Provider, T> load,
            BiFunction<T, HolderLookup.Provider, CompoundTag> save) {
        return CompoundTag.CODEC.xmap(tag -> load.apply(tag, registries(level)), data -> save.apply(data, registries(level)));
    }

    private static HolderLookup.Provider registries(@Nullable ServerLevel level) {
        if (level != null) {
            return level.registryAccess();
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.registryAccess() : RegistryAccess.EMPTY;
    }

    /** The data of this level (usually {@code server.overworld()}), migrating a 1.0.0 file first. */
    public static <T extends SavedData> T get(ServerLevel level, SavedDataType<T> type) {
        migrate(level, type.id());
        return level.getDataStorage().computeIfAbsent(type);
    }

    /** Moves {@code <old>.dat} of a 1.21.1 world to the 26.1 location, if there is no new file yet. */
    private static void migrate(ServerLevel level, Identifier id) {
        String old = LEGACY.get(id);
        if (old == null) {
            return;
        }
        MinecraftServer server = level.getServer();
        Path root = server.getWorldPath(LevelResource.ROOT);
        Path folder = DimensionType.getStorageFolder(level.dimension(), root).resolve("data");
        if (!CHECKED.add(folder.toAbsolutePath().normalize() + "|" + id)) {
            return;
        }
        Path target = id.withSuffix(".dat").resolveAgainst(folder);
        if (Files.exists(target)) {
            return;
        }
        for (Path candidate : candidates(level.dimension(), root, folder, old)) {
            if (Files.isRegularFile(candidate)) {
                try {
                    Files.createDirectories(target.getParent());
                    Files.move(candidate, target);
                    LOGGER.info("Guhs: moved 1.0.0 saved data {} to {}", candidate, target);
                } catch (IOException e) {
                    LOGGER.error("Guhs: could not move 1.0.0 saved data {} to {}", candidate, target, e);
                }
                return;
            }
        }
    }

    private static List<Path> candidates(net.minecraft.resources.ResourceKey<Level> dim, Path root, Path folder, String old) {
        String file = old + ".dat";
        List<Path> out = new ArrayList<>();
        out.add(folder.resolve(file));                              // same folder, no namespace
        out.add(folder.resolve("minecraft").resolve(file));         // if the vanilla file fixer moved it along
        if (dim == Level.OVERWORLD) {
            out.add(root.resolve("data").resolve(file));            // 1.21.1 overworld: <world>/data/<name>.dat
        } else if (dim == Level.NETHER) {
            out.add(root.resolve("DIM-1").resolve("data").resolve(file));
        } else if (dim == Level.END) {
            out.add(root.resolve("DIM1").resolve("data").resolve(file));
        }
        return out;
    }
}
