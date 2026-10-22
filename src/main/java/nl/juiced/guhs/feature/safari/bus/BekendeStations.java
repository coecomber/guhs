package nl.juiced.guhs.feature.safari.bus;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * 1.6.1: the busstations the world knows (SavedData {@code guhs:safari_busstations}, with the overworld): every station
 * whose chunk was ever loaded, and every one an op placed. The ranger, the Superkompas and {@code busstation waar} only
 * look here: a real structure search (findNearestMapStructure) on the server thread loads chunks and waited on the
 * guaranteed-placement lock until the watchdog stopped the live server (1.6.0, 2026-10-22).
 */
public final class BekendeStations extends SavedData {
    public static final SavedDataType<BekendeStations> TYPE = GuhSavedData.tagType("safari_busstations", BekendeStations::new,
            BekendeStations::load, BekendeStations::save);

    /** Per station: the dimension id and the middle of its start piece. */
    record Station(String dim, BlockPos pos) {
    }

    private final List<Station> stations = new ArrayList<>();

    public static BekendeStations get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    static void registreer() {
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load e) -> {
            if (!(e.getLevel() instanceof ServerLevel level) || !(e.getChunk() instanceof LevelChunk chunk)) {
                return;
            }
            Structure structuur = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(Busstation.KEY);
            if (structuur == null) {
                return;
            }
            StructureStart start = chunk.getStartForStructure(structuur);
            if (start != null && start.isValid()) {
                onthoud(level, start.getBoundingBox().getCenter());
            }
        });
    }

    /** Remember a station (idempotent: a station within 32 blocks of a known one is the same one). */
    public static void onthoud(ServerLevel level, BlockPos pos) {
        BekendeStations b = get(level.getServer());
        String dim = level.dimension().identifier().toString();
        for (Station s : b.stations) {
            if (s.dim().equals(dim) && s.pos().distSqr(pos) < 32 * 32) {
                return;
            }
        }
        b.stations.add(new Station(dim, pos.immutable()));
        b.setDirty();
    }

    /** The nearest known station in this level, or null. Never searches or loads anything. */
    @Nullable
    public static BlockPos dichtstbij(ServerLevel level, BlockPos bij) {
        String dim = level.dimension().identifier().toString();
        BlockPos beste = null;
        for (Station s : get(level.getServer()).stations) {
            if (s.dim().equals(dim) && (beste == null || s.pos().distSqr(bij) < beste.distSqr(bij))) {
                beste = s.pos();
            }
        }
        return beste;
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        for (Station s : stations) {
            CompoundTag c = new CompoundTag();
            c.putString("Dim", s.dim());
            c.putLong("Pos", s.pos().asLong());
            l.add(c);
        }
        t.put("Stations", l);
        return t;
    }

    private static BekendeStations load(CompoundTag t) {
        BekendeStations b = new BekendeStations();
        ListTag l = t.getListOrEmpty("Stations");
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompoundOrEmpty(i);
            b.stations.add(new Station(c.getStringOr("Dim", ""), BlockPos.of(c.getLongOr("Pos", 0L))));
        }
        return b;
    }
}
