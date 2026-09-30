package nl.juiced.guhs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.registry.ModVillagers;

/**
 * Guh villagers are real villagers, dressed as guhs: the villager type texture (textures/entity/villager/type/guh.png)
 * gives them pink fur and big guh eyes, and this layer adds round guh ears on the head and a little tail.
 */
public class GuhVillagerFeaturesLayer extends RenderLayer<Villager, VillagerModel<Villager>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Guhs.id("guh_villager_features"), "main");
    private static final Identifier TEXTURE = Guhs.id("textures/entity/villager/guh_features.png");

    private final ModelPart ears;
    private final ModelPart tail;

    public GuhVillagerFeaturesLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent, ModelPart root) {
        super(parent);
        this.ears = root.getChild("ears");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("ears", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-7.5f, -13.5f, -1f, 4, 4, 1)          // left ear
                        .texOffs(0, 0).mirror().addBox(3.5f, -13.5f, -1f, 4, 4, 1)  // right ear
                        .texOffs(0, 6).addBox(-7f, -14f, -0.9f, 3, 1, 0.8f)        // rounded tops
                        .texOffs(0, 6).addBox(4f, -14f, -0.9f, 3, 1, 0.8f),
                PartPose.ZERO);
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5f, 0, 0, 1, 1, 7),
                PartPose.offsetAndRotation(0, 10.5f, 2.5f, 0.55f, 0, 0));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, Villager villager, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (villager.isInvisible() || villager.getVillagerData().getType() != ModVillagers.GUH.get()) {
            return;
        }
        var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        poseStack.pushPose();
        getParentModel().getHead().translateAndRotate(poseStack);
        ears.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        poseStack.pushPose();
        getParentModel().root().getChild("body").translateAndRotate(poseStack);
        tail.render(poseStack, buffer, light, LivingEntityRenderer.getOverlayCoords(villager, 0f));
        poseStack.popPose();
    }
}
