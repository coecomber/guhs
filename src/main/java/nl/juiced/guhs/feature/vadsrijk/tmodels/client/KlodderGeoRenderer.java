package nl.juiced.guhs.feature.vadsrijk.tmodels.client;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.ProefKlodder;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.tmodels.KlodderGeo;

/**
 * T-MODELS: kern's slimes as GeckoLib models (they used the vanilla slime model). Registered in KernClient (// T-MODELS):
 * <ul>
 *   <li>{@link #reus}: the Grauwe Klodderreus: grey + glowing purple eyes, pink and happy after the plof (x3).</li>
 *   <li>{@link #klein}: proefklodders and the kloddertjes he spits: the zone-1 klodder model, size from {@code grootte()}.</li>
 * </ul>
 * After the plof: translucent, like every Vadsrijk creature.
 */
public class KlodderGeoRenderer extends GeoReplacedEntityRenderer<KlodderGeo, VadsrijkMob, LivingEntityRenderState> {
    static final DataTicket<Float> GROOTTE = DataTicket.create("guhs_tm_grootte", Float.class);

    private static final Identifier REUS_GRIJS = Guhs.id("textures/vadsrijk/tmodels/grauwe_klodderreus.png");
    private static final Identifier REUS_BLIJ = Guhs.id("textures/vadsrijk/tmodels/grauwe_klodderreus_blij.png");
    private static final Identifier REUS_GLOED = Guhs.id("textures/vadsrijk/tmodels/grauwe_klodderreus_gloed.png");
    private static final float REUS_SCHAAL = 3.0f;

    /** Per entity: was it on the ground last frame, and the tick it landed (for the "land" squash). */
    private final Map<VadsrijkMob, int[]> landing = new WeakHashMap<>();

    public static KlodderGeoRenderer reus(EntityRendererProvider.Context c) {
        return new KlodderGeoRenderer(c, KlodderGeo.REUS, "vadsrijk/tmodels/grauwe_klodderreus", REUS_GRIJS, REUS_BLIJ);
    }

    public static KlodderGeoRenderer klein(EntityRendererProvider.Context c) {
        return new KlodderGeoRenderer(c, KlodderGeo.KLEIN, "vadsrijk/wezens/klodder", null, null);
    }

    private KlodderGeoRenderer(EntityRendererProvider.Context context, KlodderGeo geo, String model, @Nullable Identifier grijs,
            @Nullable Identifier blij) {
        super(context, new DefaultedEntityGeoModel<KlodderGeo>(Guhs.id(model)) {
            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                boolean plof = Boolean.TRUE.equals(state.getGeckolibData(KlodderGeo.GEPLOFD));
                if (grijs != null) {
                    return plof ? blij : grijs;
                }
                float g = state.getOrDefaultGeckolibData(GROOTTE, 2f);
                String naam = g < 1.5f ? "kloddertje" : "klodder";
                return Guhs.id("textures/vadsrijk/wezens/" + naam + (plof ? "_blij" : "") + ".png");
            }
        }, geo);
        this.shadowRadius = geo.reus() ? 1.4f : 0.5f;
        if (geo.reus()) {
            withRenderLayer(new GloedLaag(this));
        }
    }

    @Override
    public void addRenderData(KlodderGeo geo, @Nullable VadsrijkMob e, LivingEntityRenderState state, float partialTick) {
        if (e == null) {
            return;
        }
        boolean grond = e.onGround();
        int[] l = landing.computeIfAbsent(e, k -> new int[] {1, -100});
        if (grond && l[0] == 0) {
            l[1] = e.tickCount;
        }
        l[0] = grond ? 1 : 0;
        double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
        state.addGeckolibData(KlodderGeo.GEPLOFD, e.geplofd());
        state.addGeckolibData(KlodderGeo.LUCHT, !grond && !e.isPassenger());
        state.addGeckolibData(KlodderGeo.LAND, grond && e.tickCount - l[1] < 12);
        state.addGeckolibData(KlodderGeo.ROLT, grond && dx * dx + dz * dz > 0.3 * 0.3);
        state.addGeckolibData(GROOTTE, e instanceof ProefKlodder p ? (float) p.grootte() : e instanceof Klodderreus ? 6f : 2f);
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> info, float widthScale, float heightScale) {
        float f;
        if (getAnimatable().reus()) {
            f = REUS_SCHAAL;
        } else {
            f = info.getOrDefaultGeckolibData(GROOTTE, 2f) * 0.68f;   // vanilla slime size n is 0.52 n wide; the klodder model is 0.75
        }
        super.scaleModelForRender(info, widthScale * f, heightScale * f);
    }

    @Override
    public int getRenderColor(KlodderGeo geo, VadsrijkMob e, float partialTick) {
        int k = super.getRenderColor(geo, e, partialTick);
        return e != null && e.geplofd() ? (k & 0x00FFFFFF) | (VadsrijkMob.PLOF_ALPHA << 24) : k;
    }

    @Override
    public net.minecraft.client.renderer.rendertype.RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
        if (Boolean.TRUE.equals(state.getGeckolibData(KlodderGeo.GEPLOFD)) && !state.isInvisible) {
            return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture);
        }
        return super.getRenderType(state, texture);
    }

    /** The glowing eyes of the grey giant (not after the plof). */
    static class GloedLaag extends GeoRenderLayer<KlodderGeo, VadsrijkMob, LivingEntityRenderState> {
        GloedLaag(GeoRenderer<KlodderGeo, VadsrijkMob, LivingEntityRenderState> r) {
            super(r);
        }

        @Override
        public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> info, SubmitNodeCollector collector) {
            if (info.willRender() && !info.getOrDefaultGeckolibData(KlodderGeo.GEPLOFD, false)) {
                getRenderer().submitRenderTasks(info, collector.order(1), GuhRenderTypes.eyes(REUS_GLOED));
            }
        }
    }
}
