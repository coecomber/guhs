package nl.juiced.guhs.feature.vadsrijk.ambacht;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.10.4 "Grondstoffen zie je nu echt": a REAL, shared gathering node in the world ({@link Grondstoffen}): one block per spot kind
 * ({@code vr_grondstof_erts/hout/kruid/vis/duik}; hives stay per-player stands). Properties: {@code tier} (1..6: the look),
 * {@code vol} (full = harvestable, false = the empty look: mined-out rock, a low stump, a bare bush, calm water, dead coral) and
 * {@code waterlogged} (fishing and dive spots sit in their water). Unbreakable, no loot, never pushed, water never washes it away,
 * nothing spawns on it. The click itself is handled by the client (the minigame, {@code AmbachtClient}); the server side only
 * swallows the use so nothing else happens.
 */
public class GrondstofBlok extends Block implements LiquidBlockContainer {
    public static final MapCodec<GrondstofBlok> CODEC = simpleCodec(p -> new GrondstofBlok(p, NodeSoort.ERTS));
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 1, 6);
    public static final BooleanProperty VOL = BooleanProperty.create("vol");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape STRONK = Block.box(1, 0, 1, 15, 16, 15), STRONK_LEEG = Block.box(1, 0, 1, 15, 5, 15);
    private static final VoxelShape STRUIK = Block.box(2, 0, 2, 14, 13, 14), STRUIK_LEEG = Block.box(3, 0, 3, 13, 8, 13);
    private static final VoxelShape DOBBER = Block.box(1, 12, 1, 15, 16, 15), KORAAL = Block.box(2, 0, 2, 14, 12, 14);

    public final NodeSoort soort;

    public GrondstofBlok(BlockBehaviour.Properties p, NodeSoort soort) {
        super(p);
        this.soort = soort;
        registerDefaultState(stateDefinition.any().setValue(TIER, 1).setValue(VOL, true).setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(TIER, VOL, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        boolean vol = state.getValue(VOL);
        return switch (soort) {
            case ERTS -> Shapes.block();
            case HOUT -> vol ? STRONK : STRONK_LEEG;
            case KRUID -> vol ? STRUIK : STRUIK_LEEG;
            case VIS -> vol ? DOBBER : Shapes.empty();   // (an empty fishing spot is just calm water)
            case DUIK -> KORAAL;
            default -> Shapes.block();
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (soort) {
            case ERTS -> Shapes.block();
            case HOUT -> state.getValue(VOL) ? STRONK : STRONK_LEEG;
            default -> Shapes.empty();   // (walk through bushes, swim through coral, boats over fishing spots)
        };
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.SUCCESS;   // (the client plays the minigame and reports to the server: Verzamelen.oogst)
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        return InteractionResult.SUCCESS;   // (a weapon in your hand must not place or use anything on a node)
    }

    // ---- water never flows into a node (a bush must not be washed away) ------------------------------------------------------

    @Override
    public boolean canPlaceLiquid(LivingEntity e, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        return false;
    }
}
