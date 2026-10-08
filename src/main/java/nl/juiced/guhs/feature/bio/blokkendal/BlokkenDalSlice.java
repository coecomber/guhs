package nl.juiced.guhs.feature.bio.blokkendal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.BioZelftest;

/**
 * biomes3 slice "blokken-dal": the block sets of the Klaterdal. Resources: tools/features/bio_blokken_dal.py.
 * <ul>
 *   <li>Roze lakhout: planks, slab, stairs, fence, gate, door, trapdoor and the balk (a pillar, for torii posts).</li>
 *   <li>Guh-dakpannen in roze, wit and grijs: block, slab, stairs and the krul ({@link DakpanHoekBlock}) that turns an eave up.</li>
 *   <li>{@link ShojiBlock} (slides open) and {@link TatamiBlock} (two blocks become one mat).</li>
 *   <li>Tuinspul: {@link ToroBlock} (lights at dusk), {@link GeharktZand} (straight and rings), {@link GuhBamboeBlock},
 *       {@link BonsaiPotBlock}, gladde knuffelsteen with slab and stairs.</li>
 *   <li>Esdoorn: trunk, red and orange leaves, the sapling ({@link Esdoorn}).</li>
 * </ul>
 */
public final class BlokkenDalSlice {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    /** Every block of the slice in the order of the creative tab. */
    private static final List<DeferredBlock<?>> ALLES = new ArrayList<>();
    public static final String[] DAKPAN_KLEUREN = {"roze", "wit", "grijs"};

    public static final ResourceKey<ConfiguredFeature<?, ?>> ESDOORN_BOOM_ROOD = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("esdoorn_boom_rood"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> ESDOORN_BOOM_ORANJE = ResourceKey.create(Registries.CONFIGURED_FEATURE, Guhs.id("esdoorn_boom_oranje"));
    /** The sapling grows a red esdoorn or, as often, an orange one. */
    public static final TreeGrower ESDOORN_GROEI = new TreeGrower("guhs:esdoorn", 0.5f, Optional.empty(), Optional.empty(),
            Optional.of(ESDOORN_BOOM_ROOD), Optional.of(ESDOORN_BOOM_ORANJE), Optional.empty(), Optional.empty());

    // (the gate, door and trapdoor only need a wood type for their sounds and "opens by hand": cherry's)
    private static final BlockSetType HOUT_SET = BlockSetType.CHERRY;
    private static final WoodType HOUT = WoodType.CHERRY;

    private static BlockBehaviour.Properties lak() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PLANKS).mapColor(MapColor.COLOR_RED);
    }

    private static BlockBehaviour.Properties dakpan(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                .strength(1.25f, 4.2f).sound(SoundType.DEEPSLATE_TILES);
    }

    private static BlockBehaviour.Properties steen() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_SANDSTONE).mapColor(MapColor.QUARTZ).strength(1.5f, 6f).sound(SoundType.CALCITE);
    }

    private static <T extends Block> DeferredBlock<T> blok(String id, Function<BlockBehaviour.Properties, T> maker, Supplier<BlockBehaviour.Properties> props) {
        DeferredBlock<T> block = BLOCKS.registerBlock(id, maker, props);
        ALLES.add(block);
        return block;
    }

    // --- roze lakhout ---------------------------------------------------------------------------------------------------
    public static final DeferredBlock<Block> LAKHOUT_PLANKEN = blok("roze_lakhout_planken", Block::new, () -> lak());
    public static final DeferredBlock<SlabBlock> LAKHOUT_PLAAT = blok("roze_lakhout_plaat", SlabBlock::new, () -> lak());
    public static final DeferredBlock<StairBlock> LAKHOUT_TRAP = blok("roze_lakhout_trap",
            p -> new StairBlock(LAKHOUT_PLANKEN.get().defaultBlockState(), p), () -> lak());
    public static final DeferredBlock<FenceBlock> LAKHOUT_HEK = blok("roze_lakhout_hek", FenceBlock::new, () -> lak());
    public static final DeferredBlock<FenceGateBlock> LAKHOUT_POORT = blok("roze_lakhout_poort", p -> new FenceGateBlock(HOUT, p), () -> lak().forceSolidOn());
    public static final DeferredBlock<DoorBlock> LAKHOUT_DEUR = blok("roze_lakhout_deur", p -> new DoorBlock(HOUT_SET, p),
            () -> lak().strength(3f).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<TrapDoorBlock> LAKHOUT_LUIK = blok("roze_lakhout_luik", p -> new TrapDoorBlock(HOUT_SET, p),
            () -> lak().strength(3f).noOcclusion().isValidSpawn((s, l, pos, e) -> false));
    /** The beam: a pillar (axis), for the posts and beams of a torii. */
    public static final DeferredBlock<RotatedPillarBlock> LAKHOUT_BALK = blok("roze_lakhout_balk", RotatedPillarBlock::new, () -> lak());

    // --- guh-dakpannen: per colour the block, the slab, the stairs and the krul -------------------------------------------
    public static final List<DeferredBlock<Block>> DAKPAN = new ArrayList<>();
    public static final List<DeferredBlock<SlabBlock>> DAKPAN_PLAAT = new ArrayList<>();
    public static final List<DeferredBlock<StairBlock>> DAKPAN_TRAP = new ArrayList<>();
    public static final List<DeferredBlock<DakpanHoekBlock>> DAKPAN_HOEK = new ArrayList<>();

    static {
        MapColor[] kleuren = {MapColor.COLOR_PINK, MapColor.SNOW, MapColor.COLOR_LIGHT_GRAY};
        for (int i = 0; i < DAKPAN_KLEUREN.length; i++) {
            MapColor kleur = kleuren[i];
            String id = "guh_dakpan_" + DAKPAN_KLEUREN[i];
            DeferredBlock<Block> vol = blok(id, Block::new, () -> dakpan(kleur));
            DAKPAN.add(vol);
            DAKPAN_PLAAT.add(blok(id + "_plaat", SlabBlock::new, () -> dakpan(kleur)));
            DAKPAN_TRAP.add(blok(id + "_trap", p -> new StairBlock(vol.get().defaultBlockState(), p), () -> dakpan(kleur)));
            DAKPAN_HOEK.add(blok(id + "_hoek", DakpanHoekBlock::new, () -> dakpan(kleur).noOcclusion()));
        }
    }

    // --- shoji and tatami ---------------------------------------------------------------------------------------------------
    public static final DeferredBlock<ShojiBlock> SHOJI = blok("shoji", ShojiBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW)
            .strength(0.6f).sound(SoundType.BAMBOO_WOOD).noOcclusion().ignitedByLava().isValidSpawn((s, l, pos, e) -> false)
            .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false).isRedstoneConductor((s, l, pos) -> false));
    public static final DeferredBlock<TatamiBlock> TATAMI = blok("tatami", TatamiBlock::new, () -> BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.6f).sound(SoundType.MOSS_CARPET).ignitedByLava());

    // --- tuinspul ---------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<ToroBlock> TORO = blok("toro", ToroBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ)
            .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5f, 6f).sound(SoundType.CALCITE).noOcclusion()
            .lightLevel(s -> s.getValue(ToroBlock.LIT) ? 13 : 0));
    public static final DeferredBlock<GeharktZand.Recht> GEHARKT_ZAND = blok("geharkt_zand", GeharktZand.Recht::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SAND));
    public static final DeferredBlock<GeharktZand.Ring> GEHARKT_ZAND_RING = blok("geharkt_zand_ring", GeharktZand.Ring::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SAND));
    public static final DeferredBlock<GuhBamboeBlock> GUH_BAMBOE = blok("guh_bamboe", GuhBamboeBlock::new, () -> BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT).strength(0.8f).sound(SoundType.BAMBOO).noOcclusion().dynamicShape().offsetType(BlockBehaviour.OffsetType.XZ)
            .ignitedByLava().pushReaction(PushReaction.DESTROY).isRedstoneConductor((s, l, pos) -> false));
    public static final DeferredBlock<BonsaiPotBlock> BONSAI_POT = blok("bonsai_pot", BonsaiPotBlock::new, () -> BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_WHITE).strength(0.4f).sound(SoundType.DECORATED_POT).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> GLADDE_KNUFFELSTEEN = blok("gladde_knuffelsteen", Block::new, () -> steen());
    public static final DeferredBlock<SlabBlock> GLADDE_KNUFFELSTEEN_PLAAT = blok("gladde_knuffelsteen_plaat", SlabBlock::new, () -> steen());
    public static final DeferredBlock<StairBlock> GLADDE_KNUFFELSTEEN_TRAP = blok("gladde_knuffelsteen_trap",
            p -> new StairBlock(GLADDE_KNUFFELSTEEN.get().defaultBlockState(), p), () -> steen());

    // --- esdoorn -------------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<RotatedPillarBlock> ESDOORN_STAM = blok("esdoorn_stam", RotatedPillarBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LOG));
    public static final DeferredBlock<Esdoorn.Bladeren> ESDOORN_BLADEREN_ROOD = blok("esdoorn_bladeren_rood", p -> new Esdoorn.Bladeren(Esdoorn.ROOD, p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES).mapColor(MapColor.COLOR_RED));
    public static final DeferredBlock<Esdoorn.Bladeren> ESDOORN_BLADEREN_ORANJE = blok("esdoorn_bladeren_oranje", p -> new Esdoorn.Bladeren(Esdoorn.ORANJE, p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES).mapColor(MapColor.COLOR_ORANGE));
    public static final DeferredBlock<Esdoorn.Zaailing> ESDOORN_ZAAILING = blok("esdoorn_zaailing", Esdoorn.Zaailing::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_SAPLING).mapColor(MapColor.COLOR_RED));

    /** The blocks whose item explains in a grey line what you can do with them (lang block.guhs.&lt;id&gt;.lore). */
    private static final List<DeferredBlock<?>> MET_UITLEG = new ArrayList<>();

    static {
        MET_UITLEG.addAll(DAKPAN_HOEK);
        MET_UITLEG.addAll(List.of(SHOJI, TATAMI, TORO, GEHARKT_ZAND, GEHARKT_ZAND_RING, BONSAI_POT));
        for (DeferredBlock<?> block : ALLES) {
            String id = block.getId().getPath();
            if (block == LAKHOUT_DEUR) {
                ITEMS.registerItem(id, p -> new DoubleHighBlockItem(block.get(), p), p -> p.useBlockDescriptionPrefix());
            } else if (MET_UITLEG.contains(block)) {
                ITEMS.registerItem(id, p -> new UitlegItem(block.get(), p), p -> p.useBlockDescriptionPrefix());
            } else {
                ITEMS.registerSimpleBlockItem(block);
            }
        }
    }

    /** A block item with one grey line of explanation. */
    public static class UitlegItem extends BlockItem {
        public UitlegItem(Block block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(BlokkenDalSlice::setup);
        NeoForge.EVENT_BUS.register(GeharktZand.Harken.class);
        BioZelftest.registreer("blokken-dal", BlokkenDalSlice::zelftest);
    }

    /**
     * In the real Guhmensie: both esdoorn trees grow (the sapling's two features load and place our blocks). Done on a
     * little platform high in the air above 0,0, which is cleared away again.
     */
    private static void zelftest(MinecraftServer server, ServerLevel level, BioZelftest.Melder meld) {
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                level.getChunk(cx, cz);
            }
        }
        var features = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
        BlockPos voet = new BlockPos(8, 200, 8);
        int nr = 0;
        for (var key : List.of(ESDOORN_BOOM_ROOD, ESDOORN_BOOM_ORANJE)) {
            DeferredBlock<Esdoorn.Bladeren> blad = nr++ == 0 ? ESDOORN_BLADEREN_ROOD : ESDOORN_BLADEREN_ORANJE;
            var feature = features.get(key);
            if (feature.isEmpty()) {
                meld.fout("no configured feature " + key.identifier());
                continue;
            }
            level.setBlock(voet.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            boolean gelukt = false;
            for (int poging = 0; poging < 8 && !gelukt; poging++) {
                gelukt = feature.get().value().place(level, level.getChunkSource().getGenerator(), RandomSource.create(20261007L + poging), voet);
            }
            int stam = 0, bladeren = 0;
            for (BlockPos p : BlockPos.betweenClosed(voet.offset(-8, -1, -8), voet.offset(8, 16, 8))) {
                BlockState state = level.getBlockState(p);
                stam += state.is(ESDOORN_STAM.get()) ? 1 : 0;
                bladeren += state.is(blad.get()) ? 1 : 0;
                if (!state.isAir()) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            meld.check(gelukt && stam >= 4 && bladeren >= 20, key.identifier().getPath() + ": " + stam + " stam, " + bladeren + " bladeren");
        }
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            for (DeferredBlock<?> hout : List.of(LAKHOUT_PLANKEN, LAKHOUT_PLAAT, LAKHOUT_TRAP, LAKHOUT_HEK, LAKHOUT_POORT, LAKHOUT_BALK)) {
                fire.setFlammable(hout.get(), 5, 20);
            }
            fire.setFlammable(ESDOORN_STAM.get(), 5, 5);
            fire.setFlammable(ESDOORN_BLADEREN_ROOD.get(), 30, 60);
            fire.setFlammable(ESDOORN_BLADEREN_ORANJE.get(), 30, 60);
            fire.setFlammable(GUH_BAMBOE.get(), 60, 60);
            fire.setFlammable(SHOJI.get(), 30, 60);
            fire.setFlammable(TATAMI.get(), 30, 60);
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<?> block : ALLES) {
            output.accept(new ItemStack(block.get()));
        }
    }

    /** Every block of the slice (creative order). */
    public static List<DeferredBlock<?>> alles() {
        return List.copyOf(ALLES);
    }

    private BlokkenDalSlice() {
    }
}
