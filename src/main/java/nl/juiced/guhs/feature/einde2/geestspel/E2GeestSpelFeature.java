package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * einde2 phase B/C, slice geest_spel: the play of the Geestenwereld (zone access, the five trials, lanterns, Koh, the chakra path, fast travel, sightings, Wan-guh).
 * Skelet stub (CONTRACT_E2_B 9): the slice fills this class in place (it is already wired in feature/einde2/Einde2).
 * Resources: tools/features/einde2_geest_spel.py.
 */
public final class E2GeestSpelFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private E2GeestSpelFeature() {
    }
}
