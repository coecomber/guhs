package nl.juiced.guhs.feature.einde2.zij;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.LeegteBouwStructure;
import nl.juiced.guhs.feature.einde2.Plekken;

/**
 * einde2 zij: one copy of a side place and its markers in the world. Every side place may stand more than once (a rare
 * copy besides the guaranteed one), so nothing here asks for "the" copy: a {@link Kopie} is found from where a player
 * stands ({@link #bij}: the chunk's own structure references, as {@code Einde2Ontdekt.bouwBij} does: cheap enough for a
 * tick) and its markers are turned into world positions with {@link Plekken#wereld(StructureStart, Plekken.Plek)}.
 */
public final class ZijPlek {
    /** A copy of a {@code guhs:leegte_bouw} side place. */
    public record Kopie(ServerLevel level, String structuur, StructureStart start, BoundingBox doos) {
        /** The world block of a marker of this copy (null: the sidecar has no such marker). */
        @Nullable
        public BlockPos pos(String plek) {
            Plekken.Plek p = Plekken.plek(structuur, plek);
            return p == null ? null : Plekken.wereld(start, p);
        }

        /** The middle of a marker's block (a zone: the middle of its floor). */
        @Nullable
        public Vec3 midden(String plek) {
            AABB z = zone(plek);
            if (z != null) {
                return new Vec3((z.minX + z.maxX) / 2, z.minY, (z.minZ + z.maxZ) / 2);
            }
            BlockPos b = pos(plek);
            return b == null ? null : Vec3.atBottomCenterOf(b);
        }

        /** The box of a zone marker (a prop or npc: its block), in world coordinates. */
        @Nullable
        public AABB zone(String plek) {
            Plekken.Plek p = Plekken.plek(structuur, plek);
            BlockPos a = p == null ? null : Plekken.wereld(start, p);
            if (a == null) {
                return null;
            }
            if (p.tot() == null) {
                return new AABB(a);
            }
            BlockPos b = a.offset(p.tot().subtract(p.pos()));
            return new AABB(Vec3.atLowerCornerOf(a), Vec3.atLowerCornerOf(b).add(1, 1, 1));
        }

        /** Is this spot inside the zone (grown by {@code rand} blocks; a few blocks of height above its floor count too)? */
        public boolean in(String plek, Vec3 waar, double rand) {
            AABB z = zone(plek);
            return z != null && z.inflate(rand, rand + 1, rand).contains(waar);
        }

        /** Is this spot within {@code straal} blocks of the middle of a marker's block? */
        public boolean bij(String plek, Vec3 waar, double straal) {
            BlockPos b = pos(plek);
            return b != null && Vec3.atBottomCenterOf(b).distanceToSqr(waar) <= straal * straal;
        }
    }

    /** A remembered answer: the copy (null: none) and until when a "none" holds (a found copy holds while the player is in it). */
    private record Antwoord(@Nullable Kopie kopie, long tot) {
    }

    /** (per player and place, not saved) the last answer, so most ticks cost one box test. */
    private static final Map<String, Antwoord> LAATST = new ConcurrentHashMap<>();
    /** A "not here" is asked again after this many ticks. */
    private static final int NIET_HIER = 20;

    /**
     * The copy of a {@code guhs:leegte_bouw} place whose box (grown by {@code rand}) holds this spot, or null. Only
     * {@code structuur} when given; any of the big places when null.
     */
    @Nullable
    public static Kopie bij(ServerLevel level, BlockPos pos, @Nullable String structuur, int rand) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (rand <= 0 && (dx != 0 || dz != 0)) {
                    continue;
                }
                ChunkPos cp = new ChunkPos((pos.getX() >> 4) + dx, (pos.getZ() >> 4) + dz);
                if (!level.hasChunk(cp.x(), cp.z())) {
                    continue;
                }
                for (StructureStart start : level.structureManager().startsForStructure(cp, s -> s instanceof LeegteBouwStructure)) {
                    if (!start.isValid() || !(start.getStructure() instanceof LeegteBouwStructure bouw)) {
                        continue;
                    }
                    Identifier key = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(bouw);
                    if (key == null || (structuur != null && !structuur.equals(key.getPath()))) {
                        continue;
                    }
                    BoundingBox doos = bouw.doos(start.getChunkPos());
                    if (doos.inflatedBy(Math.max(0, rand)).isInside(pos)) {
                        return new Kopie(level, key.getPath(), start, doos);
                    }
                }
            }
        }
        return null;
    }

    /** The copy of this place the player is in (or near: {@code rand}); remembered per player so most ticks cost nothing. */
    @Nullable
    public static Kopie bij(ServerPlayer p, String structuur, int rand) {
        String sleutel = p.getStringUUID() + "|" + structuur;
        Antwoord a = LAATST.get(sleutel);
        BlockPos pos = p.blockPosition();
        long nu = p.level().getGameTime();
        if (a != null) {
            Kopie k = a.kopie();
            if (k != null && k.level() == p.level() && k.doos().inflatedBy(rand).isInside(pos)) {
                return k;
            }
            if (k == null && nu < a.tot()) {
                return null;
            }
        }
        Kopie k = bij(p.level(), pos, structuur, rand);
        LAATST.put(sleutel, new Antwoord(k, nu + NIET_HIER));
        return k;
    }

    /** (logout) forgets the remembered copy. */
    public static void vergeet(UUID speler) {
        String voor = speler + "|";
        LAATST.keySet().removeIf(k -> k.startsWith(voor));
    }
}
