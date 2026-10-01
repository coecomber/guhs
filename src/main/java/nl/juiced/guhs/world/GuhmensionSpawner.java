package nl.juiced.guhs.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Vanilla caps animals at ~10 per player, which is far too few guhs for the Guhmension.
 * This keeps ~{@link #GUHS_PER_PLAYER} guhs around every player there (wild ones despawn again when you walk away,
 * see GuhEntity#removeWhenFarAway).
 */
public final class GuhmensionSpawner {
    public static final int GUHS_PER_PLAYER = 50;
    private static final int CHECK_EVERY_TICKS = 20;
    private static final double COUNT_RADIUS = 48;
    /** 1.1.1: never more than this many guhs within the despawn distance (128) of a player: the spawner waits. */
    public static final int MAX_GUHS_WIDE = 150;
    private static final double WIDE_RADIUS = 128;
    /** 1.1.1: never more wild (come-and-go) guhs in the whole Guhmension than this (plus this much per player there). */
    public static final int MAX_WILD_BASE = 300;
    public static final int MAX_WILD_PER_PLAYER = 150;
    private static final int CULL_EVERY_TICKS = 20 * 30;
    /** A wild guh this close to a player is never tidied away (you'd see it vanish). */
    private static final double CULL_SAFE_DISTANCE = 64;

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != ModDimensions.GUHMENSION
                || level.getGameTime() % CHECK_EVERY_TICKS != 0 || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)) {
            return;
        }
        if (level.getGameTime() % CULL_EVERY_TICKS == 0) {
            tidyUp(level);
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            int nearby = level.getEntitiesOfClass(GuhEntity.class, new AABB(player.blockPosition()).inflate(COUNT_RADIUS)).size();
            if (nearby < GUHS_PER_PLAYER
                    && level.getEntitiesOfClass(GuhEntity.class, new AABB(player.blockPosition()).inflate(WIDE_RADIUS)).size() < MAX_GUHS_WIDE) {
                spawnGroup(level, player, level.getRandom());
            }
        }
    }

    /**
     * 1.1.1: a safety net: when there are more wild come-and-go guhs in the Guhmension than {@link #maxWild} (e.g. loaded
     * from an older world, where they were still saved), the ones far from every player go (farthest first). Returns how
     * many went.
     */
    public static int tidyUp(ServerLevel level) {
        List<GuhEntity> wild = new ArrayList<>();
        level.getEntities(ModEntities.GUH.get(), GuhEntity::isKomEnGaGuh).forEach(e -> wild.add((GuhEntity) e));
        int max = maxWild(level);
        if (wild.size() <= max) {
            return 0;
        }
        List<ServerPlayer> players = level.players();
        record Afstand(GuhEntity guh, double d) {
        }
        List<Afstand> ver = new ArrayList<>();
        for (GuhEntity guh : wild) {
            double d = Double.MAX_VALUE;
            for (ServerPlayer p : players) {
                d = Math.min(d, p.distanceToSqr(guh));
            }
            if (d > CULL_SAFE_DISTANCE * CULL_SAFE_DISTANCE) {
                ver.add(new Afstand(guh, d));
            }
        }
        ver.sort(Comparator.comparingDouble(Afstand::d).reversed());
        int weg = Math.min(wild.size() - max, ver.size());
        for (int i = 0; i < weg; i++) {
            ver.get(i).guh().discard();
        }
        return weg;
    }

    public static int maxWild(ServerLevel level) {
        return MAX_WILD_BASE + MAX_WILD_PER_PLAYER * level.players().size();
    }

    private static void spawnGroup(ServerLevel level, ServerPlayer player, RandomSource random) {
        // somewhere 16-40 blocks away, so they don't pop in right in front of you
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 16 + random.nextDouble() * 24;
        int x = (int) (player.getX() + Math.cos(angle) * dist);
        int z = (int) (player.getZ() + Math.sin(angle) * dist);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return;
        }
        int groupSize = 2 + random.nextInt(3);
        for (int i = 0; i < groupSize; i++) {
            int gx = x + random.nextInt(5) - 2;
            int gz = z + random.nextInt(5) - 2;
            BlockPos pos = new BlockPos(gx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz), gz);
            if (!GuhEntity.checkGuhSpawnRules(ModEntities.GUH.get(), level, EntitySpawnReason.NATURAL, pos, random)
                    || nl.juiced.guhs.quest.VerstopGame.inHouse(level, pos) || nl.juiced.guhs.feature.beauty.BeautyProtection.inTheatre(level, pos)
                    || !nl.juiced.guhs.feature.weerder.WeerderFeature.wildeGuhMag(level, pos)) {   // (1.2.0: a Wilde-guhweerder's area)
                continue;
            }
            if (i == 0) {
                nl.juiced.guhs.quest.Reisguh.maybeWander(level, pos, random);   // (very rarely: a Reisguh)
            }
            GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
            if (guh == null) {
                return;
            }
            guh.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360f, 0);
            if (level.noCollision(guh)) {
                guh.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
                level.addFreshEntity(guh);
            }
        }
    }

    private GuhmensionSpawner() {
    }
}
