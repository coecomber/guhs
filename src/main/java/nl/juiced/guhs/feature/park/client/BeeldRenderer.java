package nl.juiced.guhs.feature.park.client;

import javax.annotation.Nullable;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.park.BeeldBlockEntity;
import nl.juiced.guhs.feature.park.ParkBlokken;
import nl.juiced.guhs.feature.park.ParkFeature;

/**
 * 1.4.2 (Het Knabbelpark): draws the guh of a statue on top of its plinth: the real guh model (geckolib/models/entity/guh)
 * in its rest pose (a statue does not move), in stone or bronze. The park's statues have their own textures
 * (textures/entity/beeld_&lt;soort&gt;.png, made from the guh's fur by tools/features/park_modellen.py: the look of each
 * character painted on); the little statue of your own guh is your guh's own fur, tinted bronze.
 */
public class BeeldRenderer extends GeoBlockRenderer<BeeldBlockEntity, BlockEntityRenderState> {
    private static final Identifier MODEL = Guhs.id("entity/guh");
    private static final DataTicket<Identifier> TEXTUUR = DataTicket.create("guhs_park_beeld_textuur", Identifier.class);
    private static final DataTicket<Float> SCHAAL = DataTicket.create("guhs_park_beeld_schaal", Float.class);
    private static final DataTicket<Float> BREED = DataTicket.create("guhs_park_beeld_breed", Float.class);
    private static final DataTicket<Float> HOOG = DataTicket.create("guhs_park_beeld_hoog", Float.class);
    private static final DataTicket<GuhVariant> VARIANT = DataTicket.create("guhs_park_beeld_variant", GuhVariant.class);
    /** Bronze: the own statue's fur times this colour. */
    private static final int BRONS = 0xFFD7A26A;

    public BeeldRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new GeoModel<>() {
            @Override
            public Identifier getModelResource(GeoRenderState state) {
                return MODEL;
            }

            @Override
            public Identifier getTextureResource(GeoRenderState state) {
                Identifier t = state.getGeckolibData(TEXTUUR);
                return t != null ? t : GuhVariant.NORMAL.texture();
            }

            @Override
            public Identifier getAnimationResource(BeeldBlockEntity animatable) {
                return MODEL;
            }
        });
    }

    /** Scale, extra width, plinth height (blocks) and texture per statue. */
    private record Vorm(float schaal, float breed, float hoog, Identifier textuur) {
    }

    private static Vorm vorm(BeeldBlockEntity be) {
        BlockState state = be.getBlockState();
        if (state.is(ParkFeature.EIGEN_BEELD.get())) {
            return new Vorm(0.42f, 1f, 5 / 16f, be.variant().texture());
        }
        ParkBlokken.Soort soort = state.hasProperty(ParkBlokken.BEELD) ? state.getValue(ParkBlokken.BEELD) : ParkBlokken.Soort.LUNDE;
        Identifier tex = Guhs.id("textures/entity/beeld_" + soort.getSerializedName() + ".png");
        return switch (soort) {
            case JOOSTVADSJE, SUUSVADSJE -> new Vorm(0.95f, 1.35f, 10 / 16f, tex);   // (a vadsje: extra round)
            case TRAPGUH -> new Vorm(0.5f, 1f, 10 / 16f, tex);
            case OPA_KAASGUH -> new Vorm(0.9f, 1.1f, 11 / 16f, tex);
            default -> new Vorm(0.85f, 1f, 10 / 16f, tex);
        };
    }

    @Override
    public void addRenderData(BeeldBlockEntity be, @Nullable Void related, BlockEntityRenderState state, float partialTick) {
        Vorm v = vorm(be);
        state.addGeckolibData(TEXTUUR, v.textuur());
        state.addGeckolibData(SCHAAL, v.schaal());
        state.addGeckolibData(BREED, v.breed());
        state.addGeckolibData(HOOG, v.hoog());
        state.addGeckolibData(VARIANT, be.getBlockState().is(ParkFeature.EIGEN_BEELD.get()) ? be.variant() : GuhVariant.NORMAL);
    }

    @Override
    public int getRenderColor(BeeldBlockEntity be, @Nullable Void related, float partialTick) {
        return be.getBlockState().is(ParkFeature.EIGEN_BEELD.get()) ? BRONS : 0xFFFFFFFF;
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<BlockEntityRenderState> info) {
        super.adjustRenderPose(info);
        info.poseStack().translate(0, info.getOrDefaultGeckolibData(HOOG, 10 / 16f), 0);
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<BlockEntityRenderState> info, float widthScale, float heightScale) {
        float s = info.getOrDefaultGeckolibData(SCHAAL, 1f), b = info.getOrDefaultGeckolibData(BREED, 1f);
        super.scaleModelForRender(info, widthScale * s * b, heightScale * s);
    }

    /** The plain guh: no saddle, no armour, no clothes; the variant's own bones only (the own statue). */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<BlockEntityRenderState> info, BoneSnapshots bones) {
        GuhVariant variant = info.getOrDefaultGeckolibData(VARIANT, GuhVariant.NORMAL);
        for (GeoBone bone : info.model().boneLookup().get().values()) {
            String name = bone.name();
            if (name.equals("saddle") || name.startsWith("armor_")) {
                bones.get(bone).skipRender(true).skipChildrenRender(true);
            } else if (GuhVariant.VARIANT_BONES.stream().anyMatch(name::startsWith)) {
                bones.get(bone).skipRender(!variant.shows(name));
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(BeeldBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(0.6, 0, 0.6).expandTowards(0, 2, 0);
    }
}
