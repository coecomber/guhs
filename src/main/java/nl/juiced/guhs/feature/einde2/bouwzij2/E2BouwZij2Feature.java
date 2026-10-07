package nl.juiced.guhs.feature.einde2.bouwzij2;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * einde2, slice bouw_zij2: the builds of five side places of the outer islands: the Kyoshi-dorp, the Bibliotheek van Wan Shi
 * Guh, the Leeuwschildpad-eiland, the Leegtehaven and the Guhvleugel-ringenrace. Each is a guhs:leegte_bouw in the high band
 * (feature/einde2/LeegteBouwStructure), listed in the Superkompas and protected by feature/einde2/Einde2: nothing of that
 * needs code here. The builds hold no characters and no quest logic; the spots a later phase needs are markers
 * ({@link nl.juiced.guhs.feature.einde2.Plekken}; {@link Zij2Plekken} names the ones of this slice).
 * <p>
 * The one thing registered here is the block of the race's hoops. Resources: tools/features/einde2_bouw_zij2.py and its
 * five builders (einde2_bouw_zij2_kyoshi / _bieb / _schildpad / _haven / _race).
 */
public final class E2BouwZij2Feature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** A glowing segment of a ring of the Guhvleugel-ringenrace. No behaviour: the race reads the ring markers. */
    public static final DeferredBlock<Block> RINGENRACE_RING = BLOCKS.registerSimpleBlock("ringenrace_ring",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.5f, 6.0f).sound(SoundType.AMETHYST).lightLevel(s -> 12));

    static {
        ITEMS.registerSimpleBlockItem(RINGENRACE_RING);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(RINGENRACE_RING.get()));
    }

    private E2BouwZij2Feature() {
    }
}
