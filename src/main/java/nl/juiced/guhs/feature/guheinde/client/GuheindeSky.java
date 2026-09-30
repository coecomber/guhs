package nl.juiced.guhs.feature.guheinde.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import org.joml.Matrix4f;

/** The Guheinde sky: like the End's, but a slow purple-pink swirl of knabbel crumbs (textures/environment/guheinde_sky.png). */
public class GuheindeSky extends DimensionSpecialEffects {
    private static final ResourceLocation SKY = Guhs.id("textures/environment/guheinde_sky.png");

    public GuheindeSky() {
        super(Float.NaN, false, SkyType.END, true, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(0.24, 0.12, 0.2);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        PoseStack pose = new PoseStack();
        pose.mulPose(modelViewMatrix);
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SKY);
        Tesselator tesselator = Tesselator.getInstance();
        float drift = (ticks + partialTick) * 0.0004f;
        for (int i = 0; i < 6; i++) {
            pose.pushPose();
            switch (i) {
                case 1 -> pose.mulPose(Axis.XP.rotationDegrees(90f));
                case 2 -> pose.mulPose(Axis.XP.rotationDegrees(-90f));
                case 3 -> pose.mulPose(Axis.XP.rotationDegrees(180f));
                case 4 -> pose.mulPose(Axis.ZP.rotationDegrees(90f));
                case 5 -> pose.mulPose(Axis.ZP.rotationDegrees(-90f));
                default -> {
                }
            }
            Matrix4f m = pose.last().pose();
            int colour = 0xFF6A3A64;
            BufferBuilder b = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            b.addVertex(m, -100f, -100f, -100f).setUv(drift, 0f).setColor(colour);
            b.addVertex(m, -100f, -100f, 100f).setUv(drift, 8f).setColor(colour);
            b.addVertex(m, 100f, -100f, 100f).setUv(8f + drift, 8f).setColor(colour);
            b.addVertex(m, 100f, -100f, -100f).setUv(8f + drift, 0f).setColor(colour);
            BufferUploader.drawWithShader(b.buildOrThrow());
            pose.popPose();
        }
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        return true;
    }
}
