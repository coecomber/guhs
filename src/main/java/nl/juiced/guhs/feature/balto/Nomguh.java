package nl.juiced.guhs.feature.balto;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import nl.juiced.guhs.feature.baltoslee.NomguhRoute;

/**
 * 3.0: Nomguh, the snowy guh town of the Sneeuwguhtoendra (balto), as balto-slee needs it (CONTRACT_30 §4.9). The town is ONE
 * template placed unrotated (guhs:regio_jigsaw), so the world anchor is the piece's corner + the template anchor of
 * {@code assets/guhs/nomguh/route.json} (the block guhs:nomguh_midden in the middle of the plaza).
 */
public final class Nomguh {
    /** How far {@link #anker} looks. */
    public static final int ZOEK = 200;

    /** The Nomguh start (the world position of its anchor block guhs:nomguh_midden) within 200 blocks of near, or null. */
    @Nullable
    public static BlockPos anker(ServerLevel level, BlockPos near) {
        Structure s = structure(level);
        if (s == null) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureAt(near, s);
        if (start == null || !start.isValid()) {
            start = null;
            var holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(BaltoFeature.NOMGUH).orElse(null);
            if (holder == null) {
                return null;
            }
            Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder),
                    near, ZOEK / 16 + 1, false);
            if (found != null) {
                ChunkPos cp = new ChunkPos(found.getFirst());
                ChunkAccess chunk = level.getChunk(cp.x, cp.z, ChunkStatus.STRUCTURE_STARTS);
                start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), s, chunk);
            }
        }
        BlockPos a = anker(start);
        if (a == null) {
            return null;
        }
        double dx = a.getX() - near.getX(), dz = a.getZ() - near.getZ();
        return dx * dx + dz * dz <= (double) ZOEK * ZOEK ? a : null;
    }

    /** The anchor of this Nomguh start (null when it isn't one). */
    @Nullable
    public static BlockPos anker(@Nullable StructureStart start) {
        if (start == null || !start.isValid() || start.getPieces().isEmpty()) {
            return null;
        }
        BoundingBox box = start.getPieces().get(0).getBoundingBox();
        BlockPos t = NomguhRoute.laad().anker();
        return new BlockPos(box.minX() + t.getX(), box.minY() + t.getY(), box.minZ() + t.getZ());
    }

    /** Is this spot inside a Nomguh (its whole square)? */
    public static boolean in(ServerLevel level, BlockPos pos) {
        Structure s = structure(level);
        return s != null && level.structureManager().getStructureAt(pos, s).isValid();
    }

    @Nullable
    static Structure structure(ServerLevel level) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(BaltoFeature.NOMGUH);
    }

    /** Is p's Nomguh questline done (the Steele-Mika race and the own sneeuwslee unlocked)? */
    public static boolean verhaalKlaar(ServerPlayer p) {
        return BaltoVerhaal.stap(p) >= BaltoVerhaal.KLAAR;
    }

    private Nomguh() {
    }
}
