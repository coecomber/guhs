package nl.juiced.guhs.feature.bio.wereld;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * biomes3 wereld, the Wolkenweide: what its land is made of, and its sounds and particles (resources:
 * tools/features/bio_wereld_wolk.py).
 * <ul>
 *   <li>{@code wolkenweide_gras}: the soft pale grass of the meadow and the island tops;</li>
 *   <li>{@code wolkenweide_steen} (rose), {@code wolkenweide_steen_lila} and {@code wolkenweide_steen_blauw} (pale blue): the
 *       pastel rock of the islands' undersides, in layers with a seam of parelmoer (the surface rule);</li>
 *   <li>{@code wolkenweide_kristal}: the softly glowing tip of a drip point, and {@code wolkenweide_kristalpunt}, the
 *       little point that hangs under it;</li>
 *   <li>{@code wolkenweide_rank}: a pale pink vine that hangs under the islands.</li>
 * </ul>
 * Particles {@code wolkenweide_pluisje} (floating fluff) and {@code wolkenweide_glinster} (a glint), both ambient
 * particles of the biome; sounds {@code wolkenweide.muziek} and {@code wolkenweide.sfeer}.
 */
public final class WolkBlokken {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);

    public static final DeferredBlock<Block> GRAS = BLOCKS.registerSimpleBlock("wolkenweide_gras",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.6f).sound(SoundType.MOSS));
    public static final DeferredBlock<Block> STEEN = BLOCKS.registerSimpleBlock("wolkenweide_steen",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.2f, 3.0f).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> STEEN_LILA = BLOCKS.registerSimpleBlock("wolkenweide_steen_lila",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(1.2f, 3.0f).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> STEEN_BLAUW = BLOCKS.registerSimpleBlock("wolkenweide_steen_blauw",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.2f, 3.0f).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> KRISTAL = BLOCKS.registerSimpleBlock("wolkenweide_kristal",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(1.0f).sound(SoundType.AMETHYST).lightLevel(s -> 9));
    public static final DeferredBlock<Kristalpunt> KRISTALPUNT = BLOCKS.registerBlock("wolkenweide_kristalpunt", Kristalpunt::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.4f).sound(SoundType.AMETHYST_CLUSTER).lightLevel(s -> 6)
                    .noCollision().noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<HangingMossBlock> RANK = BLOCKS.registerBlock("wolkenweide_rank", HangingMossBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollision().instabreak().sound(SoundType.MOSS_CARPET)
                    .pushReaction(PushReaction.DESTROY).ignitedByLava());

    public static final DeferredItem<BlockItem> GRAS_ITEM = ITEMS.registerSimpleBlockItem(GRAS);
    public static final DeferredItem<BlockItem> STEEN_ITEM = ITEMS.registerSimpleBlockItem(STEEN);
    public static final DeferredItem<BlockItem> STEEN_LILA_ITEM = ITEMS.registerSimpleBlockItem(STEEN_LILA);
    public static final DeferredItem<BlockItem> STEEN_BLAUW_ITEM = ITEMS.registerSimpleBlockItem(STEEN_BLAUW);
    public static final DeferredItem<BlockItem> KRISTAL_ITEM = ITEMS.registerSimpleBlockItem(KRISTAL);
    public static final DeferredItem<BlockItem> KRISTALPUNT_ITEM = ITEMS.registerSimpleBlockItem(KRISTALPUNT);
    public static final DeferredItem<BlockItem> RANK_ITEM = ITEMS.registerSimpleBlockItem(RANK);

    /** Dreamy music of the biome (a stream; the biome file names it). */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUZIEK = sound("wolkenweide.muziek");
    /** The biome's ambience loop: soft wind, distant chimes. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SFEER = sound("wolkenweide.sfeer");

    /** A bit of fluff drifting on the wind. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLUISJE = PARTICLES.register("wolkenweide_pluisje",
            () -> new SimpleParticleType(false));
    /** A glint that twinkles once. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLINSTER = PARTICLES.register("wolkenweide_glinster",
            () -> new SimpleParticleType(false));

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** The little crystal point that hangs under a drip point: no collision, it goes when what it hangs from goes. */
    public static class Kristalpunt extends Block {
        public static final MapCodec<Kristalpunt> CODEC = simpleCodec(Kristalpunt::new);
        private static final VoxelShape VORM = Block.box(5, 4, 5, 11, 16, 11);

        public Kristalpunt(Properties properties) {
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
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            BlockPos boven = pos.above();
            return level.getBlockState(boven).isFaceSturdy(level, boven, Direction.DOWN);
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighbourPos,
                BlockState neighbour, RandomSource random) {
            return direction == Direction.UP && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
        }
    }

    public static List<DeferredBlock<? extends Block>> blokken() {
        return List.of(GRAS, STEEN, STEEN_LILA, STEEN_BLAUW, KRISTAL, KRISTALPUNT, RANK);
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        PARTICLES.register(modBus);
    }

    static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> item : List.of(GRAS_ITEM, STEEN_ITEM, STEEN_LILA_ITEM, STEEN_BLAUW_ITEM, KRISTAL_ITEM, KRISTALPUNT_ITEM, RANK_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private WolkBlokken() {
    }
}
