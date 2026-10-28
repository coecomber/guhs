package nl.juiced.guhs.feature.dirigent;

import java.util.function.Consumer;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * 1.8.0 (uitbreiding U3): the Dirigent in de Koorkom. The dirigeerstokje ({@link Dirigeerstokje}) starts a {@link Concert} at a
 * Koorkom's podium in De Zingende Weide; the concerts tick here. Texts, models and the recipe: tools/features/dirigent.py.
 */
public final class DirigentFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredItem<Dirigeerstokje> STOKJE = ITEMS.registerItem("dirigeerstokje",
            p -> new Dirigeerstokje(p, false), () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Dirigeerstokje> GOUDEN_STOKJE = ITEMS.registerItem("gouden_dirigeerstokje",
            p -> new Dirigeerstokje(p, true), () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    private DirigentFeature() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> Concert.tickAlles());
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> Concert.wisAlles());
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(STOKJE.get()));
        output.accept(new ItemStack(GOUDEN_STOKJE.get()));
    }
}
