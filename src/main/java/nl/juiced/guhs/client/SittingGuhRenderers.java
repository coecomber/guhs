package nl.juiced.guhs.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.entity.QuestGuhEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renderers for the sitting guh model (geo/entity/guh_sitting.geo.json): the quest guh and the Bank Guh block. */
public final class SittingGuhRenderers {
    private static final ResourceLocation MODEL = Guhs.id("geo/entity/guh_sitting.geo.json");
    private static final ResourceLocation TEXTURE = Guhs.id("textures/entity/guh_sitting.png");
    private static final ResourceLocation ANIMATIONS = Guhs.id("animations/entity/guh_sitting.animation.json");
    private static final ResourceLocation ZEEMEER_MODEL = Guhs.id("geo/entity/guh_npc_zeemeerguh.geo.json");

    /**
     * 2.8: a guh character (NPC kind) can have its own model and animation file (e.g. a hat, a whistle, a fish tail),
     * and its own code that moves bones every frame. Fill these from your feature's client init; every other kind is
     * the plain sitting guh. The Guhdex page draws the character through this same renderer.
     */
    public static final java.util.Map<nl.juiced.guhs.entity.GuhNpcEntity.Kind, ResourceLocation> NPC_MODELEN = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<nl.juiced.guhs.entity.GuhNpcEntity.Kind, ResourceLocation> NPC_ANIMATIES = new java.util.concurrent.ConcurrentHashMap<>();
    public static final java.util.Map<nl.juiced.guhs.entity.GuhNpcEntity.Kind, NpcAnimator> NPC_ANIMATORS = new java.util.concurrent.ConcurrentHashMap<>();

    /** Moves bones of an NPC every frame (after its animation): bot(name) gives the bone, if the model has it. */
    @FunctionalInterface
    public interface NpcAnimator {
        void animeer(nl.juiced.guhs.entity.GuhNpcEntity npc, software.bernie.geckolib.animation.AnimationState<nl.juiced.guhs.entity.GuhNpcEntity> state,
                     java.util.function.Function<String, java.util.Optional<software.bernie.geckolib.cache.object.GeoBone>> bot);
    }

    static {
        // the Zeemeerguh sits on a fish tail instead of its paws (its own model, see tools/features/onderwater.py), with a lazy swish of its fin
        NPC_MODELEN.put(nl.juiced.guhs.entity.GuhNpcEntity.Kind.ZEEMEERGUH, ZEEMEER_MODEL);
        NPC_ANIMATORS.put(nl.juiced.guhs.entity.GuhNpcEntity.Kind.ZEEMEERGUH, (npc, state, bot) -> {
            float t = (float) state.getAnimationTick() * 0.08f;
            bot.apply("zeemeer_staartvin").ifPresent(fin -> fin.setRotY((float) Math.sin(t) * 0.25f));
        });
    }

    /** The Hungry Guh NPC (about 2 blocks tall, looks at you). */
    public static class QuestGuhRenderer extends GeoEntityRenderer<QuestGuhEntity> {
        public QuestGuhRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id("guh_sitting"), true));
            this.shadowRadius = 0.6f;
        }
    }

    /** The Bank Guh block: the same sitting guh, extra chubby: ~2 blocks wide (ears included) and ~2 blocks tall. */
    public static class BankGuhRenderer extends GeoBlockRenderer<BankGuhBlockEntity> {
        public BankGuhRenderer(BlockEntityRendererProvider.Context context) {
            super(new GeoModel<>() {
                @Override
                public ResourceLocation getModelResource(BankGuhBlockEntity animatable) {
                    return MODEL;
                }

                @Override
                public ResourceLocation getTextureResource(BankGuhBlockEntity animatable) {
                    return TEXTURE;
                }

                @Override
                public ResourceLocation getAnimationResource(BankGuhBlockEntity animatable) {
                    return ANIMATIONS;
                }
            });
            withScale(1.35f, 1.05f);
        }

        /** It is bigger than its block, so don't hide it as soon as the block itself is off-screen. */
        @Override
        public AABB getRenderBoundingBox(BankGuhBlockEntity blockEntity) {
            return new AABB(blockEntity.getBlockPos()).inflate(1.5, 0, 1.5).expandTowards(0, 1.5, 0);
        }
    }

    /** The quest characters: the sitting guh in their own colours (Moeder Vadsig is 2.6x as big, set by her SCALE). */
    public static class NpcRenderer extends GeoEntityRenderer<nl.juiced.guhs.entity.GuhNpcEntity> {
        public NpcRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<nl.juiced.guhs.entity.GuhNpcEntity>(Guhs.id("guh_sitting"), true) {
                @Override
                public ResourceLocation getTextureResource(nl.juiced.guhs.entity.GuhNpcEntity npc) {
                    return Guhs.id("textures/entity/npc_" + npc.getKind().id() + ".png");
                }

                /** Its own model (NPC_MODELEN), or the plain sitting guh. */
                @Override
                public ResourceLocation getModelResource(nl.juiced.guhs.entity.GuhNpcEntity npc) {
                    return NPC_MODELEN.getOrDefault(npc.getKind(), super.getModelResource(npc));
                }

                /** Its own animations (NPC_ANIMATIES), or the sitting guh's. */
                @Override
                public ResourceLocation getAnimationResource(nl.juiced.guhs.entity.GuhNpcEntity npc) {
                    return NPC_ANIMATIES.getOrDefault(npc.getKind(), super.getAnimationResource(npc));
                }

                @Override
                public void setCustomAnimations(nl.juiced.guhs.entity.GuhNpcEntity npc, long instanceId,
                                                software.bernie.geckolib.animation.AnimationState<nl.juiced.guhs.entity.GuhNpcEntity> state) {
                    super.setCustomAnimations(npc, instanceId, state);
                    NpcAnimator animator = NPC_ANIMATORS.get(npc.getKind());
                    if (animator != null) {
                        animator.animeer(npc, state, this::getBone);
                    }
                }
            });
            this.shadowRadius = 0.6f;
        }
    }

    /** The Mika-baas: Mika, but bigger (SCALE 1.6). */
    public static class MikaBaasRenderer extends GeoEntityRenderer<nl.juiced.guhs.entity.MikaBaasEntity> {
        public MikaBaasRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<nl.juiced.guhs.entity.MikaBaasEntity>(Guhs.id("mika"), true).withAltAnimations(Guhs.id("guh")));
            this.shadowRadius = 0.7f;
        }
    }

    private SittingGuhRenderers() {
    }
}
