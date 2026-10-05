package nl.juiced.guhs.feature.guhpixel.guhkade.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.feature.guhpixel.PxVlaggen;
import nl.juiced.guhs.feature.guhpixel.guhkade.GuhkadePayloads;
import nl.juiced.guhs.feature.guhpixel.guhkade.GuhkadeSlice;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * Client side of the guhpixel slice "guhkade": the game screen ({@link KastScherm}), the moving screen on the cabinet
 * block ({@link KastRenderer}), and a guh at the buttons: it bobs along with its game and nods at the screen.
 */
public final class GuhkadeClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(GuhkadeSlice.KAST_BE.get(), KastRenderer::new));
        GuhkadePayloads.opener = p -> opClient(() -> Minecraft.getInstance().setScreen(new KastScherm(p)));
        GuhkadePayloads.uitslagOntvanger = p -> opClient(() -> {
            if (Minecraft.getInstance().screen instanceof KastScherm scherm && scherm.pos().equals(p.pos())) {
                scherm.uitslag(p);
            }
        });
        // a guh that plays: little excited hops and a nodding head (values only: the entity is not kept)
        GuhRenderer.hook((guh, partialTick, frame) -> {
            if (!GuhHooks.heeft(guh, PxVlaggen.SPEELT_KAST)) {
                return;
            }
            float t = (guh.tickCount + partialTick) * 0.55f + guh.getId();
            float hop = Math.max(0f, Mth.sin(t)) * 0.05f;
            float knik = Mth.sin(t * 0.5f) * 0.12f;
            frame.pose(ps -> ps.translate(0f, hop, 0f));
            frame.bones(bones -> bones.ifPresent("head", b -> b.setRotX(b.getRotX() - 0.18f + knik)));
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

    private GuhkadeClient() {
    }
}
