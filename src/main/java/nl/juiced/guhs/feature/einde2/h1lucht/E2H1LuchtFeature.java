package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import net.neoforged.neoforge.registries.DeferredHolder;
import nl.juiced.guhs.feature.einde2.Einde2;
import nl.juiced.guhs.feature.einde2.Einde2Plaatshouder;

/**
 * einde2 phase B/C, slice h1_lucht: chapter 1 of the Guhvatar story (the kaasbol, the opening film, the Zuidelijke Luchttempel, Zuka at the pier), the recipe of the Oog van Vadsig, the travel map.
 * Skelet stub (CONTRACT_E2_B 9): the slice fills this class in place (it is already wired in feature/einde2/Einde2).
 * Resources: tools/features/einde2_h1_lucht.py.
 */
public final class E2H1LuchtFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /** The kaasbol at the Knabbelberg (CONTRACT_E2_B 7.2): a stand-in until this slice registers the real entity under this id. */
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> KAASBOL = Einde2.plaatshouder(ENTITY_TYPES, "kaasbol");

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private E2H1LuchtFeature() {
    }
}
