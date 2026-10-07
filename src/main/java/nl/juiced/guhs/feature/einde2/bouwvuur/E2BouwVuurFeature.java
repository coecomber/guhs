package nl.juiced.guhs.feature.einde2.bouwvuur;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.balto.BaltoBlocks;

/**
 * einde2, slice bouw_vuur: the build of the Vuurnatie-hoofdstad (DESIGN 5, chapter 4), the fourth story structure of the
 * Guhvatar: a floating slab of scorched kaaskorst with the capital in its big crater, Het Kokend Rotsblok, the ruin of the
 * Zonnekrijgers, Sintelstrand, the camp of the Witte Lotus, the duel arena and the spot where the Leeuwschildpad-guh
 * surfaces (the overlay {@code vuurnatie_hoofdstad/laag_leeuwschildpad}).
 * <p>
 * The build itself is data: the structure {@code guhs:vuurnatie_hoofdstad} (type guhs:leegte_bouw, placed and protected by
 * the einde2 skeleton) and its markers in {@code data/guhs/einde2/plekken/vuurnatie_hoofdstad.json}, which a later phase
 * reads through {@code Plekken} ({@link #STRUCTUUR}). This class only registers the six decoration blocks the capital is
 * built with. None of them does anything, and none of them hurts: the lava is lukewarm and solid, the flame only glows.
 * <p>
 * Resources: tools/features/einde2_bouw_vuur.py (the build: einde2_bouw_vuur_bouw.py, _grond.py, _stad.py, _buiten.py;
 * the textures: einde2_bouw_vuur_tex.py).
 */
public final class E2BouwVuurFeature {
    /** The structure id of the capital (its sidecar of markers, its overlay, its protection all go by this name). */
    public static final String STRUCTUUR = "vuurnatie_hoofdstad";
    /** The overlay a later phase puts down and takes away: {@code Plekken.plaatsLaag(level, STRUCTUUR, LAAG_LEEUWSCHILDPAD)}. */
    public static final String LAAG_LEEUWSCHILDPAD = "leeuwschildpad";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    private static BlockBehaviour.Properties steen(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).instrument(NoteBlockInstrument.BASEDRUM).strength(1.5f, 6.0f).sound(SoundType.STONE);
    }

    /** Lava that went lukewarm: a solid, glowing block you can walk on. It looks dangerous and feels like a hot-water bottle. */
    public static final DeferredBlock<Block> LAUWE_LAVA = BLOCKS.registerSimpleBlock("einde2_bouw_vuur_lauwe_lava",
            () -> steen(MapColor.FIRE).sound(SoundType.BASALT).lightLevel(s -> 15));
    /** A flame that only glows: you walk straight through it (the flame curtain of the throne room, braziers, camp fires). */
    public static final DeferredBlock<Block> LAUWE_VLAM = BLOCKS.registerSimpleBlock("einde2_bouw_vuur_lauwe_vlam",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).noCollision().instabreak().sound(SoundType.WOOL).lightLevel(s -> 15)
                    .noOcclusion().pushReaction(PushReaction.DESTROY));
    /** The emblem of the Vuurnatie: a golden flame with Mika's angry eyes. */
    public static final DeferredBlock<Block> EMBLEEM = BLOCKS.registerSimpleBlock("einde2_bouw_vuur_embleem", () -> steen(MapColor.NETHER));
    /** Vuurheer Mikai in a golden frame (the same portrait on all four sides: he looks at everybody). */
    public static final DeferredBlock<Block> PORTRET_MIKAI = BLOCKS.registerSimpleBlock("einde2_bouw_vuur_portret_mikai",
            () -> steen(MapColor.GOLD).sound(SoundType.WOOD));
    /** Sandstone with the carved sun of the Zonnekrijgers. */
    public static final DeferredBlock<Block> ZONNESTEEN = BLOCKS.registerSimpleBlock("einde2_bouw_vuur_zonnesteen", () -> steen(MapColor.SAND));
    /** The tile of the Witte Lotus. */
    public static final DeferredBlock<Block> LOTUSTEGEL = BLOCKS.registerSimpleBlock("einde2_bouw_vuur_lotustegel", () -> steen(MapColor.QUARTZ));

    /** Every block (each has an item with a grey lore line), in creative-tab order. */
    public static final List<DeferredBlock<Block>> BLOKKEN = List.of(LAUWE_LAVA, LAUWE_VLAM, EMBLEEM, PORTRET_MIKAI, ZONNESTEEN, LOTUSTEGEL);
    public static final List<DeferredItem<BlockItem>> BLOK_ITEMS = BLOKKEN.stream()
            .map(blok -> ITEMS.<BlockItem>registerItem(blok.getId().getPath(), p -> new BaltoBlocks.LoreBlock(blok.get(), p),
                    () -> new Item.Properties().useBlockDescriptionPrefix()))
            .toList();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        // hook fase B: the story of chapter 4 (the Verhaallijn "guhvatar_vuur", Einde2Haken.GROTE_VERHALEN) finds its spots
        // with Plekken.plek(STRUCTUUR, "<id>") / Plekken.wereld(level, STRUCTUUR, "<id>"); nothing here needs changing for it
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<Block> blok : BLOKKEN) {
            output.accept(new ItemStack(blok.get()));
        }
    }

    private E2BouwVuurFeature() {
    }
}
