package nl.juiced.guhs.feature.doolhof.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.doolhof.DoolhofFeature;
import nl.juiced.guhs.feature.doolhof.DoolhofMikaEntity;
import nl.juiced.guhs.feature.doolhof.DoolhofPayloads;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.GeoEntityRenderer;

/**
 * Client side of Het Guhdoolhof: the Heg-Mika (the Mika's model with twigs and leaves, own texture), Meneer
 * Vadskronkel's own model (a gardener's hat with a leaf sprig and a curly moustache that wiggles when he talks) and his
 * screen ({@link DoolhofScherm}).
 */
public final class DoolhofClient {
    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(DoolhofFeature.MIKA.get(),
                context -> new GeoEntityRenderer<DoolhofMikaEntity, LivingEntityRenderState>(context,
                        new DefaultedEntityGeoModel<DoolhofMikaEntity>(Guhs.id("doolhof_mika")).withAltAnimations(Guhs.id("guh"))) {
                    {
                        this.shadowRadius = 0.35f;
                    }

                    /** The "head" bone follows where the Mika looks (GeckoLib 4: DefaultedEntityGeoModel(id, true)). */
                    @Override
                    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
                        DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
                    }
                }));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.DOOLHOFGUH, Guhs.id("entity/guh_npc_doolhofguh"));
        SittingGuhRenderers.NPC_ANIMATORS.put(GuhNpcEntity.Kind.DOOLHOFGUH, (npc, tick) -> {
            float t = (float) tick * 0.09f;
            return bones -> {
                bones.ifPresent("vadskronkel_snor", b -> b.setRotZ((float) Math.sin(t) * 0.05f));
                bones.ifPresent("vadskronkel_takje", b -> b.setRotX((float) Math.sin(t * 0.7f) * 0.08f));
            };
        });
    }

    /** guhs:doolhof_open: Meneer Vadskronkel's screen. */
    public static void open(DoolhofPayloads.Open payload) {
        Minecraft.getInstance().setScreen(new DoolhofScherm(payload.npcId(), payload.data()));
    }

    private DoolhofClient() {
    }
}
