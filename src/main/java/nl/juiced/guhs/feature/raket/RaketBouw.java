package nl.juiced.guhs.feature.raket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.Guhs;

/**
 * What the Kaasraket looks like, stage by stage, as blocks around (above) its Lanceerplatform (offsets from the platform;
 * the platform itself stays at 0,0,0). A round body of radius 2 (5 x 5 without corners) from y 2 to 11, four fins, a nose
 * cone with guh ears up to y 15:
 * <ol>
 *   <li>romp: the white body with pink bands, portholes, a guh face and the four fins;</li>
 *   <li>motor: the engine floor (y 1) with four smoking exhausts;</li>
 *   <li>brandstof: two yellow cheese tanks on the sides and a cheese band round the middle;</li>
 *   <li>bemanning: the cockpit floor, the cockpit windows, the nose cone and the ears (the crew sits in the cockpit).</li>
 * </ol>
 * {@link #plaats} puts a stage (again: also to repair one that someone broke); {@link #spook} shows the next one as particles.
 */
public final class RaketBouw {
    /** Where the crew sits (offsets): in the cockpit, on its floor at y 9. */
    public static final List<BlockPos> STOELEN = List.of(new BlockPos(-1, 9, 0), new BlockPos(1, 9, 0), new BlockPos(0, 9, -1));
    /** The top of the rocket (for the camera of the launch). */
    public static final int TOP = 15;

    private static final int R = 2;

    private RaketBouw() {
    }

    private static boolean inLijf(int x, int z) {
        return Math.abs(x) <= R && Math.abs(z) <= R && !(Math.abs(x) == R && Math.abs(z) == R);
    }

    private static boolean rand(int x, int z) {
        return inLijf(x, z) && !(Math.abs(x) < R && Math.abs(z) < R);
    }

    private static BlockState guhs(String id, BlockState anders) {
        Block b = BuiltInRegistries.BLOCK.getValue(Guhs.id(id));
        return b == Blocks.AIR ? anders : b.defaultBlockState();
    }

    /** The blocks of one stage (1..4): offset -> state (in build order: bottom first). */
    public static Map<BlockPos, BlockState> stap(int stap) {
        Map<BlockPos, BlockState> m = new LinkedHashMap<>();
        BlockState wit = Blocks.WHITE_CONCRETE.defaultBlockState(), roze = Blocks.PINK_CONCRETE.defaultBlockState();
        BlockState raam = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        switch (stap) {
            case 1 -> {
                for (int y = 2; y <= 11; y++) {
                    for (int x = -R; x <= R; x++) {
                        for (int z = -R; z <= R; z++) {
                            if (!rand(x, z)) {
                                continue;
                            }
                            boolean band = y == 3 || y == 8;
                            boolean patrijspoort = y >= 5 && y <= 6 && (x == 0 || z == 0);
                            m.put(new BlockPos(x, y, z), patrijspoort ? raam : band ? roze : wit);
                        }
                    }
                }
                // a guh face on the south side (y 10), the fins (pink, three high, one out from the body)
                m.put(new BlockPos(0, 10, R), guhs("knuffelsteen_gezicht", roze));
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    for (int y = 1; y <= 3; y++) {
                        m.put(new BlockPos(d.getStepX() * (R + 1), y, d.getStepZ() * (R + 1)), roze);
                    }
                    m.put(new BlockPos(d.getStepX() * (R + 2), 1, d.getStepZ() * (R + 2)), roze);
                }
            }
            case 2 -> {
                for (int x = -R; x <= R; x++) {
                    for (int z = -R; z <= R; z++) {
                        if (!inLijf(x, z)) {
                            continue;
                        }
                        boolean uitlaat = Math.abs(x) == 1 && Math.abs(z) == 1;
                        m.put(new BlockPos(x, 1, z), uitlaat ? Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true)
                                : Blocks.IRON_BLOCK.defaultBlockState());
                    }
                }
            }
            case 3 -> {
                BlockState kaas = guhs("block_of_kaasknabbels", Blocks.YELLOW_CONCRETE.defaultBlockState());
                for (int x = -R; x <= R; x++) {
                    for (int z = -R; z <= R; z++) {
                        if (rand(x, z) && !(x == 0 || z == 0)) {
                            m.put(new BlockPos(x, 4, z), kaas);
                        }
                    }
                }
                for (int s : new int[]{-1, 1}) {
                    for (int y = 2; y <= 7; y++) {
                        m.put(new BlockPos(s * (R + 1), y + 2, 0), y == 7 ? Blocks.YELLOW_CONCRETE.defaultBlockState() : kaas);
                    }
                }
            }
            case 4 -> {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        m.put(new BlockPos(x, 8, z), wit);                                   // the cockpit floor
                    }
                }
                for (int x = -R; x <= R; x++) {
                    for (int z = -R; z <= R; z++) {
                        if (rand(x, z) && (x == 0 || z == 0)) {
                            m.put(new BlockPos(x, 9, z), raam);                             // the cockpit windows
                        }
                    }
                }
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        m.put(new BlockPos(x, 12, z), roze);
                        if (x == 0 || z == 0) {
                            m.put(new BlockPos(x, 13, z), roze);
                        }
                    }
                }
                m.put(new BlockPos(0, 14, 0), Blocks.PINK_WOOL.defaultBlockState());
                m.put(new BlockPos(-1, 14, 0), Blocks.WHITE_WOOL.defaultBlockState());         // the guh ears
                m.put(new BlockPos(1, 14, 0), Blocks.WHITE_WOOL.defaultBlockState());
                m.put(new BlockPos(-1, 15, 0), Blocks.PINK_WOOL.defaultBlockState());
                m.put(new BlockPos(1, 15, 0), Blocks.PINK_WOOL.defaultBlockState());
            }
            default -> {
            }
        }
        return m;
    }

    /** Builds stage {@code stap} above the platform at {@code platform} (only where it's air or something of the rocket). */
    public static int plaats(ServerLevel level, BlockPos platform, int stap, boolean feest) {
        int n = 0;
        java.util.Set<BlockPos> eerder = new java.util.HashSet<>(blokken(stap - 1));   // (a later stage may repaint an earlier one)
        for (Map.Entry<BlockPos, BlockState> e : stap(stap).entrySet()) {
            BlockPos p = platform.offset(e.getKey());
            BlockState nu = level.getBlockState(p);
            if (nu.equals(e.getValue())) {
                continue;
            }
            if (!nu.isAir() && !nu.canBeReplaced() && !eerder.contains(e.getKey())) {
                continue;
            }
            BlockState s = e.getValue();
            if (s.hasProperty(HorizontalDirectionalBlock.FACING)) {
                s = s.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
            }
            level.setBlock(p, s, 3);
            n++;
        }
        if (feest) {
            double y = platform.getY() + 2 + stap * 3;
            level.sendParticles(ParticleTypes.CLOUD, platform.getX() + 0.5, y, platform.getZ() + 0.5, 40, 2, 2, 2, 0.02);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, platform.getX() + 0.5, y, platform.getZ() + 0.5, 30, 2.5, 3, 2.5, 0.1);
            level.playSound(null, platform.above(2), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7f, 1.3f);
            level.playSound(null, platform.above(4), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8f, 1.2f);
        }
        return n;
    }

    /** All rocket blocks of stages 1..t (offsets). */
    public static List<BlockPos> blokken(int tot) {
        List<BlockPos> l = new ArrayList<>();
        for (int s = 1; s <= tot; s++) {
            l.addAll(stap(s).keySet());
        }
        return l;
    }

    /** A ghost of the next stage: little white sparks where its blocks will come. */
    public static void spook(ServerLevel level, BlockPos platform, int volgende) {
        if (volgende < 1 || volgende > 4) {
            return;
        }
        int i = 0;
        for (BlockPos o : stap(volgende).keySet()) {
            if ((i++ + (int) (level.getGameTime() / 40)) % 3 != 0) {
                continue;
            }
            BlockPos p = platform.offset(o);
            if (level.getBlockState(p).isAir()) {
                level.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.1, 0.1, 0.1, 0);
            }
        }
    }
}
