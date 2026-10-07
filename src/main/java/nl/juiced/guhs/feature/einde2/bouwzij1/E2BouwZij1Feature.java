package nl.juiced.guhs.feature.einde2.bouwzij1;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * einde2, slice bouw_zij1: three big side places of the outer islands of the Guheinde, as buildings with markers (their
 * residents and side quests are a later phase): Omashguh (Kristalkaasklippen: a mountain town of three tiers with the
 * mail chutes, the palace of koning Bumi-guh and three trial rooms), the Westelijke Luchttempel (Kruimelwolken: it hangs
 * upside down under its island, with wind corridors and the stable of the luchtbizon-guhs) and the Vuurtempel van
 * Roku-guh (Knabbelkoorbos: a crescent island, a pagoda with five fire doors and a sanctum that is shut).
 * <p>
 * All three are guhs:leegte_bouw structures: placement, protection, discovery and their markers are the skeleton's
 * ({@code LeegteBouwStructure}, {@code Einde2Bescherming}, {@code Einde2Ontdekt}, {@code Plekken}). The one thing of its
 * own this slice has in the game is the block of the mail chutes, {@link PostgootBlock}.
 * Resources: tools/features/einde2_bouw_zij1.py (the builds: einde2_bouw_zij1_omashguh.py, _luchttempel.py,
 * _vuurtempel.py; their shared tools: _gereedschap.py).
 */
public final class E2BouwZij1Feature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** The ids of the three places (structure ids, sidecars, Superkompas entries). */
    public static final String OMASHGUH = "omashguh", LUCHTTEMPEL = "westelijke_luchttempel", VUURTEMPEL = "vuurtempel_roku";

    /** A block of a mail chute: slippery, and it carries whoever stands on it along (see {@link PostgootBlock}). */
    public static final DeferredBlock<PostgootBlock> POSTGOOT = BLOCKS.registerBlock("einde2_bouw_zij1_postgoot", PostgootBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PACKED_ICE).mapColor(MapColor.ICE).friction(0.98f).strength(1.5f));

    static {
        ITEMS.registerSimpleBlockItem(POSTGOOT);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        // hook fase B: the side quests of the three places (the trials of Bumi-guh and the slide race, the wind corridors and
        // the strayed herd, the five fire doors and Roku-guh) read the markers of data/guhs/einde2/plekken/<id>.json through
        // Plekken and ask Einde2Haken.ELEMENT_GELEERD before a resident gives the quest; nothing of that lives here yet.
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(POSTGOOT.get()));
    }

    private E2BouwZij1Feature() {
    }
}
