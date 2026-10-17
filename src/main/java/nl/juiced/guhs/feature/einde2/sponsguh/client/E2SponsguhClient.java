package nl.juiced.guhs.feature.einde2.sponsguh.client;

import java.util.List;

import javax.annotation.Nullable;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.E2SponsguhFeature;
import nl.juiced.guhs.feature.einde2.sponsguh.KnabbelburgermobielEntity;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;

/**
 * einde2, slice sponsguh_wezens, client side: the GeckoLib renderers of the cast of the Sponsguh story and of the
 * Knabbelburgermobiel (models, animations and textures from tools/features/einde2_sponsguh_wezens_modellen.py).
 * One renderer class for the whole cast ({@link WezenRenderer}): the table {@link Cast} says which model a character wears
 * (Octoliam wears Octo's with his own texture), whether it has looks (a texture per Soort), and whether its head follows
 * its eyes; the characters with glass, ice or a ghost body are drawn see-through, the monsters' eyes glow in the dark.
 */
public final class E2SponsguhClient {
    /** The character's look (render state ticket; the model picks the texture from it). */
    static final DataTicket<Integer> SOORT = DataTicket.create("guhs_e2_sponsguh_soort", Integer.class);
    /** sp_cast: the story looks that are on (the mask of SponsguhWezen.looks(), bits in the order of Cast.LOOKS). */
    static final DataTicket<Integer> LOOKS = DataTicket.create("guhs_e2_sponsguh_looks", Integer.class);
    /** sp_cast: the colour a dried-out character is drawn in (grey-brown) and how big it is then. */
    static final int DROOG_KLEUR = 0xFFB4A088;
    static final float DROOG_MAAT = 0.85f;

    public static void init(IEventBus modBus) {
        modBus.addListener(E2SponsguhClient::renderers);
    }

    @SuppressWarnings("unchecked")
    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        for (Cast cast : Cast.values()) {
            event.registerEntityRenderer((EntityType<SponsguhWezen>) E2SponsguhFeature.type(cast), context -> new WezenRenderer(context, cast));
        }
        event.registerEntityRenderer(E2SponsguhFeature.KNABBELBURGERMOBIEL.get(), MobielRenderer::new);
    }

    /** One of the cast: its own model, its texture (per look), see-through where it has glass, and glowing eyes for the monsters. */
    public static class WezenRenderer extends GeoEntityRenderer<SponsguhWezen, LivingEntityRenderState> {
        private final Cast cast;

        public WezenRenderer(EntityRendererProvider.Context context, Cast cast) {
            super(context, new DefaultedEntityGeoModel<SponsguhWezen>(Guhs.id(cast.model())) {
                @Override
                public Identifier getTextureResource(GeoRenderState state) {
                    if (cast.soorten > 0) {
                        return Guhs.id("textures/entity/" + cast.id() + "_" + Math.floorMod(state.getOrDefaultGeckolibData(SOORT, 0), cast.soorten) + ".png");
                    }
                    return cast == Cast.OCTOLIAM ? Guhs.id("textures/entity/octoliam.png") : super.getTextureResource(state);
                }
            });
            this.cast = cast;
            this.shadowRadius = cast == Cast.SPOOKKAPITEIN ? 0f : Math.min(1.6f, cast.breed * 0.5f);
            if (cast == Cast.LOOPGRAAFMONSTER) {
                withRenderLayer(new AutoGlowingGeoLayer<>(this));              // (<texture>_glowmask.png: the eyes, the lure)
            }
        }

        @Override
        public void addRenderData(SponsguhWezen wezen, @Nullable Void related, LivingEntityRenderState state, float partialTick) {
            state.addGeckolibData(SOORT, wezen.soort());
            state.addGeckolibData(LOOKS, wezen.looks());
        }

        /** sp_cast: a dried-out character (look droog) is grey-brown. */
        @Override
        public int getRenderColor(SponsguhWezen wezen, @Nullable Void related, float partialTick) {
            int kleur = super.getRenderColor(wezen, related, partialTick);
            return wezen.heeftLook("droog") ? DROOG_KLEUR & kleur : kleur;
        }

        /** David Hasselguh is far longer than his hitbox, Neptuguh's trident far taller. */
        @Override
        protected AABB getBoundingBoxForCulling(SponsguhWezen wezen) {
            return wezen.getBoundingBox().inflate(cast == Cast.DAVID_HASSELGUH ? 5.0 : 1.0);
        }

        /** Ice, a glass helmet and a ghost are see-through. */
        @Override
        public @Nullable RenderType getRenderType(LivingEntityRenderState state, Identifier texture) {
            if ((cast == Cast.MENEER_KRABGUH || cast == Cast.SANDY_GUH || cast == Cast.SPOOKKAPITEIN) && !state.isInvisible) {
                return RenderTypes.entityTranslucent(texture);
            }
            return super.getRenderType(state, texture);
        }

        @Override
        public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots bones) {
            if (cast.kijkt) {
                DefaultAnimations.hardcodedHeadRotation(info, bones, "head");
            }
            // sp_cast: the story looks: every look bone of a look that is off is hidden; some looks hide bones of the model
            List<String> looks = cast.looks();
            if (looks.isEmpty()) {
                return;
            }
            int mask = info.getGeckolibData(LOOKS) == null ? 0 : info.getGeckolibData(LOOKS);
            for (String look : looks) {
                boolean aan = (mask & (1 << Cast.LOOKS.indexOf(look))) != 0;
                if (!aan) {
                    bones.ifPresent("look_" + look, b -> b.skipRender(true).skipChildrenRender(true));
                } else if (look.equals("kroon_vast")) {
                    bones.ifPresent("arm_left", b -> b.skipRender(true).skipChildrenRender(true));
                    bones.ifPresent("arm_right", b -> b.skipRender(true).skipChildrenRender(true));
                } else if (look.equals("droog")) {
                    bones.ifPresent("root", b -> b.setScale(DROOG_MAAT, DROOG_MAAT * 0.94f, DROOG_MAAT));
                }
            }
        }
    }

    /** The Knabbelburgermobiel. */
    public static class MobielRenderer extends GeoEntityRenderer<KnabbelburgermobielEntity, EntityRenderState> {
        public MobielRenderer(EntityRendererProvider.Context context) {
            super(context, new DefaultedEntityGeoModel<KnabbelburgermobielEntity>(Guhs.id("knabbelburgermobiel")));
            this.shadowRadius = 1.1f;
        }

        @Override
        public void addRenderData(KnabbelburgermobielEntity mobiel, @Nullable Void related, EntityRenderState state, float partialTick) {
            state.addGeckolibData(DataTickets.ENTITY_BODY_YAW, Mth.rotLerp(partialTick, mobiel.yRotO, mobiel.getYRot()));
        }

        /** The roof bun stands above its hitbox. */
        @Override
        protected AABB getBoundingBoxForCulling(KnabbelburgermobielEntity mobiel) {
            return mobiel.getBoundingBox().inflate(0.6, 0, 0.6).expandTowards(0, 1.4, 0);
        }
    }

    private E2SponsguhClient() {
    }
}
