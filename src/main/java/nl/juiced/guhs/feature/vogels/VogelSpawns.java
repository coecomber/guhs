package nl.juiced.guhs.feature.vogels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Where and when the birds spawn (the biomes come from the biome modifiers data/guhs/neoforge/biome_modifier/vogels_*.json,
 * written by tools/features/vogels.py). The flocks mostly appear with the world (chunk generation); later a few more when
 * there are not many around.
 * <ul>
 *   <li>under the open sky (full sky light: on the ground or a tree top, not in a cave or under leaves);</li>
 *   <li>on leaves or a solid block; the zeemeeuwtje also on sand and on the sea;</li>
 *   <li>the day birds only by day, the guh-uiltje only at night (with the world they may come at any time: an owl
 *   that appears by day just sleeps on its perch);</li>
 *   <li>never more than {@link #max(EntityType)} of a kind within 32 blocks.</li>
 * </ul>
 */
public final class VogelSpawns {
    public static boolean check(EntityType<? extends Vogeltje> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (MobSpawnType.isSpawner(spawnType) || spawnType == MobSpawnType.STRUCTURE || spawnType == MobSpawnType.SPAWN_EGG
                || spawnType == MobSpawnType.COMMAND || spawnType == MobSpawnType.BUCKET) {
            return true;
        }
        if (spawnType == MobSpawnType.NATURAL && isNacht(level) != (type == VogelsFeature.GUH_UILTJE.get())) {
            return false;
        }
        return plekOk(type, level, pos) && level.getEntitiesOfClass(Vogeltje.class, new AABB(pos).inflate(32), v -> v.getType() == type).size() < max(type);
    }

    /** The place itself (no time, no crowd): open air on top of the world, on something a bird can sit on. */
    public static boolean plekOk(EntityType<?> type, LevelAccessor level, BlockPos pos) {
        if (!level.canSeeSky(pos)) {          // under the open sky (not in a cave, not under a roof of leaves)
            return false;
        }
        BlockState here = level.getBlockState(pos);
        if (!here.getFluidState().isEmpty() || !here.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        BlockState below = level.getBlockState(pos.below());
        if (type == VogelsFeature.ZEEMEEUWTJE.get()) {
            return below.getFluidState().is(FluidTags.WATER) || below.is(BlockTags.SAND) || below.isFaceSturdy(level, pos.below(), Direction.UP);
        }
        return below.getFluidState().isEmpty() && (below.is(BlockTags.LEAVES) || below.isFaceSturdy(level, pos.below(), Direction.UP));
    }

    /** At most this many of a kind within 32 blocks. */
    public static int max(EntityType<?> type) {
        if (type == VogelsFeature.PLUISVINKJE.get()) {
            return 12;
        }
        if (type == VogelsFeature.ZEEMEEUWTJE.get()) {
            return 10;
        }
        if (type == VogelsFeature.GUH_UILTJE.get()) {
            return 3;
        }
        return 8;
    }

    /** Night in this level (for the natural spawns; chunk generation doesn't ask). */
    public static boolean isNacht(LevelAccessor level) {
        if (level instanceof ServerLevelAccessor s) {
            return s.getLevel().isNight();
        }
        return false;
    }

    // --- the gentle top-up around players ---------------------------------------------------------------------------
    /** Every this many ticks per player a new little flock may come (when there are few birds around). */
    public static final int AANVUL_TIJD = 600;
    /** No top-up when this many birds are already within 64 blocks of the player. */
    public static final int VOL = 20;

    /**
     * Lets a new little flock fly in 24 to 48 blocks from the player, in the biome's own birds (the biome modifiers), when
     * there are few around: vanilla only spawns animals with the world (and the guhs' spawn weights stay untouched this way).
     * Returns the birds that came (tests).
     */
    public static java.util.List<Vogeltje> aanvullen(net.minecraft.server.level.ServerPlayer player, RandomSource random) {
        java.util.List<Vogeltje> nieuw = new java.util.ArrayList<>();
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        if (level.getEntitiesOfClass(Vogeltje.class, player.getBoundingBox().inflate(64)).size() >= VOL) {
            return nieuw;
        }
        double a = random.nextDouble() * Math.PI * 2, d = 24 + random.nextInt(25);
        int x = (int) Math.floor(player.getX() + Math.cos(a) * d), z = (int) Math.floor(player.getZ() + Math.sin(a) * d);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return nieuw;
        }
        BlockPos pos = new BlockPos(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z), z);
        var spawners = level.getBiome(pos).value().getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.CREATURE).unwrap().stream()
                .filter(sd -> sd.type == VogelsFeature.PLUISVINKJE.get() || sd.type == VogelsFeature.KAASMEESJE.get()
                        || sd.type == VogelsFeature.GUH_UILTJE.get() || sd.type == VogelsFeature.ZEEMEEUWTJE.get()).toList();
        if (spawners.isEmpty()) {
            return nieuw;
        }
        int total = spawners.stream().mapToInt(sd -> sd.getWeight().asInt()).sum();
        int pick = random.nextInt(Math.max(1, total));
        var gekozen = spawners.get(0);
        for (var sd : spawners) {
            pick -= sd.getWeight().asInt();
            if (pick < 0) {
                gekozen = sd;
                break;
            }
        }
        @SuppressWarnings("unchecked")
        EntityType<? extends Vogeltje> type = (EntityType<? extends Vogeltje>) gekozen.type;
        if (!check(type, level, MobSpawnType.NATURAL, pos, random)) {
            return nieuw;
        }
        int n = gekozen.minCount + random.nextInt(Math.max(1, gekozen.maxCount - gekozen.minCount + 1));
        for (int i = 0; i < n; i++) {
            int px = x + random.nextInt(5) - 2, pz = z + random.nextInt(5) - 2;
            BlockPos p = new BlockPos(px, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, px, pz), pz);
            if (!plekOk(type, level, p)) {
                continue;
            }
            Vogeltje v = type.create(level);
            if (v == null) {
                continue;
            }
            v.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360f, 0);
            v.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.NATURAL, null);
            v.zetThuis(p);
            level.addFreshEntity(v);
            nieuw.add(v);
        }
        return nieuw;
    }

    private VogelSpawns() {
    }
}
