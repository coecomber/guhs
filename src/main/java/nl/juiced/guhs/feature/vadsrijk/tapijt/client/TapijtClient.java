package nl.juiced.guhs.feature.vadsrijk.tapijt.client;

import java.util.Locale;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.rijdier.client.UiterlijkLaag;
import nl.juiced.guhs.feature.vadsrijk.tapijt.TapijtBalans;
import nl.juiced.guhs.feature.vadsrijk.tapijt.TapijtEntity;
import nl.juiced.guhs.feature.vadsrijk.tapijt.TapijtFysica;

/**
 * U-TAPIJT, client: the rider of a flying carpet sits cross-legged ({@link #ZIT}, read by mixin.client.TapijtZitMixin) and tilts and
 * bobs along with the carpet ({@link #KANTEL}); the cushion's anchor on the carpet model (the tack layer of the mount renderer); and
 * the carpet's extra lines in the mount screen (N): hover height and how far it glides.
 */
public final class TapijtClient {
    /** On a rider's render state: sits cross-legged on a carpet. */
    public static final ContextKey<Boolean> ZIT = new ContextKey<>(Guhs.id("vr_tapijt_zit"));
    /** On a rider's render state: {roll, pitch, carpet yaw, bob, seat height} of the carpet below. */
    static final ContextKey<float[]> KANTEL = new ContextKey<>(Guhs.id("vr_tapijt_kantel"));
    private static final ContextKey<Boolean> GEKANTELD = new ContextKey<>(Guhs.id("vr_tapijt_gekanteld"));

    private TapijtClient() {
    }

    public static void init(IEventBus modBus) {
        // the cushion (tack) sits on the middle piece of the carpet ("midden", see tools/features/vadsrijk_u_tapijt.py)
        UiterlijkLaag.ANKERS.put(RijdierSoort.TAPIJT, java.util.Map.of(UiterlijkLaag.Plek.KOP, new UiterlijkLaag.Anker("midden", 0, 1.2f, 1.6f, 1.0f),
                UiterlijkLaag.Plek.NEK, new UiterlijkLaag.Anker("midden", 0, 1.2f, 1.6f, 1.0f),
                UiterlijkLaag.Plek.RUG, new UiterlijkLaag.Anker("midden", 0, 1.2f, 1.6f, 1.0f)));
        modBus.addListener((RegisterRenderStateModifiersEvent event) -> event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                if (avatar.getVehicle() instanceof TapijtEntity t) {
                    float pt = state.partialTick;
                    state.setRenderData(ZIT, Boolean.TRUE);
                    state.setRenderData(KANTEL, new float[] {t.roll(pt), t.pitch(pt), Mth.rotLerp(pt, t.yRotO, t.getYRot()),
                            TapijtRenderer.bob(t, pt), (float) t.soort().zit});
                }
            }
        }));
        NeoForge.EVENT_BUS.register(TapijtClient.class);
    }

    /** The rider tilts and bobs with the carpet (around the carpet's middle, the same turns as TapijtRenderer). */
    @SubscribeEvent
    public static void renderPre(RenderLivingEvent.Pre<?, ?, ?> event) {
        float[] k = event.getRenderState().getRenderData(KANTEL);
        if (k == null) {
            return;
        }
        PoseStack ps = event.getPoseStack();
        ps.pushPose();
        ps.translate(0, k[3] - k[4], 0);
        ps.mulPose(Axis.YP.rotationDegrees(180f - k[2]));
        ps.mulPose(Axis.ZP.rotationDegrees(-k[0]));
        ps.mulPose(Axis.XP.rotationDegrees(-k[1]));
        ps.mulPose(Axis.YP.rotationDegrees(-(180f - k[2])));
        ps.translate(0, k[4], 0);
        event.getRenderState().setRenderData(GEKANTELD, Boolean.TRUE);
    }

    @SubscribeEvent
    public static void renderPost(RenderLivingEvent.Post<?, ?, ?> event) {
        if (event.getRenderState().getRenderData(GEKANTELD) != null) {
            event.getRenderState().setRenderData(GEKANTELD, null);
            event.getPoseStack().popPose();
        }
    }

    /** The mount screen (N), under the speed/jump bars: the carpet's hover height and glide. Returns the new y. */
    public static int statRegels(GuiGraphicsExtractor g, Font font, RijdierSoort s, int x, int y, int w) {
        if (!s.glijdt()) {
            return y;
        }
        TapijtBalans.Knoppen k = TapijtBalans.nu();
        double top = s.snelheid(1) * k.topfactor();
        Component regel = Component.translatable("vadsrijk.tapijt.scherm.zweeft", String.format(Locale.ROOT, "%.1f", k.zweefhoogte()),
                Math.round(TapijtFysica.uitglijAfstand(top, k)));
        for (var line : font.split(regel, w)) {
            g.text(font, line, x, y, 0xFF7A4A9A, false);
            y += 9;
        }
        return y + 3;
    }
}
