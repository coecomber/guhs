package nl.juiced.guhs.feature.guhvillagers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import nl.juiced.guhs.registry.ModVillagers;

/**
 * 1.7.0: the renderer of every villager. A vanilla villager is drawn exactly as before (this IS the vanilla renderer);
 * a villager of the type guhs:guh is handed to {@link GuhVillagerRenderer} (the new guh look). The render state is the
 * vanilla one, filled in here, so both draw from the same state.
 */
public class GuhsVillagerRenderer extends VillagerRenderer {
    private final GuhVillagerRenderer guh;

    public GuhsVillagerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.guh = new GuhVillagerRenderer(context);
    }

    public static boolean isGuh(VillagerRenderState state) {
        return state.villagerData != null && state.villagerData.type().is(ModVillagers.GUH.getKey());
    }

    @Override
    public void submit(VillagerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (isGuh(state)) {
            this.guh.submit(state, poseStack, collector, camera);
        } else {
            super.submit(state, poseStack, collector, camera);
        }
    }
}
