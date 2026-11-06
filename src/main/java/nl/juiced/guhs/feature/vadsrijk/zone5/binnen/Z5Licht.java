package nl.juiced.guhs.feature.vadsrijk.zone5.binnen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bouwhulp.blokken.BouwBlokken;

/**
 * U-Z5BINNEN: the Hoogvads light blocks for the interiors (round U feedback: "the chandeliers read as wooden scaffolding, the
 * interiors are too dark"). Resources: tools/features/vadsrijk_t_z5blokken.py (section LICHT, same ids).
 * <ul>
 *   <li>{@code kroonluchter}: a round chandelier as ONE block with a 3x3-wide 3D model (gold rings, candles, crystal drops);</li>
 *   <li>{@code gouden_kaarsenrand}: a thin gold rail with a candle on its node; joins its neighbours (north/east/south/west) so
 *       big chandelier rings can be laid in the world; {@code kristal} hangs a crystal drop under the node;</li>
 *   <li>{@code kristallen_hanger}: a hanging string of crystal drops;</li>
 *   <li>{@code gouden_wandluchter}: a wall sconce with three candles;</li>
 *   <li>{@code gouden_kandelaar}: a standing candelabrum (two blocks tall model);</li>
 *   <li>{@code gouden_kroonlijst_muur}: a golden cornice wall (trim, balustrades);</li>
 *   <li>{@code gouden_lichtlijst}: a golden cornice that glows (light hidden in the vault ribs).</li>
 * </ul>
 * All light-emitting, all in the "Guhs: Bouwen" tab. Registered from {@code Z5WereldRegistry} (one marked line).
 */
public final class Z5Licht {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final Map<String, DeferredBlock<?>> PER_ID = new LinkedHashMap<>();

    static {
        Supplier<BlockBehaviour.Properties> goudLicht = () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.0f)
                .sound(SoundType.METAL).noOcclusion().noCollision();
        reg("kroonluchter", Kroonluchter::new, () -> goudLicht.get().lightLevel(s -> 15));
        reg("gouden_kaarsenrand", Kaarsenrand::new, () -> goudLicht.get()
                .lightLevel(s -> s.getValue(Kaarsenrand.KAARS) ? 13 : s.getValue(Kaarsenrand.KRISTAL) ? 7 : 0));
        reg("kristallen_hanger", Hanger::new, () -> goudLicht.get().sound(SoundType.AMETHYST).lightLevel(s -> 9));
        reg("gouden_wandluchter", Wandluchter::new, () -> goudLicht.get().lightLevel(s -> 14));
        reg("gouden_kandelaar", Kandelaar::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.0f)
                .sound(SoundType.METAL).noOcclusion().lightLevel(s -> 14));
        Supplier<BlockBehaviour.Properties> goud = () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
                .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.5f, 6f).sound(SoundType.METAL);
        reg("gouden_kroonlijst_muur", WallBlock::new, () -> goud.get().forceSolidOn());
        reg("gouden_lichtlijst", Block::new, () -> goud.get().lightLevel(s -> 15));
        for (DeferredBlock<?> b : PER_ID.values()) {
            ITEMS.registerSimpleBlockItem(b);
        }
        BouwBlokken.TAB_EXTRA.add(out -> PER_ID.values().forEach(b -> out.accept(new ItemStack(b.get()))));
    }

    private static <B extends Block> DeferredBlock<B> reg(String id, Function<BlockBehaviour.Properties, B> maker, Supplier<BlockBehaviour.Properties> p) {
        DeferredBlock<B> b = BLOCKS.registerBlock(id, maker, p);
        PER_ID.put(id, b);
        return b;
    }

    public static Block blok(String id) {
        return PER_ID.get(id).get();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    private Z5Licht() {
    }

    /** The round chandelier (one block, the model hangs 3 blocks wide around it). */
    public static final class Kroonluchter extends Block {
        public static final MapCodec<Kroonluchter> CODEC = simpleCodec(Kroonluchter::new);
        private static final VoxelShape VORM = Block.box(4, 0, 4, 12, 16, 12);

        public Kroonluchter(BlockBehaviour.Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return VORM;
        }
    }

    /** A hanging string of crystal drops. */
    public static final class Hanger extends Block {
        public static final MapCodec<Hanger> CODEC = simpleCodec(Hanger::new);
        private static final VoxelShape VORM = Block.box(6, 0, 6, 10, 16, 10);

        public Hanger(BlockBehaviour.Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return VORM;
        }
    }

    /** A standing candelabrum: the model is two blocks tall, the collision one thin post. */
    public static final class Kandelaar extends Block {
        public static final MapCodec<Kandelaar> CODEC = simpleCodec(Kandelaar::new);
        private static final VoxelShape VORM = Block.box(5, 0, 5, 11, 16, 11);

        public Kandelaar(BlockBehaviour.Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return VORM;
        }
    }

    /** A wall sconce: FACING = the side it sticks out to (the wall is behind it). */
    public static final class Wandluchter extends HorizontalDirectionalBlock {
        public static final MapCodec<Wandluchter> CODEC = simpleCodec(Wandluchter::new);
        private static final VoxelShape N = Block.box(1, 2, 6, 15, 15, 16), S = Block.box(1, 2, 0, 15, 15, 10),
                O = Block.box(0, 2, 1, 10, 15, 15), W = Block.box(6, 2, 1, 16, 15, 15);

        public Wandluchter(BlockBehaviour.Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext c) {
            Direction d = c.getClickedFace().getAxis().isHorizontal() ? c.getClickedFace() : c.getHorizontalDirection().getOpposite();
            return defaultBlockState().setValue(FACING, d);
        }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return switch (s.getValue(FACING)) {
                case SOUTH -> S;
                case EAST -> O;
                case WEST -> W;
                default -> N;
            };
        }
    }

    /** The gold candle rail of the big chandeliers: joins other rails and chandeliers on its four sides. */
    public static final class Kaarsenrand extends Block {
        public static final MapCodec<Kaarsenrand> CODEC = simpleCodec(Kaarsenrand::new);
        public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
                SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST;
        public static final BooleanProperty KAARS = BooleanProperty.create("kaars"), KRISTAL = BooleanProperty.create("kristal");
        private static final VoxelShape KERN = Block.box(6, 0, 6, 10, 10, 10);
        private static final VoxelShape[] ARM = {Block.box(7, 1, 0, 9, 4, 8), Block.box(8, 1, 7, 16, 4, 9),
                Block.box(7, 1, 8, 9, 4, 16), Block.box(0, 1, 7, 8, 4, 9)};

        public Kaarsenrand(BlockBehaviour.Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                    .setValue(WEST, false).setValue(KAARS, true).setValue(KRISTAL, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(NORTH, EAST, SOUTH, WEST, KAARS, KRISTAL);
        }

        private static BooleanProperty prop(Direction d) {
            return switch (d) {
                case NORTH -> NORTH;
                case EAST -> EAST;
                case SOUTH -> SOUTH;
                default -> WEST;
            };
        }

        private boolean verbindt(BlockState s) {
            return s.is(this) || s.getBlock() instanceof Kroonluchter;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext c) {
            BlockState s = defaultBlockState();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                s = s.setValue(prop(d), verbindt(c.getLevel().getBlockState(c.getClickedPos().relative(d))));
            }
            return s;
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                         BlockPos neighborPos, BlockState neighborState, RandomSource random) {
            if (direction.getAxis().isHorizontal()) {
                return state.setValue(prop(direction), verbindt(neighborState));
            }
            return state;
        }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            VoxelShape v = KERN;
            int i = 0;
            for (Direction d : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                if (s.getValue(prop(d))) {
                    v = Shapes.or(v, ARM[i]);
                }
                i++;
            }
            return v;
        }

        @Override
        protected BlockState rotate(BlockState s, Rotation r) {
            return switch (r) {
                case CLOCKWISE_180 -> s.setValue(NORTH, s.getValue(SOUTH)).setValue(EAST, s.getValue(WEST))
                        .setValue(SOUTH, s.getValue(NORTH)).setValue(WEST, s.getValue(EAST));
                case CLOCKWISE_90 -> s.setValue(NORTH, s.getValue(WEST)).setValue(EAST, s.getValue(NORTH))
                        .setValue(SOUTH, s.getValue(EAST)).setValue(WEST, s.getValue(SOUTH));
                case COUNTERCLOCKWISE_90 -> s.setValue(NORTH, s.getValue(EAST)).setValue(EAST, s.getValue(SOUTH))
                        .setValue(SOUTH, s.getValue(WEST)).setValue(WEST, s.getValue(NORTH));
                default -> s;
            };
        }

        @Override
        protected BlockState mirror(BlockState s, Mirror m) {
            return switch (m) {
                case LEFT_RIGHT -> s.setValue(NORTH, s.getValue(SOUTH)).setValue(SOUTH, s.getValue(NORTH));
                case FRONT_BACK -> s.setValue(EAST, s.getValue(WEST)).setValue(WEST, s.getValue(EAST));
                default -> s;
            };
        }
    }
}
