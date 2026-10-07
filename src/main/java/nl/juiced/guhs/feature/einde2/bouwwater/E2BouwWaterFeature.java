package nl.juiced.guhs.feature.einde2.bouwwater;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;

/**
 * einde2, slice bouw_water: the build of the Noordelijke Waterstam, the second story structure of the Guhvatar (water): a
 * city of ice and canals behind a great ice wall, on a floating berg of its own with a sea in front of it.
 * <p>
 * BUILD ONLY: the structure {@code guhs:noordelijke_waterstam} (a guhs:leegte_bouw, see
 * {@link nl.juiced.guhs.feature.einde2.LeegteBouwStructure}) is data; it is protected and found by the shared code of
 * einde2 ({@code Einde2Bescherming}, {@code Einde2Ontdekt}), and a later phase reads where its characters, props and scenes
 * go from the sidecar ({@code Plekken.van("noordelijke_waterstam")}). What lives here are the six decorative blocks the
 * build is made of. None of them does anything.
 * <p>
 * Resources: tools/features/einde2_bouw_water.py (the build: einde2_bouw_water_bouw.py and its helpers; the textures:
 * einde2_bouw_water_tex.py).
 */
public final class E2BouwWaterFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** Ice of the glacier, sawn into bricks: as hard as packed ice, but you do not slip on it and it never melts. */
    private static BlockBehaviour.Properties ijs() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.ICE).instrument(NoteBlockInstrument.CHIME).strength(1.2f, 3.0f).sound(SoundType.GLASS);
    }

    public static final DeferredBlock<Block> IJSSTEEN = BLOCKS.registerSimpleBlock("einde2_bouw_water_ijssteen", E2BouwWaterFeature::ijs);
    public static final DeferredBlock<StairBlock> IJSSTEEN_TRAP = BLOCKS.registerBlock("einde2_bouw_water_ijssteen_trap",
            p -> new StairBlock(IJSSTEEN.get().defaultBlockState(), p), E2BouwWaterFeature::ijs);
    public static final DeferredBlock<SlabBlock> IJSSTEEN_PLAAT = BLOCKS.registerBlock("einde2_bouw_water_ijssteen_plaat", SlabBlock::new,
            E2BouwWaterFeature::ijs);
    /** A light frozen into a block of clear ice: the street lamp, the chandelier and the paving light of the Waterstam. */
    public static final DeferredBlock<Block> IJSLANTAARN = BLOCKS.registerSimpleBlock("einde2_bouw_water_ijslantaarn",
            () -> ijs().lightLevel(s -> 15));
    /** The emblem of the Waterstam: a crescent moon over three waves (and two guh ears). */
    public static final DeferredBlock<Block> STAMTEKEN = BLOCKS.registerSimpleBlock("einde2_bouw_water_stamteken",
            () -> ijs().mapColor(MapColor.LAPIS));
    /** The emblem of the Vuurnatie, on the ships of admiraal Zhao-Mika: a black flame with the eyes of a Mika. */
    public static final DeferredBlock<Block> VUURNATIE_TEKEN = BLOCKS.registerSimpleBlock("einde2_bouw_water_vuurnatie_teken",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE).mapColor(MapColor.COLOR_RED));

    /** Every block (each has an item), in creative-tab order. */
    public static final List<DeferredBlock<? extends Block>> BLOCK_ITEMS = List.of(IJSSTEEN, IJSSTEEN_TRAP, IJSSTEEN_PLAAT, IJSLANTAARN, STAMTEKEN,
            VUURNATIE_TEKEN);

    static {
        for (DeferredBlock<? extends Block> block : BLOCK_ITEMS) {
            if (block == IJSSTEEN_TRAP || block == IJSSTEEN_PLAAT) {
                ITEMS.registerSimpleBlockItem(block);
            } else {
                // (one grey line under the name: block.guhs.<id>.lore)
                ITEMS.registerItem(block.getId().getPath(), p -> new LoreBlockItem(block.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
            }
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(E2BouwWaterCommando::register);
        // hook fase B: the story of the Waterstam (Verhaallijn "guhvatar_water") puts its characters, props and scenes on the
        // markers of Plekken.van(Einde2Ids.NOORDELIJKE_WATERSTAM); the overlay E2BouwWaterCommando.VLOOT (Plekken.plaatsLaag /
        // ruimLaag, or shown to one player) is Zhao's fleet in front of the wall. Nothing of that is here.
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<? extends Block> block : BLOCK_ITEMS) {
            output.accept(new ItemStack(block.get()));
        }
    }

    private E2BouwWaterFeature() {
    }
}
