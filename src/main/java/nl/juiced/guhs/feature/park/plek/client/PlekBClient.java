package nl.juiced.guhs.feature.park.plek.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.park.plek.PlekB;
import nl.juiced.guhs.feature.park.plek.Verrekijker;
import nl.juiced.guhs.feature.vogels.client.VogelsClient;

/**
 * 1.4.2 (park-plekjes b), client side:
 * <ul>
 *   <li>the Duifguhtje through the birds' renderer (its model, texture and animations: tools/features/park_plek_b_modellen.py);</li>
 *   <li>the NPCs' looks: Oma-guh (her own sitting-guh model; now and then she shakes her bag of crumbs), the sulking Mika (the
 *       Mika's model with a rain cloud, his own sulky animations), the guhkindjes (the plain sitting guh, small);</li>
 *   <li>looking through a {@link Verrekijker}: the view zoomed in like a spyglass (FOV x {@value #ZOOM}), a slower mouse, the
 *       spyglass scope over the screen; it stops when you sneak, step away ({@link Verrekijker#BIJ}) or open a screen.</li>
 * </ul>
 */
public final class PlekBClient {
    public static final float ZOOM = 0.1f;
    private static final Identifier SCOPE = Identifier.withDefaultNamespace("textures/misc/spyglass_scope.png");
    @Nullable
    private static BlockPos kijkt;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(PlekB.DUIFGUHTJE.get(), c -> new VogelsClient.VogelRenderer<>(c, "duifguhtje", 0.22f)));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.OMAGUH, Guhs.id("entity/guh_npc_omaguh"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.MOKMIKA, Guhs.id("entity/guh_npc_mokmika"));
        SittingGuhRenderers.NPC_ANIMATIES.put(GuhNpcEntity.Kind.MOKMIKA, Guhs.id("entity/guh_npc_mokmika"));
        // Oma-guh shakes her bag of crumbs now and then (every ~15 seconds, for a second)
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.OMAGUH, (npc, tick) -> {
            double fase = (tick + npc.getId() * 37) % 300;
            float schud = fase < 24 ? Mth.sin((float) fase * 0.8f) * 0.35f : 0f;
            return bones -> bones.ifPresent("oma_zakje", b -> b.setRotX(schud));
        });
        NeoForge.EVENT_BUS.addListener((ViewportEvent.ComputeFov event) -> {
            if (kijkt != null) {
                event.setFOV(event.getFOV() * ZOOM);
            }
        });
        NeoForge.EVENT_BUS.addListener((CalculatePlayerTurnEvent event) -> {
            if (kijkt != null) {
                event.setMouseSensitivity(event.getMouseSensitivity() * 0.3);
            }
        });
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> {
            if (kijkt == null) {
                return;
            }
            var g = event.getGuiGraphics();
            int w = g.guiWidth(), h = g.guiHeight(), maat = Math.min(w, h), x = (w - maat) / 2, y = (h - maat) / 2;
            g.blit(RenderPipelines.GUI_TEXTURED, SCOPE, x, y, 0f, 0f, maat, maat, 256, 256, 256, 256);
            g.fill(0, 0, w, y, 0xFF000000);
            g.fill(0, y + maat, w, h, 0xFF000000);
            g.fill(0, y, x, y + maat, 0xFF000000);
            g.fill(x + maat, y, w, y + maat, 0xFF000000);
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            if (kijkt == null) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer p = mc.player;
            if (p == null || !p.isAlive() || p.isShiftKeyDown() || mc.screen != null
                    || p.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(kijkt)) > Verrekijker.BIJ + 1.0) {
                stop();
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> kijkt = null);
    }

    /** (Verrekijker.Kijk) start looking through the verrekijker at this block. */
    public static void kijk(BlockPos pos) {
        kijkt = pos;
    }

    private static void stop() {
        kijkt = null;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            p.playSound(SoundEvents.SPYGLASS_STOP_USING, 1f, 1f);
        }
    }

    private PlekBClient() {
    }
}
