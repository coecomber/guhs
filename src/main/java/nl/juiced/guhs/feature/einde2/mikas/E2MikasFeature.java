package nl.juiced.guhs.feature.einde2.mikas;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Einde2;
import nl.juiced.guhs.feature.einde2.Einde2Plaatshouder;

/**
 * einde2, slice mikas: the Ender-Mika, the Kaasmijt, the Schuilguh and the Schuildoos.
 * Skeleton stub: the slice fills this class in place (it is already wired in feature/einde2/Einde2).
 * Resources: tools/features/einde2_mikas.py.
 */
public final class E2MikasFeature {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    // --- the entity ids buildings may already place (build contract 3.4): stand-ins until this slice makes the real creatures ---
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> ENDER_MIKA = Einde2.plaatshouder(ENTITY_TYPES, "ender_mika");
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> KAASMIJT = Einde2.plaatshouder(ENTITY_TYPES, "kaasmijt");
    public static final DeferredHolder<EntityType<?>, EntityType<Einde2Plaatshouder>> SCHUILGUH = Einde2.plaatshouder(ENTITY_TYPES, "schuilguh");
    public static final DeferredItem<Item> FLITSKRUIMEL = ITEMS.registerItem("flitskruimel", Item::new, () -> new Item.Properties());
    public static final DeferredItem<Item> DOOSSCHIL = ITEMS.registerItem("doosschil", Item::new, () -> new Item.Properties());

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private E2MikasFeature() {
    }
}
