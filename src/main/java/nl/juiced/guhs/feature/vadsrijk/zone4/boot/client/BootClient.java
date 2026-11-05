package nl.juiced.guhs.feature.vadsrijk.zone4.boot.client;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.zone4.boot.BootEntity;
import nl.juiced.guhs.feature.vadsrijk.zone4.boot.BootFysica;
import nl.juiced.guhs.feature.vadsrijk.zone4.boot.BootPayloads;
import nl.juiced.guhs.feature.vadsrijk.zone4.boot.BootSpul;
import nl.juiced.guhs.feature.vadsrijk.zone4.boot.BootVariant;

/**
 * T-Z4BOOT client: the Zeilbootje's renderer (sail texture per colour, only the chosen pennant / figurehead bone, a glowing
 * lantern), its screen ({@link BootScherm}, a tab "Zeilbootje" next to Rijdieren / Huisdieren), and a small wind panel while you
 * sail (right side: where the wind blows relative to your bow, your speed, "wind mee!").
 */
public final class BootClient {
    public static final String TAB = "zeilbootje";
    static final DataTicket<Integer> ZEIL = DataTicket.create("guhs_vr_boot_zeil", Integer.class);
    static final DataTicket<Integer> WIMPEL = DataTicket.create("guhs_vr_boot_wimpel", Integer.class);
    static final DataTicket<Integer> BOEG = DataTicket.create("guhs_vr_boot_boeg", Integer.class);
    static final DataTicket<Float> GROEI = DataTicket.create("guhs_vr_boot_groei", Float.class);
    /** The model is drawn smaller in a screen preview (it is a big boat). */
    public static final float VOORBEELD_SCHAAL = 0.45f;

    private BootClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(BootSpul.ZEILBOOTJE.get(), BootRenderer::new));
        nl.juiced.guhs.feature.vadsrijk.client.CollectieTabs.registreer(TAB, 15, () -> Component.translatable("vadsrijk.z4boot.tab"),
                () -> ClientPacketDistributor.sendToServer(new BootPayloads.Doe("open", "")));
        BootPayloads.schermOntvanger = s -> Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof BootScherm b) {
                b.update(s.data(), java.util.List.of());
            } else {
                mc.setScreen(new BootScherm(s.data()));
            }
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.HOTBAR, Guhs.id("vr_z4boot_wind"), BootClient::wind));
    }

    /** The wind panel while sailing. */
    private static void wind(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getVehicle() instanceof BootEntity b)) {
            return;
        }
        Font f = mc.font;
        long t = mc.level == null ? 0 : mc.level.getGameTime();
        float rel = Mth.wrapDegrees(BootFysica.windRichting(t) - b.getYRot());   // 0 = the wind blows the way you sail
        double mee = Math.cos(Math.toRadians(rel));
        int w = 74, h = 44, x = g.guiWidth() - w - 6, y = g.guiHeight() / 2 - 40;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF7CC4F4);
        g.fill(x, y, x + w, y + h, 0xC0182838);
        // the compass: the boat points up, the arrow is the wind
        int cx = x + 15, cy = y + 22;
        g.fill(cx - 1, cy - 9, cx + 1, cy + 9, 0x40FFFFFF);
        g.fill(cx - 9, cy - 1, cx + 9, cy + 1, 0x40FFFFFF);
        double a = Math.toRadians(rel);
        for (int i = -7; i <= 7; i++) {   // the arrow: blows from the tail to the tip; up = behind you (wind mee)
            int px = cx + (int) Math.round(Math.sin(a) * i), py = cy - (int) Math.round(Math.cos(a) * i);
            g.fill(px, py, px + 2, py + 2, i > 3 ? 0xFFFFE070 : 0xFFB8E4FF);
        }
        String woord = mee > 0.5 ? "vadsrijk.z4boot.wind.mee" : mee < -0.5 ? "vadsrijk.z4boot.wind.tegen" : "vadsrijk.z4boot.wind.zij";
        int kleur = mee > 0.5 ? 0xFF7CF0A0 : mee < -0.5 ? 0xFFFF9A9A : 0xFFFFE070;
        g.text(f, Component.translatable("vadsrijk.z4boot.wind").withStyle(ChatFormatting.BOLD), x + 30, y + 4, 0xFFFFFFFF, true);
        g.text(f, Component.translatable(woord), x + 30, y + 15, kleur, true);
        double bps = Math.abs(b.vaart()) * 20;
        g.text(f, String.format(java.util.Locale.ROOT, "%.0f bl/s", bps), x + 30, y + 27, 0xFFB8E4FF, true);
        if (b.stroomDiepte() > 0) {
            g.text(f, Component.translatable("vadsrijk.z4boot.wind.stroom"), x + 3, y + h + 4, 0xFF7CE0FF, true);
        }
    }

    /** The Zeilbootje on screen. */
    public static class BootRenderer extends GeoEntityRenderer<RijdierEntity, LivingEntityRenderState> {
        public BootRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<RijdierEntity>(Guhs.id("vr_eco_zeilbootje")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    BootVariant v = BootVariant.van(BootVariant.As.ZEIL, state.getOrDefaultGeckolibData(ZEIL, 0));
                    return Guhs.id("textures/entity/vr_eco_zeilbootje_" + v.kort + ".png");
                }
            });
            this.shadowRadius = 0f;
            withRenderLayer(new AutoGlowingGeoLayer<RijdierEntity, Void, LivingEntityRenderState>(this) {
                @Nullable
                @Override
                protected RenderType getRenderType(LivingEntityRenderState state) {
                    return GuhRenderTypes.eyes(Guhs.id("textures/entity/vr_eco_zeilbootje_glowmask.png"));
                }
            });
        }

        @Override
        public void addRenderData(RijdierEntity r, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            if (r instanceof BootEntity b) {
                state.addGeckolibData(ZEIL, b.variant(BootVariant.As.ZEIL).index());
                state.addGeckolibData(WIMPEL, b.variant(BootVariant.As.WIMPEL).index());
                state.addGeckolibData(BOEG, b.variant(BootVariant.As.BOEG).index());
            }
            float t = r.voorbeeld ? 99 : r.tickCount + partialTick;
            float groei = t >= 9 ? 1f : t < 6 ? 0.2f + 0.92f * (t / 6f) : 1.12f - 0.12f * ((t - 6) / 3f);
            state.addGeckolibData(GROEI, groei * (r.voorbeeld ? VOORBEELD_SCHAAL : 1f));
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float w, float h) {
            float g = info.getOrDefaultGeckolibData(GROEI, 1f);
            super.scaleModelForRender(info, w * g, h * g);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            BootVariant wimpel = BootVariant.van(BootVariant.As.WIMPEL, info.getOrDefaultGeckolibData(WIMPEL, 0));
            BootVariant boeg = BootVariant.van(BootVariant.As.BOEG, info.getOrDefaultGeckolibData(BOEG, 0));
            for (BootVariant v : BootVariant.values()) {
                if (v.as == BootVariant.As.ZEIL) {
                    continue;
                }
                boolean weg = v != wimpel && v != boeg;
                bones.ifPresent(v.id(), b -> b.skipRender(weg).skipChildrenRender(weg));
            }
        }

        @Override
        public int getRenderColor(RijdierEntity r, @Nullable Void related, float partialTick) {
            return r.silhouet ? 0xFF3A2C48 : super.getRenderColor(r, related, partialTick);
        }
    }
}
