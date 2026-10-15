package nl.juiced.guhs.feature.einde2.geestenwereld;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.balto.BaltoBlocks;
import nl.juiced.guhs.feature.einde2.Bewoners;

/**
 * einde2, slice geestenwereld: De Geestenwereld (DESIGN_EINDE2 5 "De Geestenwereld" + "Uitwerking"), a small dimension of
 * its own ({@code guhs:geestenwereld}): floating islands in lila mist, no sun, a sky of slow pastel northern lights, seven
 * zones built by hand (the Meditatiecirkel where you arrive, the Gefluisterwoud, Koh's Grot, the Witte Lotus-ruines with the
 * Top der Voorgangers, the Hemelvijver, the Theepaviljoen and Fang's Hemelpad), connected by floating paths with lotus
 * leaves. Nothing can be broken or built there and nobody gets hurt.
 * <p>
 * This class registers the blocks with "turned around" colours ({@link GeestBlokken}), the feature that puts the build down
 * ({@link GeestBouw}), the sounds, protection ({@link GeestBescherming}), the dev commands ({@link GeestCommando}) and the
 * residents at the markers (the shared {@link Bewoners} rule, through {@link Bewoners#vast}). The creatures themselves are the
 * slice geestmodellen's; a marker that names an entity type which is not there yet is skipped (Bewoners logs it once).
 * <p>
 * hook fase B: the way in ({@link Geestenwereld#stuur}, "mediteren": the story's cutscene, guests within 5 blocks), the
 * puzzle's gate ({@link Lotuspuzzel#OPGELOST}), the lost lanterns ({@code verdwaalde_lantaarn_1..8} -> {@code
 * lantaarnboom_haak_1..8}), Hei Bai-guh's acorn ({@code eikel_plek}), Koh's moment ({@code koh_raadsel}), Fang's flight
 * ({@code fang_vlucht}). Resources: tools/features/einde2_geestenwereld.py (+ _bouw, _tex, _tekst, _geluid).
 */
public final class E2GeestenwereldFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredHolder<Feature<?>, GeestBouw> STUKKEN = FEATURES.register("einde2_geestenwereld_stukken",
            () -> new GeestBouw(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<SoundEvent, SoundEvent> SFEER = sound("einde2_geestenwereld.sfeer");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEFLUISTER = sound("einde2_geestenwereld.gefluister");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEDITATIE = sound("einde2_geestenwereld.meditatie");
    public static final DeferredHolder<SoundEvent, SoundEvent> TEGEL = sound("einde2_geestenwereld.tegel");

    private static Supplier<BlockBehaviour.Properties> steen(MapColor kleur) {
        return () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).mapColor(kleur);
    }

    private static Supplier<BlockBehaviour.Properties> hout(MapColor kleur) {
        return () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_LOG).mapColor(kleur);
    }

    private static Supplier<BlockBehaviour.Properties> blad(MapColor kleur, int licht) {
        return () -> BlockBehaviour.Properties.of().mapColor(kleur).strength(0.2f).sound(SoundType.AZALEA_LEAVES).noOcclusion()
                .lightLevel(s -> licht).isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                .pushReaction(PushReaction.DESTROY);
    }

    public static final DeferredBlock<Block> GEESTENGRAS = BLOCKS.registerSimpleBlock("geestengras",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).mapColor(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<Block> GEESTENAARDE = BLOCKS.registerSimpleBlock("geestenaarde",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).mapColor(MapColor.COLOR_LIGHT_BLUE));
    public static final DeferredBlock<RotatedPillarBlock> GEESTENHOUT = BLOCKS.registerBlock("geestenhout", RotatedPillarBlock::new, hout(MapColor.SNOW));
    public static final DeferredBlock<Block> GEESTENPLANKEN = BLOCKS.registerSimpleBlock("geestenplanken",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_PLANKS).mapColor(MapColor.SNOW));
    public static final DeferredBlock<RotatedPillarBlock> SCHADUWHOUT = BLOCKS.registerBlock("schaduwhout", RotatedPillarBlock::new, hout(MapColor.COLOR_BLACK));
    public static final DeferredBlock<Block> SCHADUWPLANKEN = BLOCKS.registerSimpleBlock("schaduwplanken",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS).mapColor(MapColor.COLOR_BLACK));
    public static final DeferredBlock<Block> GLOEIBLADEREN_CYAAN = BLOCKS.registerBlock("gloeibladeren_cyaan", Block::new, blad(MapColor.COLOR_CYAN, 13));
    public static final DeferredBlock<Block> GLOEIBLADEREN_ROZE = BLOCKS.registerBlock("gloeibladeren_roze", Block::new, blad(MapColor.COLOR_PINK, 13));
    public static final DeferredBlock<Block> SCHADUWBLADEREN = BLOCKS.registerBlock("schaduwbladeren", Block::new, blad(MapColor.COLOR_PURPLE, 0));
    public static final DeferredBlock<Block> MAANSTEEN = BLOCKS.registerSimpleBlock("maansteen", steen(MapColor.QUARTZ));
    public static final DeferredBlock<Block> MAANSTEEN_TEGELS = BLOCKS.registerSimpleBlock("maansteen_tegels", steen(MapColor.QUARTZ));
    public static final DeferredBlock<StairBlock> MAANSTEEN_TRAP = BLOCKS.registerBlock("maansteen_trap",
            p -> new StairBlock(MAANSTEEN_TEGELS.get().defaultBlockState(), p), steen(MapColor.QUARTZ));
    public static final DeferredBlock<SlabBlock> MAANSTEEN_PLAAT = BLOCKS.registerBlock("maansteen_plaat", SlabBlock::new, steen(MapColor.QUARTZ));
    public static final DeferredBlock<GeestBlokken.Nevelwater> NEVELWATER = BLOCKS.registerBlock("nevelwater", GeestBlokken.Nevelwater::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).noCollision().strength(0.3f).sound(SoundType.WOOL).noOcclusion()
                    .lightLevel(s -> 8).isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<RotatedPillarBlock> GEESTPADDENSTOEL_STEEL = BLOCKS.registerBlock("geestpaddenstoel_steel", RotatedPillarBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.MUSHROOM_STEM).mapColor(MapColor.SNOW));
    public static final DeferredBlock<Block> GEESTPADDENSTOEL_HOED = BLOCKS.registerSimpleBlock("geestpaddenstoel_hoed",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM_BLOCK).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 12));
    public static final DeferredBlock<GeestBlokken.Lotusblad> LOTUSBLAD = BLOCKS.registerBlock("lotusblad", GeestBlokken.Lotusblad::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.3f).sound(SoundType.LILY_PAD).noOcclusion());
    public static final DeferredBlock<LanternBlock> GEESTLANTAARN = BLOCKS.registerBlock("geestlantaarn", LanternBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_LANTERN).mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 15));
    public static final DeferredBlock<GeestBlokken.Geestenmasker> GEESTENMASKER = BLOCKS.registerBlock("geestenmasker", GeestBlokken.Geestenmasker::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.5f).sound(SoundType.WOOD).noCollision().noOcclusion()
                    .lightLevel(s -> 3).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<GeestBlokken.Meditatiesteen> MEDITATIESTEEN = BLOCKS.registerBlock("meditatiesteen", GeestBlokken.Meditatiesteen::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 7));
    public static final DeferredBlock<GeestBlokken.Gedenksteen> GEDENKSTEEN = BLOCKS.registerBlock("gedenksteen", GeestBlokken.Gedenksteen::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).mapColor(MapColor.COLOR_GRAY).lightLevel(s -> 4));
    public static final DeferredBlock<GeestBlokken.Lotuspuzzeltegel> LOTUSPUZZELTEGEL = BLOCKS.registerBlock("lotuspuzzeltegel",
            GeestBlokken.Lotuspuzzeltegel::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_DEEPSLATE).mapColor(MapColor.COLOR_BROWN));

    /** Every block (each has an item with a grey line under its name: {@code <block>.lore}), in creative-tab order. */
    public static final List<DeferredBlock<? extends Block>> BLOKKEN = List.of(GEESTENGRAS, GEESTENAARDE, GEESTENHOUT, GEESTENPLANKEN, SCHADUWHOUT,
            SCHADUWPLANKEN, GLOEIBLADEREN_CYAAN, GLOEIBLADEREN_ROZE, SCHADUWBLADEREN, MAANSTEEN, MAANSTEEN_TEGELS, MAANSTEEN_TRAP, MAANSTEEN_PLAAT,
            NEVELWATER, GEESTPADDENSTOEL_STEEL, GEESTPADDENSTOEL_HOED, LOTUSBLAD, GEESTLANTAARN, GEESTENMASKER, MEDITATIESTEEN, GEDENKSTEEN,
            LOTUSPUZZELTEGEL);

    static {
        for (DeferredBlock<? extends Block> block : BLOKKEN) {
            ITEMS.registerItem(block.getId().getPath(), p -> new BaltoBlocks.LoreBlock(block.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
        }
    }

    /**
     * The residents at the markers of the build (CONTRACT_E2 11.7 through {@link Bewoners#vast}): the looks of the creatures
     * that come more than once, and the ones that move about (a creature type that is not there yet is skipped).
     */
    private static void bewoners() {
        Bewoners.vast(Geestenwereld.NAAM, Geestenwereld.DIM, Geestenwereld.OORSPRONG);
        for (int i = 1; i <= 4; i++) {
            Bewoners.anders(Geestenwereld.NAAM + "/lantaarngeest_" + i, "guhs:lantaarngeest", i - 1, true, 0);
        }
        Bewoners.anders(Geestenwereld.NAAM + "/koi_tui", "guhs:geest_koi", 0, false, 14);
        Bewoners.anders(Geestenwereld.NAAM + "/koi_la", "guhs:geest_koi", 1, false, 14);
        Bewoners.anders(Geestenwereld.NAAM + "/hei_bai", "guhs:hei_bai_guh", 0, true, 0);
        for (int i = 1; i <= 4; i++) {
            Bewoners.anders(Geestenwereld.NAAM + "/schaduwgeest_" + i, "guhs:schaduwgeest", -1, false, 5);
        }
        for (int i = 1; i <= 6; i++) {
            Bewoners.anders(Geestenwereld.NAAM + "/libelkonijnguh_" + i, "guhs:libelkonijnguh", -1, false, 8);
        }
        // geest_bouw (CONTRACT_E2_B 4.5): the masks that watch you in Koh's cave (each its own face), the knowledge foxes
        // that walk about, and the looks of the lantern spirits at the tea, at Miroh's table and at the fishing jetty
        for (int i = 1; i <= 12; i++) {
            Bewoners.anders(Geestenwereld.NAAM + "/kijkmasker_" + i, "guhs:kijkmasker", i - 1, true, 0);
        }
        for (int i = 1; i <= 4; i++) {
            Bewoners.anders(Geestenwereld.NAAM + "/kennisvos_" + i, "guhs:kennisvosje", -1, false, 8);
        }
        Bewoners.anders(Geestenwereld.NAAM + "/roddel_1", "guhs:lantaarngeest", 1, true, 0);
        Bewoners.anders(Geestenwereld.NAAM + "/geest_gast_1", "guhs:lantaarngeest", 2, true, 0);
        Bewoners.anders(Geestenwereld.NAAM + "/visgeest_1", "guhs:lantaarngeest", 3, true, 0);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
        SOUNDS.register(modBus);
        GeestBescherming.register();
        NeoForge.EVENT_BUS.addListener(GeestCommando::register);
        NeoForge.EVENT_BUS.addListener(GeestBouw::opLevelLoad);
        NeoForge.EVENT_BUS.addListener(GeestBouw::opChunkLoad);
        NeoForge.EVENT_BUS.addListener(GeestBouw::opTick);
        bewoners();
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<? extends Block> block : BLOKKEN) {
            output.accept(new ItemStack(block.get()));
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    private E2GeestenwereldFeature() {
    }
}
