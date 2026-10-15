package nl.juiced.guhs.feature.bio.blokkendal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * De guh-dakkrul: the curved piece that turns a roof's eave up. It starts at slab height on the roof side and sweeps up
 * to a tip above the block. FACING is the way the tip points: away from the roof.
 * <p>The shape follows from what is beside it (left and right, seen along FACING), so a builder never has to choose:
 * <ul>
 *   <li>{@code recht}: free on both sides or built in on both: a lip that rises outward along its whole width.</li>
 *   <li>{@code hoek_links / hoek_rechts}: something beside it on ONE side (a slab, a stair, a wall): the end of an eave.
 *       It lies flat against that neighbour and against the roof and sweeps up to the free outer corner.</li>
 *   <li>{@code eind_links / eind_rechts}: the one neighbour is another krul pointing the same way (an eave made of
 *       krullen): it carries that lip on and turns it up along the free side as well, the corner of a hip roof.</li>
 * </ul>
 * Placing: on the SIDE of a block it points away from that block (stuck onto a krul that runs across, it joins that
 * row). On top of or under a block it points away from the one side that has something behind it, else where you look.
 */
public class DakpanHoekBlock extends Block {
    public static final MapCodec<DakpanHoekBlock> CODEC = simpleCodec(DakpanHoekBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Vorm> VORM = EnumProperty.create("vorm", Vorm.class);

    public enum Vorm implements StringRepresentable {
        RECHT("recht"), HOEK_LINKS("hoek_links"), HOEK_RECHTS("hoek_rechts"), EIND_LINKS("eind_links"), EIND_RECHTS("eind_rechts");

        private final String naam;

        Vorm(String naam) {
            this.naam = naam;
        }

        @Override
        public String getSerializedName() {
            return naam;
        }

        boolean links() {
            return this == HOEK_LINKS || this == EIND_LINKS;
        }
    }

    /**
     * The curl in eight bands of two pixels, from the roof side (0) to the tip (7): top and underside in pixels. The same
     * numbers as tools/features/bio_blokken_dal.py (the model); the collision stops at the top of the block.
     */
    public static final int[] BOVEN = {8, 8, 9, 10, 12, 14, 16, 19}, ONDER = {0, 0, 2, 4, 6, 8, 10, 13};

    private static final Map<Direction, VoxelShape> RECHT, HOEK, EIND;

    static {
        List<VoxelShape> recht = new ArrayList<>(), hoek = new ArrayList<>(), eind = new ArrayList<>();
        for (int k = 0; k < 8; k += 2) {       // (two bands at a time: four steps are plenty to walk on)
            int onder = ONDER[k], boven = Math.min(16, BOVEN[k + 1]);
            int ver = 16 - 2 * k, dicht = ver - 4;        // this step: from `ver` (roof side) to `dicht` (tip side) pixels
            // tip to the north: the roof is south (z 16)
            recht.add(Block.box(0, onder, dicht, 16, boven, ver));
            // the corner of north and west, low against the east and the south: bands around the south-east
            hoek.add(Block.box(0, onder, dicht, ver, boven, ver));
            hoek.add(Block.box(dicht, onder, 0, ver, boven, dicht));
            // the corner of north and west, high along both: bands around the north-west
            eind.add(Block.box(dicht, onder, dicht, 16, boven, ver));
            eind.add(Block.box(dicht, onder, ver, ver, boven, 16));
        }
        RECHT = draai(recht);
        HOEK = draai(hoek);
        EIND = draai(eind);
    }

    private static Map<Direction, VoxelShape> draai(List<VoxelShape> boxes) {
        VoxelShape shape = boxes.stream().reduce(Shapes.empty(), Shapes::or);
        return new EnumMap<>(Shapes.rotateHorizontal(shape));
    }

    public DakpanHoekBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(VORM, Vorm.RECHT));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VORM);
    }

    /** The corner a corner piece turns up: between FACING and this direction. */
    public static Direction zijkant(BlockState state) {
        Direction f = state.getValue(FACING);
        return state.getValue(VORM).links() ? f.getCounterClockWise() : f.getClockWise();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction f = state.getValue(FACING);
        Vorm vorm = state.getValue(VORM);
        if (vorm == Vorm.RECHT) {
            return RECHT.get(f);
        }
        // the corner shapes are made for "north and west" (north, links); north and east is that shape a quarter turn on
        Direction draai = vorm.links() ? f : f.getClockWise();
        return (vorm == Vorm.HOEK_LINKS || vorm == Vorm.HOEK_RECHTS ? HOEK : EIND).get(draai);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    private static boolean bezet(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(level, pos).isEmpty();
    }

    /** The shape for a krul that points to `facing` here. */
    public static Vorm vorm(BlockGetter level, BlockPos pos, Direction facing) {
        BlockPos l = pos.relative(facing.getCounterClockWise()), r = pos.relative(facing.getClockWise());
        boolean links = bezet(level, l), rechts = bezet(level, r);
        if (links == rechts) {
            return Vorm.RECHT;
        }
        BlockState buur = level.getBlockState(links ? l : r);
        boolean krul = buur.getBlock() instanceof DakpanHoekBlock && buur.getValue(FACING) == facing;
        if (links) {
            return krul ? Vorm.EIND_RECHTS : Vorm.HOEK_RECHTS;       // (it turns up at the FREE side)
        }
        return krul ? Vorm.EIND_LINKS : Vorm.HOEK_LINKS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction vlak = context.getClickedFace();
        Direction facing = null;
        if (vlak.getAxis().isHorizontal()) {
            facing = vlak;
            BlockState tegen = level.getBlockState(pos.relative(vlak.getOpposite()));
            if (tegen.getBlock() instanceof DakpanHoekBlock && tegen.getValue(FACING).getAxis() != vlak.getAxis()) {
                facing = tegen.getValue(FACING);        // stuck onto the side of a krul: the same eave goes on
            }
        } else {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (bezet(level, pos.relative(d.getOpposite())) && !bezet(level, pos.relative(d))) {
                    if (facing != null) {
                        facing = null;      // more than one side fits: let the player's look decide
                        break;
                    }
                    facing = d;
                }
            }
            if (facing == null) {
                facing = context.getHorizontalDirection();
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(VORM, vorm(level, pos, facing));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
            BlockState neighbor, RandomSource random) {
        if (direction.getAxis().isHorizontal() && direction.getAxis() != state.getValue(FACING).getAxis()) {
            return state.setValue(VORM, vorm(level, pos, state.getValue(FACING)));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        Vorm v = state.getValue(VORM);
        Vorm gespiegeld = switch (v) {
            case HOEK_LINKS -> Vorm.HOEK_RECHTS;
            case HOEK_RECHTS -> Vorm.HOEK_LINKS;
            case EIND_LINKS -> Vorm.EIND_RECHTS;
            case EIND_RECHTS -> Vorm.EIND_LINKS;
            default -> v;
        };
        return state.rotate(mirror.getRotation(state.getValue(FACING))).setValue(VORM, gespiegeld);
    }
}
