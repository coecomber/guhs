package nl.juiced.guhs.feature.vadsrijk.zone6.wereld;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
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
 * U-Z6WERELD: the Zijdeland building blocks (zone 6 "Het Zijdeland", ZONE6_PLAN decision Z6-6: a small set): rose sandstone
 * (+ stairs, slab, wall, carved with the guh motif), three mosaic tiles, the mashrabiya lattice (a pane, connects like iron bars),
 * turquoise dome tiles (+ stairs), silk cloth and a floating silk awning, the brass lantern and date-palm wood (log, planks,
 * stairs, slab, fence). Resources: tools/features/vadsrijk_u_z6blokken.py (same ids, same order). Craftable; in "Guhs: Bouwen".
 */
public final class Z6Blokken {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    /** Every block of this slice by id, in creative order. */
    public static final Map<String, DeferredBlock<?>> PER_ID = new LinkedHashMap<>();

    static {
        Supplier<BlockBehaviour.Properties> zs = () -> steen(MapColor.TERRACOTTA_PINK, 1.5f);
        Supplier<BlockBehaviour.Properties> tegel = () -> steen(MapColor.COLOR_CYAN, 1.4f);
        Supplier<BlockBehaviour.Properties> hout = () -> BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(2f, 3f)
                .sound(SoundType.WOOD).ignitedByLava();
        Supplier<BlockBehaviour.Properties> zijde = () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f)
                .sound(SoundType.WOOL).ignitedByLava();
        DeferredBlock<Block> rozen = reg("rozenzandsteen", Block::new, zs);
        reg("rozenzandsteen_trap", q -> new StairBlock(rozen.get().defaultBlockState(), q), zs);
        reg("rozenzandsteen_plaat", SlabBlock::new, zs);
        reg("rozenzandsteen_muur", WallBlock::new, () -> zs.get().forceSolidOn());
        reg("rozenzandsteen_gebeiteld", Block::new, zs);
        reg("mozaiek_ster", Block::new, tegel);
        reg("mozaiek_guh", Block::new, tegel);
        reg("mozaiek_bloem", Block::new, tegel);
        reg("mashrabiya_tralies", IronBarsBlock::new, () -> hout.get().mapColor(MapColor.COLOR_PINK).strength(1.5f).noOcclusion());
        DeferredBlock<Block> koepel = reg("koepeltegel_turkoois", Block::new, tegel);
        reg("koepeltegel_turkoois_trap", q -> new StairBlock(koepel.get().defaultBlockState(), q), tegel);
        reg("zijden_doek", Block::new, zijde);
        reg("zijden_luifel", Luifel::new, () -> zijde.get().noCollision().noOcclusion());
        reg("messing_lantaarn", LanternBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).forceSolidOn()
                .requiresCorrectToolForDrops().strength(3.5f).sound(SoundType.LANTERN).lightLevel(s -> 15).noOcclusion());
        reg("dadelpalm_stam", RotatedPillarBlock::new, hout);
        DeferredBlock<Block> planken = reg("dadelpalm_planken", Block::new, hout);
        reg("dadelpalm_trap", q -> new StairBlock(planken.get().defaultBlockState(), q), hout);
        reg("dadelpalm_plaat", SlabBlock::new, hout);
        reg("dadelpalm_hek", FenceBlock::new, () -> hout.get().forceSolidOn());
        for (DeferredBlock<?> b : PER_ID.values()) {
            ITEMS.registerSimpleBlockItem(b);
        }
        BouwBlokken.TAB_EXTRA.add(out -> PER_ID.values().forEach(b -> out.accept(new ItemStack(b.get()))));
    }

    private static BlockBehaviour.Properties steen(MapColor kleur, float hard) {
        return BlockBehaviour.Properties.of().mapColor(kleur).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                .strength(hard, 6f).sound(SoundType.STONE);
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

    private Z6Blokken() {
    }

    /** The silk awning: a 1 px cloth at the bottom of its block that needs no support (it may hang free over a door). */
    public static final class Luifel extends Block {
        private static final VoxelShape VORM = Block.box(0, 0, 0, 16, 1, 16);

        public Luifel(BlockBehaviour.Properties p) {
            super(p);
        }

        @Override
        protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return VORM;
        }
    }
}
