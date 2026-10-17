package nl.juiced.guhs.feature.einde2.spd2finale;

import java.util.function.Consumer;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 7.8), slice sp_d2_finale: part 2 steps 9-11 of {@code sponsguh_kroon}: home on David
 * Hasselguh's back, Plankmika's bucket helmets, the rock song "Ik ben een Guhfy Guhber"; part 2's rewards (the Sponsguh buddy,
 * Gerrit, Plankmika's antennas, the title Guhfy Guhber) and the cast's talk after the story. Wired in feature/einde2/Einde2
 * (marker {@code <einde2_sp_d2_finale/>}); the story itself: {@link E2SpD2Finale}.
 */
public final class E2SpD2FinaleFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    /** The bubble blaster of step 10 (kept after the story, a toy). */
    public static final DeferredItem<BellenblazerItem> BELLENBLAZER = ITEMS.registerItem("einde2_sp_d2_finale_bellenblazer", BellenblazerItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    private E2SpD2FinaleFeature() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        E2SpD2Finale.register();
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BELLENBLAZER.get()));
    }
}
