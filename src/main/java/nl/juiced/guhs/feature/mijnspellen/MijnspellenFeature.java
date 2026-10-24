package nl.juiced.guhs.feature.mijnspellen;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.mijnspellen.karretjes.KarretjesFeature;
import nl.juiced.guhs.feature.mijnspellen.kegelgrot.KegelgrotFeature;

/**
 * 1.7.0 "Onder de Wol" (design: ontwerp-guhs170-mijnen.md, ronde 41-43): two of the five cave minigames, each in its own
 * cave hall under a little entrance hut with a Wolkenliftje, with its own coin (also good in the grijpmachine), shop,
 * highscores and clothes:
 * <ul>
 *   <li>Karretjes-wissels in the Wisselgrot ({@link KarretjesFeature}): ertsbonnetjes, the conducteurspetje;</li>
 *   <li>De Kegelgrot ({@link KegelgrotFeature}): guh bowling, kegelknopen, the bowlingshirtje and the zweetbandje.</li>
 * </ul>
 * Resources (blocks, halls, texts): tools/features/mijnspellen.py.
 */
public final class MijnspellenFeature {
    public static void register(IEventBus modBus) {
        KarretjesFeature.register(modBus);
        KegelgrotFeature.register(modBus);
        NeoForge.EVENT_BUS.register(Mijnspel.class);
        Protected.add(Mijnspel::inGebouw);
    }

    public static void creative(Consumer<ItemStack> output) {
        KarretjesFeature.creative(output);
        KegelgrotFeature.creative(output);
    }

    private MijnspellenFeature() {
    }
}
