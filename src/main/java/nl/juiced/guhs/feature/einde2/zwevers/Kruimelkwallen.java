package nl.juiced.guhs.feature.einde2.zwevers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.world.WildeDieren;

/**
 * einde2, slice zwevers: where the swarms of Kruimelkwallen come. By themselves only in the Kruimelwolken; another slice
 * names more areas here (the Kwallenvelden of Bikinibodem, whatever biome that town lies in).
 * <p>
 * The Guheinde spawns nothing by itself, so the swarms come by a gentle top-up around every player ({@link #aanvullen}):
 * every {@link #TIJD} ticks, 1 in {@link #KANS}, a swarm of {@link #MIN}-{@link #MAX} drifts in 24 to 48 blocks away, when
 * fewer than {@link #VOL} swarms are within {@link #BEREIK} blocks. They come and go (WildeDieren: never saved, gone when
 * everybody is far away).
 */
public final class Kruimelkwallen {
    public static final int TIJD = 600, KANS = 3;
    public static final int MIN = 4, MAX = 7;
    /** No new swarm when this many are within {@link #BEREIK} blocks of the player. */
    public static final int VOL = 2, BEREIK = 96;

    private static final List<BiPredicate<ServerLevel, BlockPos>> OOK = new CopyOnWriteArrayList<>();

    /** Also where this rule says yes the top-up brings swarms (same caps as in the Kruimelwolken). */
    public static void ookIn(BiPredicate<ServerLevel, BlockPos> waar) {
        OOK.add(waar);
    }

    /** Does one of the registered rules name this spot? */
    public static boolean ook(ServerLevel level, BlockPos pos) {
        for (BiPredicate<ServerLevel, BlockPos> waar : OOK) {
            if (waar.test(level, pos)) {
                return true;
            }
        }
        return false;
    }

    /** Is this a spot for a swarm: the Kruimelwolken, or a place another slice named? */
    public static boolean hier(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(Einde2Ids.KRUIMELWOLKEN) || ook(level, pos);
    }

    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !GuheindeFeature.isGuheinde(level) || level.getGameTime() % TIJD != 170
                || !level.getGameRules().get(GameRules.SPAWN_MOBS)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (level.getRandom().nextInt(KANS) == 0) {
                aanvullen(level, player, level.getRandom());
            }
        }
    }

    /** How many different swarms drift within {@link #BEREIK} blocks of this spot. */
    public static int zwermen(ServerLevel level, Vec3 rond) {
        Set<Integer> zwermen = new HashSet<>();
        for (KruimelkwalEntity k : level.getEntities(E2ZweversFeature.KRUIMELKWAL.get(),
                k -> k.isAlive() && WildeDieren.isKomEnGa(k) && k.distanceToSqr(rond) < (double) BEREIK * BEREIK)) {
            zwermen.add(k.zwerm());
        }
        return zwermen.size();
    }

    /** May one more swarm come, with this many around already? */
    public static boolean magErbij(int zwermen) {
        return zwermen < VOL;
    }

    /** One swarm 24 to 48 blocks from this player, when the spot is one for swarms and few are around. Empty: none came. */
    public static List<KruimelkwalEntity> aanvullen(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (player.isSpectator() || !magErbij(zwermen(level, player.position()))) {
            return List.of();
        }
        double a = random.nextDouble() * Math.PI * 2, d = 24 + random.nextInt(25);
        int x = (int) Math.floor(player.getX() + Math.cos(a) * d), z = (int) Math.floor(player.getZ() + Math.sin(a) * d);
        BlockPos plek = plek(level, x, z, player.getBlockY(), random);
        if (plek == null || !hier(level, plek)) {
            return List.of();
        }
        return zwerm(level, plek, random);
    }

    /**
     * Where a swarm hangs in this column: open air one to three blocks above the first ground at or below the player's
     * height (it looks down from 10 blocks above the player, never through a roof), or, where the column is all void
     * (between the crumb islets), around the player's own height.
     */
    @Nullable
    static BlockPos plek(ServerLevel level, int x, int z, int spelerY, RandomSource random) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, 0, z);
        if (!level.hasChunkAt(p)) {
            return null;
        }
        int top = Math.min(level.getMaxY() - 4, spelerY + 10), bodem = Math.max(level.getMinY() + 1, spelerY - 24);
        if (!level.getBlockState(p.setY(top)).isAir()) {
            return null;
        }
        for (int y = top; y >= bodem; y--) {
            if (!level.getBlockState(p.setY(y)).isAir()) {
                return y + 4 > top ? null : new BlockPos(x, y + 2 + random.nextInt(2), z);
            }
        }
        return new BlockPos(x, Math.max(bodem + 2, spelerY - 2 + random.nextInt(6)), z);
    }

    /** A swarm of come-and-go kwallen around this spot. */
    public static List<KruimelkwalEntity> zwerm(ServerLevel level, BlockPos midden, RandomSource random) {
        List<KruimelkwalEntity> uit = new ArrayList<>();
        int n = MIN + random.nextInt(MAX - MIN + 1), nummer = random.nextInt();
        for (int i = 0; i < n * 3 && uit.size() < n; i++) {
            BlockPos pos = midden.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
                continue;
            }
            KruimelkwalEntity kwal = E2ZweversFeature.KRUIMELKWAL.get().create(level, EntitySpawnReason.TRIGGERED);
            if (kwal == null) {
                break;
            }
            kwal.snapTo(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, random.nextFloat() * 360f, 0);
            kwal.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
            kwal.setZwerm(nummer);
            kwal.setThuis(kwal.position(), KruimelkwalEntity.ZWERF);
            WildeDieren.markeer(kwal);
            level.addFreshEntity(kwal);
            uit.add(kwal);
        }
        return uit;
    }

    private Kruimelkwallen() {
    }
}
