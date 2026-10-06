package nl.juiced.guhs.feature.torenpeper;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A kweekbak of the Pepertuin's kas: a trough of one of the three grounds ({@code soort}) in which EVERY player grows an own
 * pepper plant ({@link Kweek}). The block itself never changes: it is invisible in the world's own drawing and the client
 * draws it (client.KweekRenderer) with the plant the LOCAL player has in it, by giving the model the state
 * {@code groei} = 0 (empty) .. 4 (ripe). {@code groei} is never set in the world.
 */
public class KweekbakBlock extends Block implements EntityBlock {
    public static final MapCodec<KweekbakBlock> CODEC = simpleCodec(KweekbakBlock::new);
    public static final EnumProperty<PeperSoort> SOORT = PeperplantBlock.SOORT;
    /** What the client draws: 0 an empty trough, 1..4 the plant's four stages (4 = ripe). Always 0 in the world. */
    public static final IntegerProperty GROEI = IntegerProperty.create("groei", 0, 4);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    public KweekbakBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SOORT, PeperSoort.GROEN).setValue(GROEI, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOORT, GROEI);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1f;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Bak(pos, state);
    }

    /** (an item in the hand: the block decides, in {@link #useWithoutItem}; seeds are looked for in both hands there) */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Kweek.klik(sp, pos, state.getValue(SOORT));
        }
        return InteractionResult.SUCCESS;
    }

    /** The block entity only exists so the client has something to hang its renderer on: it holds nothing. */
    public static class Bak extends BlockEntity {
        public Bak(BlockPos pos, BlockState state) {
            super(TorenpeperFeature.KWEEKBAK_BE.get(), pos, state);
        }
    }
}
