package nl.juiced.guhs.feature.bedden;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A family bed (1.8.0): one item places the whole bed of several blocks (its cells, {@link #DEEL} = the cell's index in the bed's
 * frame), breaking one cell breaks the whole bed. The shapes and the vakjes come from {@link FamilieVormen} (written by
 * tools/features/bedden.py from the family bed renders); critters sleep in it by themselves ({@link BedSlaapGoal}).
 */
public class FamilieBedBlock extends HorizontalDirectionalBlock {
    /** The cell's index: x + sx * (z + sz * y), in the bed's own frame (drawn facing north). */
    public static final IntegerProperty DEEL = IntegerProperty.create("deel", 0, 127);

    public static final MapCodec<FamilieBedBlock> CODEC = simpleCodec(p -> new FamilieBedBlock(p, "guh_hoofdbank"));

    private final String id;
    private final FamilieVormen.Vorm vorm;
    private final Map<Integer, Map<Direction, VoxelShape>> vormen = new java.util.HashMap<>();

    public FamilieBedBlock(Properties properties, String id) {
        super(properties);
        this.id = id;
        this.vorm = FamilieVormen.VORMEN.get(id);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DEEL, ankerDeel()));
        for (int deel : vorm.cellen()) {
            VoxelShape s = Shapes.empty();
            for (double[] b : vorm.blokjes(deel)) {
                s = Shapes.or(s, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
            }
            vormen.put(deel, s.isEmpty() ? new EnumMap<>(Map.of(Direction.NORTH, Shapes.empty(), Direction.EAST, Shapes.empty(),
                    Direction.SOUTH, Shapes.empty(), Direction.WEST, Shapes.empty())) : Shapes.rotateHorizontal(s));
        }
    }

    public String id() {
        return id;
    }

    public FamilieVormen.Vorm vorm() {
        return vorm;
    }

    /** The anchor cell (front-left, on the floor): where the bed is placed. */
    int ankerDeel() {
        return deel(0, 0, vorm.sz() - 1);
    }

    int deel(int lx, int ly, int lz) {
        return lx + vorm.sx() * (lz + vorm.sz() * ly);
    }

    /** This cell's place in the bed's frame: {lx, ly, lz}. */
    int[] lokaal(BlockState s) {
        int d = s.getValue(DEEL);
        int lx = d % vorm.sx(), rest = d / vorm.sx();
        return new int[] {lx, rest / vorm.sz(), rest % vorm.sz()};
    }

    /** The anchor block of the bed this cell belongs to. */
    BlockPos anker(BlockState s, BlockPos pos) {
        int[] l = lokaal(s);
        Direction f = s.getValue(FACING);
        double[] d = Vakjes.draai(f, l[0], l[2] - (vorm.sz() - 1));
        return pos.offset(-(int) Math.round(d[0]), -l[1], -(int) Math.round(d[1]));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DEEL);
    }

    /** Placed by the item: only when every cell of the bed is free (the bed faces the way the player looks). */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction f = context.getHorizontalDirection();
        BlockPos anker = context.getClickedPos();
        Level level = context.getLevel();
        for (int deel : vorm.cellen()) {
            int lx = deel % vorm.sx(), rest = deel / vorm.sx();
            BlockPos p = Vakjes.cel(anker, f, vorm.sz(), lx, rest / vorm.sz(), rest % vorm.sz());
            if (!level.isInsideBuildHeight(p.getY()) || !level.getBlockState(p).canBeReplaced(context)) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, f).setValue(DEEL, ankerDeel());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!level.isClientSide()) {
            plaatsRest(level, pos, state.getValue(FACING));
        }
    }

    /** Puts every cell of the bed around its anchor (the anchor itself too). */
    public void plaatsRest(Level level, BlockPos anker, Direction f) {
        for (int deel : vorm.cellen()) {
            int lx = deel % vorm.sx(), rest = deel / vorm.sx();
            BlockPos p = Vakjes.cel(anker, f, vorm.sz(), lx, rest / vorm.sz(), rest % vorm.sz());
            level.setBlock(p, defaultBlockState().setValue(FACING, f).setValue(DEEL, deel), 3);
        }
    }

    /** Breaking one cell breaks the whole bed (only the broken cell drops the bed). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos anker = anker(state, pos);
            Direction f = state.getValue(FACING);
            for (int deel : vorm.cellen()) {
                int lx = deel % vorm.sx(), rest = deel / vorm.sx();
                BlockPos p = Vakjes.cel(anker, f, vorm.sz(), lx, rest / vorm.sz(), rest % vorm.sz());
                if (!p.equals(pos) && level.getBlockState(p).is(this)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Map<Direction, VoxelShape> m = vormen.get(state.getValue(DEEL));
        return m == null ? Shapes.empty() : m.get(state.getValue(FACING));
    }
}
