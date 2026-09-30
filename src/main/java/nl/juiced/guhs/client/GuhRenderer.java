package nl.juiced.guhs.client;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.item.GuhArmorItem;

/**
 * Renders the guh from assets/guhs/geckolib/models/entity/guh.geo.json + textures/entity/guh.png (edit both in Blockbench).
 * GeckoLib already applies the SCALE attribute (random guh size); here we add baby size and the "gravity" squish.
 * <p>
 * 1.1.0 (GeckoLib 5, the reference port for every GeckoLib renderer, see MIGRATION_NOTES "How to port a renderer"):
 * <ol>
 *   <li>{@link #addRenderData} (extract, entity available) copies everything into a {@link GuhRenderFrame} (ticket
 *       {@link #FRAME}) and runs the feature {@link Hook}s;</li>
 *   <li>{@link #adjustModelBonesForRender} (submit, no entity) hides/moves bones through {@link BoneSnapshots}
 *       (was GeoBone.setHidden/setPosY/setRotX in preRender/setCustomAnimations);</li>
 *   <li>render layers submit the clothes, glow, paper and hook passes.</li>
 * </ol>
 */
public class GuhRenderer extends GeoEntityRenderer<GuhEntity, LivingEntityRenderState> {
    private static final float BASE_SHADOW = 0.45f;

    /** This frame's look of the guh (see {@link GuhRenderFrame}). */
    public static final DataTicket<GuhRenderFrame> FRAME = DataTicket.create("guhs_guh_frame", GuhRenderFrame.class);

    /** The sleeping textures with closed eyes (textures/entity/guh_slaap/, made by tools/make_sleep_eyes.py); empty: none. */
    private static final Map<Identifier, Optional<Identifier>> SLAAP = new ConcurrentHashMap<>();

    /** A feature's change to how every guh looks (register from the feature's client init with {@link #hook}). */
    @FunctionalInterface
    public interface Hook {
        /** Extract time: read the guh, put what should happen at render time into the frame (values only, not the guh). */
        void extract(GuhEntity guh, float partialTick, GuhRenderFrame frame);
    }

    private static final List<Hook> HOOKS = new CopyOnWriteArrayList<>();

    /** Add a {@link Hook} (runs for every guh drawn by this renderer and its subclasses, in registration order). */
    public static void hook(Hook hook) {
        HOOKS.add(hook);
    }

    /** Asleep: the SLAPEN emote (a nap, the night) or in a guh nest. */
    public static boolean slaapt(GuhEntity guh) {
        return guh.emotes.current() == nl.juiced.guhs.feature.emotes.Emote.SLAPEN
                || (guh.getKnusVlaggen() & nl.juiced.guhs.feature.vadswoud.SleepInNestGoal.OOGJES_DICHT) != 0;
    }

    /** This texture with closed eyes (the texture itself when there's no sleeping one). */
    public static Identifier slaap(Identifier texture) {
        return SLAAP.computeIfAbsent(texture, t -> {
            Identifier s = t.withPath(p -> p.replace("textures/entity/", "textures/entity/guh_slaap/"));
            return Minecraft.getInstance().getResourceManager().getResource(s).map(r -> s);
        }).orElse(texture);
    }

    /** This frame's data (null outside a guh render pass). */
    @Nullable
    public static GuhRenderFrame frame(GeoRenderState state) {
        return state.getGeckolibData(FRAME);
    }

    public GuhRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<GuhEntity>(Guhs.id("guh")) {
            /** The texture was chosen at extract time (variant, feature hooks, sleeping eyes). */
            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                GuhRenderFrame frame = frame(state);
                return frame == null ? super.getTextureResource(state) : frame.sleeping ? slaap(frame.texture()) : frame.texture();
            }
        });
        this.shadowRadius = BASE_SHADOW;
        withRenderLayer(new ClothesLayer(this));
        withRenderLayer(new GlowLayer(this));
        withRenderLayer(new PaperLayer(this));
        withRenderLayer(new HookLayer(this));
    }

    // --- extract -----------------------------------------------------------------------------------------------------

    @Override
    public void addRenderData(GuhEntity guh, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
        GuhRenderFrame frame = new GuhRenderFrame(guh, partialTick);
        for (Hook hook : HOOKS) {
            hook.extract(guh, partialTick, frame);
        }
        state.addGeckolibData(FRAME, frame);
    }

    /** The shadow grows with the guh (its size attribute and baby size). */
    @Override
    protected float getShadowRadius(LivingEntityRenderState state) {
        return BASE_SHADOW * state.scale * state.ageScale;
    }

    /** The ghost guh is see-through. */
    @Override
    public int getRenderColor(GuhEntity guh, @Nullable Void related, float partialTick) {
        int colour = super.getRenderColor(guh, related, partialTick);
        return guh.getVariant() == GuhVariant.GHOST ? (colour & 0x00FFFFFF) | (Math.min(0x88, colour >>> 24) << 24) : colour;
    }

    // --- submit ------------------------------------------------------------------------------------------------------

    @Override
    public @Nullable RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
        GuhRenderFrame frame = frame(state);
        if (frame != null && frame.variant == GuhVariant.GHOST && !state.isInvisible) {
            return RenderTypes.entityTranslucent(texture);
        }
        return super.getRenderType(state, texture);
    }

    /** Baby size and the "gravity" squish (wider and flatter). */
    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
        GuhRenderFrame frame = frame(info.renderState());
        float age = frame == null ? 1f : frame.ageScale;
        float squish = frame == null ? 0f : frame.squish;
        super.scaleModelForRender(info, widthScale * age * (1f + 0.12f * squish), heightScale * age * (1f - 0.28f * squish));
    }

    /** After the body rotation: the features' pose changes (2.9: the Pinguh's waddle and slide). */
    @Override
    protected void applyRotations(RenderPassInfo<LivingEntityRenderState> info, PoseStack poseStack, float nativeScale) {
        super.applyRotations(info, poseStack, nativeScale);
        GuhRenderFrame frame = frame(info.renderState());
        if (frame != null) {
            for (GuhRenderFrame.PoseMove move : frame.poseMoves) {
                move.apply(poseStack);
            }
        }
    }

    /** Head look, variant bones, saddle, armour; then the features' bone moves. Runs after the animations. */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
        // the bone called "head" follows where the guh is looking (was DefaultedEntityGeoModel(id, true))
        DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        GuhRenderFrame frame = frame(info.renderState());
        if (frame == null) {
            return;
        }
        // the brontosaurus guh's head sits on top of its long neck (the ears follow the head)
        boolean bronto = frame.variant == GuhVariant.BRONTOSAURUS;
        bones.ifPresent("head", head -> head.setTranslateY(bronto ? GuhVariant.NECK_UP : 0).setTranslateZ(bronto ? -GuhVariant.NECK_FORWARD : 0));
        // the teckel guh: back legs and tail further back, its middle legs walk along with the front ones
        if (frame.variant == GuhVariant.TECKEL) {
            for (String bone : new String[]{"leg_back_left", "leg_back_right", "tail"}) {
                bones.ifPresent(bone, b -> b.setTranslateZ(GuhVariant.TECKEL_STRETCH));
            }
            bones.ifPresent("leg_front_right", front -> bones.ifPresent("teckel_leg_left", mid -> mid.setRotX(front.getRotX())));
            bones.ifPresent("leg_front_left", front -> bones.ifPresent("teckel_leg_right", mid -> mid.setRotX(front.getRotX())));
        }
        applyVisibility(info.model(), bones, frame);
        for (GuhRenderFrame.BoneMove move : frame.boneMoves) {
            move.apply(bones);
        }
    }

    /** Which bones the main pass shows: saddle, the worn armour tier, and the variant's clothes / neck. */
    static void applyVisibility(BakedGeoModel model, BoneSnapshots bones, GuhRenderFrame frame) {
        hideWithChildren(bones, "saddle", !frame.saddled);
        for (GuhArmorItem.Tier tier : GuhArmorItem.Tier.values()) {
            String name = "armor_" + tier.name().toLowerCase(Locale.ROOT);
            boolean hidden = tier != frame.armour;
            hideWithChildren(bones, name, hidden);
            hideWithChildren(bones, name + "_body", hidden);
        }
        for (GeoBone bone : model.boneLookup().get().values()) {
            String name = bone.name();
            if (GuhVariant.VARIANT_BONES.stream().anyMatch(name::startsWith)) {
                boolean covered = false;
                for (Map.Entry<GuhClothes.Slot, GuhClothes> worn : frame.clothes.entrySet()) {
                    if (GuhClothes.slotBones(worn.getKey()).stream().anyMatch(name::startsWith)) {
                        covered = true;
                    }
                }
                // (only the bone's own cubes: its child bones decide for themselves)
                bones.get(bone).skipRender(covered || !frame.variant.shows(name));
            }
        }
    }

    /** GeckoLib 4's setHidden(true): the bone and everything on it. */
    private static void hideWithChildren(BoneSnapshots bones, String name, boolean hidden) {
        if (hidden) {
            bones.ifPresent(name, b -> b.skipRender(true).skipChildrenRender(true));
        }
    }

    // --- extra passes ------------------------------------------------------------------------------------------------

    /**
     * Submit the model once more with its own render type and bone visibility: {@code shows(bone)} decides for every bone
     * whether its own cubes are drawn (child bones are still visited). Animation poses of this frame are kept.
     * This replaces GeckoLib 4's {@code reRender} with bones hidden before and shown again after.
     */
    public static <R extends GeoRenderState> void submitPass(RenderPassInfo<R> info, OrderedSubmitNodeCollector collector, RenderType type,
                                                            int light, int colour, Predicate<String> shows) {
        if (!info.willRender()) {
            return;
        }
        BakedGeoModel model = info.model();
        int overlay = info.packedOverlay();
        collector.submitCustomGeometry(info.poseStack(), type, (pose, buffer) -> {
            PoseStack poseStack = info.poseStack();
            poseStack.pushPose();
            poseStack.last().pose().set(pose.pose());
            poseStack.last().normal().set(pose.normal());
            info.renderPosed(() -> withVisibility(model, shows, () -> model.render(info, buffer, light, overlay, colour)));
            poseStack.popPose();
        });
    }

    /** Run {@code render} with every bone's own visibility set by {@code shows}; puts everything back afterwards. */
    private static void withVisibility(BakedGeoModel model, Predicate<String> shows, Runnable render) {
        var all = model.boneLookup().get().values();
        GeoBone[] changed = new GeoBone[all.size()];
        boolean[][] was = new boolean[all.size()][];
        int n = 0;
        for (GeoBone bone : all) {
            BoneSnapshot snap = bone.frameSnapshot;
            if (snap == null) {
                bone.frameSnapshot = snap = BoneSnapshot.create(bone);
                was[n] = null;                                    // temporary: removed again below
            } else {
                was[n] = new boolean[]{snap.isHidden(), snap.areChildrenHidden()};
            }
            changed[n++] = bone;
            snap.skipRender(!shows.test(bone.name())).skipChildrenRender(false);
        }
        try {
            render.run();
        } finally {
            for (int i = 0; i < n; i++) {
                if (was[i] == null) {
                    changed[i].frameSnapshot = null;
                } else {
                    changed[i].frameSnapshot.skipRender(was[i][0]).skipChildrenRender(was[i][1]);
                }
            }
        }
    }

    /** For hooks: an item render state (extract time) to submit later with {@link ItemStackRenderState#submit}. */
    public static ItemStackRenderState itemState(ItemStack stack, ItemDisplayContext context, GuhEntity guh) {
        ItemStackRenderState state = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, context, guh.level(), guh, guh.getId());
        return state;
    }

    // --- layers ------------------------------------------------------------------------------------------------------

    /**
     * The clothes a guh wears (from the Guh kleermaker): for each worn piece, the model again with only that piece's bones
     * visible and the piece's own texture, so clothes look the same on every guh variant.
     */
    static class ClothesLayer extends GeoRenderLayer<GuhEntity, Void, LivingEntityRenderState> {
        ClothesLayer(GeoRenderer<GuhEntity, Void, LivingEntityRenderState> renderer) {
            super(renderer);
        }

        @Override
        public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
            GuhRenderFrame frame = frame(info.renderState());
            if (frame == null || frame.clothes.isEmpty()) {
                return;
            }
            for (Map.Entry<GuhClothes.Slot, GuhClothes> e : frame.clothes.entrySet()) {
                GuhClothes worn = e.getValue();
                // (2.8, kapper: the high part of a hairstyle, bones "*_kruin", hides under a hat)
                boolean kruin = e.getKey() != GuhClothes.Slot.HAAR || frame.worn(GuhClothes.Slot.HEAD) == null;
                // (2.8: hair is tinted with the guh's hair colour, -1 = its own colours)
                int colour = e.getKey() == GuhClothes.Slot.HAAR && frame.haarkleur >= 0 ? 0xFF000000 | frame.haarkleur : 0xFFFFFFFF;
                submitPass(info, collector.order(1), RenderTypes.entityCutout(worn.texture()), info.packedLight(), colour,
                        name -> worn.shows(name) && (kruin || !name.endsWith("_kruin")));
            }
        }
    }

    /** The starry/ender guh's glowmask and a story variant's glow (feature hooks: {@link GuhRenderFrame#glow}), full bright. */
    static class GlowLayer extends GeoRenderLayer<GuhEntity, Void, LivingEntityRenderState> {
        private static final Identifier GLOW = Guhs.id("textures/entity/guh_starry_glowmask.png");
        private static final Identifier ENDER_GLOW = Guhs.id("textures/entity/guh_ender_glowmask.png");
        private static final Identifier VAHOEGE_GLOW = Guhs.id("textures/entity/guh_vahoege_ender_glowmask.png");

        GlowLayer(GeoRenderer<GuhEntity, Void, LivingEntityRenderState> renderer) {
            super(renderer);
        }

        @Override
        public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
            GuhRenderFrame frame = frame(info.renderState());
            if (frame == null || !info.willRender()) {
                return;
            }
            Identifier own = switch (frame.variant) {
                case STARRY -> GLOW;
                case ENDER -> frame.sleeping ? slaap(ENDER_GLOW) : ENDER_GLOW;
                case VAHOEGE_ENDER -> VAHOEGE_GLOW;
                default -> null;
            };
            if (own != null) {
                getRenderer().submitRenderTasks(info, collector.order(1), nl.juiced.guhs.client.GuhRenderTypes.eyes(own));
            }
            for (Identifier glow : frame.glows) {
                getRenderer().submitRenderTasks(info, collector.order(1), nl.juiced.guhs.client.GuhRenderTypes.eyes(glow));
            }
        }
    }

    /** A guh with a secret note holds a paper in its mouth (on the "head" bone). */
    static class PaperLayer extends GeoRenderLayer<GuhEntity, Void, LivingEntityRenderState> {
        private static final DataTicket<ItemStackRenderState> PAPER = DataTicket.create("guhs_guh_paper", ItemStackRenderState.class);
        /** Made on first use: renderers are built during the resource reload, before item components are bound. */
        private @Nullable ItemStack paper;

        PaperLayer(GeoRenderer<GuhEntity, Void, LivingEntityRenderState> renderer) {
            super(renderer);
        }

        @Override
        public void addRenderData(GuhEntity guh, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            if (guh.hasSecretNote()) {
                if (paper == null) {
                    paper = new ItemStack(Items.PAPER);
                }
                state.addGeckolibData(PAPER, itemState(paper, ItemDisplayContext.FIXED, guh));
            }
        }

        @Override
        public void addPerBoneRender(RenderPassInfo<LivingEntityRenderState> info,
                                     BiConsumer<GeoBone, PerBoneRender<LivingEntityRenderState>> consumer) {
            ItemStackRenderState item = info.getGeckolibData(PAPER);
            if (item == null || item.isEmpty() || !info.willRender()) {
                return;
            }
            info.model().getBone("head").ifPresent(head -> consumer.accept(head, (pass, bone, collector) -> {
                PoseStack pose = pass.poseStack();
                pose.translate(0, -0.14, -0.66);                 // in front of the mouth
                pose.mulPose(Axis.XP.rotationDegrees(80));        // lying (almost) flat, sticking out
                pose.scale(0.42f, 0.42f, 0.42f);
                item.submit(pose, collector, pass.packedLight(), OverlayTexture.NO_OVERLAY, pass.renderState().outlineColor);
            }));
        }
    }

    /** The feature hooks' model passes and extras (1.0.0: feature.knus.client.GuhRenderHooks' layer), after the clothes. */
    static class HookLayer extends GeoRenderLayer<GuhEntity, Void, LivingEntityRenderState> {
        HookLayer(GeoRenderer<GuhEntity, Void, LivingEntityRenderState> renderer) {
            super(renderer);
        }

        @Override
        public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
            GuhRenderFrame frame = frame(info.renderState());
            if (frame == null) {
                return;
            }
            for (GuhRenderFrame.ModelPass pass : frame.passes) {
                submitPass(info, collector.order(1), pass.type(), pass.fullBright() ? LightCoordsUtil.FULL_BRIGHT : info.packedLight(),
                        pass.colour(), pass.shows());
            }
            if (frame.extras.isEmpty() && frame.layerExtras.isEmpty()) {
                return;
            }
            // entity space: the pose from before the renderer turned and scaled the model
            PoseStack pose = info.poseStack();
            pose.pushPose();
            pose.last().pose().set(info.getPreRenderMatrixPose().pose());
            pose.last().normal().set(info.getPreRenderMatrixPose().normal());
            for (GuhRenderFrame.Extra extra : frame.extras) {
                pose.pushPose();
                extra.submit(pose, collector, info.packedLight());
                pose.popPose();
            }
            pose.scale(frame.ageScale * (1f + 0.12f * frame.squish), frame.ageScale * (1f - 0.28f * frame.squish),
                    frame.ageScale * (1f + 0.12f * frame.squish));
            for (GuhRenderFrame.Extra extra : frame.layerExtras) {
                pose.pushPose();
                extra.submit(pose, collector, info.packedLight());
                pose.popPose();
            }
            pose.popPose();
        }
    }
}
