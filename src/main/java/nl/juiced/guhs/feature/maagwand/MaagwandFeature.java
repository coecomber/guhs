package nl.juiced.guhs.feature.maagwand;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 1.8.1: the maagwand ({@link Maagwand}, {@link MaagwandEten}, on the client {@code client/MaagwandClient}: the living tongue next
 * to the inventory with its guhlichaampjes). Resources: tools/features/maagwand.py. Test command: /guhs maagwand.
 */
public final class MaagwandFeature {
    private MaagwandFeature() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(MaagwandEten::opStart);
        NeoForge.EVENT_BUS.addListener(MaagwandEten::opTick);
        NeoForge.EVENT_BUS.addListener(MaagwandEten::opKlaar);
        NeoForge.EVENT_BUS.addListener(MaagwandEten::opEffect);
        NeoForge.EVENT_BUS.addListener(MaagwandEten::opSpelerTick);
        NeoForge.EVENT_BUS.addListener(MaagwandEten::opLogin);
        NeoForge.EVENT_BUS.addListener(MaagwandCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
        MaagwandPayloads.register(registrar);
    }

    /** For food eaten from a placed block: may it, or does the stomach say BOERP? */
    public static boolean blokEten(ServerPlayer p, ItemStack stack) {
        return MaagwandEten.blokMag(p, stack);
    }
}
