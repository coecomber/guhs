package nl.juiced.guhs.feature.vadsrijk.stadsleven.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.vadsrijk.stadsleven.LevenProp;
import nl.juiced.guhs.feature.vadsrijk.stadsleven.StadslevenPayloads;
import nl.juiced.guhs.feature.vadsrijk.stadsleven.StadslevenRegistry;

/**
 * U-LEVEN-STAD, the client: the renderers (any Guhs model for the props, with the synced tilt for the seesaw and swings; the guh
 * renderer for the town guhs; the vanilla cat renderer) with a render LOD, and the setting "Stadsleven: VOL / ZACHT / UIT"
 * ({@link StadslevenConfig}, also in the Instellingen-tab of the Vadsrijk-menu), sent to the server when it changes.
 */
public final class StadslevenClient {
    private static final DataTicket<String> MODEL = DataTicket.create("guhs_sl_model", String.class);
    private static final DataTicket<String> TEX = DataTicket.create("guhs_sl_tex", String.class);
    private static final DataTicket<Float> SCHAAL = DataTicket.create("guhs_sl_schaal", Float.class);
    private static final DataTicket<Float> YAW = DataTicket.create("guhs_sl_yaw", Float.class);
    private static final DataTicket<Float> KANTEL = DataTicket.create("guhs_sl_kantel", Float.class);
    @Nullable
    private static StadslevenConfig.Niveau verstuurd;

    private StadslevenClient() {
    }

    public static void init(IEventBus modBus) {
        ModList.get().getModContainerById(Guhs.MODID).ifPresent(c -> c.registerConfig(ModConfig.Type.CLIENT, StadslevenConfig.SPEC,
                "guhs-vadsrijk-stadsleven.toml"));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(StadslevenRegistry.PROP.get(), PropRenderer::new);
            e.registerEntityRenderer(StadslevenRegistry.GUH.get(), GuhLod::new);
            e.registerEntityRenderer(StadslevenRegistry.KAT.get(), KatLod::new);
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> verstuurd = null);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.getConnection() == null || mc.player.tickCount % 20 != 5) {
                return;
            }
            StadslevenConfig.Niveau n = StadslevenConfig.niveau();
            if (n != verstuurd) {
                verstuurd = n;
                try {
                    ClientPacketDistributor.sendToServer(new StadslevenPayloads.Stand(n.ordinal()));
                } catch (RuntimeException ex) {
                    verstuurd = null;   // (a server without this mod)
                }
            }
        });
    }

    /** The render distance for this setting (UIT: nothing; the server stops sending too). */
    static boolean zichtbaar(double dist2, boolean groot) {
        double d = switch (StadslevenConfig.niveau()) {
            case VOL -> groot ? 80 : 48;
            case ZACHT -> groot ? 56 : 28;
            case UIT -> 0;
        };
        return dist2 < d * d;
    }

    static class PropRenderer extends GeoEntityRenderer<LevenProp, EntityRenderState> {
        PropRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<LevenProp>(Guhs.id("pieppiepmuisje")) {
                @Override
                public Identifier getModelResource(GeoRenderState state) {
                    return Guhs.id("entity/" + state.getOrDefaultGeckolibData(MODEL, "pieppiepmuisje"));
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return Guhs.id(state.getOrDefaultGeckolibData(TEX, "textures/entity/pieppiepmuisje.png"));
                }

                @Override
                public Identifier getAnimationResource(LevenProp f) {
                    return Guhs.id("entity/" + f.animBestand());
                }
            });
            this.shadowRadius = 0.15f;
        }

        @Override
        public boolean shouldRender(LevenProp f, Frustum frustum, double x, double y, double z) {
            return zichtbaar(f.distanceToSqr(x, y, z), f.groot()) && super.shouldRender(f, frustum, x, y, z);
        }

        @Override
        public void addRenderData(LevenProp f, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(MODEL, f.model());
            state.addGeckolibData(TEX, f.textuur());
            state.addGeckolibData(SCHAAL, f.schaal());
            float yaw = Mth.rotLerp(partialTick, f.yRotO, f.getYRot());
            state.addGeckolibData(YAW, yaw);
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, yaw);
            state.addGeckolibData(KANTEL, Mth.lerp(partialTick, f.kantelO, f.kantel()));
        }

        @Override
        protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
            float yaw = info.getOrDefaultGeckolibData(YAW, 0f);
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - yaw));
            float k = info.getOrDefaultGeckolibData(KANTEL, 0f);
            if (k != 0f) {
                poseStack.mulPose(Axis.XP.rotationDegrees(k));
            }
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<EntityRenderState> info, float w, float h) {
            float k = info.getOrDefaultGeckolibData(SCHAAL, 1f);
            super.scaleModelForRender(info, w * k, h * k);
        }

        @Override
        protected AABB getBoundingBoxForCulling(LevenProp f) {
            return f.cullDoos();
        }

        /** W-MODELLEN: a town blob on the Blub-guh model only shows the blue Blub's body, ears and feet. */
        @Override
        public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info, com.geckolib.renderer.base.BoneSnapshots bones) {
            if (nl.juiced.guhs.feature.vadsrijk.stadsleven.BlubLook.MODEL.equals(info.getOrDefaultGeckolibData(MODEL, ""))) {
                for (String b : nl.juiced.guhs.feature.vadsrijk.stadsleven.BlubLook.VERBORGEN) {
                    bones.ifPresent(b, sn -> sn.skipRender(true).skipChildrenRender(true));
                }
            }
        }
    }

    /** The guh renderer, with the town-life LOD. */
    static class GuhLod extends GuhRenderer {
        GuhLod(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public boolean shouldRender(GuhEntity g, Frustum frustum, double x, double y, double z) {
            return zichtbaar(g.distanceToSqr(x, y, z), true) && super.shouldRender(g, frustum, x, y, z);
        }
    }

    /** The vanilla cat, with the town-life LOD. */
    static class KatLod extends CatRenderer {
        KatLod(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public boolean shouldRender(Cat c, Frustum frustum, double x, double y, double z) {
            return zichtbaar(c.distanceToSqr(x, y, z), false) && super.shouldRender(c, frustum, x, y, z);
        }
    }
}
