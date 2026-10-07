package nl.juiced.guhs.feature.einde2.bouwaarde;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.balto.BaltoBlocks;

/**
 * einde2, slice bouw_aarde: the build of Ba Sing Guh, the walled city of the earth chapter of the Guhvatar story.
 * <p>
 * The city itself is data: the structure {@code guhs:ba_sing_guh} (a guhs:leegte_bouw of 256 x 110 x 256 in 8 x 8 tiles,
 * one guaranteed copy per world, protected and found by the skeleton: Einde2Bescherming, Einde2Ontdekt), its markers in
 * {@code data/guhs/einde2/plekken/ba_sing_guh.json} (read through Plekken) and its two overlays (the koolman's stall, whole
 * and wrecked: {@link Koolkraam}). This class only registers the six decorative blocks the city is built with; none of them
 * does anything. NO story logic lives here: a later phase puts the characters on the markers.
 * <p>
 * Resources: tools/features/einde2_bouw_aarde.py (the build: einde2_bouw_aarde_bouw.py, _stad.py, _onder.py; textures:
 * einde2_bouw_aarde_tex.py).
 */
public final class E2BouwAardeFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** The big, pale stone of the walls of Ba Sing Guh (dry klipkaas). */
    public static final DeferredBlock<Block> STADSSTEEN = BLOCKS.registerSimpleBlock("einde2_bouw_aarde_stadssteen",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.SAND));
    /** The coin of the earth kingdom of the guhs (round, with a square bite out of it), over every gate. */
    public static final DeferredBlock<Block> AARDEMUNT = BLOCKS.registerSimpleBlock("einde2_bouw_aarde_aardemunt",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS).mapColor(MapColor.COLOR_GREEN));
    /** One head of kool: the whole trade of the koolman (and what his stall, whole or wrecked, is made of). */
    public static final DeferredBlock<Block> KOOL = BLOCKS.registerBlock("einde2_bouw_aarde_kool", E2BouwAardeBlokken.Kool::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.6f).sound(SoundType.MOSS).noOcclusion());
    /** The beam the monorail of Ba Sing Guh runs on. */
    public static final DeferredBlock<Block> SPOOR = BLOCKS.registerSimpleBlock("einde2_bouw_aarde_spoor",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE).mapColor(MapColor.SAND));
    /** The green, glowing crystal of the catacombs of Old Ba Sing Guh. */
    public static final DeferredBlock<Block> GROEN_KRISTAL = BLOCKS.registerSimpleBlock("einde2_bouw_aarde_groen_kristal",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).mapColor(MapColor.EMERALD).lightLevel(s -> 12));
    /** A crate of jasmine tea (De Jasmijn Draak has stacks of them; so has the base under the lake). */
    public static final DeferredBlock<Block> THEEKIST = BLOCKS.registerSimpleBlock("einde2_bouw_aarde_theekist",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).mapColor(MapColor.WOOD));

    /** Every block (each has an item with a grey line under its name), in creative-tab order. */
    public static final List<DeferredBlock<Block>> BLOCK_ITEMS = List.of(STADSSTEEN, AARDEMUNT, SPOOR, GROEN_KRISTAL, THEEKIST, KOOL);

    static {
        for (DeferredBlock<Block> block : BLOCK_ITEMS) {
            ITEMS.registerItem(block.getId().getPath(), p -> new BaltoBlocks.LoreBlock(block.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(E2BouwAardeCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<Block> block : BLOCK_ITEMS) {
            output.accept(new ItemStack(block.get()));
        }
    }

    private E2BouwAardeFeature() {
    }
}
