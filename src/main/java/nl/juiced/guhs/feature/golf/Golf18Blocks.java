package nl.juiced.guhs.feature.golf;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.7.0 "Rondje Guhmension": the block classes of the back 9 of the guh golf course (registered in {@link Golf18}) and the
 * golf shop's deco.
 * <ul>
 *   <li>{@link Schuifrail}: the rail under a row of sliding guh blocks; {@link #STAND} says in which of the two positions
 *       a block stands on it (the Golfguh swaps them, see GolfGame);</li>
 *   <li>{@link Bankhoek}: a 45 degree bank wall: the ball bounces off it at an angle (GolfBallEntity);</li>
 *   <li>{@link Looping}: the start of a vahoeg looping: fast enough along FACING and the ball goes round;</li>
 *   <li>{@link GolfbalTee}: placeable golf balls on little tees (1-4 per block, like candles);</li>
 *   <li>{@link GolfVlag}: a hole with a flag to put in your garden (right-click: flag in or out);</li>
 *   <li>{@link Golftrofee}: the trophy for 18 holes at or under par, with your name and record.</li>
 * </ul>
 */
public final class Golf18Blocks {

    /** Where a sliding guh block stands: on this rail in position A, in position B, or never (just rail). */
    public enum Stand implements StringRepresentable {
        GEEN, A, B;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Stand> STAND = EnumProperty.create("stand", Stand.class);

    /** The rail of the Schuifvadsjes: the ball rolls over it like felt. */
    public static class Schuifrail extends Block {
        public static final MapCodec<Schuifrail> CODEC = simpleCodec(Schuifrail::new);

        public Schuifrail(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(STAND, Stand.GEEN));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(STAND);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** Turns a box (pixels, facing north) to a facing, as GuhFurnitureBlock does. */
    static VoxelShape rotated(double[] b, Direction dir) {
        double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
        return switch (dir) {
            case SOUTH -> Block.box(16 - x1, b[1], 16 - z1, 16 - x0, b[4], 16 - z0);
            case EAST -> Block.box(16 - z1, b[1], x0, 16 - z0, b[4], x1);
            case WEST -> Block.box(z0, b[1], 16 - x1, z1, b[4], 16 - x0);
            default -> Block.box(x0, b[1], z0, x1, b[4], z1);
        };
    }

    static Map<Direction, VoxelShape> shapes(double[]... boxes) {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : boxes) {
                shape = Shapes.or(shape, rotated(b, dir));
            }
            map.put(dir, shape.optimize());
        }
        return map;
    }

    /**
     * A 45 degree bank wall: a triangle (four steps) in the corner between FACING and the side clockwise of it; its slanted
     * face looks the other way, and that is where the ball bounces off (see {@link #normal}).
     */
    public static class Bankhoek extends HorizontalDirectionalBlock {
        public static final MapCodec<Bankhoek> CODEC = simpleCodec(Bankhoek::new);
        /** Facing north: the corner is north-east (x 16, z 0); column i (x 4i..4i+4) is solid from z 0 to 4(i+1). */
        private static final Map<Direction, VoxelShape> SHAPES = shapes(
                new double[]{0, 0, 0, 4, 16, 4}, new double[]{4, 0, 0, 8, 16, 8}, new double[]{8, 0, 0, 12, 16, 12}, new double[]{12, 0, 0, 16, 16, 16});

        public Bankhoek(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPES.get(state.getValue(FACING));
        }

        /** The way the slanted face looks (unit vector in x/z): away from the solid corner. */
        public static Vec3 normal(Direction facing) {
            Direction side = facing.getClockWise();
            return new Vec3(-(facing.getStepX() + side.getStepX()), 0, -(facing.getStepZ() + side.getStepZ())).normalize();
        }
    }

    /** The start of a vahoeg looping (in the floor): a ball that rolls over it fast enough along FACING goes round. */
    public static class Looping extends HorizontalDirectionalBlock {
        public static final MapCodec<Looping> CODEC = simpleCodec(Looping::new);

        public Looping(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** Little golf balls on tees: 1-4 per block (click with another to add one), like candles. */
    public static class GolfbalTee extends Block {
        public static final MapCodec<GolfbalTee> CODEC = simpleCodec(GolfbalTee::new);
        public static final IntegerProperty BALLETJES = IntegerProperty.create("balletjes", 1, 4);
        private static final VoxelShape ONE = Block.box(6, 0, 6, 10, 5, 10), MORE = Block.box(2, 0, 2, 14, 5, 14);

        public GolfbalTee(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(BALLETJES, 1));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BALLETJES);
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState there = context.getLevel().getBlockState(context.getClickedPos());
            if (there.is(this)) {
                return there.setValue(BALLETJES, Math.min(4, there.getValue(BALLETJES) + 1));
            }
            return defaultBlockState();
        }

        @Override
        protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
            return !context.isSecondaryUseActive() && context.getItemInHand().is(asItem()) && state.getValue(BALLETJES) < 4
                    || super.canBeReplaced(state, context);
        }

        @Override
        protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
            return Block.canSupportCenter(level, pos.below(), Direction.UP);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(BALLETJES) == 1 ? ONE : MORE;
        }
    }

    /** A hole with a pink guh flag, for your garden: right-click to take the flag out or put it back. */
    public static class GolfVlag extends Block {
        public static final MapCodec<GolfVlag> CODEC = simpleCodec(GolfVlag::new);
        public static final BooleanProperty VLAG = BooleanProperty.create("vlag");

        public GolfVlag(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(VLAG, true));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(VLAG);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.cycle(VLAG), 3);
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, state.getValue(VLAG) ? 0.8f : 1.2f);
            }
            return InteractionResult.SUCCESS;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /**
     * The golf trophy: a golden cup on a pink plinth. The item carries the winner's name (its custom name) and the record
     * (its lore); the block keeps both and tells them when you right-click it, and gives them back when it is broken.
     */
    public static class Golftrofee extends HorizontalDirectionalBlock implements EntityBlock {
        public static final MapCodec<Golftrofee> CODEC = simpleCodec(Golftrofee::new);
        private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 4, 13), Block.box(6, 4, 6, 10, 9, 10), Block.box(3, 9, 3, 13, 16, 13));

        public Golftrofee(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TrofeeBlockEntity be) {
                player.sendSystemMessage((be.naam() != null ? be.naam().copy() : Component.translatable("block.guhs.golftrofee"))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                for (Component line : be.regels().lines()) {
                    player.sendSystemMessage(line.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.7f, 1.5f);
            }
            return InteractionResult.SUCCESS;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TrofeeBlockEntity(pos, state);
        }
    }

    /** What is engraved on a golf trophy: the custom name and lore of the item it was placed from. */
    public static class TrofeeBlockEntity extends BlockEntity {
        @Nullable
        private Component naam;
        private ItemLore regels = ItemLore.EMPTY;

        public TrofeeBlockEntity(BlockPos pos, BlockState state) {
            super(Golf18.TROFEE_BE.get(), pos, state);
        }

        @Nullable
        public Component naam() {
            return naam;
        }

        public ItemLore regels() {
            return regels;
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.storeNullable("naam", ComponentSerialization.CODEC, naam);
            output.store("regels", ItemLore.CODEC, regels);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            naam = input.read("naam", ComponentSerialization.CODEC).orElse(null);
            regels = input.read("regels", ItemLore.CODEC).orElse(ItemLore.EMPTY);
        }

        @Override
        protected void applyImplicitComponents(DataComponentGetter components) {
            super.applyImplicitComponents(components);
            naam = components.get(DataComponents.CUSTOM_NAME);
            regels = components.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        }

        @Override
        protected void collectImplicitComponents(DataComponentMap.Builder components) {
            super.collectImplicitComponents(components);
            components.set(DataComponents.CUSTOM_NAME, naam);
            components.set(DataComponents.LORE, regels);
        }

        @Override
        public void removeComponentsFromTag(ValueOutput output) {
            super.removeComponentsFromTag(output);
            output.discard("naam");
            output.discard("regels");
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveCustomOnly(registries);
        }
    }

    private Golf18Blocks() {
    }
}
