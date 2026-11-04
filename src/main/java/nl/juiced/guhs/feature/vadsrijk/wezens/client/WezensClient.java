package nl.juiced.guhs.feature.vadsrijk.wezens.client;

import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.wezens.Kampklodder;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.wezens.draak.Vahoegnir;
import nl.juiced.guhs.feature.vadsrijk.wezens.loot.SetBonussen;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Klodders;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.RijkWezen;

/**
 * Het Vadsrijk, slice "wezens": the client side. Every creature uses {@link WezenRenderer} (its GeckoLib model + the grey
 * "ontvadst" texture, the bright happy one after the plof, bigger + a gold glow for elites, small for split copies, upside
 * down for a Druipslijm on the ceiling); the Kampklodder and Vahoegnir have their own; armour tooltips show the set lines.
 */
public final class WezensClient {
    static final DataTicket<Boolean> GEPLOFD = DataTicket.create("guhs_vr_wezen_geplofd", Boolean.class);
    static final DataTicket<Boolean> ELITE = DataTicket.create("guhs_vr_wezen_elite", Boolean.class);
    static final DataTicket<Boolean> KLEIN = DataTicket.create("guhs_vr_wezen_klein", Boolean.class);
    static final DataTicket<Float> PLAFOND = DataTicket.create("guhs_vr_wezen_plafond", Float.class);
    static final DataTicket<Float> PITCH = DataTicket.create("guhs_vr_draak_pitch", Float.class);

    private WezensClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            wezen(e, WezensRegistry.KLODDER.get(), "klodder", "klodder", 1.0f, false);
            wezen(e, WezensRegistry.KLODDERTJE.get(), "klodder", "kloddertje", 0.55f, false);
            wezen(e, WezensRegistry.BUBBELKLODDER.get(), "bubbelklodder", "bubbelklodder", 1.0f, false);
            wezen(e, WezensRegistry.STUITERKLODDER.get(), "stuiterklodder", "stuiterklodder", 1.0f, false);
            wezen(e, WezensRegistry.MOEDERKLODDER.get(), "moederklodder", "moederklodder", 1.8f, false);
            wezen(e, WezensRegistry.DRUIPSLIJM.get(), "druipslijm", "druipslijm", 1.0f, false);
            wezen(e, WezensRegistry.KRISTALKLODDER.get(), "kristalklodder", "kristalklodder", 1.05f, true);
            wezen(e, WezensRegistry.HOEDJESBOL.get(), "hoedjesbol", "hoedjesbol", 1.0f, false);
            wezen(e, WezensRegistry.SPORENPUFFER.get(), "sporenpuffer", "sporenpuffer", 1.0f, false);
            wezen(e, WezensRegistry.ZWAMWACHTER.get(), "zwamwachter", "zwamwachter", 1.25f, false);
            wezen(e, WezensRegistry.GLIMZWAM.get(), "glimzwam", "glimzwam", 1.0f, true);
            wezen(e, WezensRegistry.TEMPELRIDDER.get(), "tempelridder", "tempelridder", 1.2f, false);
            wezen(e, WezensRegistry.SPORENPRIESTER.get(), "sporenpriester", "sporenpriester", 1.1f, true);
            wezen(e, WezensRegistry.MOSGEEST.get(), "mosgeest", "mosgeest", 1.0f, false);
            wezen(e, WezensRegistry.KNAAG.get(), "knaag", "knaag", 1.0f, false);
            e.registerEntityRenderer(WezensRegistry.KAMPKLODDER.get(), KampklodderRenderer::new);
            e.registerEntityRenderer(WezensRegistry.DRAAK.get(), DraakRenderer::new);
        });
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> setRegels(e));
        SfeerClient.init();
    }

    private static <T extends RijkWezen> void wezen(EntityRenderersEvent.RegisterRenderers e, EntityType<T> type, String model, String tex, float schaal,
            boolean gloed) {
        e.registerEntityRenderer(type, c -> new WezenRenderer<>(c, model, tex, schaal, gloed));
    }

    static Identifier tex(String naam) {
        return Guhs.id("textures/vadsrijk/wezens/" + naam + ".png");
    }

    /** One creature: model + texture, grey until the plof, gold-glowing and 25 % bigger as an elite. */
    public static class WezenRenderer<T extends RijkWezen> extends GeoEntityRenderer<T, LivingEntityRenderState> {
        private final float schaal;
        private final Identifier grauw, blij, elite;

        public WezenRenderer(EntityRendererProvider.Context context, String model, String tex, float schaal, boolean gloed) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id("vadsrijk/wezens/" + model)) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Boolean.TRUE.equals(state.getGeckolibData(GEPLOFD)) ? tex(tex + "_blij") : tex(tex);
                }
            });
            this.schaal = schaal;
            this.grauw = tex(tex);
            this.blij = tex(tex + "_blij");
            this.elite = tex(tex + "_elite");
            this.shadowRadius = 0.4f * schaal;
            withRenderLayer(new EliteLaag<>(this, this.elite));
            if (gloed) {
                withRenderLayer(new GloedLaag<>(this, tex(tex + "_gloed")));
            }
        }

        @Override
        public void addRenderData(T w, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(GEPLOFD, w.geplofd());
            state.addGeckolibData(ELITE, w.eliteZichtbaar());
            state.addGeckolibData(KLEIN, w.klein());
            state.addGeckolibData(PLAFOND, w instanceof Klodders.Druipslijm d && d.aanPlafond() ? w.getBbHeight() : 0f);
        }

        /** (balans 2026-11) After the plof: translucent (and no hitbox, see VadsrijkMob#isPickable). */
        @Override
        public int getRenderColor(T w, @Nullable Void related, float partialTick) {
            int k = super.getRenderColor(w, related, partialTick);
            return w.geplofd() ? (k & 0x00FFFFFF) | (nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob.PLOF_ALPHA << 24) : k;
        }

        @Override
        public @Nullable net.minecraft.client.renderer.rendertype.RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
            if (Boolean.TRUE.equals(state.getGeckolibData(GEPLOFD)) && !state.isInvisible) {
                return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture);
            }
            return super.getRenderType(state, texture);
        }

        float factor(RenderPassInfo<LivingEntityRenderState> info) {
            return schaal * (info.getOrDefaultGeckolibData(ELITE, false) ? 1.25f : 1f) * (info.getOrDefaultGeckolibData(KLEIN, false) ? 0.6f : 1f);
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float f = factor(info);
            super.scaleModelForRender(info, widthScale * f, heightScale * f);
        }

        @Override
        protected void applyRotations(RenderPassInfo<LivingEntityRenderState> info, PoseStack poseStack, float nativeScale) {
            super.applyRotations(info, poseStack, nativeScale);
            float h = info.getOrDefaultGeckolibData(PLAFOND, 0f);
            if (h > 0) {
                poseStack.translate(0, h, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            }
        }

        @Override
        protected float getShadowRadius(LivingEntityRenderState state) {
            Float h = state.getGeckolibData(PLAFOND);
            return h != null && h > 0 ? 0 : this.shadowRadius;
        }
    }

    /** Elites: their own texture once more, as an additive gold glow on top. */
    static class EliteLaag<T extends RijkWezen> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
        private final Identifier gloed;

        EliteLaag(GeoRenderer<T, Void, LivingEntityRenderState> r, Identifier gloed) {
            super(r);
            this.gloed = gloed;
        }

        @Override
        public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
            if (info.willRender() && info.getOrDefaultGeckolibData(ELITE, false) && !info.getOrDefaultGeckolibData(GEPLOFD, false)) {
                getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(gloed));
            }
        }
    }

    /** A glowmask (Glimzwam cap, the priest's orb, the crystals), always on. */
    static class GloedLaag<T extends net.minecraft.world.entity.Entity & com.geckolib.animatable.GeoAnimatable, R extends EntityRenderState & GeoRenderState>
            extends GeoRenderLayer<T, Void, R> {
        private final Identifier gloed;

        GloedLaag(GeoRenderer<T, Void, R> r, Identifier gloed) {
            super(r);
            this.gloed = gloed;
        }

        @Override
        public void submitRenderTask(RenderPassInfo<R> info, SubmitNodeCollector collector) {
            if (info.willRender()) {
                getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(gloed));
            }
        }
    }

    /** The Kampklodder: the big pink guh slime with a flower (x3.2). */
    public static class KampklodderRenderer extends GeoEntityRenderer<Kampklodder, LivingEntityRenderState> {
        public KampklodderRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<Kampklodder>(Guhs.id("vadsrijk/wezens/kampklodder")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex("kampklodder_blij");
                }
            });
            withScale(3.2f);
            this.shadowRadius = 1.3f;
        }
    }

    /** Vahoegnir: the huge model (x3.75 since balans 2026-11, was x2.5), turned along its flight, leaning into climbs and dives; glowing eyes and sparks. */
    public static class DraakRenderer extends GeoEntityRenderer<Vahoegnir, EntityRenderState> {
        public DraakRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<Vahoegnir>(Guhs.id("vadsrijk/wezens/draak")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return tex("draak");
                }
            });
            withScale(nl.juiced.guhs.feature.vadsrijk.wezens.draak.Vahoegnir.SCHAAL);
            this.shadowRadius = 0f;
            withRenderLayer(new GloedLaag<>(this, tex("draak_gloed")));
        }

        @Override
        protected AABB getBoundingBoxForCulling(Vahoegnir d) {
            return d.getBoundingBoxForCulling();
        }

        @Override
        public void addRenderData(Vahoegnir d, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, Mth.rotLerp(partialTick, d.yRotO, d.getYRot()));
            state.addGeckolibData(PITCH, Mth.lerp(partialTick, d.xRotO, d.getXRot()));
        }

        @Override
        protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
            super.applyRotations(info, poseStack, nativeScale);
            poseStack.mulPose(Axis.XP.rotationDegrees(-info.getOrDefaultGeckolibData(PITCH, 0f)));
        }
    }

    /** Set lines under a set piece: "Klodderset (2/4)" and both bonuses (green when you have them). */
    private static void setRegels(ItemTooltipEvent e) {
        Uitrusting u = Rollen.van(e.getItemStack());
        String set = u == null ? null : SetBonussen.setVan(u.type());
        if (set == null) {
            return;
        }
        var p = e.getEntity() != null ? e.getEntity() : Minecraft.getInstance().player;
        int n = p == null ? 0 : SetBonussen.stukken(p, set);
        List<Component> t = e.getToolTip();
        int waar = Math.max(1, t.size() - 1);
        t.add(waar++, Component.translatable("vadsrijk.wezens.set.stukken", Component.translatable("vadsrijk.wezens.set." + set), n)
                .withStyle(ChatFormatting.GOLD));
        t.add(waar++, nl.juiced.guhs.feature.vadsrijk.client.UitrustingTips.stukken((SetBonussen.KLODDER.equals(set) ? SetBonussen.KLODDERSET : SetBonussen.ZWAMSET), p));   // fix round 2 (g-ui)
        t.add(waar++, Component.translatable("vadsrijk.wezens.set." + set + ".2").withStyle(n >= 2 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        t.add(waar, Component.translatable("vadsrijk.wezens.set." + set + ".4").withStyle(n >= 4 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
    }
}
