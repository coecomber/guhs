package nl.juiced.guhs.world;

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

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != ModDimensions.GUHMENSION
                || level.getGameTime() % CHECK_EVERY_TICKS != 0 || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            int nearby = level.getEntitiesOfClass(GuhEntity.class, new AABB(player.blockPosition()).inflate(COUNT_RADIUS)).size();
            if (nearby < GUHS_PER_PLAYER) {
                spawnGroup(level, player, level.getRandom());
            }
        }
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
                    || nl.juiced.guhs.quest.VerstopGame.inHouse(level, pos) || nl.juiced.guhs.feature.beauty.BeautyProtection.inTheatre(level, pos)) {
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
