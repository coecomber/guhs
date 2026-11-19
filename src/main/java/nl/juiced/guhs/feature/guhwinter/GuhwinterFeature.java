package nl.juiced.guhs.feature.guhwinter;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.guhwinter.kern.KernEvents;
import nl.juiced.guhs.feature.guhwinter.kern.KernRegistry;

/**
 * Guhs 1.10.10 "Guhwinter": the winter event on the Winterplein in the Guhmension (design: guhswinter/GUHSWINTER.md,
 * API: guhs_winter/CONTRACT.md). Kern first (the clock, moments, zone, pepernoot, commands); every slice adds ONE line
 * per method below (client side: {@link nl.juiced.guhs.feature.guhwinter.client.GuhwinterClient}).
 */
public final class GuhwinterFeature {
    private GuhwinterFeature() {
    }

    public static void register(IEventBus modBus) {
        KernRegistry.register(modBus);
        GuhwinterZone.register();
        KernEvents.register();
        GuhwinterCommando.register();
        // --- slices (one line each) ---
        nl.juiced.guhs.feature.guhwinter.hoentjes.GuhwinterHoentjes.register(modBus);   // waterhoentje + alpine hoentje
        nl.juiced.guhs.feature.guhwinter.tekenen.TekenenFeature.register(modBus);   // drawing editor, 2x2 drawing, kerstboom, Kerstalbum
        nl.juiced.guhs.feature.guhwinter.npc.GuhwinterNpcFeature.register(modBus);   // the Winterplein's guhs
        nl.juiced.guhs.feature.guhwinter.plein.GuhwinterPleinFeature.register(modBus);   // the plaza, frame, arrival, anchors, /inrichten
        nl.juiced.guhs.feature.guhwinter.spellen.SpellenFeature.register(modBus);   // race, schaatsrondje, oliebollen, shop, hidden pepernoten
        nl.juiced.guhs.feature.guhwinter.verhaal.WinterVerhaal.register(modBus);   // the 3-chapter story, its scenes, prikbord, reward blocks
        nl.juiced.guhs.feature.guhwinter.lootjes.LootjesFeature.register(modBus);   // Het Grote Boek, the draw, the 8 plots, the Surprise, pakjesavond
    }

    public static void payloads(PayloadRegistrar registrar) {
        // --- slices (one line each) ---
        nl.juiced.guhs.feature.guhwinter.tekenen.TekenenFeature.payloads(registrar);
        nl.juiced.guhs.feature.guhwinter.spellen.SpellenFeature.payloads(registrar);
        nl.juiced.guhs.feature.guhwinter.verhaal.WinterVerhaal.payloads(registrar);
        nl.juiced.guhs.feature.guhwinter.lootjes.LootjesFeature.payloads(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        KernRegistry.creative(output);
        // --- slices (one line each) ---
        nl.juiced.guhs.feature.guhwinter.hoentjes.GuhwinterHoentjes.creative(output);   // the hoentjes' spawn eggs
        nl.juiced.guhs.feature.guhwinter.tekenen.TekenenFeature.creative(output);   // the Kerstalbum
        nl.juiced.guhs.feature.guhwinter.spellen.SpellenFeature.creative(output);   // the games' blocks, chocomel, letters, skates, hats
        nl.juiced.guhs.feature.guhwinter.verhaal.WinterVerhaal.creative(output);   // Pakjesstapel, Kerststukje, Oliebollenkraam
        nl.juiced.guhs.feature.guhwinter.lootjes.LootjesFeature.creative(output);   // Het Grote Boek, the plot sign, the Surprise, the sack
    }
}
