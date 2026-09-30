package nl.juiced.guhs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.item.GuhArmorItem;
import com.mojang.math.Axis;
import net.minecraft.resources.ResourceLocation;
import nl.juiced.guhs.entity.GuhVariant;
import software.bernie.geckolib.animation.AnimationState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders the guh from assets/guhs/geo/entity/guh.geo.json + textures/entity/guh.png (edit both in Blockbench).
 * GeckoLib already applies the SCALE attribute (random guh size); here we add baby size and the "gravity" squish.
 */
public class GuhRenderer extends GeoEntityRenderer<GuhEntity> {
    private static final float BASE_SHADOW = 0.45f;

    /** The sleeping textures with closed eyes (textures/entity/guh_slaap/, made by tools/make_sleep_eyes.py); null: none. */
    private static final java.util.Map<ResourceLocation, java.util.Optional<ResourceLocation>> SLAAP = new java.util.concurrent.ConcurrentHashMap<>();

    /** Asleep: the SLAPEN emote (a nap, the night) or in a guh nest. */
    public static boolean slaapt(GuhEntity guh) {
        return guh.emotes.current() == nl.juiced.guhs.feature.emotes.Emote.SLAPEN
                || (guh.getKnusVlaggen() & nl.juiced.guhs.feature.vadswoud.SleepInNestGoal.OOGJES_DICHT) != 0;
    }

    /** This texture with closed eyes (the texture itself when there's no sleeping one). */
    public static ResourceLocation slaap(ResourceLocation texture) {
        return SLAAP.computeIfAbsent(texture, t -> {
            ResourceLocation s = t.withPath(p -> p.replace("textures/entity/", "textures/entity/guh_slaap/"));
            return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(s).map(r -> s);
        }).orElse(texture);
    }

    public GuhRenderer(EntityRendererProvider.Context context) {
        // true = the bone called "head" follows where the guh is looking
        super(context, new DefaultedEntityGeoModel<>(Guhs.id("guh"), true) {
            @Override
            public ResourceLocation getTextureResource(GuhEntity guh) {
                ResourceLocation pinguh = nl.juiced.guhs.feature.guhpolder.client.PinguhRender.texture(guh);   // 2.9: the Pinguh's look
                ResourceLocation eigen = nl.juiced.guhs.feature.verhaal.client.VariantUiterlijk.texture(guh);   // 3.0: a story variant's look
                ResourceLocation tex = pinguh != null ? pinguh : eigen != null ? eigen : guh.getVariant().texture(guh.tickCount + guh.getId());
                return slaapt(guh) ? GuhRenderer.slaap(tex) : tex;
            }

            @Override
            public void setCustomAnimations(GuhEntity guh, long instanceId, AnimationState<GuhEntity> animationState) {
                super.setCustomAnimations(guh, instanceId, animationState);
                // the brontosaurus guh's head sits on top of its long neck (the ears follow the head)
                boolean bronto = guh.getVariant() == GuhVariant.BRONTOSAURUS;
                getBone("head").ifPresent(head -> {
                    head.setPosY(bronto ? GuhVariant.NECK_UP : 0);
                    head.setPosZ(bronto ? -GuhVariant.NECK_FORWARD : 0);
                });
                // the teckel guh: back legs and tail further back, its middle legs walk along with the front ones
                boolean teckel = guh.getVariant() == GuhVariant.TECKEL;
                for (String bone : new String[]{"leg_back_left", "leg_back_right", "tail"}) {
                    getBone(bone).ifPresent(b -> b.setPosZ(teckel ? GuhVariant.TECKEL_STRETCH : 0));
                }
                if (teckel) {
                    getBone("leg_front_right").ifPresent(front -> getBone("teckel_leg_left").ifPresent(mid -> mid.setRotX(front.getRotX())));
                    getBone("leg_front_left").ifPresent(front -> getBone("teckel_leg_right").ifPresent(mid -> mid.setRotX(front.getRotX())));
                }
                // 2.9: the Pinguh's belly-slide and flippers (feature.guhpolder)
                nl.juiced.guhs.feature.guhpolder.client.PinguhRender.animate(guh, this::getBone, animationState.getPartialTick());
                // 3.0: the story variants' own bone moves (feature.verhaal.client.VariantUiterlijk)
                nl.juiced.guhs.feature.verhaal.client.VariantUiterlijk.botten(guh, this::getBone, animationState.getPartialTick());
            }
        });
        this.shadowRadius = BASE_SHADOW;
        addRenderLayer(new GuhClothesLayer(this));
        // the starry guh's stars light up in the dark
        addRenderLayer(new software.bernie.geckolib.renderer.layer.GeoRenderLayer<>(this) {
            private final ResourceLocation glow = nl.juiced.guhs.Guhs.id("textures/entity/guh_starry_glowmask.png");
            private final ResourceLocation enderGlow = nl.juiced.guhs.Guhs.id("textures/entity/guh_ender_glowmask.png");
            private final ResourceLocation vahoegeGlow = nl.juiced.guhs.Guhs.id("textures/entity/guh_vahoege_ender_glowmask.png");

            @Override
            public void render(PoseStack poseStack, GuhEntity guh, BakedGeoModel model, net.minecraft.client.renderer.RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (guh.getVariant() == GuhVariant.STARRY || guh.getVariant() == GuhVariant.ENDER || guh.getVariant() == GuhVariant.VAHOEGE_ENDER) {
                    var type = net.minecraft.client.renderer.RenderType.eyes(guh.getVariant() == GuhVariant.ENDER ? (slaapt(guh) ? slaap(enderGlow) : enderGlow)
                            : guh.getVariant() == GuhVariant.VAHOEGE_ENDER ? vahoegeGlow : glow);
                    getRenderer().reRender(model, poseStack, bufferSource, guh, type, bufferSource.getBuffer(type), partialTick,
                            net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, packedOverlay, 0xFFFFFFFF);
                }
            }
        });
        // 3.0: a story variant's own glow (feature.verhaal.client.VariantUiterlijk.glow), full bright like the stars
        addRenderLayer(new software.bernie.geckolib.renderer.layer.GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack, GuhEntity guh, BakedGeoModel model, net.minecraft.client.renderer.RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                ResourceLocation glow = nl.juiced.guhs.feature.verhaal.client.VariantUiterlijk.glow(guh);
                if (glow != null) {
                    var type = net.minecraft.client.renderer.RenderType.eyes(glow);
                    getRenderer().reRender(model, poseStack, bufferSource, guh, type, bufferSource.getBuffer(type), partialTick,
                            net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, packedOverlay, 0xFFFFFFFF);
                }
            }
        });
        // a guh with a secret note holds a paper in its mouth
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            private final ItemStack paper = new ItemStack(Items.PAPER);

            @Override
            protected ItemStack getStackForBone(GeoBone bone, GuhEntity guh) {
                return guh.hasSecretNote() && bone.getName().equals("head") ? paper : null;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, GuhEntity guh) {
                return ItemDisplayContext.FIXED;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, GuhEntity guh,
                                              MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
                poseStack.translate(0, -0.14, -0.66);                 // in front of the mouth
                poseStack.mulPose(Axis.XP.rotationDegrees(80));        // lying (almost) flat, sticking out
                poseStack.scale(0.42f, 0.42f, 0.42f);
                super.renderStackForBone(poseStack, bone, stack, guh, bufferSource, partialTick, packedLight, packedOverlay);
            }
        });
        // 2.8: the extra passes of the Knus features (feature.knus.client.GuhRenderHooks), last
        addRenderLayer(new nl.juiced.guhs.feature.knus.client.GuhRenderHooks.HookLayer(this));
    }

    @Override
    public void render(GuhEntity guh, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.shadowRadius = BASE_SHADOW * guh.getScale() * guh.getAgeScale();
        super.render(guh, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        nl.juiced.guhs.feature.verhaal.client.VariantUiterlijk.extra(guh, poseStack, bufferSource, packedLight, partialTick);   // 3.0
    }

    @Override
    protected void applyRotations(GuhEntity guh, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(guh, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        nl.juiced.guhs.feature.guhpolder.client.PinguhRender.pose(guh, poseStack, partialTick);   // 2.9: the Pinguh's waddle and slide
    }

    @Override
    public void preRender(PoseStack poseStack, GuhEntity guh, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (!isReRender) { // re-renders (the clothes pass) set their own visibility
            applyVisibility(model, guh);
        }
        super.preRender(poseStack, guh, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    /** Which bones the main pass shows: saddle, the worn armour tier, and the variant's clothes / neck. */
    static void applyVisibility(BakedGeoModel model, GuhEntity guh) {
        model.getBone("saddle").ifPresent(bone -> bone.setHidden(!guh.isSaddled()));
        applyVariantBones(model, guh);
        GuhArmorItem.Tier worn = guh.getArmorTier();
        for (GuhArmorItem.Tier tier : GuhArmorItem.Tier.values()) {
            String name = "armor_" + tier.name().toLowerCase(java.util.Locale.ROOT);
            boolean hidden = tier != worn;
            model.getBone(name).ifPresent(bone -> bone.setHidden(hidden));
            model.getBone(name + "_body").ifPresent(bone -> bone.setHidden(hidden));
        }
    }

    /** Clothes and the brontosaurus neck: only the bones of this guh's variant, minus slots covered by worn clothes. */
    static void applyVariantBones(BakedGeoModel model, GuhEntity guh) {
        for (GeoBone bone : model.topLevelBones()) {
            showVariantBones(bone, guh);
        }
    }

    private static void showVariantBones(GeoBone bone, GuhEntity guh) {
        String name = bone.getName();
        if (GuhVariant.VARIANT_BONES.stream().anyMatch(name::startsWith)) {
            boolean covered = false;
            for (nl.juiced.guhs.entity.GuhClothes.Slot slot : nl.juiced.guhs.entity.GuhClothes.Slot.values()) {
                nl.juiced.guhs.entity.GuhClothes worn = guh.getClothes(slot);
                if (worn != null && slotBones(slot).stream().anyMatch(name::startsWith)) {
                    covered = true;
                }
            }
            bone.setHidden(covered || !guh.getVariant().shows(name));
            bone.setChildrenHidden(false);
        }
        for (GeoBone child : bone.getChildBones()) {
            showVariantBones(child, guh);
        }
    }

    /** All bones that belong to a clothing slot (whatever piece is worn). */
    static java.util.List<String> slotBones(nl.juiced.guhs.entity.GuhClothes.Slot slot) {
        return nl.juiced.guhs.entity.GuhClothes.slotBones(slot);
    }

    @Override
    public net.minecraft.client.renderer.RenderType getRenderType(GuhEntity guh, ResourceLocation texture, @javax.annotation.Nullable MultiBufferSource bufferSource,
                                                                 float partialTick) {
        return guh.getVariant() == GuhVariant.GHOST ? net.minecraft.client.renderer.RenderType.entityTranslucent(texture)
                : super.getRenderType(guh, texture, bufferSource, partialTick);
    }

    @Override
    public software.bernie.geckolib.util.Color getRenderColor(GuhEntity guh, float partialTick, int packedLight) {
        return guh.getVariant() == GuhVariant.GHOST ? software.bernie.geckolib.util.Color.ofARGB(0x88, 0xFF, 0xFF, 0xFF)
                : super.getRenderColor(guh, partialTick, packedLight);
    }

    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack, GuhEntity guh, BakedGeoModel model,
                                    boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        float age = guh.getAgeScale();
        float squish = guh.getSquish(partialTick);
        super.scaleModelForRender(widthScale * age * (1f + 0.12f * squish), heightScale * age * (1f - 0.28f * squish),
                poseStack, guh, model, isReRender, partialTick, packedLight, packedOverlay);
    }
}
