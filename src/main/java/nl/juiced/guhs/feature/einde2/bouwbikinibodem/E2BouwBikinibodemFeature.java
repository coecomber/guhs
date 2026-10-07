package nl.juiced.guhs.feature.einde2.bouwbikinibodem;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.zwevers.Kruimelkwallen;

/**
 * einde2, slice bouw_bikinibodem: the build of Bikinibodem onder de leegte, the town of the Sponsguh story under its
 * soap-bubble dome, deep between the islands of the Guheinde. BUILD only: the structure {@code guhs:bikinibodem} (a
 * guhs:leegte_bouw in the deep band, one guaranteed copy) with its markers, and what the town itself needs to work:
 * <ul>
 *   <li>its three blocks: {@code zeepbel} (the dome), {@code bubbelstroom} (the lift between the island above the dome and
 *       the plein) and {@code bloemwolk} (the flowers in its sky), and eight decorative blocks of its houses
 *       ({@link BikinibodemBlokken});</li>
 *   <li>the purple leegtemist under the dome ({@link Leegtemist}: fog and drifting particles on the client);</li>
 *   <li>the Kwallenvelden: the zones {@code kwallenveld_*} are named to the Kruimelkwallen as places where their swarms
 *       come too ({@link Leegtemist#inKwallenveld}).</li>
 * </ul>
 * No story, no characters: a later phase reads the markers (feature/einde2/Plekken). Protection and discovery are those of
 * the skeleton (Einde2Bescherming, Einde2Ontdekt). Resources: tools/features/einde2_bouw_bikinibodem.py.
 */
public final class E2BouwBikinibodemFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    private static final String P = "einde2_bouw_bikinibodem_";

    // --- the ids the build of Schelpenstad may place too (build contract 3.4): do not rename them ---
    /** The dome: see-through with a soap sheen, solid like glass, no drops. */
    public static final DeferredBlock<Block> ZEEPBEL = BLOCKS.<Block>registerBlock("zeepbel", BikinibodemBlokken.Zeepbel::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.8f).noLootTable());
    /** The lift: carries whoever stands in it (block state {@code richting}). */
    public static final DeferredBlock<Block> BUBBELSTROOM = BLOCKS.<Block>registerBlock("bubbelstroom", BikinibodemBlokken.Bubbelstroom::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).noCollision().strength(0.4f).sound(SoundType.WOOL).noLootTable()
                    .noOcclusion().lightLevel(s -> 10).isValidSpawn((s, l, pos, e) -> false).isSuffocating((s, l, pos) -> false)
                    .isViewBlocking((s, l, pos) -> false).pushReaction(PushReaction.BLOCK));
    /** A soft flower cloud (block state {@code kleur} 0-3); it glows a little, so the flowers show in the dark sky of the dome. */
    public static final DeferredBlock<Block> BLOEMWOLK = BLOCKS.<Block>registerBlock("bloemwolk", BikinibodemBlokken.Bloemwolk::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.6f).sound(SoundType.WOOL).instrument(NoteBlockInstrument.GUITAR)
                    .lightLevel(s -> 10));

    // --- decoration of the houses ---
    /** Glowing coral: the street lighting of the town (on a post, or as a lump in the ground). */
    public static final DeferredBlock<Block> KORAAL = BLOCKS.registerSimpleBlock(P + "koraal",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BRAIN_CORAL_BLOCK).mapColor(MapColor.COLOR_ORANGE).lightLevel(s -> 14));
    public static final DeferredBlock<Block> PYLON = BLOCKS.<Block>registerBlock(P + "pylon", p -> new BikinibodemBlokken.Sierblok(Block.column(8, 0, 14), p),
            () -> sier(MapColor.COLOR_ORANGE).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> ZELFPORTRET = BLOCKS.<Block>registerBlock(P + "zelfportret", BikinibodemBlokken.Zelfportret::new,
            () -> sier(MapColor.WOOD).sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> KLARINET = BLOCKS.<Block>registerBlock(P + "klarinet", p -> new BikinibodemBlokken.Sierblok(Block.column(6, 0, 15), p),
            () -> sier(MapColor.COLOR_BLACK).sound(SoundType.WOOD));
    public static final DeferredBlock<Block> KLUIS = BLOCKS.<Block>registerBlock(P + "kluis", p -> new BikinibodemBlokken.Sierblok(Shapes.block(), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> KASSA = BLOCKS.<Block>registerBlock(P + "kassa", p -> new BikinibodemBlokken.Sierblok(Block.column(12, 0, 11), p),
            () -> sier(MapColor.COLOR_CYAN).sound(SoundType.METAL));
    public static final DeferredBlock<Block> CAMERA = BLOCKS.<Block>registerBlock(P + "camera", p -> new BikinibodemBlokken.Sierblok(Block.box(5, 8, 3, 11, 16, 13), p),
            () -> sier(MapColor.METAL).sound(SoundType.METAL).noCollision());
    public static final DeferredBlock<Block> ARCHIEFKAST = BLOCKS.<Block>registerBlock(P + "archiefkast", p -> new BikinibodemBlokken.Sierblok(Shapes.block(), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_GREEN));

    /** Every block (each has an item), in creative-tab order. */
    public static final List<DeferredBlock<Block>> BLOCK_ITEMS = List.of(ZEEPBEL, BUBBELSTROOM, BLOEMWOLK, KORAAL, PYLON, ZELFPORTRET, KLARINET, KLUIS, KASSA,
            CAMERA, ARCHIEFKAST);

    static {
        for (DeferredBlock<Block> block : BLOCK_ITEMS) {
            ITEMS.registerSimpleBlockItem(block);
        }
    }

    private static BlockBehaviour.Properties sier(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(1.0f).noOcclusion();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(BikinibodemBlokken.Bubbelstroom::onVal);
        Leegtemist.register();
        // the Kwallenvelden: the swarms of Kruimelkwallen also come in the fields of the town, whatever biome it lies in
        Kruimelkwallen.ookIn(Leegtemist::inKwallenveld);
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToClient(Leegtemist.Mist.TYPE, Leegtemist.Mist.STREAM_CODEC, Leegtemist.Mist::handle);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<Block> block : BLOCK_ITEMS) {
            output.accept(new ItemStack(block.get()));
        }
    }

    private E2BouwBikinibodemFeature() {
    }
}
