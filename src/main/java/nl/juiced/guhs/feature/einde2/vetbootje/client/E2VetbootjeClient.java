package nl.juiced.guhs.feature.einde2.vetbootje.client;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.feature.einde2.vetbootje.E2VetbootjeFeature;
import nl.juiced.guhs.feature.einde2.vetbootje.HeteluchtballonEntity;
import nl.juiced.guhs.feature.einde2.vetbootje.VetbootjeEntity;
import nl.juiced.guhs.feature.einde2.vetbootje.WerfMenu;
import nl.juiced.guhs.feature.einde2.vetbootje.Zeil;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;

/**
 * einde2, slice vetbootje, client side: the two renderers (the vetbootje in layers: its base texture, the parts in the
 * boat's colour, the picture on the sail, the glowing lampion; the heteluchtballon with its flame and its wind vane), the
 * scheepswerf screen, and the rider's keys (W/S/A/D, jump = up or the burner, sprint = down).
 */
public final class E2VetbootjeClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(E2VetbootjeFeature.VETBOOTJE.get(), VetbootjeRenderer::new);
            event.registerEntityRenderer(E2VetbootjeFeature.HETELUCHTBALLON.get(), HeteluchtballonRenderer::new);
        });
        modBus.addListener((RegisterMenuScreensEvent event) -> event.register(E2VetbootjeFeature.WERF_MENU.get(), WerfScreen::new));
        VetbootjeEntity.invoer = E2VetbootjeClient::toetsen;
        VetbootjeEntity.clientTick = E2VetbootjeClient::kielzog;
        HeteluchtballonEntity.branderToets = E2VetbootjeClient::springt;
        HeteluchtballonEntity.meld = (ballon, aan) -> ClientPacketDistributor.sendToServer(new WerfMenu.Brander(ballon, aan));
        HeteluchtballonEntity.clientTick = E2VetbootjeClient::vonken;
    }

    /** The keys of the local player (nothing while a screen is open). */
    private static VetbootjeEntity.Invoer toetsen() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || Minecraft.getInstance().screen != null) {
            return VetbootjeEntity.Invoer.NIKS;
        }
        // (26.1: the move vector is normalised; the boat wants the plain keys)
        var keys = p.input.keyPresses;
        double vooruit = keys.forward() == keys.backward() ? 0 : keys.forward() ? 1 : -1;
        double links = keys.left() == keys.right() ? 0 : keys.left() ? 1 : -1;
        double omhoog = keys.jump() == keys.sprint() ? 0 : keys.jump() ? 1 : -1;
        return new VetbootjeEntity.Invoer(vooruit, -links, omhoog);
    }

    private static boolean springt() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && Minecraft.getInstance().screen == null && p.input.keyPresses.jump();
    }

    /** A sailing vetbootje leaves a drop of vet behind now and then. */
    private static void kielzog(VetbootjeEntity boot) {
        double v = Math.hypot(boot.getX() - boot.xo, boot.getZ() - boot.zo);
        if (v > 0.05 && boot.getRandom().nextInt(6) == 0) {
            Vec3 achter = Vec3.directionFromRotation(0, boot.getYRot()).scale(-1.2);
            boot.level().addParticle(ParticleTypes.FALLING_HONEY, boot.getX() + achter.x + (boot.getRandom().nextDouble() - 0.5) * 0.8, boot.getY() + 0.05,
                    boot.getZ() + achter.z + (boot.getRandom().nextDouble() - 0.5) * 0.8, 0, 0, 0);
        }
    }

    /** The burner's sparks and a little smoke. */
    private static void vonken(HeteluchtballonEntity ballon) {
        if (ballon.brander() && ballon.getRandom().nextInt(2) == 0) {
            ballon.level().addParticle(ParticleTypes.SMALL_FLAME, ballon.getX() + (ballon.getRandom().nextDouble() - 0.5) * 0.25, ballon.getY() + 1.6,
                    ballon.getZ() + (ballon.getRandom().nextDouble() - 0.5) * 0.25, 0, 0.04, 0);
        }
    }

    // =================================================================================================================

    /**
     * The vetbootje: geckolib/models/entity/vetbootje.geo.json, drawn in layers (see the python module): the base texture,
     * then only the coloured parts in the boat's dye colour, then only the picture on the sail, then the lampion full
     * bright. It leans into its bends and its wheel turns with the helm.
     */
    public static class VetbootjeRenderer extends GeoEntityRenderer<VetbootjeEntity, EntityRenderState> {
        private static final DataTicket<Integer> KLEUR = DataTicket.create("guhs_einde2_vetbootje_kleur", Integer.class);
        private static final DataTicket<Integer> ZEIL = DataTicket.create("guhs_einde2_vetbootje_zeil", Integer.class);
        /** {yaw, lean (degrees), the wheel's turn (degrees)} */
        private static final DataTicket<float[]> DRAAI = DataTicket.create("guhs_einde2_vetbootje_draai", float[].class);
        private static final Identifier GLOED = Guhs.id("textures/entity/vetbootje_gloed.png");
        private static final Identifier[] KLEUREN = new Identifier[DyeColor.values().length];
        private static final Identifier[] ZEILEN = new Identifier[Zeil.values().length];

        static {
            for (DyeColor c : DyeColor.values()) {
                KLEUREN[c.getId()] = Guhs.id("textures/entity/vetbootje_kleur_" + c.getName() + ".png");
            }
            for (Zeil z : Zeil.values()) {
                ZEILEN[z.ordinal()] = Guhs.id("textures/entity/vetbootje_zeil_" + z.id() + ".png");
            }
        }

        public VetbootjeRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<VetbootjeEntity>(Guhs.id("vetbootje")));
            this.shadowRadius = 0.9f;
            withRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void submitRenderTask(RenderPassInfo<EntityRenderState> info, SubmitNodeCollector collector) {
                    if (!info.willRender()) {
                        return;
                    }
                    Integer kleur = info.getGeckolibData(KLEUR), zeil = info.getGeckolibData(ZEIL);
                    if (kleur != null) {
                        getRenderer().submitRenderTasks(info, collector.order(1), RenderTypes.entityCutout(KLEUREN[Math.floorMod(kleur, KLEUREN.length)]));
                    }
                    if (zeil != null) {
                        getRenderer().submitRenderTasks(info, collector.order(2), RenderTypes.entityCutout(ZEILEN[Math.floorMod(zeil, ZEILEN.length)]));
                    }
                    getRenderer().submitRenderTasks(info, collector.order(3), GuhRenderTypes.eyes(GLOED));
                }
            });
        }

        /** The mast stands high above the hull: don't cull it as soon as the hull is off-screen. */
        @Override
        protected AABB getBoundingBoxForCulling(VetbootjeEntity boot) {
            return boot.getBoundingBox().inflate(1.2, 0, 1.2).expandTowards(0, 2.4, 0);
        }

        @Override
        public void addRenderData(VetbootjeEntity boot, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(KLEUR, boot.kleur().getId());
            state.addGeckolibData(ZEIL, boot.zeil().ordinal());
            state.addGeckolibData(DRAAI, new float[]{Mth.rotLerp(partialTick, boot.yRotO, boot.getYRot()), Mth.lerp(partialTick, boot.hellingO, boot.helling),
                    Mth.lerp(partialTick, boot.wielO, boot.wiel)});
        }

        @Override
        protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
            float[] d = info.getGeckolibData(DRAAI);
            if (d == null) {
                return;
            }
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - d[0]));
            poseStack.mulPose(Axis.ZP.rotationDegrees(d[1]));
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info, BoneSnapshots bones) {
            float[] d = info.getGeckolibData(DRAAI);
            if (d != null) {
                bones.ifPresent("stuurwiel", b -> b.setRotZ(d[2] * Mth.DEG_TO_RAD));
            }
        }
    }

    /**
     * The heteluchtballon: geckolib/models/entity/heteluchtballon.geo.json. The flame only shows while the burner is on
     * (and glows), the little vane on the basket points where the wind blows.
     */
    public static class HeteluchtballonRenderer extends GeoEntityRenderer<HeteluchtballonEntity, EntityRenderState> {
        private static final DataTicket<Boolean> BRANDER = DataTicket.create("guhs_einde2_vetbootje_brander", Boolean.class);
        /** {yaw, the vane's turn against the basket (radians)} */
        private static final DataTicket<float[]> DRAAI = DataTicket.create("guhs_einde2_vetbootje_ballon_draai", float[].class);
        private static final Identifier GLOED = Guhs.id("textures/entity/heteluchtballon_gloed.png");

        public HeteluchtballonRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<HeteluchtballonEntity>(Guhs.id("heteluchtballon")));
            this.shadowRadius = 1.2f;
            withRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void submitRenderTask(RenderPassInfo<EntityRenderState> info, SubmitNodeCollector collector) {
                    if (info.willRender() && Boolean.TRUE.equals(info.getGeckolibData(BRANDER))) {
                        getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(GLOED));
                    }
                }
            });
        }

        /** The envelope floats high above the basket: don't cull it as soon as the basket is off-screen. */
        @Override
        protected AABB getBoundingBoxForCulling(HeteluchtballonEntity ballon) {
            return ballon.getBoundingBox().inflate(1.4, 0, 1.4).expandTowards(0, 5, 0);
        }

        @Override
        public void addRenderData(HeteluchtballonEntity ballon, @Nullable Void related, EntityRenderState state, float partialTick) {
            float yaw = Mth.rotLerp(partialTick, ballon.yRotO, ballon.getYRot());
            Vec3 wind = HeteluchtballonEntity.wind(ballon.level(), ballon.getY(), ballon.level().getGameTime());
            float windYaw = (float) Math.toDegrees(Math.atan2(-wind.x, wind.z));
            state.addGeckolibData(BRANDER, ballon.brander());
            state.addGeckolibData(DRAAI, new float[]{yaw, Mth.wrapDegrees(windYaw - yaw) * Mth.DEG_TO_RAD});
        }

        @Override
        protected void applyRotations(RenderPassInfo<EntityRenderState> info, PoseStack poseStack, float nativeScale) {
            float[] d = info.getGeckolibData(DRAAI);
            if (d != null) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180f - d[0]));
            }
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<EntityRenderState> info, BoneSnapshots bones) {
            boolean aan = Boolean.TRUE.equals(info.getGeckolibData(BRANDER));
            bones.ifPresent("vlam", b -> b.skipRender(!aan).skipChildrenRender(!aan));
            float[] d = info.getGeckolibData(DRAAI);
            if (d != null) {
                bones.ifPresent("windvaan", b -> b.setRotY(-d[1]));
            }
        }
    }

    private E2VetbootjeClient() {
    }
}
