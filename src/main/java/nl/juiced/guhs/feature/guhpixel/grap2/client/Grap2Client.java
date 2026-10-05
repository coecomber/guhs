package nl.juiced.guhs.feature.guhpixel.grap2.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhpixel.grap2.Grap2Payloads;
import nl.juiced.guhs.feature.guhpixel.grap2.Grap2Slice;

/**
 * Client side of the guhpixel slice "grap2": the stand-in guhs are drawn as guhs, the three NPCs get their own models
 * (cap, hairdo, straw hat), and the screens of the two games open when the server says so: the guh picker and the battle
 * screen of the Guhmon-gevecht, the letters and the credits of Boer zoekt Guh.
 */
public final class Grap2Client {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(Grap2Slice.GUHMON_GUH.get(), GuhRenderer::new);
            event.registerEntityRenderer(Grap2Slice.BZG_GUH.get(), GuhRenderer::new);
        });
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.GUHMON_GYMLEIDER, Guhs.id("entity/guh_npc_guhmon_gymleider"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.BZG_PRESENTATRICE, Guhs.id("entity/guh_npc_bzg_presentatrice"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.BZG_BOER, Guhs.id("entity/guh_npc_bzg_boer"));
        Grap2Payloads.guhmonOpener = p -> opClient(() -> {
            Minecraft mc = Minecraft.getInstance();
            if ("kies".equals(p.data().getStringOr("Scherm", ""))) {
                mc.setScreen(new GuhmonKiesScherm(p.data()));
            } else if (mc.screen instanceof GuhmonGevechtScherm s) {
                s.update(p.data());
            } else {
                mc.setScreen(new GuhmonGevechtScherm(p.data()));
            }
        });
        Grap2Payloads.bzgOpener = p -> opClient(() -> {
            Minecraft mc = Minecraft.getInstance();
            if ("aftiteling".equals(p.data().getStringOr("Scherm", ""))) {
                mc.setScreen(new BzgAftitelingScherm(p.data()));
            } else {
                mc.setScreen(new BzgBriefScherm(p.data()));
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

    private Grap2Client() {
    }
}
