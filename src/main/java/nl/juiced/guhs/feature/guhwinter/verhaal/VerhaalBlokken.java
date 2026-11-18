package nl.juiced.guhs.feature.guhwinter.verhaal;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * Guhwinter (verhaal slice): the three deco blocks you KEEP, one per chapter of the winter story (R4). They never melt
 * (no smelt tag), break like furniture and drop themselves. Models: tools/features/guhwinter_verhaal.py.
 * <ul>
 *   <li>{@link #PAKJESSTAPEL}: a pile of wrapped presents (chapter 1, "Verdwaalde Sint");</li>
 *   <li>{@link #KERSTSTUKJE}: a Dutch Christmas centrepiece; right-click lights or blows out its candle (chapter 2);</li>
 *   <li>{@link #OLIEBOLLENKRAAM}: a little oliebollen stall (chapter 3).</li>
 * </ul>
 */
public final class VerhaalBlokken {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    private static BlockBehaviour.Properties deco(MapColor kleur, SoundType geluid) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(0.6f).noOcclusion().sound(geluid);
    }

    public static final DeferredBlock<Deco> PAKJESSTAPEL = BLOCKS.registerBlock("guhwinter_pakjesstapel",
            p -> new Deco(p, Shapes.or(Block.box(1, 0, 1, 15, 7, 15), Block.box(3, 7, 3, 12, 13, 12))), () -> deco(MapColor.COLOR_RED, SoundType.WOOL));
    public static final DeferredBlock<Kerststukje> KERSTSTUKJE = BLOCKS.registerBlock("guhwinter_kerststukje", Kerststukje::new,
            () -> deco(MapColor.COLOR_GREEN, SoundType.AZALEA_LEAVES).lightLevel(s -> s.getValue(Kerststukje.LIT) ? 8 : 0));
    public static final DeferredBlock<Deco> OLIEBOLLENKRAAM = BLOCKS.registerBlock("guhwinter_oliebollenkraam",
            p -> new Deco(p, Block.box(0, 0, 0, 16, 16, 16)), () -> deco(MapColor.COLOR_ORANGE, SoundType.WOOD));

    public static final DeferredItem<BlockItem> PAKJESSTAPEL_ITEM = ITEMS.registerSimpleBlockItem(PAKJESSTAPEL);
    public static final DeferredItem<BlockItem> KERSTSTUKJE_ITEM = ITEMS.registerSimpleBlockItem(KERSTSTUKJE);
    public static final DeferredItem<BlockItem> OLIEBOLLENKRAAM_ITEM = ITEMS.registerSimpleBlockItem(OLIEBOLLENKRAAM);

    private VerhaalBlokken() {
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(PAKJESSTAPEL_ITEM.get()));
        output.accept(new ItemStack(KERSTSTUKJE_ITEM.get()));
        output.accept(new ItemStack(OLIEBOLLENKRAAM_ITEM.get()));
    }

    /** A piece of deco that looks the way it was placed from (its model's front faces the player who placed it). */
    public static class Deco extends HorizontalDirectionalBlock {
        public static final MapCodec<Deco> CODEC = simpleCodec(p -> new Deco(p, Shapes.block()));
        private final VoxelShape vorm;

        public Deco(Properties p, VoxelShape vorm) {
            super(p);
            this.vorm = vorm;
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

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return vorm;
        }
    }

    /** The kerststukje: a candle in pine with a red ribbon and pinecones; right-click lights or blows out the candle. */
    public static class Kerststukje extends Deco {
        public static final MapCodec<Kerststukje> CODEC = simpleCodec(Kerststukje::new);
        public static final BooleanProperty LIT = BlockStateProperties.LIT;

        public Kerststukje(Properties p) {
            super(p, Shapes.or(Block.box(2, 0, 2, 14, 4, 14), Block.box(6, 4, 6, 10, 10, 10)));
            registerDefaultState(defaultBlockState().setValue(LIT, true));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, LIT);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                boolean aan = !state.getValue(LIT);
                level.setBlock(pos, state.setValue(LIT, aan), Block.UPDATE_ALL);
                level.playSound(null, pos, aan ? SoundEvents.FIRECHARGE_USE : SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.1f);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(LIT) && random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + 0.68, pos.getZ() + 0.5, 0, 0, 0);
            }
        }
    }
}
