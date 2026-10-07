package nl.juiced.guhs.feature.einde2.bouwschelpenstad;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
import nl.juiced.guhs.feature.balto.BaltoBlocks;

/**
 * einde2, slice bouw_schelpenstad: the build of the road out of Bikinibodem, De Ruige Kroeg, the loopgraaf and Schelpenstad
 * (DESIGN 5b, the second half of the Sponsguh story), as a BUILD: no story logic and no cast, every character and prop of
 * the story is a marker in the sidecar {@code data/guhs/einde2/plekken/schelpenstad.json} ({@code Plekken}).
 * <ul>
 *   <li>the structure {@code guhs:schelpenstad} (a {@code guhs:leegte_bouw} in the deep band that stands next to
 *       Bikinibodem; protected and discovered by the skeleton: {@code Einde2Bescherming}, {@code Einde2Ontdekt});</li>
 *   <li>eight decorative blocks {@code einde2_bouw_schelpenstad_*} ({@link SchelpenstadBlokken});</li>
 *   <li>{@link Schelpenstad}: whoever walks the whole road out to the head of the pier gets the advancement
 *       {@code guheinde/einde2_bouw_schelpenstad_zee}.</li>
 * </ul>
 * hook fase B: the markers of the sidecar (the cast, the props, the routes, the scene spot of the wink over the
 * loopgraaf); {@link Schelpenstad#wereld} gives a marker's place in the world.
 * <p>
 * Resources: tools/features/einde2_bouw_schelpenstad.py (the build: einde2_bouw_schelpenstad_bouw.py and its parts, the
 * blocks: einde2_bouw_schelpenstad_blokken.py).
 */
public final class E2BouwSchelpenstadFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    /** Random salt of this slice (build contract 3.1). */
    public static final long SALT = 21401750L;
    private static final String P = "einde2_bouw_schelpenstad_";

    /** The cream stone full of little shells that Schelpenstad is built of. */
    public static final DeferredBlock<Block> SCHELPSTEEN = BLOCKS.registerSimpleBlock(P + "schelpsteen",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.5f, 6f).sound(SoundType.CALCITE).requiresCorrectToolForDrops());
    /** The drying lamp of the souvenir shop: hangs under a ceiling, light 15. */
    public static final DeferredBlock<SchelpenstadBlokken.Vorm> DROOGLAMP = BLOCKS.registerBlock(P + "drooglamp",
            p -> new SchelpenstadBlokken.Vorm(Block.box(3, 5, 3, 13, 16, 13), p),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 6f).sound(SoundType.LANTERN).noOcclusion().lightLevel(s -> 15)
                    .pushReaction(PushReaction.DESTROY));
    /** A dried souvenir (block state soort 0-3). */
    public static final DeferredBlock<SchelpenstadBlokken.Souvenir> SOUVENIR = BLOCKS.registerBlock(P + "souvenir", SchelpenstadBlokken.Souvenir::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.5f).sound(SoundType.BAMBOO_WOOD).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    /** The velvet cushion of the crown display. */
    public static final DeferredBlock<SchelpenstadBlokken.Vorm> KROONKUSSEN = BLOCKS.registerBlock(P + "kroonkussen",
            p -> new SchelpenstadBlokken.Vorm(Block.box(2, 0, 2, 14, 10, 14), p),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.8f).sound(SoundType.WOOL).noOcclusion());
    /** The wall socket of the drying lamp. */
    public static final DeferredBlock<SchelpenstadBlokken.Wandding> STOPCONTACT = BLOCKS.registerBlock(P + "stopcontact",
            p -> new SchelpenstadBlokken.Wandding(5, 11, p),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.0f, 6f).sound(SoundType.METAL).noOcclusion().noCollision()
                    .pushReaction(PushReaction.DESTROY));
    /** A sprinkler under the ceiling. */
    public static final DeferredBlock<SchelpenstadBlokken.Vorm> SPROEIER = BLOCKS.registerBlock(P + "sproeier",
            p -> new SchelpenstadBlokken.Vorm(Block.box(5, 9, 5, 11, 16, 11), p),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f, 6f).sound(SoundType.METAL).noOcclusion().noCollision()
                    .pushReaction(PushReaction.DESTROY));
    /** The neon sign of De Ruige Kroeg: an anchor with Mika ears, light 10. */
    public static final DeferredBlock<SchelpenstadBlokken.Wandding> NEON = BLOCKS.registerBlock(P + "neon",
            p -> new SchelpenstadBlokken.Wandding(0, 16, p),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(1.0f, 6f).sound(SoundType.GLASS).noOcclusion().noCollision()
                    .lightLevel(s -> 10).pushReaction(PushReaction.DESTROY));
    /** A rack of postcards. */
    public static final DeferredBlock<SchelpenstadBlokken.Vorm> KAARTENREK = BLOCKS.registerBlock(P + "kaartenrek",
            p -> new SchelpenstadBlokken.Vorm(Block.box(4, 0, 4, 12, 16, 12), p),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f, 6f).sound(SoundType.METAL).noOcclusion());
    /** The eight, in the order of the creative tab. */
    public static final List<DeferredBlock<? extends Block>> BLOKKEN = List.of(SCHELPSTEEN, SOUVENIR, KAARTENREK, KROONKUSSEN, DROOGLAMP, STOPCONTACT,
            SPROEIER, NEON);

    static {
        for (DeferredBlock<? extends Block> block : BLOKKEN) {       // each with a grey line that says what it is (<block>.lore)
            ITEMS.registerItem(block.getId().getPath(), p -> new BaltoBlocks.LoreBlock(block.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Schelpenstad::onPlayerTick);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<? extends Block> block : BLOKKEN) {
            output.accept(new ItemStack(block.get()));
        }
    }

    private E2BouwSchelpenstadFeature() {
    }
}
