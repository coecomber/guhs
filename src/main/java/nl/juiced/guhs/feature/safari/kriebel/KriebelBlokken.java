package nl.juiced.guhs.feature.safari.kriebel;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * Round 4 (L3): the blocks of the Kriebelhuis and the new lobby (design safari-ontwerp/lobby2/ontwerp.md; resources:
 * tools/features/safari_kriebelblokken.py). All craftable, all in the creative tab.
 * <ul>
 *   <li>terrariumglas (spruce frame), kasglas (white ribs), glasvloer (water edge): full glass blocks, inner faces culled;</li>
 *   <li>mierenboerderij: an ant farm with crawling tunnels (an animated texture);</li>
 *   <li>infobord: a sign on a post; right-click shows the Guhdex text of its species ({@link InfobordBlockEntity}, NBT "Soort",
 *       set by the park generator);</li>
 *   <li>warmtelamp: a hanging lamp with a warm orange glow; nepkoraal: coral that never dies (kleur, right-click = next
 *       colour); glimpaddenstoel: a glowing mushroom; sterrenplafond: a twinkling night sky; schorsplaat: a flat piece of bark.</li>
 * </ul>
 */
public final class KriebelBlokken {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final DeferredBlock<TransparentBlock> TERRARIUMGLAS = BLOCKS.registerBlock("terrariumglas", TransparentBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(0.5f));
    public static final DeferredBlock<TransparentBlock> KASGLAS = BLOCKS.registerBlock("kasglas", TransparentBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(0.5f));
    public static final DeferredBlock<TransparentBlock> GLASVLOER = BLOCKS.registerBlock("glasvloer", TransparentBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(0.8f));
    public static final DeferredBlock<Block> MIERENBOERDERIJ = BLOCKS.registerSimpleBlock("mierenboerderij",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.8f).sound(SoundType.GLASS));
    public static final DeferredBlock<Infobord> INFOBORD = BLOCKS.registerBlock("infobord", Infobord::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1f).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<Warmtelamp> WARMTELAMP = BLOCKS.registerBlock("warmtelamp", Warmtelamp::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.8f).sound(SoundType.LANTERN).lightLevel(s -> 13).noOcclusion());
    public static final DeferredBlock<Nepkoraal> NEPKORAAL = BLOCKS.registerBlock("nepkoraal", Nepkoraal::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.3f).sound(SoundType.CORAL_BLOCK).noCollision().lightLevel(s -> 6));
    public static final DeferredBlock<Glimpaddenstoel> GLIMPADDENSTOEL = BLOCKS.registerBlock("glimpaddenstoel", Glimpaddenstoel::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instabreak().sound(SoundType.FUNGUS).noCollision().lightLevel(s -> 10));
    public static final DeferredBlock<Block> STERRENPLAFOND = BLOCKS.registerSimpleBlock("sterrenplafond",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(1.5f).sound(SoundType.STONE).lightLevel(s -> 4));
    public static final DeferredBlock<Schorsplaat> SCHORSPLAAT = BLOCKS.registerBlock("schorsplaat", Schorsplaat::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(0.5f).sound(SoundType.WOOD));

    public static final List<DeferredBlock<?>> ALLE = List.of(TERRARIUMGLAS, KASGLAS, GLASVLOER, MIERENBOERDERIJ, INFOBORD, WARMTELAMP, NEPKORAAL,
            GLIMPADDENSTOEL, STERRENPLAFOND, SCHORSPLAAT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfobordBlockEntity>> INFOBORD_BE = BLOCK_ENTITIES.register("infobord",
            () -> new BlockEntityType<>(InfobordBlockEntity::new, INFOBORD.get()));

    static {
        for (DeferredBlock<?> b : ALLE) {
            ITEMS.registerSimpleBlockItem(b);
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<?> b : ALLE) {
            output.accept(new ItemStack(b.get()));
        }
    }

    // =====================================================================================================================

    /** The info board: a sign on a post, facing its reader; right-click shows its species' text in the talking screen. */
    public static class Infobord extends BaseEntityBlock {
        public static final MapCodec<Infobord> CODEC = simpleCodec(Infobord::new);
        private static final VoxelShape NZ = Shapes.or(Block.box(7, 0, 7, 9, 9, 9), Block.box(1, 8, 6.5, 15, 16, 9.5)),
                OW = Shapes.or(Block.box(7, 0, 7, 9, 9, 9), Block.box(6.5, 8, 1, 9.5, 16, 15));

        public Infobord(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(HorizontalDirectionalBlock.FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(HorizontalDirectionalBlock.FACING).getAxis() == Direction.Axis.Z ? NZ : OW;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new InfobordBlockEntity(pos, state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof InfobordBlockEntity be) {
                be.lees(sp);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** A lamp hanging from the ceiling (a short chain, a shade, a glowing bulb) with warm orange specks drifting from it. */
    public static class Warmtelamp extends Block {
        public static final MapCodec<Warmtelamp> CODEC = simpleCodec(Warmtelamp::new);
        private static final VoxelShape VORM = Shapes.or(Block.box(7, 10, 7, 9, 16, 9), Block.box(4, 4, 4, 12, 10, 12));
        private static final DustParticleOptions GLOED = new DustParticleOptions(0xFF8A2A, 0.8f);

        public Warmtelamp(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) == 0) {
                level.addParticle(GLOED, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.15, pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                        0, -0.02, 0);
            }
        }
    }

    public enum Kleur implements StringRepresentable {
        ROZE, GEEL, BLAUW, ORANJE, PAARS;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Coral that never dies (in or out of water); right-click with an empty hand: the next colour. */
    public static class Nepkoraal extends Block implements SimpleWaterloggedBlock {
        public static final MapCodec<Nepkoraal> CODEC = simpleCodec(Nepkoraal::new);
        public static final EnumProperty<Kleur> KLEUR = EnumProperty.create("kleur", Kleur.class);
        public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
        private static final VoxelShape VORM = Block.box(2, 0, 2, 14, 15, 14);

        public Nepkoraal(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(KLEUR, Kleur.ROZE).setValue(WATERLOGGED, false));
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(KLEUR, WATERLOGGED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                BlockPos neighborPos, BlockState neighbor, RandomSource random) {
            if (state.getValue(WATERLOGGED)) {
                ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
        }

        @Override
        protected FluidState getFluidState(BlockState state) {
            return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                Kleur[] k = Kleur.values();
                level.setBlock(pos, state.setValue(KLEUR, k[(state.getValue(KLEUR).ordinal() + 1) % k.length]), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.CORAL_BLOCK_PLACE, SoundSource.BLOCKS, 0.8f, 1.3f);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** A little glowing mushroom for the Nachtzaal: now and then a glowing speck rises from it. */
    public static class Glimpaddenstoel extends Block {
        public static final MapCodec<Glimpaddenstoel> CODEC = simpleCodec(Glimpaddenstoel::new);
        private static final VoxelShape VORM = Block.box(4, 0, 4, 12, 8, 12);

        public Glimpaddenstoel(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(8) == 0) {
                level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 0.5,
                        pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
            }
        }
    }

    /** A flat piece of bark (2 pixels), for terrarium floors. */
    public static class Schorsplaat extends Block {
        public static final MapCodec<Schorsplaat> CODEC = simpleCodec(Schorsplaat::new);
        private static final VoxelShape VORM = Block.box(0, 0, 0, 16, 2, 16);

        public Schorsplaat(Properties properties) {
            super(properties);
        }

        @Override
        protected MapCodec<? extends Block> codec() {
            return CODEC;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }
    }

    private KriebelBlokken() {
    }
}
