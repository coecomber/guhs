package nl.juiced.guhs.feature.einde2.appa.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.appa.E2AppaFeature;
import nl.juiced.guhs.feature.einde2.appa.E2AppaPayloads;
import nl.juiced.guhs.feature.einde2.appa.MomoGuhEntity;

/**
 * einde2, slice appa, client side: the GeckoLib renderers of the Appa-guh and the Momo-guh (models, animations and textures
 * from tools/features/einde2_appa_modellen.py), and the Momo-guh on a player's right shoulder (a layer on every player; the
 * left shoulder is the piep maatje's, feature/piep/client/PiepClient).
 */
public final class E2AppaClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(E2AppaClient::renderers);
        NeoForge.EVENT_BUS.addListener(E2AppaClient::schouder);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(E2AppaFeature.APPA_GUH.get(), AppaRenderer::new);
        event.registerEntityRenderer(E2AppaFeature.MOMO_GUH.get(), MomoRenderer::new);
    }

    /** Render state ticket: this Appa is not in the world (the picture on his Guhdex page): drawn small enough to fit. */
    static final DataTicket<Boolean> PLAATJE = DataTicket.create("guhs_einde2_appa_plaatje", Boolean.class);
    /** How big he is on his Guhdex page (the page draws every creature at one size, and he is as big as a barn). */
    public static final float PLAATJE_SCHAAL = 0.55f;

    /** The Appa-guh: big (his head and tail stick far out of his box, so he is culled by a wider one). */
    public static class AppaRenderer extends GeoEntityRenderer<AppaGuhEntity, LivingEntityRenderState> {
        public AppaRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id("appa_guh")));
            this.shadowRadius = 1.7f;
        }

        @Override
        public void addRenderData(AppaGuhEntity appa, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(PLAATJE, !appa.isAddedToLevel());
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            float scale = info.getOrDefaultGeckolibData(PLAATJE, false) ? PLAATJE_SCHAAL : 1f;
            super.scaleModelForRender(info, widthScale * scale, heightScale * scale);
        }

        @Override
        protected AABB getBoundingBoxForCulling(AppaGuhEntity appa) {
            return appa.getBoundingBox().inflate(3.2, 1.5, 3.2);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }
    }

    /** The Momo-guh in the world (at home, or fluttering to his owner). */
    public static class MomoRenderer extends GeoEntityRenderer<MomoGuhEntity, LivingEntityRenderState> {
        public MomoRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id("momo_guh")));
            this.shadowRadius = 0.2f;
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }
    }

    // --- the Momo-guh on a shoulder ----------------------------------------------------------------------------------------------

    /** Player entity id -> the copy that is drawn on their shoulder. */
    private static final Map<Integer, MomoGuhEntity> SCHOUDER_MOMOS = new ConcurrentHashMap<>();
    /** Where he sits: this far to the right of the player, this high (the top of the shoulder), lower when they sneak. */
    public static final float SCHOUDER_OPZIJ = 0.36f, SCHOUDER_HOOGTE = 1.41f, SCHOUDER_BUKKEN = 0.22f, SCHOUDER_SCHAAL = 0.8f;

    /**
     * Draws a Momo-guh on the right shoulder of every player that carries one (E2AppaPayloads.CLIENT_SCHOUDERS), after the
     * player itself, in world space: on the shoulder of the body, looking where the player looks (as PiepClient does on the
     * left shoulder).
     */
    private static void schouder(RenderPlayerEvent.Post<?> event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(event.getRenderState().id) instanceof Player player)) {
            return;
        }
        if (!E2AppaPayloads.CLIENT_SCHOUDERS.contains(player.getId()) || player.isInvisible() || player.isSleeping()) {
            return;
        }
        MomoGuhEntity momo = SCHOUDER_MOMOS.compute(player.getId(), (id, m) -> m != null && m.level() == level ? m
                : E2AppaFeature.MOMO_GUH.get().create(level, EntitySpawnReason.TRIGGERED));
        if (momo == null) {
            return;
        }
        float pt = event.getPartialTick();
        float yaw = Mth.rotLerp(pt, player.yBodyRotO, player.yBodyRot);
        float kijk = Mth.rotLerp(pt, player.yHeadRotO, player.yHeadRot);
        momo.opSchouder(true);
        momo.tickCount = player.tickCount;
        momo.yBodyRot = momo.yBodyRotO = kijk;
        momo.yHeadRot = momo.yHeadRotO = kijk;
        momo.setYRot(kijk);
        momo.yRotO = kijk;
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderState state = dispatcher.extractEntity(momo, pt);
        state.lightCoords = event.getRenderState().lightCoords;
        state.shadowPieces.clear();
        double rad = Math.toRadians(yaw);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-Math.cos(rad) * SCHOUDER_OPZIJ, SCHOUDER_HOOGTE - (player.isCrouching() ? SCHOUDER_BUKKEN : 0f), -Math.sin(rad) * SCHOUDER_OPZIJ);
        pose.scale(SCHOUDER_SCHAAL, SCHOUDER_SCHAAL, SCHOUDER_SCHAAL);
        dispatcher.submit(state, Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState, 0, 0, 0, pose,
                event.getSubmitNodeCollector());
        pose.popPose();
    }

    private E2AppaClient() {
    }
}
