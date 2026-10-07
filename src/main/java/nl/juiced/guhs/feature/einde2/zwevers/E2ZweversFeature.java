package nl.juiced.guhs.feature.einde2.zwevers;

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
 * einde2, slice zwevers: the Kruimelkwallen, the Leegtewalvisguh and the four element animals.
 * Skeleton stub: the slice fills this class in place (it is already wired in feature/einde2/Einde2).
 * Resources: tools/features/einde2_zwevers.py.
 */
public final class E2ZweversFeature {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    // --- the entity ids buildings may already place (build contract 3.4): stand-ins until this slice makes the real creatures ---
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> KRUIMELKWAL = Einde2.plaatshouder(ENTITY_TYPES, "kruimelkwal");
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> LEEGTEWALVISGUH = Einde2.plaatshouder(ENTITY_TYPES, "leegtewalvisguh");
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> VLEERMUIS_LEMURGUH = Einde2.plaatshouder(ENTITY_TYPES, "vleermuis_lemurguh");
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> OTTER_PINGUINGUH = Einde2.plaatshouder(ENTITY_TYPES, "otter_pinguinguh");
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> DAS_MOLGUH = Einde2.plaatshouder(ENTITY_TYPES, "das_molguh");
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> SCHILDPAD_EENDGUH = Einde2.plaatshouder(ENTITY_TYPES, "schildpad_eendguh");

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private E2ZweversFeature() {
    }
}
