package nl.juiced.guhs.feature.knabbelspelen;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The blocks and special items of De Knabbelspelen. */
public final class KnabbelspelenBlocks {
    /** Half a block to the tin's own right or left (the second row of the pyramid sits between two tins). */
    public enum Schuif implements StringRepresentable {
        GEEN, LINKS, RECHTS;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * A tin with a Mika face (Mika-blikgooien). FACING is where the face looks; SCHUIF shifts it half a block to its own
     * right (clockwise of FACING) or left, so a pyramid can stand 3-2-1.
     */
    public static class Blik extends HorizontalDirectionalBlock {
        public static final MapCodec<Blik> CODEC = simpleCodec(Blik::new);
        public static final EnumProperty<Schuif> SCHUIF = EnumProperty.create("schuif", Schuif.class);
        private static final Map<BlockState, VoxelShape> SHAPES = new ConcurrentHashMap<>();

        public Blik(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SCHUIF, Schuif.GEEN));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, SCHUIF);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPES.computeIfAbsent(state, Blik::vorm);
        }

        static VoxelShape vorm(BlockState state) {
            double dx = 0, dz = 0;
            Schuif s = state.getValue(SCHUIF);
            if (s != Schuif.GEEN) {
                Direction d = s == Schuif.RECHTS ? state.getValue(FACING).getClockWise() : state.getValue(FACING).getCounterClockWise();
                dx = d.getStepX() * 8;
                dz = d.getStepZ() * 8;
            }
            double x0 = Math.max(0, 3 + dx), x1 = Math.min(16, 13 + dx), z0 = Math.max(0, 3 + dz), z1 = Math.min(16, 13 + dz);
            return Block.box(x0, 0, z0, x1, 14, z1);
        }
    }

    /** A big kaasmelk bottle (Spijkerpoepen), set into the ground; VOL once the spijker went in. */
    public static class KaasmelkFles extends Block {
        public static final MapCodec<KaasmelkFles> CODEC = simpleCodec(KaasmelkFles::new);
        public static final BooleanProperty VOL = BooleanProperty.create("vol");
        private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 10, 14), Block.box(5, 10, 5, 11, 14, 11));

        public KaasmelkFles(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(VOL, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(VOL);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    }

    /** A soft pluisbal for Mika-blikgooien: right-click to throw (only during the event). */
    public static class Pluisbal extends Item {
        public Pluisbal(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
            }
            if (player instanceof ServerPlayer p && Blikgooien.gooi(p, hand)) {
                p.getCooldowns().addCooldown(stack, 8);
                return InteractionResult.CONSUME.heldItemTransformedTo(stack);
            }
            return InteractionResult.FAIL;
        }
    }

    /** The guhguhtje's tail: right-click the guh board with it (blindfolded!). */
    public static class Staartje extends Item {
        public Staartje(Properties properties) {
            super(properties);
        }

        @Override
        public net.minecraft.world.InteractionResult useOn(UseOnContext context) {
            if (context.getLevel().isClientSide()) {
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            if (context.getPlayer() instanceof ServerPlayer p && GuhguhtjePrik.prik(p, context.getClickLocation(), context.getClickedPos())) {
                return net.minecraft.world.InteractionResult.CONSUME;
            }
            return net.minecraft.world.InteractionResult.PASS;
        }
    }

    private KnabbelspelenBlocks() {
    }
}
