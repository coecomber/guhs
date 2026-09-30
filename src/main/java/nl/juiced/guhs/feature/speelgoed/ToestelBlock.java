package nl.juiced.guhs.feature.speelgoed;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A play thing that takes up more than one block (glijbaantje, wip, schommel): the controller block (this) plus
 * invisible {@link SpeelDeelBlock} parts. Everything is described in LOCAL coordinates, as if the toy faces north
 * (its front at -z): x to the right, y up, z to the back; offsets in blocks from the controller, shapes in model pixels
 * (the controller block is 0..16, the model spans -16..32). {@link #wereld} turns local into world coordinates.
 * <p>
 * Guhs (and players) sit on it with a {@link ZitjeEntity} per seat ("plek"); {@link #zitPlek} says where that seat is
 * at a moment in time (the same on server and client, so the ride is smooth).
 */
public abstract class ToestelBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private final Map<Direction, Map<Long, VoxelShape>> vormen = new EnumMap<>(Direction.class);

    protected ToestelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // =====================================================================================================================
    // the toy's description (local, facing north)
    // =====================================================================================================================

    /** The parts' offsets (blocks, local), without the controller. */
    protected abstract List<int[]> delen();

    /** The collision boxes in model pixels (local): {x0, y0, z0, x1, y1, z1}. */
    protected abstract List<double[]> botsing();

    /** The outline boxes (for clicking), model pixels; default the collision boxes. */
    protected List<double[]> omlijning() {
        return botsing();
    }

    /** The speeltje id (lang gui.guhs.speeltje.&lt;id&gt;, favourites, the SPEELTJE moment). */
    public abstract String speeltje();

    /** How many seats. */
    public abstract int plekken();

    /** Where seat plek is at this time (absolute game time + partial tick), LOCAL blocks; null = the ride is over. */
    @Nullable
    protected abstract Vec3 zitLokaal(Level level, BlockPos pos, BlockState state, ZitjeEntity zitje, float tijd);

    /** Where someone on seat plek looks (LOCAL direction, horizontal), at this time. */
    protected Vec3 kijkLokaal(Level level, BlockPos pos, BlockState state, ZitjeEntity zitje, float tijd) {
        return new Vec3(0, 0, -1);
    }

    /** Where a guh walks to before it gets on seat plek (LOCAL). */
    public abstract Vec3 instapLokaal(int plek);

    /** Where you get off seat plek (LOCAL). */
    public abstract Vec3 uitstapLokaal(int plek);

    /** A right-click by a player (on the toy or any of its parts). */
    protected abstract InteractionResult gebruik(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player);

    /** A guh on seat plek, every server tick of its ride (sounds, particles, keeping the swing going). */
    protected void rijdt(ServerLevel level, BlockPos pos, BlockState state, ZitjeEntity zitje, LivingEntity rijder) {
    }

    // =====================================================================================================================
    // local <-> world
    // =====================================================================================================================

    /** A part's world position. */
    public static BlockPos wereld(BlockPos c, Direction f, int x, int y, int z) {
        Direction rechts = f.getClockWise(), achter = f.getOpposite();
        return c.offset(rechts.getStepX() * x + achter.getStepX() * z, y, rechts.getStepZ() * x + achter.getStepZ() * z);
    }

    /** A local point (blocks, the controller block spans 0..1) in the world. */
    public static Vec3 wereld(BlockPos c, Direction f, Vec3 lokaal) {
        Direction rechts = f.getClockWise(), achter = f.getOpposite();
        double x = lokaal.x - 0.5, z = lokaal.z - 0.5;
        return new Vec3(c.getX() + 0.5 + rechts.getStepX() * x + achter.getStepX() * z, c.getY() + lokaal.y,
                c.getZ() + 0.5 + rechts.getStepZ() * x + achter.getStepZ() * z);
    }

    /** A local direction in the world. */
    public static Vec3 richting(Direction f, Vec3 lokaal) {
        Direction rechts = f.getClockWise(), achter = f.getOpposite();
        return new Vec3(rechts.getStepX() * lokaal.x + achter.getStepX() * lokaal.z, lokaal.y, rechts.getStepZ() * lokaal.x + achter.getStepZ() * lokaal.z);
    }

    /** All the blocks of a toy (controller first). */
    public List<BlockPos> blokken(BlockPos c, Direction f) {
        List<BlockPos> out = new ArrayList<>();
        out.add(c);
        for (int[] d : delen()) {
            out.add(wereld(c, f, d[0], d[1], d[2]));
        }
        return out;
    }

    public Vec3 zitPlekWereld(Level level, BlockPos pos, BlockState state, ZitjeEntity zitje, float tijd) {
        Vec3 l = zitLokaal(level, pos, state, zitje, tijd);
        return l == null ? null : wereld(pos, state.getValue(FACING), l);
    }

    public float kijkYaw(Level level, BlockPos pos, BlockState state, ZitjeEntity zitje, float tijd) {
        Vec3 d = richting(state.getValue(FACING), kijkLokaal(level, pos, state, zitje, tijd));
        return (float) (Math.toDegrees(Math.atan2(-d.x, d.z)));
    }

    public Vec3 instap(BlockPos pos, BlockState state, int plek) {
        return wereld(pos, state.getValue(FACING), instapLokaal(plek));
    }

    public Vec3 uitstap(BlockPos pos, BlockState state, int plek) {
        return wereld(pos, state.getValue(FACING), uitstapLokaal(plek));
    }

    /** The shape of one block of the toy (offset in local blocks), turned to face f. */
    VoxelShape vorm(Direction f, int dx, int dy, int dz, boolean botsen) {
        long key = ((long) (dx + 8) << 16) | ((long) (dy + 8) << 8) | (dz + 8) | (botsen ? 1L << 40 : 0);
        return vormen.computeIfAbsent(f, k -> new ConcurrentHashMap<>()).computeIfAbsent(key, k -> {
            VoxelShape s = Shapes.empty();
            for (double[] b : botsen ? botsing() : omlijning()) {
                double x0 = Math.max(b[0], dx * 16), y0 = Math.max(b[1], dy * 16), z0 = Math.max(b[2], dz * 16);
                double x1 = Math.min(b[3], dx * 16 + 16), y1 = Math.min(b[4], dy * 16 + 16), z1 = Math.min(b[5], dz * 16 + 16);
                if (x1 - x0 < 0.01 || y1 - y0 < 0.01 || z1 - z0 < 0.01) {
                    continue;
                }
                s = Shapes.or(s, draai(f, x0 - dx * 16, y0 - dy * 16, z0 - dz * 16, x1 - dx * 16, y1 - dy * 16, z1 - dz * 16));
            }
            return s.optimize();
        });
    }

    /** A box in block pixels (local, facing north) turned to face f (around the block's middle). */
    static VoxelShape draai(Direction f, double x0, double y0, double z0, double x1, double y1, double z1) {
        double ax0, az0, ax1, az1;
        switch (f) {
            case EAST -> {
                ax0 = 16 - z1; ax1 = 16 - z0; az0 = x0; az1 = x1;
            }
            case SOUTH -> {
                ax0 = 16 - x1; ax1 = 16 - x0; az0 = 16 - z1; az1 = 16 - z0;
            }
            case WEST -> {
                ax0 = z0; ax1 = z1; az0 = 16 - x1; az1 = 16 - x0;
            }
            default -> {
                ax0 = x0; ax1 = x1; az0 = z0; az1 = z1;
            }
        }
        return Block.box(ax0, y0, az0, ax1, y1, az1);
    }

    // =====================================================================================================================
    // the block
    // =====================================================================================================================

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        for (BlockPos p : blokken(context.getClickedPos(), facing)) {
            if (!level.isInWorldBounds(p) || !level.getBlockState(p).canBeReplaced(context)) {
                return null;   // (not enough room for the whole toy)
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        zetDelen(level, pos, state);
    }

    /** Places the parts (also for tests: {@code level.setBlock(controller) + zetDelen}). */
    public void zetDelen(Level level, BlockPos pos, BlockState state) {
        BlockState deel = SpeelgoedFeature.DEEL.get().defaultBlockState();
        Direction f = state.getValue(FACING);
        for (int[] d : delen()) {
            BlockPos p = wereld(pos, f, d[0], d[1], d[2]);
            level.setBlock(p, SpeelDeelBlock.voor(deel, pos, p), 3);
        }
    }

    /** Builds the whole toy (tests, AutoCheck). */
    public static void bouw(Level level, BlockPos pos, ToestelBlock blok, Direction facing) {
        BlockState state = blok.defaultBlockState().setValue(FACING, facing);
        level.setBlock(pos, state, 3);
        blok.zetDelen(level, pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            for (BlockPos p : blokken(pos, state.getValue(FACING))) {
                if (!p.equals(pos) && level.getBlockState(p).getBlock() instanceof SpeelDeelBlock) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            for (ZitjeEntity z : level.getEntitiesOfClass(ZitjeEntity.class, new AABB(pos).inflate(3), z -> z.toestel().equals(pos))) {
                z.klaar(false);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return vorm(state.getValue(FACING), 0, 0, 0, false);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return vorm(state.getValue(FACING), 0, 0, 0, true);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        return gebruik(sl, pos, state, sp);
    }

    // =====================================================================================================================
    // seats
    // =====================================================================================================================

    /** The zitje on seat plek of the toy at pos, or null. */
    @Nullable
    public static ZitjeEntity zitje(Level level, BlockPos pos, int plek) {
        for (ZitjeEntity z : level.getEntitiesOfClass(ZitjeEntity.class, new AABB(pos).inflate(4), z -> z.toestel().equals(pos))) {
            if (z.plek() == plek && !z.isRemoved()) {
                return z;
            }
        }
        return null;
    }

    /** A free seat (no zitje on it yet), or -1. */
    public int vrijePlek(Level level, BlockPos pos) {
        for (int i = 0; i < plekken(); i++) {
            if (zitje(level, pos, i) == null) {
                return i;
            }
        }
        return -1;
    }

    /** How many seats are taken. */
    public int bezet(Level level, BlockPos pos) {
        int n = 0;
        for (int i = 0; i < plekken(); i++) {
            ZitjeEntity z = zitje(level, pos, i);
            if (z != null && z.isVehicle()) {
                n++;
            }
        }
        return n;
    }
}
