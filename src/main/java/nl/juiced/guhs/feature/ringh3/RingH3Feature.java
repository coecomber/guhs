package nl.juiced.guhs.feature.ringh3;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.wereld.Plaatshouder;

/**
 * bbq2 (ring-h3): chapter 3 of the Knabbelring: De Mijnen van Knabbelmoria.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/ring_h3.py.
 * The fields are the fixed ids of CONTRACT_130 7 as plain placeholders: the owner replaces the implementation and keeps the
 * name and the field type (subclasses are fine).
 */
public final class RingH3Feature {
    /**
     * The questline of this part of the story. F0 placeholder of one step (CONTRACT_130 6.2.5): the travel map of ring-kern
     * has its halte before the slice exists; the slice gives it its real steps. Texts: tools/features/ring_h3.py.
     */
    public static final Verhaallijn LIJN = Verhaallijn.maak("ring_h3", "knabbelring").stappen(1).na("ring_h2").registreer();

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Plaatshouder>> BARBECUEROG = Plaatshouder.type(ENTITY_TYPES, "barbecuerog");

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private RingH3Feature() {
    }
}
