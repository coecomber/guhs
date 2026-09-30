package nl.juiced.guhs.feature.guheinde.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.guheinde.HongerigeEnderguhEntity;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import org.jspecify.annotations.Nullable;

/**
 * The starved Enderguh: the guh model with only its ender bones (wings, horns, spikes), textured
 * textures/entity/guh_hongerig_&lt;vahoeg&gt;.png: grey at 0, back to all its Vahoege Enderguh colours at the top.
 * (1.1.0: the vahoeg level travels in a render state ticket; the bones are hidden on the frame's bone snapshots.)
 */
public class HongerigeEnderguhRenderer extends GeoEntityRenderer<HongerigeEnderguhEntity, LivingEntityRenderState> {
    private static final Identifier[] TEXTURES = new Identifier[HongerigeEnderguhEntity.MAX_VAHOEG + 1];
    static final DataTicket<Integer> VAHOEG = DataTicket.create("guhs_hongerig_vahoeg", Integer.class);

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = Guhs.id("textures/entity/guh_hongerig_" + i + ".png");
        }
    }

    public HongerigeEnderguhRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<HongerigeEnderguhEntity>(Guhs.id("guh")) {
            @Override
            public void addAdditionalStateData(HongerigeEnderguhEntity guh, @Nullable Object related, GeoRenderState state) {
                state.addGeckolibData(VAHOEG, guh.getVahoeg());
            }

            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                int vahoeg = state.getOrDefaultGeckolibData(VAHOEG, 0);
                return TEXTURES[Math.max(0, Math.min(TEXTURES.length - 1, vahoeg))];
            }
        });
        this.shadowRadius = 0.45f * HongerigeEnderguhEntity.SCALE;
    }

    /** Only the ender bones of the variant bones; no saddle, no armour. The head follows its look (was turnsHead). */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
        DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
        for (GeoBone bone : info.model().boneLookup().get().values()) {
            String name = bone.name();
            if (name.equals("saddle") || name.startsWith("armor_")) {
                bones.get(bone).skipRender(true).skipChildrenRender(true);
            } else if (GuhVariant.VARIANT_BONES.stream().anyMatch(name::startsWith)) {
                bones.get(bone).skipRender(!GuhVariant.ENDER.shows(name));
            }
        }
    }
}
