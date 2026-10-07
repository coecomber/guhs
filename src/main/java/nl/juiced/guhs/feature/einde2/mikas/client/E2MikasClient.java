package nl.juiced.guhs.feature.einde2.mikas.client;

import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.mikas.E2MikasFeature;
import nl.juiced.guhs.feature.einde2.mikas.EnderMikaEntity;
import nl.juiced.guhs.feature.einde2.mikas.KaasmijtEntity;
import nl.juiced.guhs.feature.einde2.mikas.SchuildoosMenu;
import nl.juiced.guhs.feature.einde2.mikas.SchuilguhEntity;

/**
 * einde2, slice mikas, client side: the GeckoLib renderers of the Ender-Mika (its eyes and ender specks glow; the knabbel
 * it stole is drawn in its hand), the Kaasmijt (glowing feeler tips) and the Schuilguh (turned to the face it is stuck to),
 * the flits sparks, and the screen of the Schuildoos. Models, animations and textures: tools/features/einde2_mikas_modellen.py.
 */
public final class E2MikasClient {
    /** The knabbel an Ender-Mika holds (render state tickets: the renderer never sees the entity at submit time). */
    private static final DataTicket<ItemStackRenderState> BUIT = DataTicket.create("guhs_einde2_mikas_buit", ItemStackRenderState.class);
    /** The face of the block a Schuilguh holds on to. */
    private static final DataTicket<Direction> STEUN = DataTicket.create("guhs_einde2_mikas_steun", Direction.class);

    public static void init(IEventBus modBus) {
        modBus.addListener(E2MikasClient::renderers);
        modBus.addListener(E2MikasClient::particles);
        modBus.addListener((RegisterMenuScreensEvent event) -> event.register(E2MikasFeature.SCHUILDOOS_MENU.get(), SchuildoosScreen::new));
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(E2MikasFeature.ENDER_MIKA.get(), E2MikasClient::enderMika);
        event.registerEntityRenderer(E2MikasFeature.KAASMIJT.get(), E2MikasClient::kaasmijt);
        event.registerEntityRenderer(E2MikasFeature.SCHUILGUH.get(), SchuilguhRenderer::new);
    }

    private static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(E2MikasFeature.FLITS.get(),
                sprites -> (type, level, x, y, z, dx, dy, dz, random) -> new Flits(level, x, y, z, dx, dy, dz, sprites));
    }

    /** The tall Mika: its head follows its look, the glow mask lights its eyes, the stolen knabbel sits in its right hand. */
    private static GeoEntityRenderer<EnderMikaEntity, LivingEntityRenderState> enderMika(EntityRendererProvider.Context ctx) {
        var r = new GeoEntityRenderer<EnderMikaEntity, LivingEntityRenderState>(ctx, new DefaultedEntityGeoModel<EnderMikaEntity>(Guhs.id("ender_mika"))) {
            {
                this.shadowRadius = 0.35f;
            }

            @Override
            public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            }
        };
        r.withRenderLayer(new AutoGlowingGeoLayer<>(r));
        r.withRenderLayer(new GeoRenderLayer<EnderMikaEntity, Void, LivingEntityRenderState>(r) {
            @Override
            public void addRenderData(EnderMikaEntity mika, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
                ItemStack held = mika.getItemBySlot(EquipmentSlot.MAINHAND);
                if (!held.isEmpty()) {
                    ItemStackRenderState item = new ItemStackRenderState();
                    Minecraft.getInstance().getItemModelResolver().updateForTopItem(item, held, ItemDisplayContext.FIXED, mika.level(), mika, mika.getId());
                    state.addGeckolibData(BUIT, item);
                }
            }

            @Override
            public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> info, BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
                ItemStackRenderState item = info.getGeckolibData(BUIT);
                if (item == null || item.isEmpty() || !info.willRender()) {
                    return;
                }
                info.model().getBone("hand_right").ifPresent(hand -> consumer.accept(hand, (pass, bone, collector) -> {
                    PoseStack pose = pass.poseStack();
                    pose.translate(0, -0.06, -0.08);            // in its long fingers, held up for everyone to see
                    pose.mulPose(Axis.XP.rotationDegrees(70f));
                    pose.scale(0.42f, 0.42f, 0.42f);
                    item.submit(pose, collector, pass.packedLight(), OverlayTexture.NO_OVERLAY, pass.renderState().outlineColor);
                }));
            }
        });
        return r;
    }

    private static GeoEntityRenderer<KaasmijtEntity, LivingEntityRenderState> kaasmijt(EntityRendererProvider.Context ctx) {
        var r = new GeoEntityRenderer<KaasmijtEntity, LivingEntityRenderState>(ctx, new DefaultedEntityGeoModel<KaasmijtEntity>(Guhs.id("kaasmijt"))) {
            {
                this.shadowRadius = 0.15f;
            }
        };
        r.withRenderLayer(new AutoGlowingGeoLayer<>(r));
        return r;
    }

    /**
     * The Schuilguh: the model sits on a floor with its lid up; like the vanilla shulker the whole thing is turned around
     * the middle of its block so that the lid points away from the block it holds on to.
     */
    static class SchuilguhRenderer extends GeoEntityRenderer<SchuilguhEntity, LivingEntityRenderState> {
        SchuilguhRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<SchuilguhEntity>(Guhs.id("schuilguh")));
            this.shadowRadius = 0f;
        }

        @Override
        public void addRenderData(SchuilguhEntity guh, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(STEUN, guh.attachFace());
        }

        @Override
        public void submit(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
            Direction steun = state.getOrDefaultGeckolibData(STEUN, Direction.DOWN);
            if (steun == Direction.DOWN) {
                super.submit(state, pose, collector, camera);
                return;
            }
            pose.pushPose();
            pose.translate(0, 0.5, 0);
            pose.mulPose(steun.getOpposite().getRotation());
            pose.translate(0, -0.5, 0);
            super.submit(state, pose, collector, camera);
            pose.popPose();
        }
    }

    /** A spark of an Ender-Mika: purple or cheese yellow, shoots off a little way and fades. */
    static class Flits extends SingleQuadParticle {
        Flits(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
            super(level, x, y, z, dx, dy, dz, sprites.get(level.getRandom()));
            lifetime = 10 + random.nextInt(10);
            quadSize = 0.06f + random.nextFloat() * 0.06f;
            gravity = -0.02f;
            xd = dx + (random.nextDouble() - 0.5) * 0.12;
            yd = dy + (random.nextDouble() - 0.3) * 0.1;
            zd = dz + (random.nextDouble() - 0.5) * 0.12;
            hasPhysics = false;
        }

        @Override
        public void tick() {
            super.tick();
            alpha = Math.max(0f, 1f - age / (float) lifetime);
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return 0xF000F0;        // (they glow)
        }

        @Override
        protected SingleQuadParticle.Layer getLayer() {
            return SingleQuadParticle.Layer.TRANSLUCENT;
        }
    }

    /** The screen of the Schuildoos: three rows of nine (the vanilla shulker box picture). */
    static class SchuildoosScreen extends AbstractContainerScreen<SchuildoosMenu> {
        private static final Identifier ACHTERGROND = Identifier.withDefaultNamespace("textures/gui/container/shulker_box.png");

        SchuildoosScreen(SchuildoosMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title, 176, 167);
        }

        @Override
        public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            super.extractBackground(graphics, mouseX, mouseY, partialTick);
            int x = (this.width - this.imageWidth) / 2;
            int y = (this.height - this.imageHeight) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, ACHTERGROND, x, y, 0f, 0f, this.imageWidth, this.imageHeight, 256, 256);
        }
    }

    private E2MikasClient() {
    }
}
