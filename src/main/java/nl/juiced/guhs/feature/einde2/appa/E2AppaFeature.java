package nl.juiced.guhs.feature.einde2.appa;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2;
import nl.juiced.guhs.feature.einde2.Einde2Plaatshouder;

/**
 * einde2, slice appa: the Appa-guh and the Momo-guh.
 * Skeleton stub: the slice fills this class in place (it is already wired in feature/einde2/Einde2).
 * Resources: tools/features/einde2_appa.py.
 */
public final class E2AppaFeature {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    // --- the entity ids buildings may already place (build contract 3.4): stand-ins until this slice makes the real creatures ---
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> APPA_GUH = Einde2.plaatshouder(ENTITY_TYPES, "appa_guh");
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> MOMO_GUH = Einde2.plaatshouder(ENTITY_TYPES, "momo_guh");

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private E2AppaFeature() {
    }
}
