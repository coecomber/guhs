package nl.juiced.guhs.feature.guhwinter.hoentjes;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhwinter.GuhwinterZone;
import nl.juiced.guhs.world.ModDimensions;
import nl.juiced.guhs.world.WildeDieren;

/**
 * Where and how often the hoentjes appear in the Guhmension: a rare, gentle top-up around players (like the birds' top-up,
 * {@code VogelSpawns.aanvullen}), no biome modifiers, so the vanilla spawner and the guh spawn weights stay untouched.
 * <ul>
 *   <li>Every {@link #TIJD} ticks per player a 1 in {@link #KANS} chance to look at up to {@link #POGINGEN} random columns
 *       {@link #MIN_AFSTAND}-{@link #MAX_AFSTAND} blocks away;</li>
 *   <li>a column on a <b>water edge</b> (open sky, water and land within 3 blocks) gets a waterhoentje; one that is <b>high,
 *       cold or snowy</b> (snow or ice, the Sneeuwguhtoendra, the Guhpieken/Vadskliffen from y {@link #BERG_Y}, anything from
 *       y {@link #HOOG_Y}) gets an alpine hoentje;</li>
 *   <li>never one when another of that kind is within {@link #NIET_BINNEN} blocks of the player, never more wild ones of a
 *       kind in the Guhmension than {@link #MAX_BASIS} + {@link #MAX_PER_SPELER} per player there, and after a spawn that
 *       player gets no new one of that kind for {@link #AFKOEL} ticks;</li>
 *   <li>never on the winter plaza ({@link #PLEIN}: its snow is decoration, and the story walks there).</li>
 * </ul>
 * They are come-and-go animals ({@link WildeDieren#KOM_EN_GA}): never saved, gone again when everyone is far. A curious
 * explorer along the water or up in the snow meets one every so many minutes; someone who stays home doesn't.
 */
public final class HoentjesSpawner {
    public static final int TIJD = 20 * 30;
    public static final int KANS = 3;
    public static final int POGINGEN = 8;
    public static final int MIN_AFSTAND = 20, MAX_AFSTAND = 48;
    public static final double NIET_BINNEN = 96;
    public static final int MAX_BASIS = 2, MAX_PER_SPELER = 2;
    public static final int AFKOEL = 20 * 60 * 6;
    public static final int BERG_Y = 110, HOOG_Y = 150;
    /** Margin around the winter plaza ({@link GuhwinterZone}) where no wild hoentjes appear. */
    public static final int PLEIN_MARGE = 6;
    /** The winter plaza ({@link GuhwinterZone}, all heights) plus {@link #PLEIN_MARGE}: no wild hoentjes there. */
    public static final AABB PLEIN = new AABB(GuhwinterZone.MIN_X - PLEIN_MARGE, -64, GuhwinterZone.MIN_Z - PLEIN_MARGE,
            GuhwinterZone.MAX_X + 1 + PLEIN_MARGE, 320, GuhwinterZone.MAX_Z + 1 + PLEIN_MARGE);

    private static final ResourceKey<Biome> SNEEUWGUHTOENDRA = ResourceKey.create(Registries.BIOME, Guhs.id("sneeuwguhtoendra"));
    private static final ResourceKey<Biome> GUH_PEAKS = ResourceKey.create(Registries.BIOME, Guhs.id("guh_peaks"));
    private static final ResourceKey<Biome> VADS_CLIFFS = ResourceKey.create(Registries.BIOME, Guhs.id("vads_cliffs"));
    private static final Set<Block> SNEEUW = Set.of(Blocks.SNOW, Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW, Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE);

    /** Per player: the server tick of their last waterhoen / alpine hoentje spawn. */
    private static final Map<UUID, long[]> LAATST = new ConcurrentHashMap<>();

    /** The regular check (from the player tick, every {@link #TIJD} ticks): the dice, then {@link #probeer}. */
    public static void misschien(ServerPlayer player) {
        ServerLevel level = player.level();
        if (player.isSpectator() || level.dimension() != ModDimensions.GUHMENSION
                || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)
                || player.getRandom().nextInt(KANS) != 0) {
            return;
        }
        probeer(player, player.getRandom(), false);
    }

    /**
     * Looks at up to {@link #POGINGEN} columns around the player and lets one hoentje appear on the first fitting one.
     * {@code forceer} (the op command): no cool-down, any dimension. Returns the new hoentje, or null.
     */
    @Nullable
    public static Hoentje probeer(ServerPlayer player, RandomSource random, boolean forceer) {
        ServerLevel level = player.level();
        int nu = level.getServer().getTickCount();
        long[] laatst = LAATST.computeIfAbsent(player.getUUID(), k -> new long[] {-AFKOEL, -AFKOEL});
        for (int i = 0; i < POGINGEN; i++) {
            double a = random.nextDouble() * Math.PI * 2, d = MIN_AFSTAND + random.nextInt(MAX_AFSTAND - MIN_AFSTAND + 1);
            int x = (int) Math.floor(player.getX() + Math.cos(a) * d), z = (int) Math.floor(player.getZ() + Math.sin(a) * d);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (PLEIN.contains(x + 0.5, pos.getY(), z + 0.5)) {
                continue;
            }
            String soort = waterRand(level, pos) ? GuhwinterHoentjes.WATERHOEN : bergSneeuw(level, pos) ? GuhwinterHoentjes.ALPINEHOEN : null;
            if (soort == null) {
                continue;
            }
            int s = GuhwinterHoentjes.WATERHOEN.equals(soort) ? 0 : 1;
            EntityType<? extends Hoentje> type = GuhwinterHoentjes.type(soort);
            if (type == null || !forceer && (nu - laatst[s] < AFKOEL || !ruimte(level, player, type))) {
                continue;
            }
            Hoentje h = type.create(level, EntitySpawnReason.NATURAL);
            if (h == null) {
                continue;
            }
            double y = level.getFluidState(pos.below()).is(FluidTags.WATER) ? pos.getY() - 0.2 : pos.getY();
            h.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360f, 0);
            if (!level.noCollision(h)) {
                continue;
            }
            h.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
            WildeDieren.markeer(h);                          // (come-and-go: never saved, gone when everyone is far)
            level.addFreshEntity(h);
            laatst[s] = nu;
            return h;
        }
        return null;
    }

    /** No other one of this kind near the player, and not too many wild ones in the whole level. */
    static boolean ruimte(ServerLevel level, ServerPlayer player, EntityType<? extends Hoentje> type) {
        if (!level.getEntitiesOfClass(Hoentje.class, player.getBoundingBox().inflate(NIET_BINNEN), h -> h.getType() == type).isEmpty()) {
            return false;
        }
        int spelers = (int) level.players().stream().filter(p -> !p.isSpectator()).count();
        return level.getEntities(type, h -> h.isAlive() && WildeDieren.isKomEnGa(h)).size() < MAX_BASIS + MAX_PER_SPELER * spelers;
    }

    /** Open air on top of the world, with water and dry land both within 3 blocks: the edge of a pond, a lake or a stream. */
    public static boolean waterRand(ServerLevel level, BlockPos pos) {
        if (!level.canSeeSky(pos) || !level.getFluidState(pos).isEmpty() || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        BlockState onder = level.getBlockState(pos.below());
        boolean opWater = level.getFluidState(pos.below()).is(FluidTags.WATER);
        if (!opWater && (onder.is(BlockTags.LEAVES) || !onder.isFaceSturdy(level, pos.below(), Direction.UP))) {
            return false;
        }
        int water = 0, land = 0;
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 1, 3))) {
            if (level.getFluidState(q).is(FluidTags.LAVA)) {
                return false;
            }
            boolean luchtBoven = level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty() && level.getFluidState(q.above()).isEmpty();
            if (!luchtBoven) {
                continue;
            }
            if (level.getFluidState(q).is(FluidTags.WATER)) {
                water++;
            } else if (level.getBlockState(q).isFaceSturdy(level, q, Direction.UP) && !level.getBlockState(q).is(BlockTags.LEAVES)) {
                land++;
            }
        }
        return water >= 2 && land >= 2;
    }

    /** Open air on top of the world, high, cold or snowy (and not on water, leaves or a roof of ice in a cave). */
    public static boolean bergSneeuw(ServerLevel level, BlockPos pos) {
        if (!level.canSeeSky(pos) || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty()) {
            return false;
        }
        BlockState hier = level.getBlockState(pos), onder = level.getBlockState(pos.below());
        if (!hier.getCollisionShape(level, pos).isEmpty() && !hier.is(Blocks.SNOW)) {
            return false;
        }
        if (onder.is(BlockTags.LEAVES) || !(onder.isFaceSturdy(level, pos.below(), Direction.UP) || SNEEUW.contains(onder.getBlock()))) {
            return false;
        }
        if (SNEEUW.contains(hier.getBlock()) || SNEEUW.contains(onder.getBlock()) || pos.getY() >= HOOG_Y) {
            return true;
        }
        var biome = level.getBiome(pos);
        return biome.is(SNEEUWGUHTOENDRA) || pos.getY() >= BERG_Y && (biome.is(GUH_PEAKS) || biome.is(VADS_CLIFFS));
    }

    /** Forgets this player's cool-downs (op command). */
    public static void vergeet(UUID speler) {
        LAATST.remove(speler);
    }

    private HoentjesSpawner() {
    }
}
