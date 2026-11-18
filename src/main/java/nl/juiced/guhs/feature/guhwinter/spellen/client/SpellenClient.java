package nl.juiced.guhs.feature.guhwinter.spellen.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import nl.juiced.guhs.feature.guhwinter.spellen.SpellenPayloads;

/** Client side of the Guhwinter games: the screens of the Pepernotenrace, oliebollen bakken and the pepernoten shop. */
public final class SpellenClient {
    public static void init(IEventBus modBus) {
        SpellenPayloads.raceOpener = p -> opClient(() -> Minecraft.getInstance().setScreen(new RaceScherm(p)));
        SpellenPayloads.raceUitslag = p -> opClient(() -> {
            if (Minecraft.getInstance().screen instanceof RaceScherm s && s.pos().equals(p.pos())) {
                s.uitslag(p);
            }
        });
        SpellenPayloads.olieOpener = p -> opClient(() -> Minecraft.getInstance().setScreen(new OliebollenScherm(p)));
        SpellenPayloads.winkelOpener = p -> opClient(() -> Minecraft.getInstance().setScreen(new WinkelScherm(p)));
        SpellenPayloads.winkelStand = p -> opClient(() -> {
            if (Minecraft.getInstance().screen instanceof WinkelScherm s) {
                s.stand(p);
            }
        });
    }

    private static void opClient(Runnable r) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isSameThread()) {
            r.run();
        } else {
            mc.execute(r);
        }
    }

    private SpellenClient() {
    }
}
