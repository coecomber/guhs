package nl.juiced.guhs.feature.torenpeper.client;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ARGB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.SittingGuhRenderers;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.torenpeper.Kweek;
import nl.juiced.guhs.feature.torenpeper.PeperSoort;
import nl.juiced.guhs.feature.torenpeper.TorenpeperFeature;
import nl.juiced.guhs.feature.torenpeper.VerdwaaldeRookguhEntity;

/**
 * Client side of bbq2 (toren-peper): the lost Rookguh (the Rookguh's own model, texture and animations, drawn less than half
 * as big), the kweekbak drawn with the local player's own plant ({@link KweekRenderer}; what the server told us in
 * Kweek.Stand), and the models of the Torenwachter-guh (rain hat, beard, pea coat) and the Peperteler-guh (straw hat with
 * a pepper, green apron). Models and textures: tools/features/toren_peper_modellen.py.
 */
public final class TorenpeperClient {
    /** When the local player planted in the green, red and pink kweekbak (Kweek.stempel; 0 = empty). */
    private static volatile long[] geplant = new long[PeperSoort.values().length];

    private TorenpeperClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(TorenpeperFeature.VERDWAALDE_ROOKGUH.get(), VerdwaaldRenderer::new);
            event.registerBlockEntityRenderer(TorenpeperFeature.KWEEKBAK_BE.get(), KweekRenderer::new);
        });
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.TORENWACHTERGUH, Guhs.id("entity/guh_npc_torenwachterguh"));
        SittingGuhRenderers.NPC_MODELEN.put(GuhNpcEntity.Kind.PEPERTELERGUH, Guhs.id("entity/guh_npc_pepertelerguh"));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> geplant = new long[PeperSoort.values().length]);
    }

    /** New state from the server (Kweek.Stand). */
    public static void zetKweek(CompoundTag data) {
        long[] a = data.getLongArray("Geplant").orElse(null);
        geplant = a != null && a.length == PeperSoort.values().length ? a.clone() : new long[PeperSoort.values().length];
    }

    /** What the local player's plant of this kind looks like now: 0 an empty trough, 1..4 its stages. */
    public static int groei(PeperSoort soort) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : Kweek.groei(geplant[soort.ordinal()], mc.level.getGameTime());
    }

    /**
     * The lost Rookguh: the Rookguh's model, {@link #SCHAAL} blocks big instead of 3.375, with the same swelling and blushing
     * as it eats at the lamp.
     */
    static class VerdwaaldRenderer extends GeoEntityRenderer<VerdwaaldeRookguhEntity, LivingEntityRenderState> {
        static final float SCHAAL = 1.5f;
        private static final DataTicket<Float> ROND = DataTicket.create("guhs_torenpeper_rookguh_rond", Float.class);

        VerdwaaldRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<VerdwaaldeRookguhEntity>(Guhs.id("rookguh")));
            this.shadowRadius = 0.7f;
        }

        @Override
        public void addRenderData(VerdwaaldeRookguhEntity guh, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(ROND, guh.plumpness());
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            float p = info.getOrDefaultGeckolibData(ROND, 0f);
            bones.ifPresent("rook", b -> {
                b.setScaleX(0.88f + 0.2f * p);
                b.setScaleZ(0.88f + 0.2f * p);
                b.setScaleY(0.94f + 0.1f * p);
            });
            if (p < 0.3f) {
                bones.ifPresent("cheeks", b -> b.skipRender(true).skipChildrenRender(true));
            }
        }

        @Override
        public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
            super.scaleModelForRender(info, widthScale * SCHAAL, heightScale * SCHAAL);
        }

        @Override
        public int getRenderColor(VerdwaaldeRookguhEntity guh, @Nullable Void related, float partialTick) {
            float p = guh.plumpness();
            return ARGB.colorFromFloat(1.0f, 0.9f + 0.1f * p, 0.9f - 0.05f * p, 0.92f);
        }
    }
}
