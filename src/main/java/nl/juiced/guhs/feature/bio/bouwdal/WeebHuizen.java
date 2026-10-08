package nl.juiced.guhs.feature.bio.bouwdal;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * biomes3 bouw-dal: every weebhuisje anybody ever talked at (world data {@code guhs:bio_bouw_dal}, kept with the
 * overworld), and the clock that moves them on. A house is written down the first time a player talks to one of the two
 * (until then it is exactly as it was generated and needs nothing). The key is the box of the building.
 * <p>
 * Cost: one pass over the known houses per server tick, and per house only "is it loaded?" unless a scene or a departure
 * is running there; a house that is not loaded costs nothing else and its trip simply ends when somebody comes back.
 */
public final class WeebHuizen extends SavedData {
    public static final SavedDataType<WeebHuizen> TYPE = GuhSavedData.tagType("bio_bouw_dal", WeebHuizen::new, WeebHuizen::load, WeebHuizen::save);
    public static final ResourceKey<Structure> STRUCTUUR = ResourceKey.create(Registries.STRUCTURE, Guhs.id("weebhuisje"));

    private final List<WeebHuis> huizen = new ArrayList<>();

    public static WeebHuizen get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    static void vuil(ServerLevel level) {
        get(level.getServer()).setDirty();
    }

    public List<WeebHuis> alle() {
        return List.copyOf(huizen);
    }

    /** The house this spot belongs to, if it is known already. */
    @Nullable
    public WeebHuis op(ServerLevel level, BlockPos pos) {
        for (WeebHuis h : huizen) {
            if (h.dim == level.dimension() && h.box.isInside(pos)) {
                return h;
            }
        }
        return null;
    }

    /** Writes down a house with this box (or gives the one that overlaps it). */
    public WeebHuis registreer(ServerLevel level, BoundingBox box) {
        for (WeebHuis h : huizen) {
            if (h.dim == level.dimension() && h.box.intersects(box)) {
                return h;
            }
        }
        WeebHuis h = new WeebHuis(level.dimension(), box);
        huizen.add(h);
        setDirty();
        return h;
    }

    /** (tests) forgets a house. */
    public void vergeet(WeebHuis huis) {
        huizen.remove(huis);
        setDirty();
    }

    /**
     * The house at this spot: a known one, else the weebhuisje structure standing there, else (a hand-built or summoned
     * pair) a box around the spot.
     */
    public static WeebHuis bij(ServerLevel level, BlockPos pos) {
        WeebHuizen data = get(level.getServer());
        WeebHuis h = data.op(level, pos);
        if (h != null) {
            return h;
        }
        Structure s = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(STRUCTUUR);
        if (s != null) {
            StructureStart start = level.structureManager().getStructureWithPieceAt(pos, s);
            if (start.isValid()) {
                return data.registreer(level, start.getBoundingBox());
            }
        }
        return data.registreer(level, new BoundingBox(pos.getX() - 8, pos.getY() - 2, pos.getZ() - 8, pos.getX() + 8, pos.getY() + 8, pos.getZ() + 8));
    }

    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        WeebHuizen data = get(server);
        if (data.huizen.isEmpty()) {
            return;
        }
        int tik = server.getTickCount();
        for (int i = 0; i < data.huizen.size(); i++) {
            WeebHuis h = data.huizen.get(i);
            if (!h.actief() && (tik + i * 7) % 20 != 0) {
                continue;
            }
            ServerLevel level = server.getLevel(h.dim);
            if (level != null && h.geladen(level)) {
                h.tick(level);
            }
        }
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        ListTag lijst = new ListTag();
        for (WeebHuis h : huizen) {
            lijst.add(h.save());
        }
        t.put("Huizen", lijst);
        return t;
    }

    /** (also for the tests: a round trip through NBT, as a restart does) */
    static WeebHuizen load(CompoundTag t) {
        WeebHuizen data = new WeebHuizen();
        ListTag lijst = t.getListOrEmpty("Huizen");
        for (int i = 0; i < lijst.size(); i++) {
            WeebHuis h = WeebHuis.load(lijst.getCompoundOrEmpty(i));
            if (h != null) {
                data.huizen.add(h);
            }
        }
        return data;
    }

    /** (tests) what a restart does to one house: written to NBT and read back, in place. */
    public WeebHuis herlaad(WeebHuis huis) {
        int i = huizen.indexOf(huis);
        WeebHuis nieuw = WeebHuis.load(huis.save());
        nieuw.klokVooruit = huis.klokVooruit;
        huizen.set(i, nieuw);
        return nieuw;
    }
}
