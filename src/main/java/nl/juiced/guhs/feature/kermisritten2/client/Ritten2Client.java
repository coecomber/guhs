package nl.juiced.guhs.feature.kermisritten2.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.kermisritten2.BotsguhEntity;
import nl.juiced.guhs.feature.kermisritten2.Ritten2Feature;
import nl.juiced.guhs.feature.kermisritten2.SpookjeEntity;
import nl.juiced.guhs.feature.kermisritten2.SpookkarEntity;
import nl.juiced.guhs.feature.safari.rit.client.RitClient;
import nl.juiced.guhs.registry.ModEntities;

/** The Botsguhs (seven colours, a little guh at the wheel of an empty car) and the Spookguhhuis' cart and ghost. */
public final class Ritten2Client {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(Ritten2Feature.BOTSGUH.get(), BotsguhRenderer::new);
            event.registerEntityRenderer(Ritten2Feature.SPOOKKAR.get(), c -> new RitClient.SimpelRenderer<SpookkarEntity>(c, "kermisritten2/spookkar", 0.5f));
            event.registerEntityRenderer(Ritten2Feature.SPOOKJE.get(), c -> new RitClient.SimpelRenderer<SpookjeEntity>(c, "kermisritten2/spookje", 0f));
        });
    }

    private Ritten2Client() {
    }

    public static class BotsguhRenderer extends GeoEntityRenderer<BotsguhEntity, EntityRenderState> {
        private static final DataTicket<Integer> KLEUR = DataTicket.create("guhs_botsguh_kleur", Integer.class);
        private static final DataTicket<EntityRenderState> BESTUURDER = DataTicket.create("guhs_botsguh_bestuurder", EntityRenderState.class);
        private static final GuhVariant[] VARIANTEN = {GuhVariant.NORMAL, GuhVariant.MINT, GuhVariant.CHOCO, GuhVariant.SNOW};

        private final EntityRenderDispatcher dispatcher;
        @Nullable
        private GuhEntity guh;

        public BotsguhRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<BotsguhEntity>(Guhs.id("kermisritten2/botsguh")) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    Integer k = state.getGeckolibData(KLEUR);
                    return Guhs.id("textures/entity/kermisritten2/botsguh" + (k == null || k == 0 ? "" : "_" + k) + ".png");
                }
            });
            this.shadowRadius = 0.8f;
            this.dispatcher = context.getEntityRenderDispatcher();
        }

        @Override
        public void addRenderData(BotsguhEntity auto, @Nullable Void related, EntityRenderState state, float partialTick) {
            float yaw = Mth.rotLerp(partialTick, auto.yRotO, auto.getYRot());
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, yaw);
            state.addGeckolibData(KLEUR, auto.kleur());
            if (auto.isVehicle()) {
                return;
            }
            if (guh == null || guh.level() != auto.level()) {
                guh = ModEntities.GUH.get().create(auto.level(), EntitySpawnReason.TRIGGERED);
            }
            if (guh != null) {
                guh.setVariant(VARIANTEN[auto.kleur() % VARIANTEN.length]);
                guh.setInSittingPose(true);
                guh.setYRot(yaw);
                guh.yRotO = yaw;
                guh.yBodyRot = guh.yBodyRotO = yaw;
                guh.yHeadRot = guh.yHeadRotO = yaw;
                guh.tickCount = auto.tickCount;
                EntityRenderState s = dispatcher.extractEntity(guh, partialTick);
                s.lightCoords = state.lightCoords;
                s.shadowPieces.clear();
                state.addGeckolibData(BESTUURDER, s);
            }
        }

        @Override
        public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            super.submit(state, poseStack, collector, camera);
            EntityRenderState s = state.getGeckolibData(BESTUURDER);
            if (s != null) {
                dispatcher.submit(s, camera, 0, 0.2, 0, poseStack, collector);
            }
        }
    }
}
