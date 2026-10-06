package nl.juiced.guhs.feature.ringsausuman;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.feature.vadskracht.BronSoort;
import nl.juiced.guhs.feature.vadskracht.VadsBron;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;

/**
 * Het Mika-rad: Sausuman's answer to the Guhrad. No guh wants to run for him, so there is an Uruk-Mika in it, and a Mika
 * does not run: it shuffles. It is a REAL source of vadskracht (a {@link VadsBron} without a kind, {@link #VERMOGEN} VK), and
 * the hall of the tower hangs five real machines on it: far too heavy, so the whole net stands still and the hover readout
 * of Guh-technologie says exactly that ("Te zwaar: er is ... vadskracht te weinig"). The nod to Guh-technologie of this
 * slice: nothing here is faked. No item, no recipe: it only stands in the tower.
 */
public class MikaradBlock extends BaseEntityBlock {
    public static final MapCodec<MikaradBlock> CODEC = simpleCodec(MikaradBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** What a shuffling Uruk-Mika gives (a guh in a Guhrad gives this and more; he is not even trying). */
    public static final int VERMOGEN = 10;

    public MikaradBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Kern(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) {
            VadsKracht.veranderd(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        VadsKracht.veranderd(level, pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    /** Poke the Mika: he looks at you and shuffles on, not a step faster. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.ringsausuman.mikarad.por"));
        }
        return InteractionResult.SUCCESS;
    }

    /** The block entity: a source without a kind (no cap per net) that always gives the same little bit. */
    public static class Kern extends BlockEntity implements VadsBron {
        public Kern(BlockPos pos, BlockState state) {
            super(RingSausumanFeature.MIKARAD_BE.get(), pos, state);
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
            return VERMOGEN;
        }

        @Override
        public void vadsRegels(Consumer<Component> regels) {
            regels.accept(Component.translatable("gui.guhs.ringsausuman.mikarad.regel"));
        }
    }
}
