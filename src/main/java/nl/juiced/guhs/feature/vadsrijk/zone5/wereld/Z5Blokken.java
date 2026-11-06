package nl.juiced.guhs.feature.vadsrijk.zone5.wereld;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bouwhulp.blokken.BouwBlokken;

/**
 * T-Z5WERELD: the Hoogvads building blocks (zone 5, the capital on Moeder Wolkwal): rose + cloud marble, golden cornices,
 * capitals and roof tiles, cathedral glass with golden lead, whale skin and the Hoogvads banner (the "minimal epic set" of
 * renders-hoogvads/BLOKKEN_VOORSTEL.md). Resources: tools/features/vadsrijk_t_z5blokken.py (same ids, same order).
 * They are ordinary building blocks (craftable, stonecutter shapes) and appear in the "Guhs: Bouwen" tab.
 */
public final class Z5Blokken {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    /** Every block of this slice by id, in creative order. */
    public static final Map<String, DeferredBlock<?>> PER_ID = new LinkedHashMap<>();

    static {
        Supplier<BlockBehaviour.Properties> roze = () -> steen(MapColor.COLOR_PINK, 2f);
        Supplier<BlockBehaviour.Properties> wit = () -> steen(MapColor.QUARTZ, 2f);
        Supplier<BlockBehaviour.Properties> goud = () -> steen(MapColor.GOLD, 2.5f).sound(SoundType.METAL);
        Supplier<BlockBehaviour.Properties> huid = () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.2f)
                .sound(SoundType.WOOL);
        familie("roze_marmer", roze, false);
        familie("wolkmarmer", wit, false);
        reg("roze_marmer_guhgezicht", Block::new, roze);
        DeferredBlock<Block> lijst = reg("gouden_kroonlijst", Block::new, goud);
        reg("gouden_kroonlijst_trap", q -> new StairBlock(lijst.get().defaultBlockState(), q), goud);
        reg("gouden_kroonlijst_plaat", SlabBlock::new, goud);
        reg("gouden_zuilkop", RotatedPillarBlock::new, goud);
        DeferredBlock<Block> dak = reg("gouden_dakpan", Block::new, goud);
        reg("gouden_dakpan_trap", q -> new StairBlock(dak.get().defaultBlockState(), q), goud);
        reg("gouden_dakpan_plaat", SlabBlock::new, goud);
        for (String g : new String[]{"guh", "hart", "roos", "ruitjes"}) {
            reg("kathedraalglas_" + g, TransparentBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).lightLevel(s -> 2));
        }
        reg("walvishuid", Block::new, huid);
        reg("walvishuid_licht", Block::new, () -> huid.get().mapColor(MapColor.COLOR_PURPLE));
        reg("walvisbuik", Block::new, () -> huid.get().mapColor(MapColor.SNOW));
        reg("walvishuid_wolkpok", Block::new, () -> huid.get().mapColor(MapColor.SNOW));
        Supplier<BlockBehaviour.Properties> doek = () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f)
                .sound(SoundType.WOOL).noCollision().noOcclusion().ignitedByLava();
        reg("hoogvads_vaandel_boven", Vaandel::new, doek);
        reg("hoogvads_vaandel_onder", Vaandel::new, doek);
        for (DeferredBlock<?> b : PER_ID.values()) {
            ITEMS.registerSimpleBlockItem(b);
        }
        BouwBlokken.TAB_EXTRA.add(out -> PER_ID.values().forEach(b -> out.accept(new ItemStack(b.get()))));
    }

    private static BlockBehaviour.Properties steen(MapColor kleur, float hard) {
        return BlockBehaviour.Properties.of().mapColor(kleur).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                .strength(hard, 6f).sound(SoundType.CALCITE);
    }

    /** Marble: raw, polished (+ stairs, slab, wall) and a column. */
    private static void familie(String n, Supplier<BlockBehaviour.Properties> p, boolean unused) {
        reg(n, Block::new, p);
        DeferredBlock<Block> glad = reg(n + "_gepolijst", Block::new, p);
        reg(n + "_gepolijst_trap", q -> new StairBlock(glad.get().defaultBlockState(), q), p);
        reg(n + "_gepolijst_plaat", SlabBlock::new, p);
        reg(n + "_gepolijst_muur", WallBlock::new, () -> p.get().forceSolidOn());
        reg(n + "_zuil", RotatedPillarBlock::new, p);
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

    private Z5Blokken() {
    }

    /** The Hoogvads banner: a flat cloth (2 px) against the back of its block, turned to face you when placed. */
    public static final class Vaandel extends HorizontalDirectionalBlock {
        public static final MapCodec<Vaandel> CODEC = simpleCodec(Vaandel::new);
        private static final VoxelShape N = Block.box(0, 0, 14, 16, 16, 16), S = Block.box(0, 0, 0, 16, 16, 2),
                O = Block.box(0, 0, 0, 2, 16, 16), W = Block.box(14, 0, 0, 16, 16, 16);

        public Vaandel(BlockBehaviour.Properties p) {
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
            return defaultBlockState().setValue(FACING, c.getHorizontalDirection());
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
}
