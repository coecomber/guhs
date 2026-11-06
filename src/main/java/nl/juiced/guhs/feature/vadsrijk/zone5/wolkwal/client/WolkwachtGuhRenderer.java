package nl.juiced.guhs.feature.vadsrijk.zone5.wolkwal.client;

import javax.annotation.Nullable;

import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.client.GuhRenderFrame;
import nl.juiced.guhs.client.GuhRenderer;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.vadsrijk.zone5.wolkwal.Wolkwacht;

/**
 * T-WOLKWAL: the Vadsrijk NPC guh renderer ({@link GuhRenderer}: guh model, clothes, hooks, markers) that draws the Wolkwacht with
 * the Wolkwacht-ridder model. A Wolkwacht NPC (t-z5verhaal's {@code z5_wacht_*}: a knight's helmet + the cloud collar,
 * {@link Wolkwacht#isWolkwacht}) gets the knight model + the knight texture in its own fur; its clothes are not drawn (the armour is
 * in the model). The knight model has the guh's core bones, so the guh animations, the head look and every hook keep working.
 * Every other NPC is drawn exactly as before.
 */
public class WolkwachtGuhRenderer extends GuhRenderer {
    /** "" = a normal guh, else the knight's fur. */
    static final DataTicket<String> RIDDER = DataTicket.create("guhs_vr_z5_wolkwacht", String.class);
    private static final Identifier MODEL = Guhs.id("entity/" + Wolkwacht.MODEL);
    @Nullable
    private final GeoModel<GuhEntity> omhulsel;

    public WolkwachtGuhRenderer(EntityRendererProvider.Context context) {
        super(context);
        GeoModel<GuhEntity> org = super.getGeoModel();
        this.omhulsel = new GeoModel<>() {
            @Override
            public Identifier getModelResource(GeoRenderState state) {
                String r = state.getOrDefaultGeckolibData(RIDDER, "");
                return r.isEmpty() ? org.getModelResource(state) : MODEL;
            }

            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                String r = state.getOrDefaultGeckolibData(RIDDER, "");
                return r.isEmpty() ? org.getTextureResource(state) : Guhs.id(Wolkwacht.textuur(r));
            }

            @Override
            public Identifier getAnimationResource(GuhEntity guh) {
                return org.getAnimationResource(guh);
            }

            @Override
            public Identifier[] getAnimationResourceFallbacks(GuhEntity guh) {
                return org.getAnimationResourceFallbacks(guh);
            }

            @Override
            public void addAdditionalStateData(GuhEntity guh, Object related, GeoRenderState state) {
                org.addAdditionalStateData(guh, related, state);
            }

            @Override
            public BakedGeoModel getBakedModel(Identifier id) {
                return org.getBakedModel(id);
            }

            @Override
            public Animation getBakedAnimation(GuhEntity guh, String name) {
                return org.getBakedAnimation(guh, name);
            }
        };
    }

    /** (During GuhRenderer's own constructor the wrapper does not exist yet: then the plain guh model.) */
    @Override
    public GeoModel<GuhEntity> getGeoModel() {
        return omhulsel == null ? super.getGeoModel() : omhulsel;
    }

    @Override
    public void addRenderData(GuhEntity guh, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
        super.addRenderData(guh, related, state, partialTick);
        GuhRenderFrame frame = GuhRenderer.frame(state);
        String r = "";
        if (guh instanceof RijkGuh && frame != null
                && Wolkwacht.isWolkwacht(frame.worn(GuhClothes.Slot.HEAD), frame.worn(GuhClothes.Slot.NECK))) {
            r = Wolkwacht.vacht(frame.variant);
            frame.clothes.clear();   // the armour is in the model
        }
        state.addGeckolibData(RIDDER, r);
    }
}
