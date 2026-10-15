package nl.juiced.guhs.feature.ringh5.client;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.ringh5.OogEntity;
import nl.juiced.guhs.feature.ringh5.RingH5Feature;
import nl.juiced.guhs.feature.ringh5.RoosterwachterEntity;
import nl.juiced.guhs.feature.verhaal.Cutscenes;

/**
 * Client side of bbq2 (ring-h5): the renderers of the two creatures of chapter 5 (models, textures and animations:
 * tools/features/ring_h5_modellen.py).
 * <ul>
 *   <li>Het Oog van Sausron: modelled small, drawn {@link #OOG_SCHAAL} times as big (it fills the socket of the Mika head
 *       on the tower); its fire glows in the dark (the glowmask); the bone "oog" turns like a head towards the spot the
 *       Eye looks at (the server turns the entity's head). As a cutscene actor it looks down by itself (an actor has no
 *       pitch); the animation name {@code omhoog} lifts its look.</li>
 *   <li>De Roosterwachter: the Mika's model with his helmet, grate visor and grill fork, the guh's idle animation, a head
 *       that turns left and right on his post.</li>
 * </ul>
 */
public final class RingH5Client {
    /** The Eye is drawn this many times as big as its model. */
    public static final float OOG_SCHAAL = 4.6f;
    /** A cutscene actor looks this far down (radians), or up with the animation name omhoog. */
    private static final float ACTEUR_OMLAAG = 0.5f, ACTEUR_OMHOOG = -0.3f;

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(RingH5Feature.OOG_VAN_SAUSRON.get(), OogRenderer::new);
            event.registerEntityRenderer(RingH5Feature.ROOSTERWACHTER.get(), WachterRenderer::new);
        });
    }

    /** The Eye. */
    public static class OogRenderer extends GeoEntityRenderer<OogEntity, LivingEntityRenderState> {
        static final DataTicket<Float> POSE = DataTicket.create("guhs_ringh5_oog_pose", Float.class);

        public OogRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<OogEntity>(Guhs.id("oog_van_sausron")));
            this.shadowRadius = 0f;
            withScale(OOG_SCHAAL, OOG_SCHAAL);
            withRenderLayer(new AutoGlowingGeoLayer<>(this));
        }

        @Override
        public void addRenderData(OogEntity oog, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            // (a scene's actor has a negative id and no pitch of its own)
            float pose = oog.getId() < 0 ? (Cutscenes.animatie(oog).equals("omhoog") ? ACTEUR_OMHOOG : ACTEUR_OMLAAG) : 0f;
            state.addGeckolibData(POSE, pose);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "oog");
            float pose = info.getOrDefaultGeckolibData(POSE, 0f);
            if (pose != 0f) {
                bones.ifPresent("kijk", b -> b.setRotX(b.getRotX() - pose));
            }
        }
    }

    /** A guard of het Wachthek. */
    public static class WachterRenderer extends GeoEntityRenderer<RoosterwachterEntity, LivingEntityRenderState> {
        public WachterRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<RoosterwachterEntity>(Guhs.id("ringh5_roosterwachter")).withAltAnimations(Guhs.id("guh")));
            this.shadowRadius = 0.45f;
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        }
    }

    private RingH5Client() {
    }
}
