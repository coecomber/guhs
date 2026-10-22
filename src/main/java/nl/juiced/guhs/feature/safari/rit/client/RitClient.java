package nl.juiced.guhs.feature.safari.rit.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.safari.rit.BootEntity;
import nl.juiced.guhs.feature.safari.rit.GondelEntity;
import nl.juiced.guhs.feature.safari.rit.KanoEntity;
import nl.juiced.guhs.feature.safari.rit.RaceStruisvogelEntity;
import nl.juiced.guhs.feature.safari.rit.SafariPoortBlockEntity;
import nl.juiced.guhs.feature.safari.rit.SafariRitten;
import nl.juiced.guhs.feature.safari.rit.TruckEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Client side of the S3 rides: the truck (geckolib safari_rit/safaritruck, with the guide at the wheel and the visitor
 * guhs in their seats, drawn as render states of their own after the truck, like the elf boat's guides), the canoe, the
 * gondola and the race ostrich (the park's own struisvogel model).
 */
public final class RitClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(SafariRitten.TRUCK.get(), TruckRenderer::new);
            event.registerEntityRenderer(SafariRitten.KANO.get(), c -> new SimpelRenderer<KanoEntity>(c, "safari_rit/kano", 0.6f));
            event.registerEntityRenderer(SafariRitten.GONDEL.get(), c -> new SimpelRenderer<GondelEntity>(c, "safari_rit/gondel", 0f));
            event.registerEntityRenderer(SafariRitten.STRUISVOGEL.get(), c -> new SimpelRenderer<RaceStruisvogelEntity>(c, "safari/struisvogel", 0.5f));
            event.registerEntityRenderer(SafariRitten.BOOT.get(), BootRenderer::new);
            event.registerBlockEntityRenderer(SafariRitten.POORT_BE.get(), PoortRenderer::new);
        });
        NeoForge.EVENT_BUS.addListener(SafariFm::tick);
    }

    /** A plain ride model, turned along its yaw. */
    public static class SimpelRenderer<T extends Entity & com.geckolib.animatable.GeoAnimatable> extends GeoEntityRenderer<T, EntityRenderState> {
        public SimpelRenderer(EntityRendererProvider.Context context, String model, float schaduw) {
            super(context, new DefaultedEntityGeoModel<T>(Guhs.id(model)));
            this.shadowRadius = schaduw;
        }

        @Override
        public void addRenderData(T rit, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, Mth.rotLerp(partialTick, rit.yRotO, rit.getYRot()));
        }
    }

    /** The truck with its guide and visitors. */
    public static class TruckRenderer extends GeoEntityRenderer<TruckEntity, EntityRenderState> {
        private static final DataTicket<Gast[]> GASTEN = DataTicket.create("guhs_safari_truck_gasten", Gast[].class);

        private record Gast(EntityRenderState state, Vec3 at) {
        }

        private final EntityRenderDispatcher dispatcher;
        @Nullable
        private GuhNpcEntity gids;
        private final List<GuhEntity> bezoekers = new ArrayList<>();

        public TruckRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<TruckEntity>(Guhs.id("safari_rit/safaritruck")));
            this.shadowRadius = 1.6f;
            this.dispatcher = context.getEntityRenderDispatcher();
        }

        @Override
        public void addRenderData(TruckEntity truck, @Nullable Void related, EntityRenderState state, float partialTick) {
            float yaw = Mth.rotLerp(partialTick, truck.yRotO, truck.getYRot());
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, yaw);
            state.addGeckolibData(GASTEN, gasten(truck, state, yaw, partialTick));
        }

        @Override
        public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            super.submit(state, poseStack, collector, camera);
            Gast[] gasten = state.getGeckolibData(GASTEN);
            if (gasten != null) {
                for (Gast g : gasten) {
                    if (g != null) {
                        dispatcher.submit(g.state(), camera, g.at().x, g.at().y, g.at().z, poseStack, collector);
                    }
                }
            }
        }

        private static Vec3 stoel(double[] z, float yaw) {
            return new Vec3(-z[0] / 16.0, z[1] / 16.0 - 0.45, -z[2] / 16.0).yRot(-yaw * Mth.DEG_TO_RAD);
        }

        private Gast[] gasten(TruckEntity truck, EntityRenderState truckState, float yaw, float partialTick) {
            List<Integer> stoelen = truck.bezoekerStoelen();
            Gast[] uit = new Gast[1 + stoelen.size()];
            if (gids == null || gids.level() != truck.level()) {
                gids = ModEntities.GUH_NPC.get().create(truck.level(), EntitySpawnReason.TRIGGERED);
                if (gids != null) {
                    gids.setKind(GuhNpcEntity.Kind.BOSWACHTERGUH);
                    gids.setCustomNameVisible(false);
                }
            }
            if (gids != null) {
                uit[0] = gast(gids, truck, truckState, stoel(TruckEntity.ZIT[0], yaw), yaw, partialTick);
            }
            while (bezoekers.size() < TruckEntity.STOELEN) {   // (at most STOELEN - VRIJ ride at once)
                GuhEntity g = ModEntities.GUH.get().create(truck.level(), EntitySpawnReason.TRIGGERED);
                if (g == null) {
                    break;
                }
                int i = bezoekers.size();
                GuhVariant[] varianten = {GuhVariant.NORMAL, GuhVariant.MINT, GuhVariant.CHOCO, GuhVariant.SNOW, GuhVariant.NORMAL};
                g.setVariant(varianten[i % varianten.length]);
                draag(g, i);
                bezoekers.add(g);
            }
            for (int j = 0; j < stoelen.size() && j < bezoekers.size(); j++) {
                int s = stoelen.get(j);
                GuhEntity g = bezoekers.get(Math.floorMod(s + truck.k(), bezoekers.size()));
                // they look out to their side of the truck, now and then at the view ahead
                double[] z = TruckEntity.ZIT[s];
                float kijk = yaw + (z[0] < 0 ? 70f : -70f) + Mth.sin((truck.tickCount + s * 40) * 0.03f) * 25f;
                // G1 (design 1.5): looking through their binoculars, the head tipped up a little and slowly searching
                float op = -7f + Mth.sin((truck.tickCount + s * 23) * 0.05f) * 4f;
                g.setXRot(op);
                g.xRotO = op;
                uit[1 + j] = gast(g, truck, truckState, stoel(z, yaw), kijk, partialTick);
            }
            return uit;
        }

        static void draag(GuhEntity g, int i) {
            g.wear(kleding("SAFARIHOEDJE", "SAFARI_HOEDJE", i % 2 == 0 ? "STRAW_HAT" : "RAIN_HAT"));
            g.wear(kleding("SAFARI_VERREKIJKER", "VERREKIJKER", "DETECTIVE_VERGROOTGLAS"));   // (G1: everyone has binoculars)
        }

        /** The first clothes that exist (the safari clothes of slice S6 when they are there, else a fitting stand-in). */
        private static GuhClothes kleding(String... namen) {
            for (String n : namen) {
                try {
                    return GuhClothes.valueOf(n);
                } catch (IllegalArgumentException e) {
                    // not (yet) in this build
                }
            }
            return GuhClothes.STRAW_HAT;
        }

        private Gast gast(Entity e, TruckEntity truck, EntityRenderState truckState, Vec3 plek, float kijk, float partialTick) {
            e.setYRot(kijk);
            e.yRotO = kijk;
            if (e instanceof net.minecraft.world.entity.LivingEntity l) {
                l.yBodyRot = l.yBodyRotO = kijk;
                l.yHeadRot = l.yHeadRotO = kijk;
            }
            e.tickCount = truck.tickCount;
            EntityRenderState s = dispatcher.extractEntity(e, partialTick);
            s.lightCoords = truckState.lightCoords;
            s.shadowPieces.clear();
            return new Gast(s, plek);
        }
    }

    /**
     * Test round 5: the glass-bottom boat always has two safari guides on board, standing on the decks at both ends: a
     * ranger at the bow and a guide with binoculars at the stern, both slowly looking round (drawn like the truck's guests,
     * never real entities).
     */
    public static class BootRenderer extends GeoEntityRenderer<BootEntity, EntityRenderState> {
        private static final DataTicket<EntityRenderState[]> GIDSEN = DataTicket.create("guhs_safari_boot_gidsen", EntityRenderState[].class);
        private static final DataTicket<Vec3[]> PLEKKEN = DataTicket.create("guhs_safari_boot_plekken", Vec3[].class);
        /** Where they stand (model pixels): the middle of the bow deck and of the stern deck. */
        private static final double[][] STA = {{0, -2, -36}, {0, -2, 36}};

        private final EntityRenderDispatcher dispatcher;
        @Nullable
        private GuhNpcEntity ranger;
        @Nullable
        private GuhEntity gids;

        public BootRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<BootEntity>(Guhs.id("safari_rit/glasboot")));
            this.shadowRadius = 0f;
            this.dispatcher = context.getEntityRenderDispatcher();
        }

        @Override
        public void addRenderData(BootEntity boot, @Nullable Void related, EntityRenderState state, float partialTick) {
            float yaw = Mth.rotLerp(partialTick, boot.yRotO, boot.getYRot());
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, yaw);
            if (ranger == null || ranger.level() != boot.level()) {
                ranger = ModEntities.GUH_NPC.get().create(boot.level(), EntitySpawnReason.TRIGGERED);
                if (ranger != null) {
                    ranger.setKind(GuhNpcEntity.Kind.BOSWACHTERGUH);
                    ranger.setCustomNameVisible(false);
                }
            }
            if (gids == null || gids.level() != boot.level()) {
                gids = ModEntities.GUH.get().create(boot.level(), EntitySpawnReason.TRIGGERED);
                if (gids != null) {
                    gids.setVariant(GuhVariant.MINT);
                    TruckRenderer.draag(gids, 0);
                }
            }
            EntityRenderState[] wie = new EntityRenderState[2];
            Vec3[] waar = new Vec3[2];
            Entity[] guhs = {ranger, gids};
            for (int i = 0; i < 2; i++) {
                Entity e = guhs[i];
                if (e == null) {
                    continue;
                }
                // looking out over the water, now to one side, now to the other (each at its own pace)
                float kijk = yaw + (i == 0 ? 0f : 180f) + Mth.sin((boot.tickCount + i * 57) * (i == 0 ? 0.02f : 0.027f)) * 80f;
                e.setYRot(kijk);
                e.yRotO = kijk;
                if (e instanceof net.minecraft.world.entity.LivingEntity l) {
                    l.yBodyRot = l.yBodyRotO = kijk;
                    l.yHeadRot = l.yHeadRotO = kijk;
                    float op = i == 1 ? -6f + Mth.sin((boot.tickCount + 31) * 0.05f) * 4f : 0f;
                    l.setXRot(op);
                    l.xRotO = op;
                }
                e.tickCount = boot.tickCount;
                EntityRenderState s = dispatcher.extractEntity(e, partialTick);
                s.lightCoords = state.lightCoords;
                s.shadowPieces.clear();
                wie[i] = s;
                waar[i] = new Vec3(-STA[i][0] / 16.0, STA[i][1] / 16.0, -STA[i][2] / 16.0).yRot(-yaw * Mth.DEG_TO_RAD);
            }
            state.addGeckolibData(GIDSEN, wie);
            state.addGeckolibData(PLEKKEN, waar);
        }

        @Override
        public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            super.submit(state, poseStack, collector, camera);
            EntityRenderState[] wie = state.getGeckolibData(GIDSEN);
            Vec3[] waar = state.getGeckolibData(PLEKKEN);
            if (wie != null && waar != null) {
                for (int i = 0; i < wie.length; i++) {
                    if (wie[i] != null) {
                        dispatcher.submit(wie[i], camera, waar[i].x, waar[i].y, waar[i].z, poseStack, collector);
                    }
                }
            }
        }
    }

    /** The safaripoort: the centred model moved to the middle of its four blocks, turned for a gate along z. */
    public static class PoortRenderer extends GeoBlockRenderer<SafariPoortBlockEntity, BlockEntityRenderState> {
        private static final DataTicket<Boolean> LANGS_Z = DataTicket.create("guhs_safaripoort_langs_z", Boolean.class);

        public PoortRenderer(BlockEntityRendererProvider.Context context) {
            super(context, new DefaultedBlockGeoModel<SafariPoortBlockEntity>(Guhs.id("safaripoort")));
        }

        @Override
        public void addRenderData(SafariPoortBlockEntity be, @Nullable Void related, BlockEntityRenderState state, float partialTick) {
            state.addGeckolibData(LANGS_Z, be.as() == Direction.Axis.Z);
        }

        @Override
        public void adjustRenderPose(RenderPassInfo<BlockEntityRenderState> info) {
            PoseStack pose = info.poseStack();
            if (info.getOrDefaultGeckolibData(LANGS_Z, false)) {
                pose.translate(0, 0, 1.5);
                pose.mulPose(Axis.YP.rotationDegrees(90));
            } else {
                pose.translate(1.5, 0, 0);
            }
        }

        @Override
        public AABB getRenderBoundingBox(SafariPoortBlockEntity be) {
            BlockPos p = be.getBlockPos();
            return new AABB(p).expandTowards(be.as() == Direction.Axis.X ? 3 : 0, 6, be.as() == Direction.Axis.Z ? 3 : 0).inflate(2.5, 0.5, 2.5);
        }
    }

    private RitClient() {
    }
}
