package nl.juiced.guhs.feature.guhvillagers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import nl.juiced.guhs.Guhs;

/**
 * 1.7.0: how a guh villager looks (render only; its data and trades are untouched, so every existing guh villager,
 * also on the server, shows it at once). Grown-ups: model A, the Guhkop-villager; children: model C, the
 * Knuffel-villager, drawn at {@link #KIND_SCHAAL}. The profession outfit is the profession texture on the vanilla villager
 * body (model A has the same body, robe, arms and legs; the vanilla head with its hat is left out), plus the level badge
 * and, for the four 1.7.0 professions, a hat on the guh head ({@link GuhVillagerHoedjesModel}).
 * <p>
 * Used by {@link GuhsVillagerRenderer}, the villager renderer, for villagers of the type guhs:guh.
 */
public class GuhVillagerRenderer extends MobRenderer<Villager, VillagerRenderState, VillagerModel> {
    public static final Identifier TEXTURE = Guhs.id("textures/entity/villager/guh_villager.png");
    public static final Identifier TEXTURE_KIND = Guhs.id("textures/entity/villager/guh_villager_kind.png");
    public static final Identifier TEXTURE_HOEDJES = Guhs.id("textures/entity/villager/guh_villager_hoedjes.png");
    public static final float KIND_SCHAAL = 0.68f;
    private static final String[] NIVEAUS = {"stone", "iron", "gold", "emerald", "diamond"};

    private final VillagerModel volwassen;
    private final VillagerModel kind;

    public GuhVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel(context.bakeLayer(GuhVillagerModellen.VOLWASSEN)), 0.5f);
        this.volwassen = this.model;
        this.kind = new VillagerModel(context.bakeLayer(GuhVillagerModellen.KIND));
        this.addLayer(new Beroep(this, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER_NO_HAT)),
                new GuhVillagerHoedjesModel(context.bakeLayer(GuhVillagerModellen.HOEDJES))));
        this.addLayer(new CrossedArmsItemLayer<>(this));
    }

    @Override
    public void submit(VillagerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        this.model = state.isBaby ? this.kind : this.volwassen;
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    protected void scale(VillagerRenderState state, PoseStack poseStack) {
        if (state.isBaby) {
            poseStack.scale(KIND_SCHAAL, KIND_SCHAAL, KIND_SCHAAL);
        }
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        return state.isBaby ? TEXTURE_KIND : TEXTURE;
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    /** The profession outfit (and level badge) on the vanilla body, and the 1.7.0 profession hats on the guh head. */
    private static class Beroep extends RenderLayer<VillagerRenderState, VillagerModel> {
        private final VillagerModel lijf;
        private final GuhVillagerHoedjesModel hoedjes;

        Beroep(GuhVillagerRenderer parent, VillagerModel lijf, GuhVillagerHoedjesModel hoedjes) {
            super(parent);
            this.lijf = lijf;
            this.hoedjes = hoedjes;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, VillagerRenderState state, float yRot, float xRot) {
            VillagerData data = state.villagerData;
            if (state.isInvisible || state.isBaby || data == null) {
                return;
            }
            Holder<VillagerProfession> beroep = data.profession();
            if (beroep.is(VillagerProfession.NONE)) {
                return;
            }
            Identifier key = beroep.unwrapKey().map(k -> k.identifier()).orElse(null);
            if (key == null) {
                return;
            }
            renderColoredCutoutModel(this.lijf, key.withPath(p -> "textures/entity/villager/profession/" + p + ".png"), poseStack, collector, light, state, -1, 2);
            if (!beroep.is(VillagerProfession.NITWIT)) {
                Identifier niveau = Identifier.withDefaultNamespace("textures/entity/villager/profession_level/"
                        + NIVEAUS[Math.max(1, Math.min(5, data.level())) - 1] + ".png");
                renderColoredCutoutModel(this.lijf, niveau, poseStack, collector, light, state, -1, 3);
            }
            if (GuhVillagerHoedjesModel.heeftHoedje(beroep)) {
                renderColoredCutoutModel(this.hoedjes, TEXTURE_HOEDJES, poseStack, collector, light, state, -1, 4);
            }
        }
    }
}
