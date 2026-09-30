package nl.juiced.guhs.feature.mewtwo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Mieuwguh lives only around the kloon-eiland, and only for players who know her story (who got to the big meal of the
 * questline: {@link MewtwoVoortgang#MAALTIJD} or further). Every few seconds, for each such player on an island: when no
 * Mieuwguh floats within 64 blocks of the island's middle, one pops up above the koepel ({@link #kijk}). A Mieuwguh brought by
 * the spawner floats off again when nobody who knows her is within 96 blocks ({@link #welkom}).
 */
public final class MewSpawner {
    /** How often a player on an island is checked (ticks). */
    public static final int ELKE = 100;
    /** (tests) island middles that count as a kloon-eiland (the test world has no generated island). */
    public static final List<BlockPos> TEST_EILANDEN = new CopyOnWriteArrayList<>();

    /** Does this player get to see Mieuwguh (the questline reached the meal)? */
    public static boolean magMew(ServerPlayer p) {
        return MewtwoVoortgang.stap(p) >= MewtwoVoortgang.MAALTIJD;
    }

    /** The middle of the kloon-eiland this spot is on (its template's middle, at the koepel's top), or null. */
    @Nullable
    public static BlockPos eiland(ServerLevel level, BlockPos pos) {
        for (BlockPos t : TEST_EILANDEN) {
            if (t.distSqr(pos) < 80 * 80) {
                return t;
            }
        }
        if (level.dimension() != ModDimensions.GUHMENSION) {
            return null;
        }
        Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(MewtwoFeature.KLOON_EILAND);
        if (structure == null) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureAt(pos, structure);
        if (!start.isValid()) {
            return null;
        }
        BoundingBox box = start.getBoundingBox();
        // (the template: the anchor in the middle at y63, the koepel's top at y82)
        return new BlockPos(box.getCenter().getX(), 84, box.getCenter().getZ());
    }

    /** One check for one player: a Mieuwguh near the island when they may see her and none is there. Returns the new one. */
    @Nullable
    public static MewEntity kijk(ServerPlayer p) {
        if (!magMew(p) || !(p.level() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos midden = eiland(level, p.blockPosition());
        if (midden == null || MewEntity.in(level, Vec3.atCenterOf(midden), 64)) {
            return null;
        }
        return spawn(level, midden.above(2), midden, true);
    }

    /** Spawns a Mieuwguh at a spot with that home (wild: the spawner takes her away again). */
    @Nullable
    public static MewEntity spawn(ServerLevel level, BlockPos at, BlockPos thuis, boolean wild) {
        MewEntity mew = MewtwoFeature.MEW.get().create(level, EntitySpawnReason.TRIGGERED);
        if (mew == null) {
            return null;
        }
        mew.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        mew.zetThuis(thuis, wild);
        mew.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
        level.addFreshEntity(mew);
        level.sendParticles(MewtwoFeature.GLOED.get(), mew.getX(), mew.getY() + 0.3, mew.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
        mew.giechel();
        return mew;
    }

    /** May this spawner Mieuwguh stay: is somebody who knows her within 96 blocks? */
    public static boolean welkom(MewEntity mew) {
        for (var player : mew.level().players()) {
            if (player instanceof ServerPlayer sp && sp.distanceTo(mew) < 96 && magMew(sp)) {
                return true;
            }
        }
        return false;
    }

    private MewSpawner() {
    }
}
