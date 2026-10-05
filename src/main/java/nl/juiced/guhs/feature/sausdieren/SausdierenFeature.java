package nl.juiced.guhs.feature.sausdieren;

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
import nl.juiced.guhs.feature.wereld.Plaatshouder;

/**
 * bbq2 (sausdieren): the Sausloper and its stable, the Sausblubje, blubroom and its Guhdrankje.
 * This is the F0 stub (CONTRACT_130 5.1): the slice fills it in place. Resources come from tools/features/sausdieren.py.
 * The fields are the fixed ids of CONTRACT_130 7 as plain placeholders: the owner replaces the implementation and keeps the
 * name and the field type (subclasses are fine).
 */
public final class SausdierenFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final DeferredItem<Item> BLUBROOM = ITEMS.registerItem("blubroom", Item::new, () -> new Item.Properties());
    public static final DeferredItem<Item> SAUSBLUBJE_POTJE = ITEMS.registerItem("sausblubje_potje", Item::new, () -> new Item.Properties());
    public static final DeferredHolder<EntityType<?>, EntityType<Plaatshouder>> SAUSBLUBJE = Plaatshouder.type(ENTITY_TYPES, "sausblubje");
    public static final DeferredHolder<EntityType<?>, EntityType<Plaatshouder>> SAUSLOPER = Plaatshouder.type(ENTITY_TYPES, "sausloper");

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BLUBROOM.get()));
        output.accept(new ItemStack(SAUSBLUBJE_POTJE.get()));
    }

    private SausdierenFeature() {
    }
}
