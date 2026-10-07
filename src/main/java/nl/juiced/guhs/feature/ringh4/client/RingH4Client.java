package nl.juiced.guhs.feature.ringh4.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ringh4.ElfenbootjeEntity;
import nl.juiced.guhs.feature.ringh4.RingH4Feature;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Client side of bbq2 (ring-h4): the elf boat (geckolib/models/entity/ringh4_elfenbootje.geo.json, made by
 * tools/features/ring_h4_modellen.py): turned along its path, rocking gently on the sauce, not drawn while its trip is
 * under way (a moored boat that is "weg"), and on the trip down the river with Leguhlas at the stern and Gimguh at the prow
 * (their own render states, drawn after the boat, like Kapitein Wolkje in the Luchtballon).
 * The Spiegel van Guhladriel is a plain block model (its render type is in the model file).
 */
public final class RingH4Client {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(RingH4Feature.ELFENBOOTJE.get(), BootRenderer::new));
    }

    /** The boat. Everything the render needs is copied at extract time. */
    public static class BootRenderer extends GeoEntityRenderer<ElfenbootjeEntity, EntityRenderState> {
        private static final DataTicket<Boolean> WEG = DataTicket.create("guhs_ringh4_weg", Boolean.class);
        /** {time, strength of the rocking} */
        private static final DataTicket<float[]> WIEG = DataTicket.create("guhs_ringh4_wieg", float[].class);
        private static final DataTicket<Gids[]> GIDSEN = DataTicket.create("guhs_ringh4_gidsen", Gids[].class);

        private record Gids(EntityRenderState state, Vec3 at) {
        }

        private final EntityRenderDispatcher dispatcher;
        private final GuhNpcEntity[] gidsen = new GuhNpcEntity[2];

        public BootRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<ElfenbootjeEntity>(Guhs.id("ringh4_elfenbootje")));
            this.shadowRadius = 0.9f;
            this.dispatcher = context.getEntityRenderDispatcher();
        }

        @Override
        public void addRenderData(ElfenbootjeEntity boot, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(WEG, boot.isWeg());
            float yaw = Mth.rotLerp(partialTick, boot.yRotO, boot.getYRot());
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, yaw);
            state.addGeckolibData(WIEG, new float[]{boot.tickCount + partialTick + boot.getId() * 11, boot.vaart() ? 1.6f : 0.9f});
            state.addGeckolibData(GIDSEN, boot.bemand() && !boot.isWeg() ? gidsen(boot, state, yaw, partialTick) : null);
        }

        @Override
        protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
            super.applyRotations(info, poseStack, nativeScale);
            float[] w = info.getGeckolibData(WIEG);
            if (w != null) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(w[0] * 0.06f) * w[1]));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.cos(w[0] * 0.047f) * w[1] * 0.7f));
            }
        }

        @Override
        public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            Boolean weg = state.getGeckolibData(WEG);
            if (weg != null && weg) {
                return;
            }
            super.submit(state, poseStack, collector, camera);
            Gids[] crew = state.getGeckolibData(GIDSEN);
            if (crew != null) {
                for (Gids g : crew) {
                    if (g != null) {
                        dispatcher.submit(g.state(), camera, g.at().x, g.at().y, g.at().z, poseStack, collector);
                    }
                }
            }
        }

        /** Leguhlas at the stern (he looks ahead), Gimguh at the prow (he looks back at the passengers, grumbling). */
        private Gids[] gidsen(ElfenbootjeEntity boot, EntityRenderState bootState, float yaw, float partialTick) {
            Gids[] uit = new Gids[2];
            for (int i = 0; i < 2; i++) {
                GuhNpcEntity npc = gidsen[i];
                if (npc == null || npc.level() != boot.level()) {
                    npc = ModEntities.GUH_NPC.get().create(boot.level(), EntitySpawnReason.TRIGGERED);
                    if (npc == null) {
                        continue;
                    }
                    npc.setKind(i == 0 ? GuhNpcEntity.Kind.LEGUHLAS : GuhNpcEntity.Kind.GIMGUH);
                    npc.setCustomNameVisible(false);
                    gidsen[i] = npc;
                }
                float kijkt = i == 0 ? yaw : yaw + 180f;
                Vec3 plek = new Vec3(0, 0.2, i == 0 ? -1.35 : 1.15).yRot(-yaw * Mth.DEG_TO_RAD);
                npc.setYRot(kijkt);
                npc.yRotO = kijkt;
                npc.yBodyRot = npc.yBodyRotO = npc.yHeadRot = npc.yHeadRotO = kijkt;
                npc.tickCount = boot.tickCount;
                EntityRenderState s = dispatcher.extractEntity(npc, partialTick);
                s.lightCoords = bootState.lightCoords;      // (the drawn guides stand nowhere: the boat's light)
                s.shadowPieces.clear();
                uit[i] = new Gids(s, plek);
            }
            return uit;
        }
    }

    private RingH4Client() {
    }
}
