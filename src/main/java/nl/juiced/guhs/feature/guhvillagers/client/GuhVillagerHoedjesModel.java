package nl.juiced.guhs.feature.guhvillagers.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import nl.juiced.guhs.feature.guhvillagers.GuhVillagersFeature;

/** The profession hats of the 1.7.0 professions on the guh head of model A; the head turns like the VillagerModel head. */
public class GuhVillagerHoedjesModel extends EntityModel<VillagerRenderState> {
    private final ModelPart head;
    private final ModelPart toverhoed;
    private final ModelPart mijnhelm;
    private final ModelPart vadsbril;
    private final ModelPart kristalband;

    public GuhVillagerHoedjesModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.toverhoed = head.getChild("toverhoed");
        this.mijnhelm = head.getChild("mijnhelm");
        this.vadsbril = head.getChild("vadsbril");
        this.kristalband = head.getChild("kristalband");
    }

    /** Does a villager of this profession wear one of these hats? */
    public static boolean heeftHoedje(Holder<VillagerProfession> p) {
        return p.is(GuhVillagersFeature.TOVERGUH.getKey()) || p.is(GuhVillagersFeature.MIJNWERKER.getKey())
                || p.is(GuhVillagersFeature.VADSMONTEUR.getKey()) || p.is(GuhVillagersFeature.KRISTALSLIJPER.getKey());
    }

    @Override
    public void setupAnim(VillagerRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        if (state.isUnhappy) {
            this.head.zRot = 0.3F * Mth.sin(0.45F * state.ageInTicks);
            this.head.xRot = 0.4F;
        } else {
            this.head.zRot = 0.0F;
        }
        Holder<VillagerProfession> p = state.villagerData == null ? null : state.villagerData.profession();
        this.toverhoed.visible = p != null && p.is(GuhVillagersFeature.TOVERGUH.getKey());
        this.mijnhelm.visible = p != null && p.is(GuhVillagersFeature.MIJNWERKER.getKey());
        this.vadsbril.visible = p != null && p.is(GuhVillagersFeature.VADSMONTEUR.getKey());
        this.kristalband.visible = p != null && p.is(GuhVillagersFeature.KRISTALSLIJPER.getKey());
    }
}
