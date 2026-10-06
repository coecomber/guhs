package nl.juiced.guhs.feature.techsaus;

import java.util.Locale;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Het Sausvat ({@code guhs:sausvat}): a barrel with a guh face for {@link SausGetallen#VAT} mB of ONE sauce (kaassaus,
 * kaasfrituursaus, water or milk). Pumps push into it, machines slurp out of it, both through Sausslangen ({@link Slangen});
 * by hand you pour a bucket in or tap one out. It needs no vadskracht. What is in it shows through the window in its side
 * ({@link #SAUS}, {@link #NIVEAU}) and on its face: asleep when empty, surprised when full. Broken, it keeps its sauce (the
 * item component {@code guhs:techsaus_inhoud}, {@link SausvatBlockEntity}). Resources: tools/features/tech_vloeistof.py.
 */
public class SausvatBlock extends BaseEntityBlock {
    /** What the window shows. */
    public enum Saus implements StringRepresentable {
        LEEG, KAASSAUS, FRITUURSAUS, WATER, MELK;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The look of this sauce; a fluid we have no picture of (a datapack added it to the tag) looks like kaassaus. */
        public static Saus van(FluidResource saus) {
            if (saus.isEmpty()) {
                return LEEG;
            }
            String id = SausTekst.id(saus);
            return id == null ? KAASSAUS : valueOf(id.toUpperCase(Locale.ROOT));
        }
    }

    public static final MapCodec<SausvatBlock> CODEC = simpleCodec(SausvatBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Saus> SAUS = EnumProperty.create("saus", Saus.class);
    /** 0 empty, 1..3 a third each, 4 only when really full. */
    public static final IntegerProperty NIVEAU = IntegerProperty.create("niveau", 0, 4);

    public SausvatBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SAUS, Saus.LEEG).setValue(NIVEAU, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SAUS, NIVEAU);
    }

    /** The window's level for this much in a tank of this size. */
    public static int niveau(int inhoud, int max) {
        if (inhoud <= 0) {
            return 0;
        }
        return inhoud >= max ? 4 : 1 + Math.min(2, (int) (inhoud * 3L / Math.max(1, max)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
        return new SausvatBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) {
            Slangen.veranderd(level);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Slangen.veranderd(level);
        level.updateNeighbourForOutputSignal(pos, this);
    }

    /** A bucket (or anything else that holds fluid): tap one out, or pour it in. It never lands in the world by accident. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.isEmpty() || ItemAccess.forStack(stack).oneByOne().getCapability(Capabilities.Fluid.ITEM) == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof SausvatBlockEntity vat) {
            vat.emmer(sp, hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof SausvatBlockEntity vat) {
            vat.status(sp);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** 0 empty .. 15 full. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof SausvatBlockEntity vat && !vat.tank().isLeeg()) {
            return 1 + (int) (vat.tank().vulling() * 14);
        }
        return 0;
    }
}
