package nl.juiced.guhs.feature.vadskracht;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * {@code guhs:vadskracht_testbron}: a source for game tests and dev worlds, so a machine can be powered without a Guhrad and
 * a guh. It gives {@code kracht * 10} VK ({@link #KRACHT} 0..15, default 1 = 10 VK), has no {@link BronSoort} (so no cap per
 * net) and is in no creative tab and has no item ({@code /setblock}). Right-click (creative): one step more.
 * <pre>helper.setBlock(pos, VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, 2));   // 20 VK</pre>
 */
public class TestbronBlock extends BaseEntityBlock {
    public static final MapCodec<TestbronBlock> CODEC = simpleCodec(TestbronBlock::new);
    public static final IntegerProperty KRACHT = IntegerProperty.create("kracht", 0, 15);
    /** VK per step of {@link #KRACHT}. */
    public static final int PER_STAP = 10;

    public TestbronBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KRACHT, 1));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KRACHT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Kern(pos, state);
    }

    /** Placed, or its kracht changed: the net looks again. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        VadsKracht.veranderd(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        VadsKracht.veranderd(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getAbilities().instabuild) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            BlockState meer = state.cycle(KRACHT);
            level.setBlock(pos, meer, Block.UPDATE_ALL);
            player.sendOverlayMessage(Component.translatable("gui.guhs.vadskracht.geeft", meer.getValue(KRACHT) * PER_STAP));
        }
        return InteractionResult.SUCCESS;
    }

    /** The test source's block entity: a source without a kind that gives what the block state says. */
    public static class Kern extends BlockEntity implements VadsBron {
        public Kern(BlockPos pos, BlockState state) {
            super(VadskrachtFeature.TESTBRON_BE.get(), pos, state);
        }

        @Override
        public BlockPos vadsPlek() {
            return worldPosition;
        }

        @Nullable
        @Override
        public BronSoort vadsSoort() {
            return null;
        }

        @Override
        public int vadsAanbod() {
            return getBlockState().hasProperty(KRACHT) ? getBlockState().getValue(KRACHT) * PER_STAP : 0;
        }
    }
}
