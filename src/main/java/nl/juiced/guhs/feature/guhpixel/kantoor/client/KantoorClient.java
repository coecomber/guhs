package nl.juiced.guhs.feature.guhpixel.kantoor.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.feature.guhpixel.kantoor.KantoorPayloads;
import nl.juiced.guhs.feature.guhpixel.kantoor.KantoorSlice;

/**
 * Client side of the guhpixel slice "kantoor": the Prikklok screen, the reading screen of the papers and the renderer
 * that draws the sleeping guh on a Bureautje's keyboard. GuhpixelClient calls {@link #init}.
 */
public final class KantoorClient {
    public static void init(IEventBus modBus) {
        KantoorSlice.papierLezer = papier -> opClient(() -> Minecraft.getInstance().setScreen(new PapierScherm(papier)));
        KantoorPayloads.standOntvanger = p -> opClient(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof PrikklokScherm scherm) {
                scherm.update(p.data());
            } else if (p.data().getBooleanOr("Open", false)) {
                mc.setScreen(new PrikklokScherm(p.data()));
            }
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(KantoorSlice.BUREAUTJE_BE.get(), BureautjeRenderer::new));
    }

    private static void opClient(Runnable r) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isSameThread()) {
            r.run();
        } else {
            mc.execute(r);
        }
    }

    private KantoorClient() {
    }
}
