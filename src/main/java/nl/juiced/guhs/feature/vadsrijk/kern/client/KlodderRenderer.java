package nl.juiced.guhs.feature.vadsrijk.kern.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.ProefKlodder;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * Placeholder look of kern's test klodders and the Grauwe Klodderreus: the guh slime model, grey ("ontvadst") until the plof,
 * then its own pink guh slime texture. Squish from the vertical speed.
 */
public class KlodderRenderer extends MobRenderer<VadsrijkMob, KlodderRenderer.Staat, SlimeModel> {
    public static final Identifier GRIJS = Guhs.id("textures/vadsrijk/kern/grauwe_klodder.png"), ROZE = Guhs.id("textures/entity/guh_slime.png");

    public static class Staat extends SlimeRenderState {
        public boolean geplofd;
    }

    public KlodderRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel(context.bakeLayer(ModelLayers.SLIME)), 0.25f);
    }

    @Override
    public Staat createRenderState() {
        return new Staat();
    }

    @Override
    public void extractRenderState(VadsrijkMob e, Staat s, float pt) {
        super.extractRenderState(e, s, pt);
        s.size = e instanceof Klodderreus k ? k.grootte() : e instanceof ProefKlodder p ? p.grootte() : 2;
        s.squish = (float) Mth.clamp(-e.getDeltaMovement().y * 1.5, -0.6, 0.6);
        s.geplofd = e.geplofd();
    }

    @Override
    protected float getShadowRadius(Staat s) {
        return s.size * 0.25f;
    }

    @Override
    protected void scale(Staat s, PoseStack pose) {
        pose.scale(0.999f, 0.999f, 0.999f);
        pose.translate(0.0f, 0.001f, 0.0f);
        float ss = s.squish / (s.size * 0.5f + 1.0f);
        float w = 1.0f / (ss + 1.0f);
        pose.scale(w * s.size, 1.0f / w * s.size, w * s.size);
    }

    /** (balans 2026-11) After the plof: translucent (and no hitbox, see VadsrijkMob#isPickable). */
    @Override
    protected int getModelTint(Staat s) {
        return s.geplofd ? (VadsrijkMob.PLOF_ALPHA << 24) | 0xFFFFFF : super.getModelTint(s);
    }

    @Override
    protected net.minecraft.client.renderer.rendertype.RenderType getRenderType(Staat s, boolean zichtbaar, boolean doorzichtig, boolean gloeit) {
        return super.getRenderType(s, zichtbaar, doorzichtig || (s.geplofd && zichtbaar), gloeit);
    }

    @Override
    public Identifier getTextureLocation(Staat s) {
        return s.geplofd ? ROZE : GRIJS;
    }
}
