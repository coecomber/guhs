package nl.juiced.guhs.feature.spiesburcht;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;

/**
 * 1.4.1: how busy the open Guhbarbecuether gets. No resources of its own: the lists of who spawns where are the biome
 * files (tools/features/barbecuether.py, spiesburcht.py and sausdieren.py).
 * <ul>
 *   <li><b>Fewer aggressive mobs in the open.</b> The Nether-Mika's, Vonk-Mika's and Knekel-Mika's of the natural spawner
 *   always filled vanilla's monster cap (70 around every player: the dimension is one big cave, there is floor everywhere),
 *   so lower weights in the biome files changed nothing but the mix. {@link #mikaMagKomen} is an extra spawn rule: in the
 *   open the natural spawner brings no new one while {@link #MAX_BOOS} aggressive mobs are around the player.</li>
 *   <li><b>Inside the buildings nothing changes</b>: a spot where a structure brings its own monsters (the Spiesburcht,
 *   the Mika-grillpaleis: {@link #inEigenGebouw}) keeps vanilla's cap, and so does every mob spawner block.</li>
 *   <li><b>The Asguhs</b> of the Asdal ({@link SpiesburchtEvents}): at most {@link #MAX_ASGUHS} wild ones around a player,
 *   so the creature cap (10) keeps room for the Rookguhs, Sauslopers and Sausblubjes.</li>
 * </ul>
 */
public final class Drukte {
    /** In the open no new aggressive mob comes while this many are around the player (vanilla's cap is 70). */
    public static final int MAX_BOOS = 35;
    /** No new wild Asguh comes while this many are around the player. */
    public static final int MAX_ASGUHS = 6;
    /** "Around the player": this far (the distance at which a wild one despawns). */
    public static final double BEREIK = 128;

    /**
     * The extra spawn rule of the three wild Mikas of the Guhbarbecuether (and-ed with their own rule). Only the natural
     * spawner in the open of the Guhbarbecuether is held back: spawners, eggs, commands, structures and every other
     * dimension are not.
     */
    public static <T extends Mob> boolean mikaMagKomen(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            RandomSource random) {
        if (reason != EntitySpawnReason.NATURAL) {
            return true;
        }
        ServerLevel wereld = level.getLevel();
        if (wereld.dimension() != BarbecuetherFeature.BARBECUETHER || inEigenGebouw(wereld, pos)) {
            return true;
        }
        return !vol(wereld, pos, BEREIK, MAX_BOOS);
    }

    /** Are there {@code max} or more aggressive mobs within {@code bereik} blocks of the player nearest to this spot? */
    public static boolean vol(ServerLevel level, BlockPos pos, double bereik, int max) {
        return rondSpeler(level, pos, bereik, Drukte::isBoos) >= max;
    }

    /** Does a structure bring its own monsters on this spot (its spawn override, like the Nether fortress)? */
    public static boolean inEigenGebouw(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        // (without an override the generator hands back the biome's own list, the very same object)
        return level.getChunkSource().getGenerator().getMobsAt(biome, level.structureManager(), MobCategory.MONSTER, pos)
                != biome.value().getMobSettings().getMobs(MobCategory.MONSTER);
    }

    /** An aggressive mob that counts for vanilla's monster cap (one that is kept on purpose does not, like there). */
    public static boolean isBoos(Mob mob) {
        return mob.isAlive() && mob.getType().getCategory() == MobCategory.MONSTER && !mob.isPersistenceRequired() && !mob.requiresCustomPersistence();
    }

    /** A wild Asguh (nobody's). */
    public static boolean isWildeAsguh(Mob mob) {
        return mob.isAlive() && mob instanceof GuhEntity guh && SpiesburchtEvents.isAsguh(guh) && !guh.isTame();
    }

    /** May the natural spawner bring another wild Asguh on this spot? */
    public static boolean asguhMagErbij(ServerLevel level, BlockPos pos) {
        return rondSpeler(level, pos, BEREIK, Drukte::isWildeAsguh) < MAX_ASGUHS;
    }

    /** How many of these mobs are within {@code bereik} blocks of the player nearest to this spot (of the spot itself without players). */
    public static int rondSpeler(ServerLevel level, BlockPos pos, double bereik, Predicate<Mob> wie) {
        Player speler = level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, -1.0, false);
        return tel(level, speler, Vec3.atBottomCenterOf(pos), bereik, wie);
    }

    /** How many of these mobs are within {@code bereik} blocks (a box) of this player, or of {@code plek} when there is none. */
    public static int tel(ServerLevel level, @Nullable Player speler, Vec3 plek, double bereik, Predicate<Mob> wie) {
        Vec3 midden = speler == null ? plek : speler.position();
        return level.getEntities(EntityTypeTest.forClass(Mob.class), new AABB(midden, midden).inflate(bereik), wie).size();
    }

    private Drukte() {
    }
}
