package nl.juiced.guhs.feature.grotspellen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The block classes of the cave games (registered in {@link GrotSpellenFeature}):
 * <ul>
 *   <li>{@link Schatgrond}: the soft field of Schatgraven (pluiswol or viltkiezel, {@link #KIEZEL}): closed, resting (outside
 *       the part that is played) or dug open, showing what was under it;</li>
 *   <li>{@link Puzzellamp}: a mushroom lamp of the Lichtjespuzzel's floor (on/off);</li>
 *   <li>{@link Glimpot}: a jar of glimwormpjes; {@link #LICHT} 0-4 says how bright (the hall's jars follow your catch;
 *       the shop's deco jar is always 4);</li>
 *   <li>{@link Glimplant}: a glowing cave bush you can walk through (the glimworms hide in them);</li>
 *   <li>{@link Lampjestegel}: deco, a floor lamp that switches when you right-click it;</li>
 *   <li>{@link Klein}: small deco with a shape (the mushroom lamp).</li>
 * </ul>
 */
public final class GrotSpellenBlocks {
    public static final BooleanProperty KIEZEL = BooleanProperty.create("kiezel");
    public static final BooleanProperty AAN = BooleanProperty.create("aan");
    public static final IntegerProperty LICHT = IntegerProperty.create("licht", 0, 4);
    /** How much light a glimpot gives per stage. */
    public static final int[] LICHT_NIVEAU = {1, 5, 9, 12, 15};

    /** A cell of the Schatgraven field. */
    public enum Staat implements StringRepresentable {
        DICHT, RUST, LEEG, SCHAT, GOUD, PIEP;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public boolean open() {
            return this != DICHT && this != RUST;
        }

        public static Staat van(SchatVeld.Inhoud inhoud) {
            return switch (inhoud) {
                case SCHAT -> SCHAT;
                case GOUD -> GOUD;
                case PIEP -> PIEP;
                default -> LEEG;
            };
        }
    }

    public static final EnumProperty<Staat> STAAT = EnumProperty.create("staat", Staat.class);

    /** The Schatgraven field: closed it's a full soft block; dug open a pit half a block deep. */
    public static class Schatgrond extends Block {
        public static final MapCodec<Schatgrond> CODEC = simpleCodec(Schatgrond::new);
        private static final VoxelShape KUIL = Block.box(0, 0, 0, 16, 8, 16);

        public Schatgrond(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(STAAT, Staat.DICHT).setValue(KIEZEL, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(STAAT, KIEZEL);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(STAAT).open() ? KUIL : Shapes.block();
        }
    }

    /** A lamp of the Lichtjespuzzel: on it glows (and lights up the cave). */
    public static class Puzzellamp extends Block {
        public static final MapCodec<Puzzellamp> CODEC = simpleCodec(Puzzellamp::new);

        public Puzzellamp(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(AAN, true));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AAN);
        }
    }

    /** A jar of glimwormpjes. */
    public static class Glimpot extends Block {
        public static final MapCodec<Glimpot> CODEC = simpleCodec(Glimpot::new);
        private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 10, 12), Block.box(5, 10, 5, 11, 12, 11));

        public Glimpot(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(LICHT, 4));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LICHT);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    }

    /** A glowing cave bush: no collision, so you (and the glimworms) can go in. */
    public static class Glimplant extends Block {
        public static final MapCodec<Glimplant> CODEC = simpleCodec(Glimplant::new);
        private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

        public Glimplant(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }
    }

    /** Deco: a mushroom floor lamp; right-click switches it. */
    public static class Lampjestegel extends Block {
        public static final MapCodec<Lampjestegel> CODEC = simpleCodec(Lampjestegel::new);

        public Lampjestegel(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(AAN, true));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AAN);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.cycle(AAN), 3);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7f, state.getValue(AAN) ? 0.8f : 1.4f);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** Small deco with its own (centred) shape, in pixels. */
    public static class Klein extends Block {
        private final VoxelShape shape;
        private final double[] box;

        public Klein(Properties properties, double[] box) {
            super(properties);
            this.box = box;
            this.shape = Block.box(box[0], box[1], box[2], box[3], box[4], box[5]);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return simpleCodec(p -> new Klein(p, box));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return shape;
        }
    }

    private GrotSpellenBlocks() {
    }
}
