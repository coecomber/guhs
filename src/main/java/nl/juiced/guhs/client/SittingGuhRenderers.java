package nl.juiced.guhs.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.MikaBaasEntity;
import nl.juiced.guhs.entity.QuestGuhEntity;

/** Renderers for the sitting guh model (geckolib/models/entity/guh_sitting.geo.json): the quest guh and the Bank Guh block. */
public final class SittingGuhRenderers {
    // GeckoLib 5: bare ids, resolved to assets/guhs/geckolib/models/<id>.geo.json and geckolib/animations/<id>.animation.json
    private static final Identifier MODEL = Guhs.id("entity/guh_sitting");
    private static final Identifier TEXTURE = Guhs.id("textures/entity/guh_sitting.png");
    private static final Identifier ANIMATIONS = Guhs.id("entity/guh_sitting");
    private static final Identifier ZEEMEER_MODEL = Guhs.id("entity/guh_npc_zeemeerguh");

    /**
     * 2.8: a guh character (NPC kind) can have its own model and animation file (e.g. a hat, a whistle, a fish tail),
     * and its own code that moves bones every frame. Fill these from your feature's client init; every other kind is
     * the plain sitting guh. The Guhdex page draws the character through this same renderer.
     * (1.1.0: model/animation ids are GeckoLib 5 bare ids, e.g. {@code Guhs.id("entity/guh_npc_zeemeerguh")}.)
     */
    public static final Map<GuhNpcEntity.Kind, Identifier> NPC_MODELEN = new ConcurrentHashMap<>();
    public static final Map<GuhNpcEntity.Kind, Identifier> NPC_ANIMATIES = new ConcurrentHashMap<>();
    public static final Map<GuhNpcEntity.Kind, NpcAnimator> NPC_ANIMATORS = new ConcurrentHashMap<>();

    /** The kind of the NPC being drawn (render state ticket; the model picks its geo file from it). */
    public static final DataTicket<GuhNpcEntity.Kind> NPC_KIND = DataTicket.create("guhs_npc_kind", GuhNpcEntity.Kind.class);
    /** This frame's bone moves of the NPC's {@link NpcAnimator}. */
    public static final DataTicket<GuhRenderFrame.BoneMove> NPC_BONES = DataTicket.create("guhs_npc_bones", GuhRenderFrame.BoneMove.class);

    /**
     * Moves bones of an NPC every frame (after its animation). 1.1.0 (GeckoLib 5): called at extract time with the NPC
     * and its animation age in ticks (was {@code state.getAnimationTick()}); return the moves for render time, capturing
     * only values: {@code (npc, tick) -> { float t = (float) tick * 0.07f; return bones -> bones.ifPresent("x", b -> b.setRotZ(..)); }}.
     * setPosX/Y/Z -> setTranslateX/Y/Z, setHidden(h) -> skipRender(h).skipChildrenRender(h); rotations are relative to the
     * model's own rotation (like before for bones without a base rotation).
     */
    @FunctionalInterface
    public interface NpcAnimator {
        @Nullable
        GuhRenderFrame.BoneMove animeer(GuhNpcEntity npc, double animationTick);
    }

    static {
        // the Zeemeerguh sits on a fish tail instead of its paws (its own model, see tools/features/onderwater.py), with a lazy swish of its fin
        NPC_MODELEN.put(GuhNpcEntity.Kind.ZEEMEERGUH, ZEEMEER_MODEL);
        NPC_ANIMATORS.put(GuhNpcEntity.Kind.ZEEMEERGUH, (npc, tick) -> {
            float t = (float) tick * 0.08f;
            return bones -> bones.ifPresent("zeemeer_staartvin", fin -> fin.setRotY((float) Math.sin(t) * 0.25f));
        });
    }

    /** The Hungry Guh NPC (about 2 blocks tall, looks at you). */
    public static class QuestGuhRenderer extends GeoEntityRenderer<QuestGuhEntity, LivingEntityRenderState> {
        public QuestGuhRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<>(Guhs.id("guh_sitting")));
            this.shadowRadius = 0.6f;
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }
    }

    /** The Bank Guh block: the same sitting guh, extra chubby: ~2 blocks wide (ears included) and ~2 blocks tall. */
    public static class BankGuhRenderer extends GeoBlockRenderer<BankGuhBlockEntity, BlockEntityRenderState> {
        public BankGuhRenderer(BlockEntityRendererProvider.Context context) {
            super(context, new GeoModel<>() {
                @Override
                public Identifier getModelResource(GeoRenderState state) {
                    return MODEL;
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    return TEXTURE;
                }

                @Override
                public Identifier getAnimationResource(BankGuhBlockEntity animatable) {
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
    public static class NpcRenderer extends GeoEntityRenderer<GuhNpcEntity, LivingEntityRenderState> {
        public NpcRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<GuhNpcEntity>(Guhs.id("guh_sitting")) {
                @Override
                public void addAdditionalStateData(GuhNpcEntity npc, @Nullable Object related, GeoRenderState state) {
                    state.addGeckolibData(NPC_KIND, npc.getKind());
                }

                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    GuhNpcEntity.Kind kind = state.getGeckolibData(NPC_KIND);
                    return kind == null ? super.getTextureResource(state) : Guhs.id("textures/entity/npc_" + kind.id() + ".png");
                }

                /** Its own model (NPC_MODELEN), or the plain sitting guh. */
                @Override
                public Identifier getModelResource(GeoRenderState state) {
                    GuhNpcEntity.Kind kind = state.getGeckolibData(NPC_KIND);
                    return kind == null ? super.getModelResource(state) : NPC_MODELEN.getOrDefault(kind, super.getModelResource(state));
                }

                /** Its own animations (NPC_ANIMATIES), or the sitting guh's. */
                @Override
                public Identifier getAnimationResource(GuhNpcEntity npc) {
                    return NPC_ANIMATIES.getOrDefault(npc.getKind(), super.getAnimationResource(npc));
                }
            });
            this.shadowRadius = 0.6f;
        }

        @Override
        public void addRenderData(GuhNpcEntity npc, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            NpcAnimator animator = NPC_ANIMATORS.get(npc.getKind());
            GuhRenderFrame.BoneMove move = animator == null ? null : animator.animeer(npc, npc.tickCount + partialTick);
            if (move != null) {
                state.addGeckolibData(NPC_BONES, move);
            }
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            GuhRenderFrame.BoneMove move = info.getGeckolibData(NPC_BONES);
            if (move != null) {
                move.apply(bones);
            }
        }
    }

    /** The Mika-baas: Mika, but bigger (SCALE 1.6). */
    public static class MikaBaasRenderer extends GeoEntityRenderer<MikaBaasEntity, LivingEntityRenderState> {
        public MikaBaasRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<MikaBaasEntity>(Guhs.id("mika")).withAltAnimations(Guhs.id("guh")));
            this.shadowRadius = 0.7f;
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }
    }

    private SittingGuhRenderers() {
    }
}
