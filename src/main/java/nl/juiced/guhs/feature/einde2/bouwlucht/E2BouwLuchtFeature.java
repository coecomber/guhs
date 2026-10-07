package nl.juiced.guhs.feature.einde2.bouwlucht;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhWorkstationBlock;
import nl.juiced.guhs.feature.balto.BaltoBlocks;

/**
 * einde2, slice bouw_lucht: the build of the Zuidelijke Luchttempel (DESIGN 5, chapter 1: lucht), the first of the four
 * story structures of the Guhvatar. A {@code guhs:leegte_bouw} that brings its own floating island, one copy per world on
 * the outer ring, protected ({@code Einde2Bescherming}, registered by {@code Einde2} for every story structure) and in no
 * compass tab. The build itself, its markers ({@code Plekken.van("zuidelijke_luchttempel")}) and the five overlay stages of
 * the Vredesballon ({@code laag_vredesballon_1 .. _5}) are data: this class only registers the blocks the temple is made of
 * that the game does not have yet.
 * <p>
 * BUILD only: no story logic and no characters. What the later phases continue with:
 * hook fase B: the markers (the cast, the training, the chase after Momo-guh, the rest points, the wink with Sjokkel);
 * hook fase C: {@link #WERKPLAATSDEUR} + {@code Einde2Haken.WERKPLAATS_OPEN} (the workshop opens per player), the markers
 * {@code werktuigguh}, {@code werkbank}, {@code oefen_1 .. oefen_6}, {@code vredesballon_anker} and
 * {@code vredesballon_teller}, and {@code Plekken.plaatsLaag / ruimLaag} for the stages.
 * <p>
 * Resources: tools/features/einde2_bouw_lucht.py (the build: einde2_bouw_lucht_bouw.py, the balloon:
 * einde2_bouw_lucht_ballon.py, textures: einde2_bouw_lucht_tex.py).
 */
public final class E2BouwLuchtFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** The closed door of the Werktuigguh: unbreakable, and solid for everybody the hook does not name. */
    public static final DeferredBlock<E2BouwLuchtBlokken.Werkplaatsdeur> WERKPLAATSDEUR = BLOCKS.registerBlock("werkplaatsdeur", E2BouwLuchtBlokken.Werkplaatsdeur::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0f, 3600000.0f).sound(SoundType.WOOD).noLootTable().dynamicShape()
                    .isValidSpawn((s, l, pos, e) -> false).isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false)
                    .pushReaction(PushReaction.BLOCK));
    /** The cream tile with the wind curl of the luchtnomaden. */
    public static final DeferredBlock<Block> TEGEL = BLOCKS.registerSimpleBlock("einde2_bouw_lucht_tegel",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).mapColor(MapColor.QUARTZ));
    public static final DeferredBlock<E2BouwLuchtBlokken.Pijltegel> PIJLTEGEL = BLOCKS.registerBlock("einde2_bouw_lucht_pijltegel", E2BouwLuchtBlokken.Pijltegel::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).mapColor(MapColor.QUARTZ));
    public static final DeferredBlock<E2BouwLuchtBlokken.Taart> TAART = BLOCKS.registerBlock("einde2_bouw_lucht_taart", E2BouwLuchtBlokken.Taart::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(0.5f).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> TANDWIEL = BLOCKS.registerSimpleBlock("einde2_bouw_lucht_tandwiel",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).mapColor(MapColor.COLOR_BROWN));
    public static final DeferredBlock<GuhWorkstationBlock> BLAUWDRUK = BLOCKS.registerBlock("einde2_bouw_lucht_blauwdruk", GuhWorkstationBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_BLUE));

    /** Every block (each has an item with a grey line under its name: {@code <block>.lore}), in creative-tab order. */
    public static final List<DeferredBlock<? extends Block>> BLOKKEN = List.of(TEGEL, PIJLTEGEL, TAART, TANDWIEL, BLAUWDRUK, WERKPLAATSDEUR);

    static {
        for (DeferredBlock<? extends Block> block : BLOKKEN) {
            ITEMS.registerItem(block.getId().getPath(), p -> new BaltoBlocks.LoreBlock(block.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(E2BouwLuchtCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<? extends Block> block : BLOKKEN) {
            output.accept(new ItemStack(block.get()));
        }
    }

    private E2BouwLuchtFeature() {
    }
}
