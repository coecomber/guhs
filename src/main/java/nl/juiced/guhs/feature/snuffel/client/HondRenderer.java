package nl.juiced.guhs.feature.snuffel.client;

import javax.annotation.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.snuffel.Honden;
import nl.juiced.guhs.feature.snuffel.Hondvorm;
import nl.juiced.guhs.feature.snuffel.SnuffelHond;

/**
 * Draws every dog of Het Snuffeleiland (residents, the stand-ins of players, cutscene actors, the dogs in the screens):
 * the approved parametric dog of tools/features/snuffel_modellen.py at scale 0.8. Which geo, animation file and texture
 * it uses follows from the dog's data ({@link Honden#model}, {@link Honden#textuur}): a named resident has its own three
 * files, any other dog the breed's model with the coat's texture. A dog that is "the viewer's" ({@link
 * SnuffelHond#alsSpeler}) takes the breed and coat of this client's own player.
 * <p>
 * The head follows where the dog looks, on top of its animation (not while it sniffs or digs: then the nose stays down).
 */
public class HondRenderer<T extends SnuffelHond> extends GeoEntityRenderer<T, LivingEntityRenderState> {
    /** What to draw this frame: the model / animation name, the texture name, and whether the head may turn. */
    record Uiterlijk(String model, String textuur, boolean kopVrij) {
    }

    private static final DataTicket<Uiterlijk> UITERLIJK = DataTicket.create("guhs_snuffel_hond", Uiterlijk.class);

    public HondRenderer(EntityRendererProvider.Context context) {
        super(context, new Model<>());
        this.shadowRadius = 0.35f;
        withScale(Honden.SCHAAL);
    }

    /** The breed, coat and puppy flag this dog is drawn with (the viewer's own for a stand-in "Speler"). */
    static String[] ziet(SnuffelHond hond) {
        if (hond.alsSpeler()) {
            return new String[] {"", EigenStand.ras(), EigenStand.kleur()};
        }
        return new String[] {hond.bewoner(), hond.ras(), hond.kleur()};
    }

    static final class Model<T extends SnuffelHond> extends GeoModel<T> {
        @Override
        public void addAdditionalStateData(T hond, @Nullable Object related, GeoRenderState state) {
            String[] z = ziet(hond);
            boolean bezig = (hond.houding() & Hondvorm.SNUFFELT) != 0 || hond.graafTot >= hond.tickCount;
            state.addGeckolibData(UITERLIJK, new Uiterlijk(Honden.model(z[0], z[1], hond.pup()), Honden.textuur(z[0], z[1], z[2], hond.pup()), !bezig));
        }

        @Override
        public Identifier getModelResource(GeoRenderState state) {
            Uiterlijk u = state.getGeckolibData(UITERLIJK);
            return Guhs.id("entity/" + (u == null ? "snuffelhond_shiba" : u.model()));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            Uiterlijk u = state.getGeckolibData(UITERLIJK);
            return Guhs.id("textures/entity/" + (u == null ? "snuffelhond_shiba_rood" : u.textuur()) + ".png");
        }

        @Override
        public Identifier getAnimationResource(T hond) {
            String[] z = ziet(hond);
            return Guhs.id("entity/" + Honden.model(z[0], z[1], hond.pup()));
        }
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
        Uiterlijk u = info.getGeckolibData(UITERLIJK);
        if (u == null || !u.kopVrij()) {
            return;
        }
        // (the signs of DefaultAnimations.hardcodedHeadRotation, but ADDED to what the animation does, and within a dog's reach)
        float pitch = Mth.clamp(info.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, 0f), -30f, 30f);
        float yaw = Mth.clamp(info.getOrDefaultGeckolibData(DataTickets.ENTITY_YAW, 0f), -50f, 50f);
        bones.ifPresent("kop", b -> {
            b.setRotX(b.getRotX() - pitch * Mth.DEG_TO_RAD);
            b.setRotY(b.getRotY() - yaw * Mth.DEG_TO_RAD);
        });
    }
}
